# DASE — 3-bosqich

## Qo‘shilgan imkoniyatlar

- “Dase” so‘zini aniqlash prototipi
- Ilova ochiq turganda takroriy tinglash
- “Dase” aniqlangach keyingi ovozli buyruqni qabul qilish
- Ovozli javob
- Bitta buyruqni alohida tinglash
- GitHub Actions orqali APK build

## Sinash

1. APK’ni o‘rnating.
2. Mikrofon ruxsatini bering.
3. “Dase” rejimini yoqing.
4. “Dase” deb ayting.
5. DASE “Ha, tinglayapman” deb javob beradi.
6. Keyingi buyruqni ayting.

## Muhim cheklov

Bu haqiqiy offline wake-word modeli emas. Android SpeechRecognizer telefonning mavjud nutq tanish xizmatidan foydalanadi va ko‘pincha internetga bog‘liq bo‘lishi mumkin.

Bu bosqich ilova ochiq turganda ishlaydi. Qulf ekranida yoki doimiy fonda ishlash hali qo‘shilmagan. Keyingi bosqichda foreground service, bildirishnoma va Android/MIUI fon cheklovlari bilan ishlash alohida qo‘shiladi.
