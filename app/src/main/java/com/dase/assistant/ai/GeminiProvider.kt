package com.dase.assistant.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * ESLATMA: endpoint va model nomi ({@link DEFAULT_MODEL}) yozilgan paytdagi
 * (2026-yil boshigacha bo'lgan) ma'lumotlarga asoslangan. Google Gemini
 * API'ni vaqti-vaqti bilan yangilab turadi — ishlatishdan oldin
 * https://ai.google.dev/gemini-api/docs orqali joriy model nomini
 * tekshiring.
 */
class GeminiProvider : AiProvider {

    companion object {
        const val DEFAULT_MODEL = "gemini-1.5-flash"
    }

    override val type = AiProviderType.GEMINI

    override fun sendMessage(prompt: String, apiKey: String, callback: AiCallback) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$DEFAULT_MODEL:generateContent?key=$apiKey"

        val body = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            ))
        }

        AiHttpClient.postJsonAsync(
            url = url,
            headers = emptyMap(), // kalit URL ichida — sarlavhada emas
            jsonBody = body.toString(),
            providerType = type,
            onSuccess = { response ->
                try {
                    val json = JSONObject(response)
                    val text = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    callback.onResult(type, text)
                } catch (e: Exception) {
                    callback.onError(type, AiErrorKind.UNKNOWN, "Javobni tahlil qilib bo'lmadi: ${e.message}")
                }
            },
            onError = { kind, message -> callback.onError(type, kind, message) }
        )
    }
}
