pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "mysql-native-sqldelight"

include(":mysql-native-sqldelight-dialect")
include(":mysql-native-sqldelight-driver")
include(":test-sqldelight")