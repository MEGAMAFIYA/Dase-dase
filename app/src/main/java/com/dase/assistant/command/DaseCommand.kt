package com.dase.assistant.command

/**
 * Foydalanuvchi buyrug'ining tan olingan (aniqlangan) turi.
 * Har bir bosqichda faqat shu turlardan foydalaniladi — erkin matn
 * to'g'ridan-to'g'ri bajarilmaydi, avval shu strukturaga ajratiladi.
 */
sealed class DaseCommand {

    /** "Telegramni och" / "Telegramga kir" */
    data class OpenApp(val appName: String) : DaseCommand()

    /** "Sardor chatini top" */
    data class FindChat(val chatName: String) : DaseCommand()

    /**
     * "Sardorga salom yoz" yoki "Salom do'stim deb yoz".
     * [chatName] null bo'lishi mumkin — bu holda oldin topilgan/ochiq
     * turgan chatga yoziladi deb tushuniladi (kontekst orchestrator
     * darajasida hal qilinadi, parser darajasida emas).
     */
    data class ComposeMessage(val chatName: String?, val messageText: String) : DaseCommand()

    /** "Yubor" / "Ha, yubor" / "Jo'nat" / "Tasdiqlayman" */
    object Send : DaseCommand()

    /** "Bekor qil" / "Yo'q" / "Yuborma" */
    object Cancel : DaseCommand()

    /** Joriy amalni (masalan, tinglashni) to'xtatish — tasdiqlash kutilmayotganda. */
    object Stop : DaseCommand()

    /** "Orqaga qayt" */
    object GoBack : DaseCommand()

    /** "Bosh menyuga qayt" */
    object GoHome : DaseCommand()

    /** Tan olinmagan buyruq — foydalanuvchidan aniqlashtirish so'raladi. */
    data class Unknown(val rawText: String) : DaseCommand()
}

/**
 * Parser natijasi: buyruq turi + tasdiqlash holati + asl matn birga saqlanadi
 * (talab: "Buyruq turini, chat nomini, xabar matnini va tasdiqlash holatini saqlash").
 */
data class ParsedCommand(
    val command: DaseCommand,
    val rawText: String,
    val requiresConfirmation: Boolean
)
