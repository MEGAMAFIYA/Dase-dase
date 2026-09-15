package com.dase.assistant.orchestrator

import android.accessibilityservice.AccessibilityService
import com.dase.assistant.DaseAccessibilityService
import com.dase.assistant.ai.AiCallback
import com.dase.assistant.ai.AiErrorKind
import com.dase.assistant.ai.AiProviderManager
import com.dase.assistant.ai.AiProviderType
import com.dase.assistant.command.CommandParser
import com.dase.assistant.command.DaseCommand
import com.dase.assistant.confirmation.ConfirmationManager
import com.dase.assistant.confirmation.PendingMessage
import com.dase.assistant.telegram.NavigationCallback
import com.dase.assistant.telegram.TelegramNavigator
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Ovozdan kelgan matnni buyruqqa aylantirib (5-bosqich), Telegram bilan
 * ishlab (6-7-bosqich), foydalanuvchi tasdig'ini kutib (8-bosqich) va
 * kerak bo'lganda AI'ga murojaat qilib (9-bosqich) bog'laydigan markaziy
 * koordinator.
 *
 * MUHIM: bu klass [DaseAccessibilityService.instance] orqali ishlaydi.
 * Agar foydalanuvchi Accessibility Service'ni yoqmagan bo'lsa, `instance`
 * `null` bo'ladi — bu holat har doim tekshiriladi va aniq xabar bilan
 * qaytariladi, ilova qulamaydi.
 */
