// Please, delete it after adding your implementation
@file:Suppress("UnusedPrivateMember")

package jub.task1.game

import kotlinx.coroutines.*
import java.util.concurrent.Executors

private const val KEY_NOT_FOUND = "key_not_found"

// Stores the number of steps and a list of all links to the destination page
// The path must include the final page
data class SearchPath(
    val steps: Int,
    val path: List<String>,
)

class PageSearch(
    private val finalPage: String = KOTLIN_PAGE,
) {
    fun restoreAnswer(startPage: String, previousLink: MutableMap<String, String>): SearchPath {
        var currentLink = finalPage
        val path = mutableListOf(currentLink)
        while (currentLink != startPage) {
            currentLink = previousLink[currentLink] ?: KEY_NOT_FOUND
            if (currentLink == KEY_NOT_FOUND) {
                return SearchPath(NOT_FOUND, listOf())
            }
            path.add(currentLink)
        }
        path.reverse()
        return SearchPath(path.size - 1, path)
    }

    fun search(startPage: String, searchDepth: Int, threadsCount: Int): SearchPath = runBlocking {
        val customDispatcher = Executors.newFixedThreadPool(threadsCount).asCoroutineDispatcher()
        val coroutineScope = CoroutineScope(customDispatcher)
        val queue: ArrayDeque<String> = ArrayDeque()
        queue.addLast(startPage)
        val visitedLinks = mutableSetOf(startPage)
        val newQueue: ArrayDeque<String> = ArrayDeque()
        val previousLink: MutableMap<String, String> = mutableMapOf()
        repeat(searchDepth) {
            val jobs: MutableList<Job> = mutableListOf()
            while (queue.isNotEmpty()) {
                val link = queue.removeFirst()
                jobs.add(coroutineScope.launch {
                    val newLinks = getLinks(link).filter { !visitedLinks.contains(it) }
                    newLinks.forEach { newLink -> previousLink[newLink] = link }
                    newQueue.addAll(newLinks)
                })
            }
            jobs.joinAll()
            visitedLinks.addAll(newQueue.toSet())
            queue.addAll(newQueue.toSet())
            newQueue.clear()
        }
        if (!visitedLinks.contains(finalPage)) {
            return@runBlocking SearchPath(NOT_FOUND, listOf())
        }
        return@runBlocking restoreAnswer(startPage, previousLink)
    }

    companion object {
        const val KOTLIN_PAGE = "https://en.wikipedia.org/wiki/Kotlin_(programming_language)"
        const val NOT_FOUND = -1
    }
}

