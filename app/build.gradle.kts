import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}
val hasReleaseSigning = keystorePropertiesFile.exists()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
}

android {
    namespace = "com.feldman.flashlight"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.feldman.flashlight"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "beta1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        getByName("debug") {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_24)
        }
    }

}

dependencies {

    //Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.material.v1130)
    implementation(platform(libs.androidx.compose.bom.v20250801))

    //Jetpack Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.material3)

    //AndroidX Libraries
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.compose.adaptive.navigation3)

    //Motion design system: theme, navigation, settings scaffold, buttons
    implementation(libs.motion)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.serialization.json)

    //Coroutines
    implementation(libs.kotlinx.coroutines.android)

    //Testing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom.v20250801))

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)

}
