package com.dase.assistant.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Foydalanuvchi ilova ichida kiritgan API kalitlarini shifrlangan holda
 * qurilmada saqlaydi. Talab: "API kalitlari kodga yozilmasin" — shuning
 * uchun bu yerda hech qanday standart/zaxira kalit YO'Q, faqat foydalanuvchi
 * o'zi kiritgan qiymat saqlanadi.
 */
class SecureKeyStore(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "dase_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveKey(provider: AiProviderType, apiKey: String) {
        prefs.edit().putString(keyFor(provider), apiKey).apply()
    }

    fun getKey(provider: AiProviderType): String? = prefs.getString(keyFor(provider), null)

    fun hasKey(provider: AiProviderType): Boolean = !getKey(provider).isNullOrBlank()

    fun clearKey(provider: AiProviderType) {
        prefs.edit().remove(keyFor(provider)).apply()
    }

    private fun keyFor(provider: AiProviderType) = "api_key_${provider.name}"
}
