plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.dase.assistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dase.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.5.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// ESLATMA: versiyalar shu loyiha yozilgan paytda (2026-yil boshi) barqaror
// deb hisoblangan qiymatlarga o'rnatilgan. Haqiqiy `gradle build` bu
// muhitda (tarmoqsiz sandbox) ishga tushirib ko'rilmadi, shuning uchun
// bog'liqliklarning muvaffaqiyatli yuklanishi GitHub Actions orqali
// tasdiqlanishi kerak. Agar versiya topilmasa, eng yaqin barqaror
// versiyaga yangilang: https://maven.google.com dan tekshirish mumkin.
dependencies {
    // Bildirishnoma (NotificationCompat) — DaseForegroundService uchun.
    implementation("androidx.core:core:1.13.1")

    // API kalitlarini shifrlangan holda saqlash (SecureKeyStore) uchun.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Sof-Kotlin unit testlar uchun (CommandParserTest, ConfirmationManagerTest).
    testImplementation("junit:junit:4.13.2")
}
