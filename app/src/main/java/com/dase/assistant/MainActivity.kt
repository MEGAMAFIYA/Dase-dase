package com.dase.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createInterface()
    }

    private fun createInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(18, 18, 18))
        }

        val title = TextView(this).apply {
            text = "DASE"
            textSize = 40f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "4-bosqich • Ilova boshqaruvi ruxsati"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        statusText = TextView(this).apply {
            text = "Holat: Accessibility Service sozlanmagan"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }

        val accessibilityButton = Button(this).apply {
            text = "⚙ Accessibility ruxsatlarini ochish"
            setOnClickListener {
                openAccessibilitySettings()
            }
        }

        val microphoneButton = Button(this).apply {
            text = "🎙 Mikrofon ruxsatini tekshirish"
            setOnClickListener {
                requestMicrophonePermission()
            }
        }

        val telegramButton = Button(this).apply {
            text = "Telegramni ochish"
            setOnClickListener {
                val intent = packageManager.getLaunchIntentForPackage(
                    "org.telegram.messenger"
                )

                if (intent != null) {
                    startActivity(intent)
                    statusText.text = "Holat: Telegram ochildi"
                } else {
                    statusText.text = "Holat: Telegram o‘rnatilmagan"
                }
            }
        }

        val warning = TextView(this).apply {
            text = "Ruxsatni faqat o‘zingiz xohlagan holda yoqing. DASE hozircha boshqa ilovalardagi tugmalarni avtomatik bosmaydi va xabar yubormaydi."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }

        root.addView(title, matchParams())
        root.addView(subtitle, matchParams())
        root.addView(statusText, matchParams())
        root.addView(accessibilityButton, matchParams())
        root.addView(microphoneButton, matchParams())
        root.addView(telegramButton, matchParams())
        root.addView(warning, matchParams())

        setContentView(root)
    }

    private fun matchParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun requestMicrophonePermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            statusText.text = "Holat: Mikrofon ruxsati berilgan"
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MICROPHONE_REQUEST_CODE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        statusText.text = if (isAccessibilityServiceEnabled()) {
            "Holat: Accessibility Service yoqilgan"
        } else {
            "Holat: Accessibility Service o‘chiq"
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = "$packageName/${DaseAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabled.split(':').any {
            it.equals(expected, ignoreCase = true)
        }
    }

    companion object {
        private const val MICROPHONE_REQUEST_CODE = 1001
    }
}