class DaseOrchestrator(
    private val confirmationManager: ConfirmationManager,
    private val aiProviderManager: AiProviderManager,
    private val aiPriority: List<AiProviderType>,
    private val callback: OrchestratorCallback
) {
    interface OrchestratorCallback {
        fun onStatus(message: String)
        fun onNeedsClarification(rawText: String)
        fun onNeedsChatChoice(candidates: List<String>)
        fun onAwaitingSendConfirmation(chatName: String?, messageText: String)
        fun onMessageSent()
        fun onAiResponse(text: String)
    }

    /** Oxirgi marta topilgan/ochilgan chat nomi — nom ko'rsatilmagan "deb yoz" buyrug'i uchun kontekst. */
    private var lastKnownChatName: String? = null
    private var pendingChatNode: AccessibilityNodeInfo? = null

    fun handleRecognizedText(rawText: String) {
        val parsed = CommandParser.parse(rawText)
        when (val cmd = parsed.command) {
            is DaseCommand.OpenApp -> openTelegram()
            is DaseCommand.FindChat -> findChat(cmd.chatName)
            is DaseCommand.ComposeMessage -> composeMessage(cmd.chatName ?: lastKnownChatName, cmd.messageText)
            DaseCommand.Send -> handleSend()
            DaseCommand.Cancel -> handleCancel()
            DaseCommand.Stop -> handleStop()
            DaseCommand.GoBack -> performGlobalNavAction(GlobalAction.BACK)
            DaseCommand.GoHome -> performGlobalNavAction(GlobalAction.HOME)
            is DaseCommand.Unknown -> callback.onNeedsClarification(cmd.rawText)
        }
    }

    private fun requireNavigator(): TelegramNavigator? {
        val service = DaseAccessibilityService.instance
        if (service == null) {
            callback.onStatus("Accessibility Service yoqilmagan — Sozlamalar > Maxsus imkoniyatlardan DASE'ni yoqing.")
            return null
        }
        return TelegramNavigator(service, buildNavigationCallback())
    }

    private fun openTelegram() {
        val service = DaseAccessibilityService.instance
        if (service == null) {
            callback.onStatus("Accessibility Service yoqilmagan — avval uni yoqing.")
            return
        }
        val launchIntent = service.packageManager.getLaunchIntentForPackage(TelegramNavigator.TELEGRAM_PACKAGE)
        if (launchIntent == null) {
            callback.onStatus("Telegram topilmadi (o'rnatilmagan yoki <queries> deklaratsiyasi yo'q).")
            return
        }
        launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        service.startActivity(launchIntent)
        callback.onStatus("Telegram ochilmoqda...")
    }

    private fun findChat(chatName: String) {
        val navigator = requireNavigator() ?: return
        callback.onStatus("\"$chatName\" chati qidirilmoqda...")
        navigator.openSearch()
        navigator.typeSearchQuery(chatName)
        val node = navigator.findChatInResults(chatName)
        if (node != null) {
            pendingChatNode = node
            lastKnownChatName = chatName
        }
    }

    private fun composeMessage(chatName: String?, messageText: String) {
        if (chatName == null) {
            callback.onStatus("Qaysi chatga yozishni bilmayman — avval chat nomini ayting.")
            return
        }
        // Agar chat hali ochilmagan bo'lsa, avval topamiz va ochamiz.
        val navigator = requireNavigator() ?: return
        if (pendingChatNode == null || lastKnownChatName != chatName) {
            findChat(chatName)
        }
        val node = pendingChatNode
        if (node != null) {
            navigator.openChat(node)
        }
        val prepared = navigator.prepareMessage(messageText)
        if (prepared) {
            confirmationManager.setPendingMessage(PendingMessage(chatName, messageText))
            callback.onAwaitingSendConfirmation(chatName, messageText)
        }
    }

    private fun handleSend() {
        val result = confirmationManager.confirmSend()
        if (result == null) {
            callback.onStatus("Hozircha tasdiqlanadigan xabar yo'q.")
            return
        }
        val (pending, token) = result
        val navigator = requireNavigator()
        if (navigator == null) {
            confirmationManager.onSendFailed(token)
            return
        }
        val clicked = navigator.clickSendButton()
        if (clicked) {
            confirmationManager.onSendSucceeded(token)
            callback.onMessageSent()
            pendingChatNode = null
        } else {
            confirmationManager.onSendFailed(token)
            // TelegramNavigator.clickSendButton allaqachon onSendFailed orqali xabar berdi.
        }
    }

    private fun handleCancel() {
        confirmationManager.cancel()
        pendingChatNode = null
        callback.onStatus("Bekor qilindi.")
    }

    private fun handleStop() {
        if (confirmationManager.isAwaitingConfirmation()) {
            // 8-bosqich: tasdiq kutilayotganda "to'xta" = bekor qilish.
            handleCancel()
        } else {
            callback.onStatus("To'xtatildi.")
        }
    }

    private enum class GlobalAction { BACK, HOME }

    private fun performGlobalNavAction(action: GlobalAction) {
        val service = DaseAccessibilityService.instance
        if (service == null) {
            callback.onStatus("Accessibility Service yoqilmagan.")
            return
        }
        val globalAction = when (action) {
            GlobalAction.BACK -> AccessibilityService.GLOBAL_ACTION_BACK
            GlobalAction.HOME -> AccessibilityService.GLOBAL_ACTION_HOME
        }
        service.performGlobalAction(globalAction)
    }

    /** AI'dan javob so'rash (9-bosqich) — bevosita buyruq emas, MainActivity orqali chaqiriladi. */
    fun askAi(prompt: String) {
        aiProviderManager.sendWithFallback(prompt, aiPriority, object : AiCallback {
            override fun onResult(providerType: AiProviderType, text: String) {
                callback.onAiResponse(text)
            }

            override fun onError(providerType: AiProviderType, kind: AiErrorKind, message: String) {
                val explanation = when (kind) {
                    AiErrorKind.NO_INTERNET -> "Internet aloqasi yo'q."
                    AiErrorKind.INVALID_KEY -> "API kalit noto'g'ri."
                    AiErrorKind.QUOTA_EXCEEDED -> "So'rovlar chegarasi tugagan."
                    AiErrorKind.TIMEOUT -> "Server javob bermadi (timeout)."
                    AiErrorKind.UNKNOWN -> message
                }
                callback.onStatus("AI xatosi: $explanation")
            }
        })
    }

    private fun buildNavigationCallback() = object : NavigationCallback {
        override fun onStatus(message: String) = callback.onStatus(message)
        override fun onChatFound(chatName: String) {
            lastKnownChatName = chatName
            callback.onStatus("Chat topildi: $chatName")
        }
        override fun onMultipleChatsFound(candidates: List<String>) = callback.onNeedsChatChoice(candidates)
        override fun onChatNotFound(query: String) = callback.onStatus("\"$query\" nomli chat topilmadi.")
        override fun onNavigationError(message: String) = callback.onStatus("Xato: $message")
        override fun onMessageFieldReady() { /* composeMessage() ichida ConfirmationManager orqali qayta ishlanadi */ }
        override fun onSendButtonClicked() { /* handleSend() ichida qayta ishlanadi */ }
        override fun onSendFailed(reason: String) = callback.onStatus("Yuborishda xato: $reason")
    }
}
