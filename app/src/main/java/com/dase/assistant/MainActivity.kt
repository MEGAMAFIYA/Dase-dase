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
            text = "Shaxsiy AI yordamchi"
            textSize = 18f
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

        val microphoneButton = Button(this).apply {
            text = "🎙 Ovozli buyruq berish"
            setOnClickListener {
                startVoiceRecognition()
            }
        }

        val repeatButton = Button(this).apply {
            text = "🔊 Javobni ovoz chiqarib aytish"
            setOnClickListener {
                val command = commandText.text.toString()
                    .removePrefix("Buyruq: ")
                    .trim()

                if (command.isNotEmpty() && command != "—") {
                    speak("Siz aytdingiz: $command")
                } else {
                    speak("Hozircha buyruq kiritilmadi")
                }
            }
        }

        val permissionButton = Button(this).apply {
            text = "Mikrofon ruxsatini tekshirish"
            setOnClickListener {
                requestMicrophonePermission()
            }
        }

        val info = TextView(this).apply {
            text = "2-bosqich: ovozni matnga aylantirish va ovozli javob."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }

        root.addView(title, matchParams())
        root.addView(subtitle, matchParams())
        root.addView(statusText, matchParams())
        root.addView(commandText, matchParams())
        root.addView(microphoneButton, matchParams())
        root.addView(repeatButton, matchParams())
        root.addView(permissionButton, matchParams())
        root.addView(info, matchParams())

        setContentView(root)
    }

    private fun matchParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun requestMicrophonePermission(): Boolean {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            statusText.text = "Holat: Mikrofon ruxsati berilgan"
            return true
        }

        requestPermissions(
            arrayOf(Manifest.permission.RECORD_AUDIO),
            MICROPHONE_REQUEST_CODE
        )
        return false
    }

    private fun startVoiceRecognition() {
        if (!requestMicrophonePermission()) {
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusText.text = "Holat: Ovoz tanish xizmati mavjud emas"
            Toast.makeText(
                this,
                "Telefoningizda ovoz tanish xizmati mavjud emas",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(
            object : android.speech.RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    statusText.text = "Holat: Tinglayapman..."
                }

                override fun onBeginningOfSpeech() {
                    statusText.text = "Holat: Ovoz qabul qilinmoqda..."
                }

                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    statusText.text = "Holat: Qayta ishlanmoqda..."
                }

                override fun onError(error: Int) {
                    statusText.text = "Holat: Ovoz tanishda xato: $error"
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    val result = matches?.firstOrNull().orEmpty()

                    commandText.text = if (result.isBlank()) {
                        "Buyruq: Aniqlanmadi"
                    } else {
                        "Buyruq: $result"
                    }

                    statusText.text = "Holat: Buyruq qabul qilindi"

                    if (result.isNotBlank()) {
                        speak("Siz aytdingiz: $result")
                    }
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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "DASE sizni tinglamoqda...")
        }

        speechRecognizer?.startListening(intent)
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
                textToSpeech?.language = Locale("en", "US")
            }
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

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

    companion object {
        private const val MICROPHONE_REQUEST_CODE = 1001
    }
}
