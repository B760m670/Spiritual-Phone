plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.spiritualphone.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.spiritualphone.app"
        minSdk = 26
        targetSdk = 35
        // Overridden in CI: -PversionCode=<run> -PversionName=0.1.<run>
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "0.1.0"
    }

    // Release signing key is provided via the environment (GitHub Secrets,
    // decoded into a file in CI) and never committed. All CI builds share this
    // one key, which is required for in-app updates to install over each other.
    // Local builds without these vars produce an unsigned release.
    val keystoreFile = System.getenv("KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug { }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // MapLibre — free, no API key, schematic vector map
    implementation("org.maplibre.gl:android-sdk:11.5.2")

    // ZXing core — pure-Java QR generation (no Android deps, no network)
    implementation("com.google.zxing:core:3.5.3")

    // Haze — real backdrop blur (RenderEffect/AGSL) for the Liquid-Glass look.
    // 1.1.0 is the last release built against Kotlin 2.0.21 / Compose 1.7.
    implementation("dev.chrisbanes.haze:haze:1.1.0")

    // ProfileInstaller — applies the bundled baseline profile (AOT-compiles the
    // startup hot path at install) so first runs render without JIT warm-up
    // jank, and enables Play Store cloud profiles.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    // CameraX — live camera preview for the AR section.
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")
}
