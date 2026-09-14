package com.dase.assistant

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class DaseAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Hozircha faqat xizmatning ishlayotganini kuzatish uchun.
        // Keyingi bosqichlarda foydalanuvchi tasdig‘i bilan
        // ekrandagi elementlarni topish va amallar bajarish qo‘shiladi.
    }

    override fun onInterrupt() {
        Toast.makeText(
            this,
            "DASE boshqaruv xizmati to‘xtatildi",
            Toast.LENGTH_SHORT
        ).show()
    }
}
