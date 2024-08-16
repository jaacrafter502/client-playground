package io.emja.client

import kotlinx.coroutines.*
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors
import kotlin.system.measureTimeMillis
import kotlin.math.min
import kotlin.math.roundToLong

@ShellComponent
class MyShellCommands {


    // TODO: Reex matched nicht den value
    //val valueExtractor = Regex(".*\"value\":(.*?)}")
    val valueExtractor = Regex(""".*"value":([0-9.]+[E]{0,1}[0-9]*).*""")
    val loadTesting = AtomicBoolean(true)
    //val metricName = "process.cpu.time" //==> 10000000000
    //val metricName = "system.cpu.usage" //==> 0.01
    val metricName = "jvm.memory.used" //==>
    val metricScale = 10000000


    @ShellMethod("Say hello to someone")
    fun sayHello(name: String): String {
        return "Hello, $name!"
    }

    @ShellMethod("Start Performance Test")
    fun startPerformanceTest(url: String, count: Int): String {
        loadTesting.set(true)
        val result = runBlocking {
            measureTimeMillis {
                val metricResult = async {
                    //delay(5000L)
                    val result = startMetricScrapping()
                    println("metrci result")
                    result
                }
                val loadResult = async {
                    //delay(1000L)
                    val result = startTestLoad(url, count)
                    println("load result")  //  <---------- WIRD NIEMALS AUSGEFÜHRT. EVTL PROBLEM MIT WARTEN AUF ANDERE ASYNC
                    result
                }
                runBlocking {
                    println("${metricResult.await()} \n${loadResult.await()}")
                }
            }
        }
        return "Finished time taken: $result ms"
    }

    suspend fun startMetricScrapping(): String {
        println("start metric scrapping")
        val metricValues = mutableListOf<String?>()
        while (loadTesting.get()) {
            //println("scrapping metric")
            metricValues.add(getMetric(metricName))
            delay(500L)
        }
        /*delay(500L)
        metricValues.add(getMetric("process.cpu.time"))
        delay(500L)
        metricValues.add(getMetric("process.cpu.time"))
        delay(500L)
        metricValues.add(getMetric("process.cpu.time"))
        delay(500L)
        metricValues.add(getMetric("process.cpu.time"))*/


        // TODO: Lauf in einer Schleife mit delay 1 sec und sammel die werte in  einer Lis
        drawSvgDiagram(metricValues)
        return metricValues.stream().collect(Collectors.joining(",","[","]"))
    }

    /*fun sampleMatric(name: String): List<Double> {
        while (loadTesting.get()) {

        }
    }*/

    fun getMetric(name: String): String? {
        val baseUrl = "http://localhost:8080/actuator/metrics"
        val url = "$baseUrl/$name"
        val connection = URL(url).openConnection() as HttpURLConnection
        val metricJson = connection.inputStream.bufferedReader().use { it.readText() }

        //println(valueExtractor.matchEntire(metricJson)?.groups)

        return valueExtractor.matchEntire(metricJson)?.groups?.get(1)?.value
    }

    suspend fun startTestLoad(url: String, count: Int): String {
        println("start test load")
        val start = System.currentTimeMillis()
        val latency = parallelGetRequests(url, count)
        val end = System.currentTimeMillis()
        loadTesting.set(false)
        println(loadTesting.get())
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

        //val connection = URL(url).openConnection() as HttpURLConnection
        //connection.inputStream.bufferedReader().use { it.readText() }

        val connection = URL(url).openConnection() as HttpURLConnection
        val operation = "5*8*10^2/"

        try {
            connection.requestMethod = "POST"
            connection.doOutput = true

            // URL-encode the operation parameter
            val encodedOperation = URLEncoder.encode(operation, StandardCharsets.UTF_8.toString())
            val requestBody = "operation=$encodedOperation"

            // Write the request body to the connection's output stream
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            // Read and return the response from the server
            val result = connection.inputStream.bufferedReader().use { it.readText() }
            //println(result)
        } finally {
            connection.disconnect()
        }





        val end = System.currentTimeMillis()
        return end - start
    }


    fun drawSvgDiagram(metricValues: List<String?>) {
        val svgContent = StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"${metricValues.size * 22}\" height=\"500\">")
        val maxSvgHeight = 750.0

        for ((index, value) in metricValues.withIndex()) {
            value?.toDoubleOrNull()?.let {
                println("val to bar")
                val height = it / metricScale  // Scale the height relative to the metric value
                val y = (maxSvgHeight - height - 600).roundToLong()  // Ensure y is never negative
                println("height: $height, y: $y")
                //svgContent.append("<rect x=\"${index * 22}\" y=\"$y\" width=\"20\" height=\"$height\" fill=\"red\" />")
                svgContent.append("<rect x=\"${index * 22}\" y=\"$y\" width=\"20\" height=\"${String.format("%.0f", height)}\" fill=\"#3cab9c\" />")
            }
        }

        svgContent.append("</svg>")

        try {
            Files.write(Paths.get("images/data.svg"), svgContent.toString().toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}