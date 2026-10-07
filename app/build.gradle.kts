@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ah.taplock"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ah.taplock"
        minSdk = 31
        targetSdk = 37
        versionCode = 23
        versionName = "1.18"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
    androidResources {
        generateLocaleConfig = true
    }
}

// Export evaluated values, so CI never guesses or increments the release version.
tasks.register("writeReleaseMetadata") {
    val metadataFile = layout.buildDirectory.file("release-metadata.json")
    val releaseName = android.defaultConfig.versionName
    val releaseCode = android.defaultConfig.versionCode
    val packageName = android.defaultConfig.applicationId
    val sdk = android.compileSdk
    doLast {
        val output = metadataFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(
            """{"versionName":"$releaseName","versionCode":$releaseCode,"packageName":"$packageName","compileSdk":$sdk}"""
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.fastlane.screengrab)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
