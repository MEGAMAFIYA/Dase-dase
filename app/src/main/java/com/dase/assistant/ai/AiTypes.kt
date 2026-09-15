package com.dase.assistant.ai

enum class AiProviderType(val displayName: String) {
    GEMINI("Gemini"),
    OPENAI("OpenAI"),
    OPENROUTER("OpenRouter"),
    DEEPSEEK("DeepSeek"),
    CLAUDE("Claude")
}

/** Talab: "Token, quota va timeout xatolari alohida ko'rsatilsin." */
enum class AiErrorKind {
    NO_INTERNET,
    INVALID_KEY,
    QUOTA_EXCEEDED,
    TIMEOUT,
    UNKNOWN
}

interface AiCallback {
    /** Chaqiriladi: fon oqimida (background thread) — UI shu yerda to'g'ridan-to'g'ri yangilanmasin. */
    fun onResult(providerType: AiProviderType, text: String)
    fun onError(providerType: AiProviderType, kind: AiErrorKind, message: String)
}
