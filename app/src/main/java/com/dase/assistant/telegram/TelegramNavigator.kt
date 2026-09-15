package com.dase.assistant.telegram

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.dase.assistant.command.TextNormalizer

/**
 * 6-bosqich: Telegram navigatsiyasi.
 *
 * MUHIM CHEKLOV (yashirilmasdan aytilishi kerak): bu klass Accessibility
 * Node daraxti bilan ishlaydi, lekin Telegram ilovasining haqiqiy XML/UI
 * tuzilishi (resurs ID'lari, komponent turlari) bu muhitda (Android SDK,
 * emulyator yoki haqiqiy qurilma yo'q) tekshirib ko'rilmadi va tekshirib
 * bo'lmaydi. Shuning uchun bu yerda resurs ID'lariga emas, ko'proq matn
 * (text) va tavsif (contentDescription) asosidagi qidiruvga tayangan —
 * bu Telegram versiyasi o'zgarganda ham nisbatan barqarorroq, lekin baribir
 * KAFOLATSIZ. Har bir bosqich muvaffaqiyatsiz bo'lganda `null`/`false`
 * qaytaradi va [NavigationCallback.onNavigationError] orqali xabar beradi —
 * ilova hech qachon qulamaydi (talab: "Accessibility node topilmasa,
 * ilovani qulatmaslik").
 *
 * Haqiqiy qurilmada tekshirilmaguncha bu kod "PROTOTIP" holatida deb
 * hisoblanishi kerak, "TAYYOR VA TEKSHIRILGAN" emas.
 */
