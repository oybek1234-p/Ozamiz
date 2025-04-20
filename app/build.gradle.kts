plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")

    id("com.google.firebase.crashlytics")
}

android {
    namespace = "uz.oybek.ozamiz"
    compileSdk = 34

    defaultConfig {
        applicationId = "uz.oybek.ozamiz"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-common-ktx:2.8.1")
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("com.google.firebase:firebase-firestore:25.1.0")
    implementation("com.google.firebase:firebase-inappmessaging-display:21.0.0")
    implementation("com.google.firebase:firebase-messaging:24.0.1")
    implementation("com.google.firebase:firebase-storage:21.0.0")
    implementation("com.google.firebase:firebase-analytics:22.1.0")
    implementation(libs.play.services.auth)

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    val nav_version = "2.8.1"
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")

    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))

    implementation("com.github.vacxe:phonemask:1.0.5")
    implementation("com.google.firebase:firebase-auth")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug:18.0.0")
   // implementation("com.google.android.gms:play-services-ads:23.4.0")
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:review:2.0.1")

    implementation ("com.google.android.flexbox:flexbox:3.0.0")
    implementation ("com.github.stfalcon-studio:StfalconImageViewer:v1.0.1")
    implementation ("jp.wasabeef:glide-transformations:4.3.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")

    implementation("com.airbnb.android:lottie:6.0.0")
    implementation ("id.zelory:compressor:3.0.1")
    implementation ("androidx.core:core-splashscreen:1.0.1")

    implementation("io.coil-kt:coil:2.6.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("com.google.android.gms:play-services-base:18.2.0")
    implementation ("com.github.ozcanalasalvar.picker:wheelview:2.0.7")
    implementation ("com.github.ozcanalasalvar.picker:datepicker:2.0.7")
}