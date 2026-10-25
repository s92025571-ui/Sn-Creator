import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Apply Google Services plugin automatically when app/google-services.json is provided locally or via CI secrets.
// Keeping google-services.json out of version control prevents committing Firebase project configuration secrets,
// while allowing debug APK compilation and unit test runs in clean GitHub Actions checkouts.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val localProps = Properties().apply {
    val propFile = rootProject.file("local.properties")
    if (propFile.exists()) {
        propFile.inputStream().use { load(it) }
    }
}

fun resolveConfig(key: String, defaultValue: String = ""): String {
    val fromGradleProp = providers.gradleProperty(key).orNull
    val fromEnv = System.getenv(key)
    val fromLocal = localProps.getProperty(key)
    return (fromGradleProp ?: fromEnv ?: fromLocal ?: defaultValue).trim()
}

val backendBaseUrl = resolveConfig("SN_BACKEND_BASE_URL", "")
val firebaseWebClientId = resolveConfig("FIREBASE_WEB_CLIENT_ID", "")
val contactWhatsappNumber = resolveConfig("SN_CONTACT_WHATSAPP_NUMBER", "")

android {
    namespace = "com.sncreator.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sncreator.app"
        // minSdk 26 (Android 8.0) covers 97%+ of active Android devices, provides native java.time
        // for Asia/Karachi timezone logic, supports adaptive launcher icons, and satisfies Credential Manager + Firebase Auth.
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BACKEND_BASE_URL", "\"${backendBaseUrl.replace("\"", "\\\"")}\"")
        buildConfigField("String", "FIREBASE_WEB_CLIENT_ID", "\"${firebaseWebClientId.replace("\"", "\\\"")}\"")
        buildConfigField("String", "SUPPORT_WHATSAPP_URL", "\"https://wa.me/923288501635\"")
        buildConfigField("String", "CONTACT_WHATSAPP_NUMBER", "\"${contactWhatsappNumber.replace("\"", "\\\"")}\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ""
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    // Firebase Authentication (Spark no-billing plan compatible)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    // Android Credential Manager for modern Google Sign-In
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Networking & JSON Serialization (for authenticated backend calls)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
