import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") // For Room
    id("com.google.gms.google-services")
}

android {
    namespace = "com.skinsense.ai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.skinsense.ai"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Enable multidex for Compose
        multiDexEnabled = true

        // Read API Key from local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }
        val apiKey = localProperties.getProperty("GOOGLE_AI_API_KEY") ?: ""
        buildConfigField("String", "GOOGLE_AI_API_KEY", "\"$apiKey\"")
        val groqApiKey = localProperties.getProperty("GROQ_API_KEY") ?: ""
        buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"")
        val imgbbApiKey = localProperties.getProperty("IMGBB_API_KEY") ?: ""
        buildConfigField("String", "IMGBB_API_KEY", "\"$imgbbApiKey\"")
        
        val razorpayApiKey = localProperties.getProperty("RAZORPAY_API_KEY") ?: ""
        buildConfigField("String", "RAZORPAY_API_KEY", "\"$razorpayApiKey\"")
        manifestPlaceholders["RAZORPAY_API_KEY"] = razorpayApiKey

        val paytmMid = localProperties.getProperty("PAYTM_MERCHANT_ID") ?: ""
        buildConfigField("String", "PAYTM_MERCHANT_ID", "\"$paytmMid\"")
        manifestPlaceholders["PAYTM_MERCHANT_ID"] = paytmMid
    }

    aaptOptions {
        noCompress.add("tflite")
        noCompress.add("lite")
    }

    ndkVersion = "27.0.12077973"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        mlModelBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // CameraX
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")

    // TensorFlow Lite (Play Services)
    implementation("com.google.android.gms:play-services-tflite-java:16.1.0")
    implementation("com.google.android.gms:play-services-tflite-support:16.1.0")
    implementation("org.tensorflow:tensorflow-lite-api:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") {
        exclude(group = "org.tensorflow", module = "tensorflow-lite")
    }
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4") {
        exclude(group = "org.tensorflow", module = "tensorflow-lite")
    }
    
    // MediaPipe Tasks Vision
    implementation("com.google.mediapipe:tasks-vision:0.10.14")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:${roomVersion}")
    implementation("androidx.room:room-ktx:${roomVersion}")
    ksp("androidx.room:room-compiler:${roomVersion}")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Compose Animation
    implementation("androidx.compose.animation:animation")
    
    // Compose Tooling (Debug)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Google AI SDK for Android (Generative AI)
    // Google AI SDK removed in favor of Direct API (HttpURLConnection)
    // implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Biometric & Security
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Paytm All-in-One SDK
    implementation("com.paytm.appinvokesdk:appinvokesdk:1.6.15")
    
    // Razorpay SDK
    implementation("com.razorpay:checkout:1.6.38")
}
