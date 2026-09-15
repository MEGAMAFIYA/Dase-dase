package com.dase.assistant.confirmation

/**
 * 8-bosqich: tasdiqlash tizimi.
 *
 * Holat mashinasi: Idle -> AwaitingConfirmation -> Sending -> Idle
 * (yoki xato bo'lsa Sending -> AwaitingConfirmation, qayta tasdiq talab qilinadi).
 *
 * Har bir "kutilayotgan xabar"ga o'ziga xos `token` beriladi. Bu — eskirgan
 * (masalan foydalanuvchi bekor qilib, keyin yangi xabar diktovka qilgan)
 * tarmoq javobi tasodifan joriy holatni buzib qo'ymasligi uchun kerak.
 *
 * Sof Kotlin — Android'ga bog'liq emas, JVM unit testlari bilan tekshiriladi.
 */
class ConfirmationManager {

    sealed class State {
        object Idle : State()
        data class AwaitingConfirmation(val pending: PendingMessage, val token: Int) : State()
        data class Sending(val pending: PendingMessage, val token: Int) : State()
    }

    @Volatile
    private var state: State = State.Idle
    private var tokenCounter = 0

    /** Yangi xabar tayyor bo'lganda chaqiriladi. Eski kutilayotgan xabar (agar bo'lsa) bekor qilinadi. */
    @Synchronized
    fun setPendingMessage(pending: PendingMessage) {
        tokenCounter++
        state = State.AwaitingConfirmation(pending, tokenCounter)
    }

    @Synchronized
    fun currentPending(): PendingMessage? = when (val s = state) {
        is State.AwaitingConfirmation -> s.pending
        is State.Sending -> s.pending
        State.Idle -> null
    }

    /**
     * Faqat AwaitingConfirmation holatida ishlaydi va bir marta muvaffaqiyatli
     * chaqirilgach darhol Sending holatiga o'tadi — shu bilan bir xil xabarni
     * tasodifan ikki marta yuborishning oldi olinadi (masalan foydalanuvchi
     * "Yubor"ni ikki marta ketma-ket aytib yuborsa, ikkinchisi e'tiborsiz
     * qoldiriladi, chunki state endi Sending, AwaitingConfirmation emas).
     *
     * @return yuborilishi kerak bo'lgan xabar va shu urinishga tegishli token,
     *   yoki tasdiqlash uchun hech narsa yo'q/band bo'lsa `null`.
     */
    @Synchronized
    fun confirmSend(): Pair<PendingMessage, Int>? {
        val s = state as? State.AwaitingConfirmation ?: return null
        state = State.Sending(s.pending, s.token)
        return s.pending to s.token
    }

    /** Yuborish muvaffaqiyatli tugagach chaqiriladi. */
    @Synchronized
    fun onSendSucceeded(token: Int) {
        val s = state
        if (s is State.Sending && s.token == token) {
            state = State.Idle
        }
        // Eski (bekor qilingan) urinishdan kelgan natija shu yerda e'tiborsiz qoldiriladi.
    }

    /**
     * Yuborish muvaffaqiyatsiz tugagach chaqiriladi. Avtomatik qayta urinish
     * YO'Q — foydalanuvchi yana aniq "Yubor" deyishi kerak (talab: "qayta
     * yuborishdan oldin foydalanuvchidan yana tasdiq so'ralishi kerak").
     */
    @Synchronized
    fun onSendFailed(token: Int) {
        val s = state
        if (s is State.Sending && s.token == token) {
            state = State.AwaitingConfirmation(s.pending, s.token)
        }
    }

    /** "Bekor qil" / "Yo'q" / "Yuborma" buyrug'i uchun. */
    @Synchronized
    fun cancel() {
        state = State.Idle
    }

    @Synchronized
    fun isAwaitingConfirmation(): Boolean = state is State.AwaitingConfirmation

    @Synchronized
    fun isSending(): Boolean = state is State.Sending
}
