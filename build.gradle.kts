plugins {
    kotlin("multiplatform") version "1.8.21"
}

group = "nl.joepeding"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosArm64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }
    println("HIERHIERHIER\n$hostOs")

    nativeTarget.apply {
        binaries {
            executable {
//                entryPoint = "nl.joepeding.mysqldriver.main"
            }
        }
        compilations["main"].cinterops {
            val mysql by creating
        }
    }
    sourceSets {
        val commonMain by getting
        val commonTest by getting
    }
}
