buildscript {
    dependencies {
        classpath(libs.google.services)
        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.6")
    }
    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20" apply true
    id("com.google.firebase.firebase-perf") version "2.0.1" apply false
    id("androidx.navigation.safeargs.kotlin") version "2.9.3" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
    id ("com.google.firebase.crashlytics") version "3.0.6" apply false
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
    }
}
