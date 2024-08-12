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
        val results = parallelGetRequests(url, count)
        val end = System.currentTimeMillis()
        return "Time taken: ${end - start} ms"
    }

    fun parallelGetRequests(url: String, count: Int): List<String> = runBlocking {
        val results = (1..count).map {
            async(Dispatchers.IO) {
                performGetRequest(url)
            }
        }.awaitAll()
        results
    }

    private fun performGetRequest(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

}