class TelegramNavigator(
    private val service: AccessibilityService,
    private val callback: NavigationCallback
) {

    companion object {
        const val TELEGRAM_PACKAGE = "org.telegram.messenger"

        // Turli Telegram build'lari (rasmiy, X versiyasi va h.k.) va tillar
        // (o'zbekcha/ruscha/inglizcha) uchun taxminiy variantlar.
        private val SEARCH_DESCRIPTIONS = listOf("search", "qidiruv", "qidirish", "поиск")
        private val SEND_DESCRIPTIONS = listOf("send", "yuborish", "jo'natish", "отправить")
    }

    private fun rootNode(): AccessibilityNodeInfo? {
        val root = service.rootInActiveWindow
        if (root == null) {
            callback.onNavigationError("Ekran tarkibini o'qib bo'lmadi (rootInActiveWindow = null).")
        }
        return root
    }

    /** Qidiruv tugmasini topib bosadi. */
    fun openSearch(): Boolean {
        val root = rootNode() ?: return false
        try {
            val node = findNodeByDescriptionOrText(root, SEARCH_DESCRIPTIONS)
            if (node == null) {
                callback.onNavigationError("Qidiruv tugmasi topilmadi — Telegram interfeysi o'zgargan bo'lishi mumkin.")
                return false
            }
            val clicked = clickNodeOrAncestor(node)
            if (!clicked) {
                callback.onNavigationError("Qidiruv tugmasi topildi, lekin bosib bo'lmadi.")
            }
            return clicked
        } finally {
            root.recycle()
        }
    }

    /** Qidiruv maydoniga matn kiritadi (qidiruv oldindan ochilgan bo'lishi kerak). */
    fun typeSearchQuery(query: String): Boolean {
        val root = rootNode() ?: return false
        try {
            val editField = findFirstEditableNode(root)
            if (editField == null) {
                callback.onNavigationError("Qidiruv matn maydoni topilmadi.")
                return false
            }
            val success = setNodeText(editField, query)
            if (!success) {
                callback.onNavigationError("Qidiruv maydoniga matn kiritib bo'lmadi.")
            }
            return success
        } finally {
            root.recycle()
        }
    }

    /**
     * Qidiruv natijalari orasidan `chatName`ga mos keladigan chat(lar)ni
     * qaytaradi. Bir nechta mos natija bo'lsa — bittasini o'zi tanlamaydi,
     * ro'yxatni [NavigationCallback.onMultipleChatsFound] orqali
     * foydalanuvchiga ko'rsatadi (xavfsizlik talabi: "noto'g'ri chatni
     * tanlamaslik").
     */
    fun findChatInResults(chatName: String): AccessibilityNodeInfo? {
        val root = rootNode() ?: return null
        try {
            val candidates = collectTextNodes(root)
            val normalizedQuery = TextNormalizer.normalize(chatName)

            val matches = candidates.filter { node ->
                val text = node.text?.toString() ?: node.contentDescription?.toString() ?: return@filter false
                TextNormalizer.normalize(text).contains(normalizedQuery)
            }

            return when {
                matches.isEmpty() -> {
                    callback.onChatNotFound(chatName)
                    null
                }
                matches.size == 1 -> {
                    val text = matches[0].text?.toString() ?: matches[0].contentDescription.toString()
                    callback.onChatFound(text)
                    matches[0]
                }
                else -> {
                    val names = matches.mapNotNull { it.text?.toString() ?: it.contentDescription?.toString() }
                    callback.onMultipleChatsFound(names)
                    null // Foydalanuvchi tanlamaguncha hech biri avtomatik ochilmaydi.
                }
            }
        } finally {
            root.recycle()
        }
    }

    fun openChat(chatNode: AccessibilityNodeInfo): Boolean {
        val clicked = clickNodeOrAncestor(chatNode)
        if (!clicked) {
            callback.onNavigationError("Chat topildi, lekin uni ochib bo'lmadi.")
        }
        return clicked
    }

    /** Xabar yozish maydonini topib, matnni kiritadi (hali YUBORMAYDI). */
    fun prepareMessage(text: String): Boolean {
        val root = rootNode() ?: return false
        try {
            val editField = findFirstEditableNode(root)
            if (editField == null) {
                callback.onNavigationError("Xabar yozish maydoni topilmadi.")
                return false
            }
            val success = setNodeText(editField, text)
            if (success) {
                callback.onMessageFieldReady()
            } else {
                callback.onNavigationError("Xabar matnini kiritib bo'lmadi.")
            }
            return success
        } finally {
            root.recycle()
        }
    }

    /**
     * Yuborish tugmasini bosadi. Bu metod FAQAT ConfirmationManager orqali
     * foydalanuvchi aniq tasdiqlagandan keyin chaqirilishi kerak — bu
     * klassning o'zi hech qachon o'z-o'zidan yubormaydi.
     */
    fun clickSendButton(): Boolean {
        val root = rootNode() ?: return false
        try {
            val node = findNodeByDescriptionOrText(root, SEND_DESCRIPTIONS)
            if (node == null) {
                callback.onSendFailed("Yuborish tugmasi topilmadi.")
                return false
            }
            val clicked = clickNodeOrAncestor(node)
            if (clicked) {
                callback.onSendButtonClicked()
            } else {
                callback.onSendFailed("Yuborish tugmasi topildi, lekin bosib bo'lmadi.")
            }
            return clicked
        } finally {
            root.recycle()
        }
    }

    // ---- Ichki yordamchi funksiyalar ----

    private fun findNodeByDescriptionOrText(root: AccessibilityNodeInfo, variants: List<String>): AccessibilityNodeInfo? {
        for (variant in variants) {
            val byDesc = root.findAccessibilityNodeInfosByText(variant)
            if (byDesc.isNotEmpty()) return byDesc[0]
        }
        return null
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.className?.toString() == "android.widget.EditText" && node.isEditable) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findFirstEditableNode(child)
            if (result != null) return result
        }
        return null
    }

    private fun collectTextNodes(node: AccessibilityNodeInfo, acc: MutableList<AccessibilityNodeInfo> = mutableListOf()): List<AccessibilityNodeInfo> {
        if (!node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank()) {
            if (node.isClickable) acc.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTextNodes(child, acc)
        }
        return acc
    }

    private fun setNodeText(node: AccessibilityNodeInfo, text: String): Boolean {
        val args = Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun clickNodeOrAncestor(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }
}
