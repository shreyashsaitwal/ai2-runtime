@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.library")
    id("maven-publish")
    id("signing")
}

group = "io.github.shreyashsaitwal.rush"
version = "nb190b.1"

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 16
        targetSdk = 33
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

repositories {
    google()
    mavenCentral()
    mavenLocal()
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
    implementation("org.twitter4j:twitter4j-core:3.0.5")

    implementation("org.pepstock:charba:2.5")

    implementation("com.google.protobuf:protobuf-java:3.0.0")
    implementation("com.google.api-client:google-api-client:1.22.0")
    implementation("com.google.api.client:google-api-client-extensions-android2:1.4.1-beta")
    implementation("com.google.apis:google-api-services-sheets:v4-rev604-1.25.0")
    implementation("com.google.apis:google-api-services-fusiontables:v1-rev20171117-1.26.0")

    implementation("io.github.shreyashsaitwal.rush:annotations:2.0.1")

    implementation(fileTree("libs"))
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "OSSRH"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = project.properties["ossrh.username"] as String
                    password = project.properties["ossrh.password"] as String
                }
            }
        }

        publications {
            create<MavenPublication>("maven") {
                artifactId = "runtime"
                from(components["release"])

                pom {
                    name.set("AI2 runtime")
                    description.set("App Inventor runtime module tailored for extensions built with Rush.")
                    url.set("https://github.com/shreyashsaitwal/ai2-runtime")

                    licenses {
                        license {
                            name.set("Apache License, Version 2.0")
                            url.set("https://raw.githubusercontent.com/shreyashsaitwal/ai2-runtime/main/LICENSE")
                        }
                    }

                    developers {
                        developer {
                            id.set("MIT App Inventor authors")
                            name.set("mit-appinventor-authors")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/shreyashsaitwal/ai2-runtime.git")
                        developerConnection.set("scm:git:ssh://github.com/shreyashsaitwal/ai2-runtime.git")
                        url.set("https://github.com/shreyashsaitwal/rush-cli")
                    }
                }
            }
        }
    }

    signing {
        sign(publishing.publications["maven"])
    }
}
