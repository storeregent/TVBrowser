/*
 * W0 validation harness — root build file.
 *
 * AGP 9.x has built-in Kotlin support and a documented runtime dependency on the
 * Kotlin Gradle plugin (KGP). The buildscript classpath entry below pins the KGP
 * version to the latest stable Kotlin at the time of work (2026-11-28), so that
 * the Compose compiler plugin and the kotlinx-serialization plugin (both versioned
 * with Kotlin) exactly match the Kotlin version used to compile the sources.
 *
 * See https://developer.android.com/build/releases/agp-9-0-0-release-notes#runtime-dependency-on-kotlin-gradle-plugin
 *
 * NOTE: keep this version in sync with `kotlin` in gradle/libs.versions.toml.
 */
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
