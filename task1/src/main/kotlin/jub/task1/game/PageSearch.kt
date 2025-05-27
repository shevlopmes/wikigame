

package jub.task1.game

import kotlinx.coroutines.*
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

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

    suspend fun getLinksFromLevel(queue: MutableList<String>, coroutineScope: CoroutineScope): Set<String> {
        val newQueue = Collections.synchronizedList(mutableListOf<String>())
        val jobs = queue.map { link ->
            coroutineScope.launch {
                val newLinks = getLinks(link).filter { !visitedLinks.contains(it) }
                newLinks.forEach { newLink -> previousLink[newLink] = link }
                newQueue.addAll(newLinks)
            }
        }
        jobs.joinAll()
        return newQueue.toSet()
    }

    fun startSearching(threadsCount: Int): CoroutineScope {
        visitedLinks.clear()
        previousLink.clear()
        val customDispatcher = Executors.newFixedThreadPool(threadsCount).asCoroutineDispatcher()
        return CoroutineScope(customDispatcher)
    }

    fun search(startPage: String, searchDepth: Int, threadsCount: Int): SearchPath = runBlocking {
        val coroutineScope = startSearching(threadsCount)
        var queue = mutableListOf<String>()
        queue.add(startPage)
        visitedLinks.add(startPage)
        var level = 0
        run breaking@ {
            repeat(searchDepth) {
                println("Level $level: ${queue.size} pages to process")
                level += 1
                val newLinks = getLinksFromLevel(queue, coroutineScope)
                visitedLinks.addAll(newLinks)
                if (newLinks.contains(finalPage)) {
                    return@breaking
                }
                queue = newLinks.toMutableList()
            }
        }
        if (!visitedLinks.contains(finalPage)) {
            return@runBlocking SearchPath(NOT_FOUND, listOf())
        }
        return@runBlocking restoreAnswer(startPage, previousLink)
    }

    companion object {
        const val KEY_NOT_FOUND = "key_not_found"
        const val KOTLIN_PAGE = "https://en.wikipedia.org/wiki/Kotlin_(programming_language)"
        const val NOT_FOUND = -1
        val visitedLinks: MutableSet<String> = ConcurrentHashMap.newKeySet()
        val previousLink: MutableMap<String, String> = ConcurrentHashMap()
    }
}

