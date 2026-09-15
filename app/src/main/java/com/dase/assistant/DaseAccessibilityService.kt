package com.dase.assistant

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Accessibility Service — [com.dase.assistant.telegram.TelegramNavigator]
 * shu klass orqali ekran tarkibiga ("rootInActiveWindow") va
 * global amallarga (orqaga/bosh menyu) kira oladi.
 *
 * Servis foydalanuvchi tomonidan tizim sozlamalaridan yoqilishi/o'chirilishi
 * mumkin bo'lgani uchun, uni ishlatuvchi kod (orchestrator) doim
 * [instance] `null` bo'lishi ehtimolini hisobga olishi kerak.
 */
class DaseAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: DaseAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Hozircha navigatsiya foydalanuvchi buyrug'i asosida to'g'ridan-to'g'ri
        // (DaseOrchestrator -> TelegramNavigator) chaqiriladi, hodisa-asosli
        // avtomatik reaksiya hali yo'q. Kelajakda, masalan, Telegram fonga
        // o'tib ketganini kuzatish kabi holatlar shu yerga qo'shilishi mumkin.
    }

    override fun onInterrupt() {
        Toast.makeText(
            this,
            "DASE boshqaruv xizmati to'xtatildi",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
    }
}
