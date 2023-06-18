plugins {
    kotlin("multiplatform") version "1.8.21"
}

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
    }
}
