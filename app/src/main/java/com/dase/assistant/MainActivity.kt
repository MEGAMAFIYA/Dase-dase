package com.dase.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var statusText: TextView
    private lateinit var commandText: TextView

    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var wakeWordMode = false
    private var waitingForCommand = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textToSpeech = TextToSpeech(this, this)
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
            text = "3-bosqich • “Dase” faollashtirish prototipi"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        statusText = TextView(this).apply {
            text = "Holat: Tayyor"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }

        commandText = TextView(this).apply {
            text = "Buyruq: —"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 24)
        }

        val wakeButton = Button(this).apply {
            text = "▶ “Dase” rejimini yoqish"
            setOnClickListener {
                if (wakeWordMode) {
                    stopWakeWordMode()
                } else {
                    startWakeWordMode()
                }
            }
        }

        val oneShotButton = Button(this).apply {
            text = "🎙 Bitta buyruqni tinglash"
            setOnClickListener {
                startOneShotRecognition()
            }
        }

        val info = TextView(this).apply {
            text = "“Dase” rejimi ilova ochiq turganda ovozni tinglaydi. “Dase” deganingizdan keyin keyingi buyruqni qabul qiladi."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }

        root.addView(title, matchParams())
        root.addView(subtitle, matchParams())
        root.addView(statusText, matchParams())
        root.addView(commandText, matchParams())
        root.addView(wakeButton, matchParams())
        root.addView(oneShotButton, matchParams())
        root.addView(info, matchParams())

        setContentView(root)
    }

    private fun matchParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    private fun hasMicrophonePermission(): Boolean {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun ensureMicrophonePermission(): Boolean {
        if (hasMicrophonePermission()) return true

        requestPermissions(
            arrayOf(Manifest.permission.RECORD_AUDIO),
            MICROPHONE_REQUEST_CODE
        )
        return false
    }

    private fun startWakeWordMode() {
        if (!ensureMicrophonePermission()) return

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusText.text = "Holat: Ovoz tanish xizmati mavjud emas"
            return
        }

        wakeWordMode = true
        waitingForCommand = false
        statusText.text = "Holat: “Dase” so‘zini kutyapman..."
        startRecognition()
    }

    private fun stopWakeWordMode() {
        wakeWordMode = false
        waitingForCommand = false
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        statusText.text = "Holat: To‘xtatildi"
    }

    private fun startOneShotRecognition() {
        if (!ensureMicrophonePermission()) return

        wakeWordMode = false
        waitingForCommand = true
        startRecognition()
    }

    private fun startRecognition() {
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(
            object : android.speech.RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    statusText.text = if (wakeWordMode && !waitingForCommand) {
                        "Holat: “Dase”ni tinglayapman..."
                    } else {
                        "Holat: Buyruqni tinglayapman..."
                    }
                }

                override fun onBeginningOfSpeech() {
                    statusText.text = "Holat: Ovoz qabul qilinmoqda..."
                }

                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    statusText.text = "Holat: Natija tekshirilmoqda..."
                }

                override fun onError(error: Int) {
                    if (wakeWordMode) {
                        statusText.text = "Holat: Qayta tinglashga tayyorlanmoqda..."
                        window.decorView.postDelayed({
                            if (wakeWordMode) startRecognition()
                        }, 700)
                    } else {
                        statusText.text = "Holat: Ovoz tanishda xato: $error"
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    val result = matches?.firstOrNull().orEmpty()
                    processRecognizedText(result)
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            }
        )

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "uz-UZ")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "uz-UZ")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "DASE sizni tinglamoqda...")
        }

        speechRecognizer?.startListening(intent)
    }

    private fun processRecognizedText(text: String) {
        val normalized = text.trim().lowercase(Locale("uz", "UZ"))
        commandText.text = if (text.isBlank()) {
            "Buyruq: Aniqlanmadi"
        } else {
            "Buyruq: $text"
        }

        if (wakeWordMode && !waitingForCommand) {
            if (normalized.contains("dase") || normalized.contains("dasi")) {
                waitingForCommand = true
                statusText.text = "Holat: Faollashdim, buyruqni ayting"
                speak("Ha, tinglayapman")
            } else {
                statusText.text = "Holat: “Dase” so‘zini kutyapman..."
            }

            window.decorView.postDelayed({
                if (wakeWordMode && waitingForCommand) {
                    startRecognition()
                } else if (wakeWordMode) {
                    startRecognition()
                }
            }, 500)
            return
        }

        waitingForCommand = false
        statusText.text = "Holat: Buyruq qabul qilindi"
        speak("Siz aytdingiz: $text")

        if (wakeWordMode) {
            window.decorView.postDelayed({
                if (wakeWordMode) startRecognition()
            }, 800)
        }
    }

    private fun speak(text: String) {
        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "DASE_RESPONSE"
        )
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("uz", "UZ"))
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                textToSpeech?.language = Locale.US
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == MICROPHONE_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED

            statusText.text = if (granted) {
                "Holat: Mikrofon ruxsati berildi"
            } else {
                "Holat: Mikrofon ruxsati berilmadi"
            }
        }
    }

    override fun onDestroy() {
        wakeWordMode = false
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val MICROPHONE_REQUEST_CODE = 1001
    }
}
