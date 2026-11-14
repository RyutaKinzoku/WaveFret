plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.wavefret"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.wavefret"
        minSdk = 24
        targetSdk = 37
        versionCode = 16
        versionName = "0.14.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    androidResources {
        // The transcription model is memory-mapped, which needs it stored uncompressed.
        noCompress += "tflite"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation(libs.androidx.recyclerview)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.litert)
    implementation(libs.mlkit.text.recognition)
}

// Android Studio's "Make Project" asks for these aggregate tasks, which the
// Android Gradle Plugin 9 no longer creates; they compile the test sources.
tasks.register("unitTestClasses") { dependsOn("compileDebugUnitTestSources") }
tasks.register("androidTestClasses") { dependsOn("compileDebugAndroidTestSources") }
