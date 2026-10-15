plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.doomx.optimizer"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.doomx.optimizer"
        minSdk = 23
        targetSdk = 35
        versionCode = 6
        versionName = "2.6"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
