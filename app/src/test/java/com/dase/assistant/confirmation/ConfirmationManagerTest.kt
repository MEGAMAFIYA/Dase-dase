package com.dase.assistant.confirmation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmationManagerTest {

    @Test
    fun `tasdiqsiz confirmSend hech narsa qaytarmaydi`() {
        val manager = ConfirmationManager()
        assertNull(manager.confirmSend())
    }

    @Test
    fun `pending qo'yilgach confirmSend xabarni qaytaradi`() {
        val manager = ConfirmationManager()
        manager.setPendingMessage(PendingMessage("Sardor", "salom"))
        val result = manager.confirmSend()
        assertEquals("Sardor", result?.first?.chatName)
        assertEquals("salom", result?.first?.messageText)
    }

    @Test
    fun `XAVFSIZLIK - ketma-ket ikki marta confirmSend faqat bitta marta ruxsat beradi`() {
        val manager = ConfirmationManager()
        manager.setPendingMessage(PendingMessage("Sardor", "salom"))
        val first = manager.confirmSend()
        val second = manager.confirmSend()
        assertTrue("Birinchi urinish muvaffaqiyatli bo'lishi kerak", first != null)
        assertNull("Ikkinchi (takroriy) urinish rad etilishi kerak", second)
    }

    @Test
    fun `cancel holatni tozalaydi`() {
        val manager = ConfirmationManager()
        manager.setPendingMessage(PendingMessage("Sardor", "salom"))
        manager.cancel()
        assertNull(manager.currentPending())
        assertNull(manager.confirmSend())
    }

    @Test
    fun `muvaffaqiyatsiz yuborishdan keyin qayta confirmSend talab qilinadi`() {
        val manager = ConfirmationManager()
        manager.setPendingMessage(PendingMessage("Sardor", "salom"))
        val (_, token) = manager.confirmSend()!!
        manager.onSendFailed(token)

        // Holat AwaitingConfirmation'ga qaytishi kerak, lekin avtomatik
        // qayta yubormaydi — foydalanuvchi yana "Yubor" deyishi kerak.
        assertTrue(manager.isAwaitingConfirmation())
        val retry = manager.confirmSend()
        assertTrue(retry != null)
    }

    @Test
    fun `muvaffaqiyatli yuborishdan keyin holat Idle bo'ladi`() {
        val manager = ConfirmationManager()
        manager.setPendingMessage(PendingMessage("Sardor", "salom"))
        val (_, token) = manager.confirmSend()!!
        manager.onSendSucceeded(token)
        assertNull(manager.currentPending())
        assertNull(manager.confirmSend())
    }

    @Test
    fun `eskirgan token bilan kelgan natija joriy holatni buzmaydi`() {
        val manager = ConfirmationManager()
        manager.setPendingMessage(PendingMessage("Sardor", "birinchi"))
        val (_, oldToken) = manager.confirmSend()!!
        // Foydalanuvchi shu orada yangi xabar diktovka qildi (masalan xato
        // tarmoq javobi hali qaytmasdan turib):
        manager.setPendingMessage(PendingMessage("Anvar", "ikkinchi"))

        // Eski urinishning (allaqachon eskirgan) natijasi keladi:
        manager.onSendSucceeded(oldToken)

        // Joriy (yangi) pending o'zgarishsiz qolishi kerak:
        assertEquals("Anvar", manager.currentPending()?.chatName)
    }
}
