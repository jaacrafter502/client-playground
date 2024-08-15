package io.emja.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.measureTimeMillis

@ShellComponent
class MyShellCommands {


    // TODO: Reex matched nicht den value
    val valueExtractor = Regex(".*\"value\":(.*?)}")

    @ShellMethod("Say hello to someone")
    fun sayHello(name: String): String {
        return "Hello, $name!"
    }

    @ShellMethod("Start Performance Test")
    fun startPerformanceTest(url: String, count: Int): String {
        val result = runBlocking {
            measureTimeMillis {
                val metricResult = async {
                    startMetricScrapping()
                }
                val loadResult = async {
                    startTestLoad(url, count)
                }
                runBlocking {
                    println("${metricResult.await()} \n${loadResult.await()}")
                }
            }
        }
        return "Finished time taken: $result ms"
    }

    suspend fun startMetricScrapping(): String {

        // TODO: Lauf in einer Schleife mit delay 1 sec und sammel die werte in  einer Liste
        return " ${getMetric("process.cpu.time")}"
    }

    fun getMetric(name: String): String? {
        val baseUrl = "http://localhost:8080/actuator/metrics"
        val url = "$baseUrl/$name"
        val connection = URL(url).openConnection() as HttpURLConnection
        val metricJson = connection.inputStream.bufferedReader().use { it.readText() }

        println(valueExtractor.matchEntire(metricJson)?.groups)

        return valueExtractor.matchEntire(metricJson)?.groups?.get(1)?.value
    }

    suspend fun startTestLoad(url: String, count: Int): String {
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