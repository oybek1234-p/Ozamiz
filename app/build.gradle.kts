plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
    id("com.google.firebase.crashlytics")
    id("androidx.navigation.safeargs.kotlin")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "uz.oybek.ozamiz"
    compileSdk = 37

    packagingOptions {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
        }
    }

    defaultConfig {
        applicationId = "uz.oybek.ozamiz"
        minSdk = 26
        targetSdk = 37
        versionCode = 105
        versionName = "11"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations.addAll(listOf("en", "ru", "uz"))
    }
    
    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            ndk {
                abiFilters.add("arm64-v8a")
            }
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
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
        viewBinding = true
        dataBinding = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-common-ktx:2.8.1")
    implementation("com.google.firebase:firebase-database:22.0.0")
    implementation("com.google.firebase:firebase-firestore:26.0.0")
    implementation("com.google.firebase:firebase-messaging:24.0.1")
    implementation("com.google.firebase:firebase-storage:21.0.0")
    implementation("com.google.firebase:firebase-analytics:22.1.0")
    implementation(libs.play.services.auth)
    implementation("com.airbnb.android:lottie:6.6.7")
    implementation("com.github.Dimezis:BlurView:version-2.0.6")
    implementation("io.github.chaosleung:pinview:1.4.4")
    implementation("com.tbuonomo:dotsindicator:5.1.0")
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-auth")

    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug:19.0.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("jp.wasabeef:glide-transformations:4.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.airbnb.android:lottie:6.6.7")
    implementation("id.zelory:compressor:3.0.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.paging.common.android)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.common)
    implementation(libs.work.runtime.ktx)

    val nav_version = "2.8.1"
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-beta01")
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:review:2.0.2")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.google.android.gms:play-services-base:18.7.2")
    implementation("com.github.ozcanalasalvar.picker:wheelview:2.0.7")
    implementation("com.github.ozcanalasalvar.picker:datepicker:2.0.7")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.2")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    implementation(libs.google.firebase.ai)
    implementation("com.google.firebase:firebase-functions")
    implementation("com.facebook.android:facebook-android-sdk:latest.release")
    val cameraxVersion = "1.4.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("com.google.mlkit:face-detection:16.1.7")
    implementation("com.github.deano2390:MaterialShowcaseView:1.3.4")
    val room_version = "2.8.4"
    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    implementation("com.yandex.android:mobileads:8.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
