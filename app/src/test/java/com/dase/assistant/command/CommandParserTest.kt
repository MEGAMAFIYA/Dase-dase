package com.dase.assistant.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sof Kotlin unit testlari — Android SDK/emulator talab qilmaydi,
 * `gradle test` bilan istalgan JVM ustida ishga tushadi.
 *
 * ESLATMA: bu fayl ishlab chiqilgan muhitda (sandbox: tarmoqsiz, Android
 * SDK va Kotlin kompilyatori o'rnatilmagan) avtomatik ishga tushirib
 * ko'rilmadi — mantiq qo'lda, qadam-baqadam kuzatib chiqilib tasdiqlandi.
 * Haqiqiy ishga tushirish `gradle test` yoki GitHub Actions orqali
 * amalga oshirilishi kerak.
 */
class CommandParserTest {

    @Test
    fun `Telegramni och ochish buyrug'ini aniqlaydi`() {
        val result = CommandParser.parse("Telegramni och")
        assertTrue(result.command is DaseCommand.OpenApp)
        assertEquals("Telegram", (result.command as DaseCommand.OpenApp).appName)
        assertEquals(false, result.requiresConfirmation)
    }

    @Test
    fun `Sardor chatini top chat qidirishni aniqlaydi`() {
        val result = CommandParser.parse("Sardor chatini top")
        assertTrue(result.command is DaseCommand.FindChat)
        assertEquals("Sardor", (result.command as DaseCommand.FindChat).chatName)
    }

    @Test
    fun `Sardorga salom do'stim deb yoz - chat va matnni birga ajratadi`() {
        val result = CommandParser.parse("Sardorga salom do'stim deb yoz")
        val cmd = result.command as DaseCommand.ComposeMessage
        assertEquals("Sardor", cmd.chatName)
        assertEquals("salom do'stim", cmd.messageText)
        assertTrue(result.requiresConfirmation)
    }

    @Test
    fun `Salom do'stim deb yoz - chat nomisiz xabar matnini ajratadi`() {
        val result = CommandParser.parse("Salom do'stim deb yoz")
        val cmd = result.command as DaseCommand.ComposeMessage
        assertEquals(null, cmd.chatName)
        assertEquals("salom do'stim", cmd.messageText)
    }

    @Test
    fun `Yubor - tasdiqlash sifatida Send qaytaradi`() {
        val result = CommandParser.parse("Yubor")
        assertEquals(DaseCommand.Send, result.command)
    }

    @Test
    fun `Ha vergul yubor ham Send sifatida tanilishi kerak`() {
        val result = CommandParser.parse("Ha, yubor")
        assertEquals(DaseCommand.Send, result.command)
    }

    @Test
    fun `XAVFSIZLIK - Yuborma Send bilan chalkashmasligi kerak`() {
        // Bu eng muhim test: "yubor" (yubor) va "yuborma" (yuborma) bir-biriga
        // qarama-qarshi ma'noli so'zlar. Fuzzy-matching xato bilan ularni
        // adashtirsa, foydalanuvchi tasdiqlamagan xabar yuborilib qolishi
        // mumkin edi.
        val result = CommandParser.parse("Yuborma")
        assertEquals(DaseCommand.Cancel, result.command)
    }

    @Test
    fun `XAVFSIZLIK - Yubor Cancel bilan chalkashmasligi kerak`() {
        val result = CommandParser.parse("Yubor")
        assertEquals(DaseCommand.Send, result.command)
    }

    @Test
    fun `Bekor qil - Cancel qaytaradi`() {
        assertEquals(DaseCommand.Cancel, CommandParser.parse("Bekor qil").command)
    }

    @Test
    fun `Yo'q - Cancel qaytaradi`() {
        assertEquals(DaseCommand.Cancel, CommandParser.parse("Yo'q").command)
    }

    @Test
    fun `To'xta - Stop qaytaradi (yakuniy qarorni orchestrator qabul qiladi)`() {
        assertEquals(DaseCommand.Stop, CommandParser.parse("To'xta").command)
    }

    @Test
    fun `Orqaga qayt - GoBack qaytaradi`() {
        assertEquals(DaseCommand.GoBack, CommandParser.parse("Orqaga qayt").command)
    }

    @Test
    fun `Bosh menyuga qayt - GoHome qaytaradi`() {
        assertEquals(DaseCommand.GoHome, CommandParser.parse("Bosh menyuga qayt").command)
    }

    @Test
    fun `Imlo xatosi bilan yozilgan Telegram ham tanilishi kerak`() {
        // "Telegramni" so'zida bitta harf xato: "Telegrmani"
        val result = CommandParser.parse("Telegrmani och")
        assertTrue(
            "Kutilgan: OpenApp, olingan: ${result.command}",
            result.command is DaseCommand.OpenApp
        )
    }

    @Test
    fun `Noma'lum buyruq Unknown sifatida qaytadi`() {
        val result = CommandParser.parse("Bugun ob-havo qanday")
        assertTrue(result.command is DaseCommand.Unknown)
    }

    @Test
    fun `Bo'sh matn Unknown sifatida qaytadi va qulamaydi`() {
        val result = CommandParser.parse("")
        assertTrue(result.command is DaseCommand.Unknown)
    }
}
