package com.dase.assistant.ai

/**
 * Barcha AI provider integratsiyalari shu interfeysga amal qiladi —
 * shunday qilib [AiProviderManager] ularni bir xil tarzda, o'zaro
 * almashtirib (fallback) chaqira oladi.
 */
interface AiProvider {
    val type: AiProviderType

    /**
     * @param apiKey chaqiruvchi tomonidan [SecureKeyStore]dan olinadi —
     *   bu klassning o'zi kalitni hech qayerda saqlamaydi yoki logga
     *   yozmaydi.
     */
    fun sendMessage(prompt: String, apiKey: String, callback: AiCallback)
}
