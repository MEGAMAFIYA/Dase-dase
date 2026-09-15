package com.dase.assistant.ai

import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

/**
 * Barcha AI providerlar uchun umumiy, qo'shimcha tashqi kutubxonasiz
 * (faqat `java.net.*` va `org.json.*`, ikkalasi ham Android SDK tarkibida
 * mavjud) HTTP mijoz. Tashqi kutubxona qo'shmaslik build muvaffaqiyatini
 * oshiradi (kamroq bog'liqlik — kamroq versiyalar to'qnashuvi xavfi).
 *
 * ESLATMA: haqiqiy tarmoq chaqiruvi bu muhitda (tarmoqsiz sandbox) sinovdan
 * o'tkazilmadi — faqat kod darajasida diqqat bilan yozilgan va standart
 * `java.net.HttpURLConnection` xatti-harakatiga tayanadi.
 */
object AiHttpClient {

    private val executor = Executors.newCachedThreadPool()

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000

    fun postJsonAsync(
        url: String,
        headers: Map<String, String>,
        jsonBody: String,
        providerType: AiProviderType,
        onSuccess: (String) -> Unit,
        onError: (AiErrorKind, String) -> Unit
    ) {
        executor.submit {
            var connection: HttpURLConnection? = null
            try {
                val parsedUrl = URL(url)
                connection = (parsedUrl.openConnection() as HttpsURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    headers.forEach { (key, value) -> setRequestProperty(key, value) }
                }

                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(jsonBody)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

                when (responseCode) {
                    in 200..299 -> onSuccess(responseText)
                    401, 403 -> onError(AiErrorKind.INVALID_KEY, "API kalit noto'g'ri yoki ruxsat yo'q (HTTP $responseCode).")
                    429 -> onError(AiErrorKind.QUOTA_EXCEEDED, "So'rovlar chegarasi (quota) tugagan (HTTP 429).")
                    else -> onError(AiErrorKind.UNKNOWN, "Kutilmagan javob (HTTP $responseCode): $responseText")
                }
            } catch (e: UnknownHostException) {
                onError(AiErrorKind.NO_INTERNET, "Internet aloqasi yo'q.")
            } catch (e: SocketTimeoutException) {
                onError(AiErrorKind.TIMEOUT, "Server javob berish vaqti tugadi (timeout).")
            } catch (e: IOException) {
                onError(AiErrorKind.NO_INTERNET, "Tarmoq xatosi: ${e.message}")
            } catch (e: Exception) {
                onError(AiErrorKind.UNKNOWN, "Kutilmagan xato: ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }
    }
}
