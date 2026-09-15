package com.dase.assistant.confirmation

/**
 * Foydalanuvchi tasdig'ini kutayotgan, hali yuborilmagan xabar.
 * [chatName] null bo'lishi mumkin — bu holda joriy ochiq chatga yoziladi.
 */
data class PendingMessage(
    val chatName: String?,
    val messageText: String
)
