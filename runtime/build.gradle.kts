@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.library")
}

android {
    compileSdk = 32

    defaultConfig {
        minSdk = 16
        targetSdk = 32
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_7
        targetCompatibility = JavaVersion.VERSION_1_7
    }

    lint {
        abortOnError = false
    }
}

repositories {
    google()
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
    jcenter() // For "org.webrtc:google-webrtc:1.0.23995"
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.0.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.0.0")

    implementation("org.apache.httpcomponents:httpmime:4.3.4")
    implementation("org.locationtech.jts:jts-core:1.16.1")

    implementation("redis.clients:jedis:3.1.0")
    implementation("com.caverock:androidsvg:1.2.1")
    implementation("com.firebase:firebase-client-android:2.5.2")
    implementation("org.webrtc:google-webrtc:1.0.23995")
    implementation("org.twitter4j:twitter4j-core:3.0.5")

    implementation("org.pepstock:charba:2.5")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation("com.google.protobuf:protobuf-java:3.0.0")
    implementation("com.google.api-client:google-api-client:1.22.0")
    implementation("com.google.api.client:google-api-client-extensions-android2:1.4.1-beta")
    implementation("com.google.apis:google-api-services-sheets:v4-rev604-1.25.0")
    implementation("com.google.apis:google-api-services-fusiontables:v1-rev20171117-1.26.0")

    implementation("io.github.shreyashsaitwal.rush:annotations:2.0.0-beta07")

    implementation(fileTree("libs"))
}

