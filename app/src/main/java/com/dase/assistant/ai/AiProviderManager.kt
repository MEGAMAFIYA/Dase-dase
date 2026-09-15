package com.dase.assistant.ai

/**
 * 9-bosqich talabi: "Provider ishlamasa boshqa providerga o'tish imkoniyati
 * bo'lsin." Berilgan ustuvorlik tartibida providerlarni birma-bir sinaydi.
 *
 * Bekor qilish (cancellation): [cancel] chaqirilganda "generation" hisoblagichi
 * oshiriladi; shu paytgacha yuborilgan, hali javob kelmagan so'rovlar javobi
 * kelganda ham foydalanuvchi callback'iga YETKAZILMAYDI. MUHIM CHEKLOV: bu
 * chinakam tarmoq darajasidagi bekor qilish emas (soket haligacha ochiq
 * turadi, faqat natija e'tiborsiz qoldiriladi) — chinakam soket-darajasidagi
 * bekor qilish uchun OkHttp kabi kutubxona kerak bo'lardi, bu loyihada
 * ataylab qo'shilmagan (keraksiz bog'liqlikni oshirmaslik uchun).
 */
class AiProviderManager(private val keyStore: SecureKeyStore) {

    private val providers: Map<AiProviderType, AiProvider> = mapOf(
        AiProviderType.GEMINI to GeminiProvider(),
        AiProviderType.OPENAI to OpenAiProvider(),
        AiProviderType.OPENROUTER to OpenRouterProvider(),
        AiProviderType.DEEPSEEK to DeepSeekProvider(),
        AiProviderType.CLAUDE to ClaudeProvider()
    )

    @Volatile
    private var generation = 0

    fun cancel() {
        generation++
    }

    /**
     * @param priority sinash tartibi. Kalit kiritilmagan providerlar
     *   avtomatik o'tkazib yuboriladi.
     */
    fun sendWithFallback(prompt: String, priority: List<AiProviderType>, callback: AiCallback) {
        val myGeneration = ++generation
        val errorLog = mutableListOf<String>()
        attemptNext(prompt, priority, 0, myGeneration, errorLog, callback)
    }

    private fun attemptNext(
        prompt: String,
        order: List<AiProviderType>,
        index: Int,
        myGeneration: Int,
        errorLog: MutableList<String>,
        callback: AiCallback
    ) {
        if (myGeneration != generation) return // bekor qilingan yoki eskirgan so'rov

        if (index >= order.size) {
            val summary = if (errorLog.isEmpty()) {
                "Hech qanday AI provider uchun API kalit kiritilmagan."
            } else {
                "Barcha providerlar muvaffaqiyatsiz tugadi:\n" + errorLog.joinToString("\n")
            }
            callback.onError(order.lastOrNull() ?: AiProviderType.GEMINI, AiErrorKind.UNKNOWN, summary)
            return
        }

        val type = order[index]
        val provider = providers[type]
        val apiKey = keyStore.getKey(type)

        if (provider == null || apiKey.isNullOrBlank()) {
            attemptNext(prompt, order, index + 1, myGeneration, errorLog, callback)
            return
        }

        provider.sendMessage(prompt, apiKey, object : AiCallback {
            override fun onResult(providerType: AiProviderType, text: String) {
                if (myGeneration != generation) return
                callback.onResult(providerType, text)
            }

            override fun onError(providerType: AiProviderType, kind: AiErrorKind, message: String) {
                if (myGeneration != generation) return
                errorLog.add("${providerType.displayName}: [$kind] $message")
                attemptNext(prompt, order, index + 1, myGeneration, errorLog, callback)
            }
        })
    }
}
