import com.android.build.api.variant.BuildConfigField
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Build info injected into BuildConfig so every session log identifies the exact build (brief §3).
val gitSha: String = runCatching {
    val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
        .directory(rootDir) // git walks up from prototype/w0-harness to the repository root
        .redirectErrorStream(true)
        .start()
    process.inputStream.bufferedReader().readText().trim().take(7).ifBlank { "unknown" }.also { process.waitFor() }
}.getOrDefault("unknown")

val buildTime: String = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

android {
    namespace = "dev.tvbrowse.w0harness"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.tvbrowse.w0harness"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.0-m2"
    }

    flavorDimensions.add("mode")
    productFlavors {
        create("standard") {
            dimension = "mode"
            // applicationId dev.tvbrowse.w0harness, OnBackPressedDispatcher-only back handling.
        }
        create("alt") {
            dimension = "mode"
            applicationIdSuffix = ".alt"
            versionNameSuffix = "-alt"
            // Manifest overlay (src/alt/AndroidManifest.xml) opts out of OnBackInvokedCallback
            // (android:enableOnBackInvokedCallback="false") and disables WebView Safe Browsing
            // via manifest meta-data. Used for T-15 / T-07 comparison only.
        }
    }

    signingConfigs {
        // CI optional signing: if W0H_DEBUG_STORE_FILE is set (by .github/workflows/w0-harness.yml
        // when the W0H_DEBUG_KEYSTORE_B64 secret exists), debug builds are signed with it so that
        // successive CI builds install over each other. Otherwise the runner's default debug key
        // is used and each CI build must be uninstalled before installing (signature changes).
        val storeFileEnv = System.getenv("W0H_DEBUG_STORE_FILE")
        if (!storeFileEnv.isNullOrBlank()) {
            create("w0hCiDebug") {
                storeFile = file(storeFileEnv)
                storePassword = System.getenv("W0H_DEBUG_STORE_PASSWORD") ?: "w0h-debug"
                keyAlias = System.getenv("W0H_DEBUG_KEY_ALIAS") ?: "w0h-debug"
                keyPassword = System.getenv("W0H_DEBUG_KEY_PASSWORD") ?: "w0h-debug"
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.findByName("w0hCiDebug") ?: signingConfigs.getByName("debug")
        }
        getByName("release") {
            // Debug-only validation harness: release builds are not built in CI (brief §7).
            isMinifyEnabled = false
        }
        // H-6 / F-H5 amendment (HARNESS_BRIEF §7): representative cold-start build for
        // T-18. Non-debuggable, R8 on, profileable (src/benchmark/AndroidManifest.xml),
        // `.bench` id suffix so it installs side by side, signed with the same CI debug
        // key. Uploaded as a CI artifact only — no Play upload, no publishing.
        create("benchmark") {
            initWith(getByName("release"))
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            matchingFallbacks += listOf("release")
            applicationIdSuffix = ".bench"
            signingConfig = signingConfigs.findByName("w0hCiDebug") ?: signingConfigs.getByName("debug")
        }
    }

    lint {
        // benchmark initWith(release) would run lintVital and fail on unused-resource
        // style issues from the release-like config; release builds are not produced.
        checkReleaseBuilds = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// AGP 9 new DSL: build info goes in via the variant API (buildConfigField DSL was removed).
androidComponents {
    onVariants { variant ->
        variant.buildConfigFields?.put("GIT_SHA", BuildConfigField("String", "\"$gitSha\"", null))
        variant.buildConfigFields?.put("BUILD_TIME", BuildConfigField("String", "\"$buildTime\"", null))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.webkit)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.tv.material)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
