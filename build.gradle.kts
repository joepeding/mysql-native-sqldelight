group = "nl.joepeding.sqldelight"

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