plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlinx-serialization")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.baytro"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.baytro"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    kotlin {
        jvmToolchain(17)
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.ai)
    implementation(libs.firebase.auth)

    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.android.gms:play-services-base:18.2.0")
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.kotlinx.coroutines.android)
    implementation(project.dependencies.platform("io.insert-koin:koin-bom:4.1.1"))
    implementation(libs.io.insert.koin.koin.core)
    implementation(libs.insert.koin.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose.viewmodel.navigation)
    implementation(libs.androidx.navigation.compose)

    implementation (libs.androidx.compose.material.icons.core)
    implementation (libs.androidx.compose.material.icons.extended)

    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")

    implementation("dev.gitlive:firebase-firestore:2.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    // Coroutines for Firebase
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    // https://mvnrepository.com/artifact/com.github.yalantis/ucrop
    implementation("com.github.yalantis:ucrop:2.2.6")
}