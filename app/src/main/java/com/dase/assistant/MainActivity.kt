package com.dase.assistant

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.dase.assistant.ai.AiProviderManager
import com.dase.assistant.ai.AiProviderType
import com.dase.assistant.ai.SecureKeyStore
import com.dase.assistant.confirmation.ConfirmationManager
import com.dase.assistant.orchestrator.DaseOrchestrator
import com.dase.assistant.voice.VoiceListener

class MainActivity : Activity(), DaseOrchestrator.OrchestratorCallback {

    private lateinit var statusText: TextView
    private lateinit var commandInput: EditText

    private lateinit var confirmationManager: ConfirmationManager
    private lateinit var keyStore: SecureKeyStore
    private lateinit var aiProviderManager: AiProviderManager
    private lateinit var orchestrator: DaseOrchestrator
    private lateinit var voiceListener: VoiceListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        confirmationManager = ConfirmationManager()
        keyStore = SecureKeyStore(this)
        aiProviderManager = AiProviderManager(keyStore)
        orchestrator = DaseOrchestrator(
            confirmationManager = confirmationManager,
            aiProviderManager = aiProviderManager,
            aiPriority = listOf(
                AiProviderType.GEMINI,
                AiProviderType.OPENAI,
                AiProviderType.OPENROUTER,
                AiProviderType.DEEPSEEK,
                AiProviderType.CLAUDE
            ),
            callback = this
        )
        voiceListener = VoiceListener(this, buildVoiceCallback())

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
            text = "Ovozli yordamchi — Telegram va AI integratsiyasi"
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
            setOnClickListener { openAccessibilitySettings() }
        }

        val microphoneButton = Button(this).apply {
            text = "🎙 Mikrofon ruxsatini tekshirish"
            setOnClickListener { requestMicrophonePermission() }
        }

        val telegramButton = Button(this).apply {
            text = "Telegramni ochish"
            setOnClickListener { orchestrator.handleRecognizedText("Telegramni och") }
        }

        val speakButton = Button(this).apply {
            text = "🎤 Buyruq gapirish"
            setOnClickListener { startVoiceCommand() }
        }

        commandInput = EditText(this).apply {
            hint = "Buyruqni shu yerga yozib ham sinab ko'rish mumkin"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }

        val runCommandButton = Button(this).apply {
            text = "▶ Yozilgan buyruqni bajarish"
            setOnClickListener {
                val text = commandInput.text.toString()
                if (text.isNotBlank()) {
                    orchestrator.handleRecognizedText(text)
                }
            }
        }

        val askAiButton = Button(this).apply {
            text = "🤖 Yozilgan matnni AI'dan so'rash"
            setOnClickListener {
                val text = commandInput.text.toString()
                if (text.isNotBlank()) {
                    statusText.text = "AI'dan so'ralmoqda..."
                    orchestrator.askAi(text)
                }
            }
        }

        val aiKeysButton = Button(this).apply {
            text = "🔑 AI provider kalitlarini sozlash"
            setOnClickListener { showProviderKeyPicker() }
        }

        val foregroundToggleButton = Button(this).apply {
            text = "🟢 Fon xizmatini yoqish / 🔴 o'chirish"
            setOnClickListener { toggleForegroundService() }
        }

        val warning = TextView(this).apply {
            text = "Ruxsatni faqat o'zingiz xohlagan holda yoqing. DASE " +
                "foydalanuvchi aniq tasdiqlamaguncha (\"Yubor\" deyilmaguncha) " +
                "hech qanday xabar yubormaydi."
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
        root.addView(speakButton, matchParams())
        root.addView(commandInput, matchParams())
        root.addView(runCommandButton, matchParams())
        root.addView(askAiButton, matchParams())
        root.addView(aiKeysButton, matchParams())
        root.addView(foregroundToggleButton, matchParams())
        root.addView(warning, matchParams())

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
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

    // Mantiqiy xato tuzatildi (avvalgi auditda topilgan): ruxsat so'rovi
    // natijasi endi aniq ko'rsatiladi.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MICROPHONE_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            statusText.text = if (granted) {
                "Holat: Mikrofon ruxsati berildi"
            } else {
                "Holat: Mikrofon ruxsati rad etildi — ovozli buyruqlar ishlamaydi"
            }
        }
    }

    private fun startVoiceCommand() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            statusText.text = "Holat: Avval mikrofon ruxsatini bering"
            return
        }
        voiceListener.startListening()
    }

    private fun buildVoiceCallback(): VoiceListener.VoiceCallback = object : VoiceListener.VoiceCallback {
        override fun onListeningStarted() {
            runOnUiThread { statusText.text = "🎤 Tinglanmoqda..." }
        }
        override fun onPartialResult(text: String) {
            runOnUiThread { statusText.text = "🎤 ...$text" }
        }
        override fun onFinalResult(text: String) {
            runOnUiThread {
                statusText.text = "Eshitildi: \"$text\""
                orchestrator.handleRecognizedText(text)
            }
        }
        override fun onError(message: String) {
            runOnUiThread { statusText.text = "Ovoz xatosi: $message" }
        }
        override fun onListeningStopped() {}
    }

    private fun toggleForegroundService() {
        // Oddiy MVP: doim START yuboradi (servis allaqachon ishlab tursa
        // START_STICKY xavfsiz qayta ishga tushiradi). To'liq
        // ishga tushirilgan/to'xtatilgan holatni kuzatish uchun keyingi
        // bosqichda ServiceConnection yoki holat bayrog'i qo'shiladi.
        val intent = Intent(this, DaseForegroundService::class.java).apply {
            action = DaseForegroundService.ACTION_START
        }
        startForegroundService(intent)
        statusText.text = "Holat: Fon xizmati ishga tushirildi"
    }

    private fun showProviderKeyPicker() {
        val providers = AiProviderType.values()
        val labels = providers.map { p ->
            val has = if (keyStore.hasKey(p)) "✓" else "—"
            "${p.displayName} [$has]"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Qaysi provider uchun kalit kiritasiz?")
            .setItems(labels) { _, index -> showKeyInputDialog(providers[index]) }
            .setNegativeButton("Yopish", null)
            .show()
    }

    private fun showKeyInputDialog(provider: AiProviderType) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "${provider.displayName} API kaliti"
            setText(keyStore.getKey(provider) ?: "")
        }

        AlertDialog.Builder(this)
            .setTitle("${provider.displayName} kaliti")
            .setView(input)
            .setPositiveButton("Saqlash") { _, _ ->
                val key = input.text.toString().trim()
                if (key.isNotEmpty()) {
                    keyStore.saveKey(provider, key)
                    statusText.text = "Holat: ${provider.displayName} kaliti saqlandi"
                }
            }
            .setNeutralButton("O'chirish") { _, _ ->
                keyStore.clearKey(provider)
                statusText.text = "Holat: ${provider.displayName} kaliti o'chirildi"
            }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        val accessibilityStatus = if (isAccessibilityServiceEnabled()) {
            "Accessibility Service yoqilgan"
        } else {
            "Accessibility Service o'chiq"
        }
        statusText.text = "Holat: $accessibilityStatus"
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceListener.stopListening()
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

    // ---- DaseOrchestrator.OrchestratorCallback ----

    override fun onStatus(message: String) {
        runOnUiThread { statusText.text = "Holat: $message" }
    }

    override fun onNeedsClarification(rawText: String) {
        runOnUiThread { statusText.text = "Tushunmadim: \"$rawText\". Qaytadan aniqroq ayting." }
    }

    override fun onNeedsChatChoice(candidates: List<String>) {
        runOnUiThread {
            statusText.text = "Bir nechta chat topildi: ${candidates.joinToString(", ")}. " +
                "Aniqroq nom ayting."
        }
    }

    override fun onAwaitingSendConfirmation(chatName: String?, messageText: String) {
        runOnUiThread {
            statusText.text = "Tayyor: ${chatName ?: "joriy chat"} uchun \"$messageText\". " +
                "Yuborish uchun \"Yubor\" deng."
        }
    }

    override fun onMessageSent() {
        runOnUiThread { statusText.text = "✅ Xabar yuborildi." }
    }

    override fun onAiResponse(text: String) {
        runOnUiThread { statusText.text = "AI: $text" }
    }

    companion object {
        private const val MICROPHONE_REQUEST_CODE = 1001
    }
}
