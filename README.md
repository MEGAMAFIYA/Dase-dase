# DASE — Ovozli Android yordamchi

Paket: `com.dase.assistant` · Versiya: 0.5.0 (versionCode 5)

DASE — o'zbek tilida ovozli buyruqlarni tushunadigan, Telegram bilan
(foydalanuvchi tasdig'i bilan) ishlay oladigan va bir nechta AI provayder
bilan gaplasha oladigan Android yordamchisi.

---

## ⚠️ Ushbu versiya haqida muhim, halol eslatma

Bu kod **tarmoqsiz, Android SDK'siz, emulyatorsiz muhitda** yozilgan va
**hech qachon kompilyatsiya yoki ishga tushirib ko'rilmagan**. Mantiq
qo'lda, qadam-baqadam diqqat bilan tekshirilgan (parser va tasdiqlash
mantig'i uchun `CommandParserTest.kt` / `ConfirmationManagerTest.kt`dagi
holatlar qo'lda kuzatib chiqilgan), lekin haqiqiy tasdiqlash quyidagilar
orqali amalga oshishi SHART:

1. GitHub Actions build (`gradle test` + `gradle assembleDebug`) — bu
   kompilyatsiya xatolarini ushlaydi.
2. Haqiqiy qurilmada (tavsiya: aynan maqsad qurilma — Redmi 9C, Android 11,
   MIUI) qo'lda sinov — bu Accessibility/Telegram navigatsiyasi va fon
   rejimi kabi qismlar uchun SHART, chunki ular haqiqiy UI daraxti va
   OS xatti-harakatiga bog'liq.

Quyidagi holat jadvalida hech bir funksiya "haqiqiy qurilmada
tekshirilgan" deb ko'rsatilmagan — bu ataylab shunday, chunki bunday emas.

---

## Holat jadvali

| # | Funksiya | Holat | Izoh |
|---|---|---|---|
| 4 | Android poydevori (Manifest/Gradle) | Tayyor (qo'lda tekshirilgan) | `<queries>` xatosi shu versiyada tuzatildi |
| 4 | Mikrofon ruxsati + natija callback'i | Tayyor (qo'lda tekshirilgan) | `onRequestPermissionsResult` qo'shildi |
| 5 | Buyruq parseri (`CommandParser`) | Yozilgan, JVM testlari bilan qo'lda tasdiqlangan | `gradle test` orqali avtomatik ishga tushirilishi kerak |
| 6 | Telegram navigatsiyasi (`TelegramNavigator`) | **Prototip** | Faqat kod darajasida yozilgan, haqiqiy Telegram UI'ga qarshi hech qachon sinalmagan |
| 7 | Xabar tayyorlash | **Prototip** | 6-bandga bog'liq |
| 8 | Tasdiqlash tizimi (`ConfirmationManager`) | Yozilgan, JVM testlari bilan qo'lda tasdiqlangan | Holat mashinasi mantig'i sof Kotlin, Android'ga bog'liq emas |
| — | Ovoz tanish (`VoiceListener`, `SpeechRecognizer`) | **Prototip** | Faqat "tugmani bosib gapirish"; doimiy tinglash yo'q |
| 9 | AI provider integratsiyasi (5 ta provider) | **Prototip** | Kod yozilgan, lekin haqiqiy API kalit/tarmoq bilan sinalmagan; model nomlari eskirgan bo'lishi mumkin |
| 10 | Fon rejimi (`DaseForegroundService`) | **Prototip, ataylab cheklangan** | Doimiy "hot mic" YO'Q — sabab pastda tushuntirilgan |
| 11 | Ekran qulflanganda ishlash | **Amalga oshirilmagan** | Pastdagi "Texnik cheklovlar" bo'limiga qarang |
| 12 | Offline AI | **Amalga oshirilmagan** | Pastdagi "Texnik cheklovlar" bo'limiga qarang |
| 13 | Testlar | Qisman (faqat sof-Kotlin qismlar uchun) | Accessibility/UI testlari uchun haqiqiy qurilma/emulyator kerak |
| 14 | Yakuniy tekshiruv | **Bajarilmagan** | Yuqoridagi sabablarga ko'ra loyiha "to'liq tayyor" deb e'lon qilinmaydi |

---

## Texnik cheklovlar — yashirilmasdan tushuntirilgan

### 11-bosqich: Ekran qulflanganda ishlash

**To'liq, kafolatlangan holda amalga oshirilmadi.** Sabablari:

- Android 10+ da fondagi ilovalar mikrofonga faqat maxsus sharoitlarda
  (masalan, mos turdagi foreground service orqali) kira oladi.
- MIUI batareya optimizatsiyasi foreground service'larni ham
  foydalanuvchi ruxsatisiz to'xtatib qo'yishi mumkin — buni kod ichidan
  to'liq kafolatlab bo'lmaydi.
- Ekran qulflangan holatda doimiy mikrofon tinglashni ishonchli qilish
  uchun haqiqiy qurilmada uzoq muddatli sinov kerak, buni bu muhitda
  bajarib bo'lmaydi.

**Taklif qilingan xavfsiz muqobil:** doimiy "hot mic" o'rniga, foreground
bildirishnoma orqali "tap-to-listen" (bosib-gapirish) — bu allaqachon
`DaseForegroundService`da shu tarzda ishlab chiqilgan.

### 12-bosqich: Offline AI

**Amalga oshirilmadi — ataylab.** Maqsad qurilma (Redmi 9C, Android 11,
taxminan 3 GB fizik RAM, cheklangan CPU) haqiqiy foydali kattalikdagi
til modelini (hatto eng kichik zamonaviy modellarni ham) oqilona tezlik
va sifat bilan ishga tushirish uchun YETARLI EMAS — bu haqiqatni
yashirish o'rniga ochiq aytish to'g'riroq.

**Muqobil yo'nalishlar:**
- Haqiqiy "AQL" uchun serverli AI (9-bosqichdagi 5 ta provider) ishlatish.
- Faqat wake-word (masalan "Dase") aniqlash uchun juda kichik,
  ixtisoslashgan model (masalan Porcupine kabi) ko'rib chiqilishi mumkin
  — bu umumiy AQL emas, faqat bitta so'zni aniqlaydigan model, va bu
  ham alohida tekshirilishi kerak bo'lgan qo'shimcha bosqich.

---

## AI provider kalitlarini sozlash

Ilova ichida **"🔑 AI provider kalitlarini sozlash"** tugmasi orqali har
bir provider uchun API kalitni kiritish mumkin. Kalitlar
`EncryptedSharedPreferences` (androidx.security) orqali qurilmada
shifrlangan holda saqlanadi — kodning hech qayerida kalit yozilmagan.

Provider ustuvorlik tartibi (birinchisi ishlamasa, keyingisiga o'tiladi):
Gemini → OpenAI → OpenRouter → DeepSeek → Claude.

**Eslatma:** har bir providerning endpoint/model nomi kodda
(`ai/` papkasidagi fayllarda) kommentariya bilan ko'rsatilgan va
"yozilgan paytdagi ma'lumot" sifatida belgilangan — ishlatishdan oldin
har bir provayderning rasmiy hujjatidan joriy model nomini tekshiring.

---

## Testlarni ishga tushirish

```
gradle test
```

Bu faqat Android SDK/emulyator talab qilmaydigan, sof Kotlin mantiqni
(`CommandParser`, `ConfirmationManager`) tekshiradi. Accessibility Service
va Telegram navigatsiyasi uchun instrumentatsiya (`androidTest`) testlari
hali yozilmagan — bu keyingi ustuvor vazifa.

## Qo'lda sinash tartibi

1. APK'ni o'rnating (GitHub Actions artifaktidan yoki `gradle assembleDebug`).
2. DASE'ni oching, "Accessibility ruxsatlarini ochish" orqali xizmatni yoqing.
3. Mikrofon ruxsatini bering.
4. AI kalitini kiriting (sinash uchun ixtiyoriy).
5. "🎤 Buyruq gapirish" yoki matn maydoniga yozib "▶ Bajarish" orqali
   buyruq bering — masalan "Telegramni och".
6. Telegram navigatsiyasi/xabar yozish qismlari HALI PROTOTIP ekanini
   yodda tuting — kutilmagan xatti-harakat haqiqiy imkoniyat.

## Muhim xavfsizlik eslatmasi

DASE foydalanuvchi aniq "Yubor" (yoki unga mos tasdiq so'zi) demaguncha
hech qanday xabarni Telegramga yubormaydi (`ConfirmationManager`
holat mashinasi orqali ta'minlangan, tegishli unit testlar bilan
tekshirilgan).
