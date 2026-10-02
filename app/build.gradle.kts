plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"

}

kotlin {
    jvmToolchain(21)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

android {
    namespace = "ai.aminrezaei.dataloggerapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "ai.aminrezaei.dataloggerapp"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
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
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android.v160)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.converter.scalars)



    implementation (libs.androidx.material.icons.extended.v164)
    implementation(libs.androidx.work.runtime.ktx)

    implementation ("com.google.accompanist:accompanist-permissions:0.20.2")

    implementation ("com.google.android.gms:play-services-location:21.3.0")

    implementation (libs.androidx.runtime.livedata.v165)


    implementation ("androidx.room:room-runtime:2.6.1")

    ksp ("androidx.room:room-compiler:2.6.1")

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation ("androidx.room:room-ktx:2.6.1")

    // optional - Test helpers
    testImplementation ("androidx.room:room-testing:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.5.2")


    // Hilt dependencies
    implementation("com.google.dagger:hilt-android:2.44")

    // Navigation
    implementation ("androidx.navigation:navigation-compose:2.5.3")

    // Animation
    implementation ("androidx.compose.animation:animation:1.4.3")

    // Accompanist for animated navigation
    implementation ("com.google.accompanist:accompanist-navigation-animation:0.28.0")

    // Lottie
    implementation ("com.airbnb.android:lottie-compose:5.2.0")


}
