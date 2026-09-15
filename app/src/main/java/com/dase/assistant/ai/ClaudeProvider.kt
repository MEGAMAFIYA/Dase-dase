package com.dase.assistant.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * ESLATMA: model nomini https://docs.claude.com orqali tekshiring —
 * Anthropic model nomlarini vaqti-vaqti bilan yangilab turadi.
 */
class ClaudeProvider : AiProvider {

    companion object {
        const val DEFAULT_MODEL = "claude-sonnet-4-6"
        const val ANTHROPIC_VERSION = "2023-06-01"
        const val MAX_TOKENS = 1024
    }

    override val type = AiProviderType.CLAUDE

    override fun sendMessage(prompt: String, apiKey: String, callback: AiCallback) {
        val body = JSONObject().apply {
            put("model", DEFAULT_MODEL)
            put("max_tokens", MAX_TOKENS)
            put(
                "messages",
                JSONArray().put(
                    JSONObject().put("role", "user").put("content", prompt)
                )
            )
        }

        val headers = mapOf(
            "x-api-key" to apiKey,
            "anthropic-version" to ANTHROPIC_VERSION
        )

        AiHttpClient.postJsonAsync(
            url = "https://api.anthropic.com/v1/messages",
            headers = headers,
            jsonBody = body.toString(),
            providerType = type,
            onSuccess = { response ->
                try {
                    val json = JSONObject(response)
                    val text = json.getJSONArray("content")
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
