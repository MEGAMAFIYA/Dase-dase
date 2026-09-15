package com.dase.assistant.command

/**
 * O'zbek tilidagi matnni normallashtirish va imlo xatolariga chidamli
 * taqqoslash uchun yordamchi funksiyalar.
 *
 * Bu klass hech qanday Android API'ga bog'liq emas — sof Kotlin mantiq,
 * shuning uchun JVM unit testlari bilan to'liq tekshirilishi mumkin.
 */
object TextNormalizer {

    /**
     * Turli apostrof belgilarini ("ʻ", "‘", "’", "`") bitta standart
     * belgiga ("'") keltiradi, ortiqcha bo'sh joylarni tozalaydi va
     * kichik harflarga o'tkazadi.
     */
    fun normalize(input: String): String {
        return input
            .trim()
            .lowercase()
            .replace('ʻ', '\'')
            .replace('‘', '\'')
            .replace('’', '\'')
            .replace('`', '\'')
            .replace(Regex("\\s+"), " ")
    }

    /** Ikki so'z orasidagi Levenshtein masofasi (tahrirlash masofasi). */
    fun editDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // o'chirish
                    dp[i][j - 1] + 1,      // qo'shish
                    dp[i - 1][j - 1] + cost // almashtirish
                )
            }
        }
        return dp[a.length][b.length]
    }

    /**
     * `word` berilgan `keyword`ga imlo xatosi darajasida (kalit so'z
     * uzunligiga qarab moslashuvchan chegara bilan) mos keladimi.
     *
     * 1-4 harf: aynan mos kelishi kerak (xato imkon bermaydi — juda qisqa
     * so'zlarda 1 ta xato butunlay boshqa so'zga aylantirib yuborishi mumkin).
     * 5-7 harf: 1 tagacha xato.
     * 8+ harf: 2 tagacha xato.
     *
     * MUHIM XAVFSIZLIK ESLATMASI: chegaralar ataylab qattiq tanlangan.
     * O'zbek tilida inkor qo'shimchasi "-ma-" fe'l ma'nosini teskarisiga
     * o'zgartiradi (masalan "yubor" — yubor, "yuborma" — yuborma!).
     * Agar chegara bo'sh qo'yib yuborilsa, "yubor" so'zi xato bilan
     * "yuborma" (bekor qilish so'zi) deb tanilib qolishi mumkin edi — bu
     * xabarni noto'g'ri tasdiqlash/bekor qilishga olib kelardi. Shuning
     * uchun CommandParserTest.kt da aynan shu holat alohida tekshiriladi.
     */
    fun fuzzyMatches(word: String, keyword: String): Boolean {
        val maxDistance = when {
            keyword.length <= 4 -> 0
            keyword.length <= 7 -> 1
            else -> 2
        }
        return editDistance(word, keyword) <= maxDistance
    }

    /** Matn ichida berilgan kalit so'zlardan biriga fuzzy mos keluvchi so'z bormi. */
    fun containsFuzzy(text: String, vararg keywords: String): Boolean {
        val words = normalize(text).split(" ")
        return words.any { w -> keywords.any { k -> fuzzyMatches(w, k) } }
    }
}
