package com.dase.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * `SpeechRecognizer` atrofidagi wrapper. Bitta martalik tinglash sessiyasini
 * boshqaradi ("Dase" wake-word'ning o'zi bu klassda YO'Q — bu faqat
 * "tugmani bosib gapirish" poydevori; doimiy fon-rejimida tinglash alohida,
 * 10-11-bosqichlarda ko'rib chiqiladigan muammo).
 *
 * Xato bo'lganda ilova QULAMAYDI — har doim [VoiceCallback.onError] orqali
 * xabar beriladi.
 */
class VoiceListener(
    private val context: Context,
    private val callback: VoiceCallback
) {
    interface VoiceCallback {
        fun onListeningStarted()
        fun onPartialResult(text: String)
        fun onFinalResult(text: String)
        fun onError(message: String)
        fun onListeningStopped()
    }

    private var recognizer: SpeechRecognizer? = null

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback.onError("Bu qurilmada ovoz tanish xizmati mavjud emas.")
            return
        }

        stopListening() // eski sessiya bo'lsa tozalab, yangisini boshlaymiz

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = speechRecognizer

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                callback.onListeningStarted()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                callback.onError(describeError(error))
                callback.onListeningStopped()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (text != null) {
                    callback.onFinalResult(text)
                } else {
                    callback.onError("Ovoz tanildi, lekin matn olinmadi.")
                }
                callback.onListeningStopped()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { callback.onPartialResult(it) }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "uz-UZ")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            callback.onError("Ovoz tanishni boshlab bo'lmadi: ${e.message}")
            callback.onListeningStopped()
        }
    }

    fun stopListening() {
        recognizer?.let {
            try {
                it.stopListening()
                it.destroy()
            } catch (e: Exception) {
                // Xavfsiz e'tiborsiz qoldiriladi — tozalash bosqichida ilovani qulatmaslik muhimroq.
            }
        }
        recognizer = null
    }

    private fun describeError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NETWORK -> "Tarmoq xatosi."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tarmoq javob berish vaqti tugadi."
        SpeechRecognizer.ERROR_NO_MATCH -> "Ovoz tanilmadi, qaytadan urinib ko'ring."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Gapirish kutilmadi (vaqt tugadi)."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon ruxsati berilmagan."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Ovoz tanish xizmati band."
        SpeechRecognizer.ERROR_AUDIO -> "Audio yozib olishda xato."
        else -> "Ovoz tanishda noma'lum xato (kod: $error)."
    }
}
