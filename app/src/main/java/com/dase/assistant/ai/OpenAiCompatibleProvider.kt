package com.dase.assistant.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * OpenAI, OpenRouter va DeepSeek uchtasi ham bir xil "chat/completions"
 * so'rov/javob formatiga amal qiladi — shuning uchun umumiy mantiq bitta
 * joyda (takroriy kodning oldi olingan, avvalgi audit tavsiyasiga muvofiq).
 */
abstract class OpenAiCompatibleProvider(
    override val type: AiProviderType,
    private val endpoint: String,
    private val model: String
) : AiProvider {

    override fun sendMessage(prompt: String, apiKey: String, callback: AiCallback) {
        val body = JSONObject().apply {
            put("model", model)
            put(
                "messages",
                JSONArray().put(
                    JSONObject().put("role", "user").put("content", prompt)
                )
            )
        }

        val headers = mapOf("Authorization" to "Bearer $apiKey")

        AiHttpClient.postJsonAsync(
            url = endpoint,
            headers = headers,
            jsonBody = body.toString(),
            providerType = type,
            onSuccess = { response ->
                try {
                    val json = JSONObject(response)
                    val text = json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    callback.onResult(type, text)
                } catch (e: Exception) {
                    callback.onError(type, AiErrorKind.UNKNOWN, "Javobni tahlil qilib bo'lmadi: ${e.message}")
                }
            },
            onError = { kind, message -> callback.onError(type, kind, message) }
        )
    }
}

/** ESLATMA: model nomini https://platform.openai.com/docs orqali tekshiring. */
class OpenAiProvider : OpenAiCompatibleProvider(
    type = AiProviderType.OPENAI,
    endpoint = "https://api.openai.com/v1/chat/completions",
    model = "gpt-4o-mini"
)

/** ESLATMA: model nomini https://openrouter.ai/docs orqali tekshiring. */
class OpenRouterProvider : OpenAiCompatibleProvider(
    type = AiProviderType.OPENROUTER,
    endpoint = "https://openrouter.ai/api/v1/chat/completions",
    model = "openai/gpt-4o-mini"
)

/** ESLATMA: model nomini https://api-docs.deepseek.com orqali tekshiring. */
class DeepSeekProvider : OpenAiCompatibleProvider(
    type = AiProviderType.DEEPSEEK,
    endpoint = "https://api.deepseek.com/chat/completions",
    model = "deepseek-chat"
)
