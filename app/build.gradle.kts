import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val local = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
fun config(name: String, fallback: String): String =
    (local.getProperty(name) ?: fallback).trimEnd('/').replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "org.houhackathon.opencity"
    compileSdk = 35
    defaultConfig {
        applicationId = "org.houhackathon.opencity"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        manifestPlaceholders["appAuthRedirectScheme"] = "opencity"
        buildConfigField("String", "IDENTITY_URL", "\"${config("IDENTITY_URL", "http://127.0.0.1:8081")}\"")
        buildConfigField("String", "GATEWAY_URL", "\"${config("GATEWAY_URL", "http://127.0.0.1:8000")}\"")
    }
    buildTypes { release { isMinifyEnabled = false } }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("net.openid:appauth:0.11.1")
    implementation("org.maplibre.gl:android-sdk:11.6.1")
}
