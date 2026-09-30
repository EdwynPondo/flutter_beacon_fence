import com.android.Version
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

group = "com.flutter.beacon_fence"
version = "1.0-SNAPSHOT"

buildscript {
    val kotlinVersion = "2.4.10"
    val agpVersion = "9.4.1"

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:$agpVersion")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.library")
    kotlin("plugin.serialization") version "2.2.0"
}

// AGP 9+ ships built-in Kotlin support and enables it by default; older
// versions need the Kotlin Gradle Plugin (KGP) applied explicitly. Flutter
// apps set `android.builtInKotlin=false` in gradle.properties
// (flutter/flutter#183910) to keep the legacy KGP path working for
// unmigrated plugins, so this module must apply KGP itself unless built-in
// Kotlin is active. See #710 and #722.
val agpMajor = Version.ANDROID_GRADLE_PLUGIN_VERSION.split(".")[0].toIntOrNull() ?: 0
val builtInKotlin = (agpMajor >= 9) && (project.findProperty("android.builtInKotlin")?.toString() != "false")
if (!builtInKotlin) {
    apply(plugin = "kotlin-android")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

android {
    namespace = "com.flutter.beacon_fence"
    compileSdk = (project.findProperty("flutter.compileSdkVersion") as? String)?.toIntOrNull() ?: 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    sourceSets {
        getByName("main").java.directories.add("src/main/kotlin")
        getByName("test").java.directories.add("src/test/kotlin")
    }

    defaultConfig {
        minSdk = 24
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()

            it.testLogging {
                events("passed", "skipped", "failed", "standardOut", "standardError")
                it.outputs.upToDateWhen { false }
                showStandardStreams = true
            }
        }
    }
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.mockito:mockito-core:5.24.0")
    implementation("com.google.android.gms:play-services-location:21.4.0")
    implementation("com.google.guava:guava:33.7.2-android")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("com.davidgyoungtech:beacon-parsers:1.0")
    implementation("org.altbeacon:android-beacon-library:2.21.2")
}
