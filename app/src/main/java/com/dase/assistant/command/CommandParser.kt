package com.dase.assistant.command

/**
 * Foydalanuvchining erkin (ovozdan matnga aylantirilgan yoki yozma) gapini
 * [DaseCommand] strukturasiga ajratadi.
 *
 * MUHIM: Bu klass hech qachon amalni o'zi BAJARMAYDI — faqat tan oladi va
 * strukturaga soladi. Bajarish qarori (ayniqsa xabar yuborish) doim
 * yuqori darajadagi orchestrator va ConfirmationManager tomonidan, faqat
 * aniq tasdiqdan keyin qabul qilinadi (5-bosqich talabi: "Noto'g'ri
 * tushunilgan buyruqni avtomatik bajarmaslik").
 *
 * Sof Kotlin — Android API'ga bog'liq emas, shuning uchun JVM unit
 * testlari bilan to'liq va tezkor tekshiriladi (bu muhitda ham
 * mantiqni qo'lda kuzatib chiqib tasdiqladim, quyida CommandParserTest.kt
 * ga qarang).
 */
object CommandParser {

    private val findChatRegex = Regex("^(.+?) chatini top$")
    // "Sardorga salom do'stim deb yoz" yoki "Sardorga salom yoz"
    private val composeWithChatDebRegex = Regex("^(\\S+)ga (.+?) deb yoz$")
    private val composeWithChatRegex = Regex("^(\\S+)ga (.+?) yoz$")
    // "Salom do'stim deb yoz" (chat nomi ko'rsatilmagan)
    private val composeNoChatRegex = Regex("^(.+?) deb yoz$")

    private val sendWords = listOf("yubor", "jo'nat", "tasdiqlayman")
    private val cancelWords = listOf("bekor", "yo'q", "yuborma")
    private val stopWords = listOf("to'xta")
    private val openWords = listOf("och", "kir")

    fun parse(rawText: String): ParsedCommand {
        val normalized = TextNormalizer.normalize(rawText)

        if (normalized.isEmpty()) {
            return ParsedCommand(DaseCommand.Unknown(rawText), rawText, requiresConfirmation = false)
        }

        // 1) "<Ism> chatini top"
        findChatRegex.find(normalized)?.let { m ->
            val name = capitalizeName(m.groupValues[1])
            return ParsedCommand(DaseCommand.FindChat(name), rawText, requiresConfirmation = false)
        }

        // 2) "<Ism>ga <matn> deb yoz"
        composeWithChatDebRegex.find(normalized)?.let { m ->
            val name = capitalizeName(m.groupValues[1])
            val text = m.groupValues[2].trim()
            if (text.isNotEmpty()) {
                return ParsedCommand(DaseCommand.ComposeMessage(name, text), rawText, requiresConfirmation = true)
            }
        }

        // 3) "<Ism>ga <matn> yoz" ("deb"siz)
        composeWithChatRegex.find(normalized)?.let { m ->
            val name = capitalizeName(m.groupValues[1])
            val text = m.groupValues[2].trim()
            if (text.isNotEmpty()) {
                return ParsedCommand(DaseCommand.ComposeMessage(name, text), rawText, requiresConfirmation = true)
            }
        }

        // 4) "<matn> deb yoz" (chat nomisiz — joriy ochiq chatga)
        composeNoChatRegex.find(normalized)?.let { m ->
            val text = m.groupValues[1].trim()
            if (text.isNotEmpty()) {
                return ParsedCommand(DaseCommand.ComposeMessage(null, text), rawText, requiresConfirmation = true)
            }
        }

        // 5) "Telegramni och" / "Telegramga kir"
        if (TextNormalizer.containsFuzzy(normalized, "telegram", "telegramni", "telegramga")
            && TextNormalizer.containsFuzzy(normalized, *openWords.toTypedArray())
        ) {
            return ParsedCommand(DaseCommand.OpenApp("Telegram"), rawText, requiresConfirmation = false)
        }

        // 6) Qisqa boshqaruv buyruqlari — faqat qisqa gaplarda tekshiriladi,
        //    aks holda xabar matni ichidagi so'zlar bilan chalkashib ketishi mumkin.
        val wordCount = normalized.split(" ").size
        if (wordCount <= 4) {
            if (TextNormalizer.containsFuzzy(normalized, *stopWords.toTypedArray())) {
                // "to'xta" ikki xil ma'noda ishlatiladi (5- va 8-bosqich talabi):
                // tasdiq kutilayotganda — bekor qilish, aks holda — to'xtatish.
                // Qarorni orchestrator/ConfirmationManager joriy holatga qarab qabul qiladi.
                return ParsedCommand(DaseCommand.Stop, rawText, requiresConfirmation = false)
            }
            if (TextNormalizer.containsFuzzy(normalized, "orqaga") &&
                TextNormalizer.containsFuzzy(normalized, "qayt")
            ) {
                return ParsedCommand(DaseCommand.GoBack, rawText, requiresConfirmation = false)
            }
            if (TextNormalizer.containsFuzzy(normalized, "bosh") &&
                TextNormalizer.containsFuzzy(normalized, "menyuga", "menyu")
            ) {
                return ParsedCommand(DaseCommand.GoHome, rawText, requiresConfirmation = false)
            }
            if (TextNormalizer.containsFuzzy(normalized, *cancelWords.toTypedArray())) {
                return ParsedCommand(DaseCommand.Cancel, rawText, requiresConfirmation = false)
            }
            if (TextNormalizer.containsFuzzy(normalized, *sendWords.toTypedArray())) {
                return ParsedCommand(DaseCommand.Send, rawText, requiresConfirmation = false)
            }
        }

        return ParsedCommand(DaseCommand.Unknown(rawText), rawText, requiresConfirmation = false)
    }

    private fun capitalizeName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return trimmed
        return trimmed.replaceFirstChar { it.uppercase() }
    }
}
