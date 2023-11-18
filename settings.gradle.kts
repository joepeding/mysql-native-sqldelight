pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "mysql-native-sqldelight"

include(":mysql-native-dialect")
include(":mysql-native-driver")
include(":test-sqldelight")