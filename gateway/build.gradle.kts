plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.sohaib.callbridge.gateway"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.sohaib.callbridge.gateway"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }
}
