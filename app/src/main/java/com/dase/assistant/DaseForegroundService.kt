package com.dase.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * 10-bosqich: fon rejimi — POYDEVOR, TO'LIQ EMAS.
 *
 * Bu servis foreground bildirishnoma orqali DASE fonda "tirik" ekanini
 * ko'rsatadi va foydalanuvchi bildirishnomani bosganda ilovani ochadi.
 *
 * ATAYLAB QILINMAGAN (halollik uchun aniq aytilishi kerak): bu servis
 * doimiy "hot mic" (doim tinglab turish) qilmaydi. Sabablari:
 *  1. Android 10+ background mikrofon cheklovlari — mikrofonni faqat
 *     foreground holatda yoki maxsus turdagi foreground service'da
 *     ishlatish mumkin (shu servis "microphone" turi bilan e'lon qilingan,
 *     lekin bu servisning o'zi hali tinglashni boshlamaydi).
 *  2. MIUI batareya optimizatsiyasi ko'pincha shunday servislarni foydalanuvchi
 *     ruxsatisiz to'xtatib qo'yadi — buni kod darajasida to'liq oldini
 *     olib bo'lmaydi, faqat foydalanuvchidan "cheklovsiz" ruxsat so'rash
 *     mumkin (bu alohida, qo'lda tekshiriladigan qadam).
 *  3. Doimiy tinglash batareya va maxfiylikka jiddiy ta'sir qiladi — shuning
 *     uchun xavfsiz standart: foydalanuvchi bildirishnoma yoki ilova
 *     ichidan ANIQ bosgandagina tinglash boshlanadi ("tap-to-listen"),
 *     avtomatik doimiy tinglash emas.
 *
 * Haqiqiy qurilmada (Redmi 9C, Android 11, MIUI) tekshirilmaguncha bu
 * "PROTOTIP" holatida hisoblanadi.
 */
class DaseForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "dase_foreground_channel"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START = "com.dase.assistant.action.START_FOREGROUND"
        const val ACTION_STOP = "com.dase.assistant.action.STOP_FOREGROUND"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                createChannelIfNeeded()
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        }
        // START_STICKY: tizim resurs tanqisligi sababli servisni to'xtatib
        // qo'ysa, keyinroq qayta ishga tushirishga harakat qiladi (talab:
        // "service to'xtatilganda tiklash" — bu shu yerda faqat OS darajasidagi
        // eng oddiy mexanizm, MIUI buni baribir bekor qilishi mumkinligi
        // yuqorida ta'kidlangan).
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DASE ishlamoqda")
            .setContentText("Buyruq berish uchun bosing")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "DASE fon xizmati",
                    NotificationManager.IMPORTANCE_LOW
                )
                manager.createNotificationChannel(channel)
            }
        }
    }
}
