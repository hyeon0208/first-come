package io.github.hyeon0208.firstcome.support.test

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun <T> runConcurrently(tasks: List<() -> T>): List<Result<T>> {
    val threadPool = Executors.newFixedThreadPool(tasks.size)
    val startLine = CyclicBarrier(tasks.size)

    try {
        val pendingResults = tasks.map { task ->
            threadPool.submit<Result<T>> {
                startLine.await()
                runCatching(task)
            }
        }
        return pendingResults.map { it.get(30, TimeUnit.SECONDS) }
    } finally {
        threadPool.shutdownNow()
    }
}
