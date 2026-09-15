package com.dase.assistant.telegram

/**
 * TelegramNavigator natijalarini yuqori darajaga (orchestrator/UI) yetkazish
 * uchun callback. Accessibility Service o'z ichida dialog ko'rsata olmaydi,
 * shuning uchun barcha foydalanuvchiga ko'rinadigan qarorlar shu callback
 * orqali tashqariga chiqariladi.
 */
interface NavigationCallback {
    fun onStatus(message: String)

    /** Chat nomiga mos keladigan bitta natija topildi. */
    fun onChatFound(chatName: String)

    /** Bir nechta mos chat topildi — foydalanuvchi birini tanlashi kerak. */
    fun onMultipleChatsFound(candidates: List<String>)

    /** Hech qanday mos chat topilmadi. */
    fun onChatNotFound(query: String)

    /**
     * Kutilmagan holat: kerakli accessibility node topilmadi (masalan,
     * Telegram interfeysi o'zgargan). Ilova QULAMAYDI — shu callback orqali
     * xato xabar qilinadi (6-bosqich xavfsizlik talabi).
     */
    fun onNavigationError(message: String)

    fun onMessageFieldReady()

    fun onSendButtonClicked()
    fun onSendFailed(reason: String)
}
