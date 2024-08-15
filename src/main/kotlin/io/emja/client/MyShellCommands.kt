package io.emja.client

import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

@ShellComponent
class MyShellCommands {

    @ShellMethod("Say hello to someone")
    fun sayHello(name: String): String {
        return "Hello, $name!"
    }

    @ShellMethod("Start Performance Test")
    fun startPerformanceTest(url: String, count: Int): String {
        val start = System.currentTimeMillis()
        val latency = parallelGetRequests(url, count)
        val end = System.currentTimeMillis()
        return "Time taken: ${end - start} ms\nLatency: $latency"
    }

    fun parallelGetRequests(url: String, count: Int): Double = runBlocking {
        val results = (1..count).map {
            async(Dispatchers.IO) {
                performGetRequest(url)
            }
        }.awaitAll()
        results.average()
    }

    private fun performGetRequest(url: String): Long {
        val start = System.currentTimeMillis()
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.inputStream.bufferedReader().use { it.readText() }
        val end = System.currentTimeMillis()
        return end - start
    }

}