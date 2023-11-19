group = "nl.joepeding"
version = "0.1.0-SNAPSHOT"

plugins {
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.grammarKitComposer) apply false
    alias(libs.plugins.dokka) apply true
}

allprojects {
    repositories {
        mavenCentral()
    }
}