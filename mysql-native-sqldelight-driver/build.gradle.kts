import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

repositories {
    mavenCentral()
}

tasks.withType(AbstractTestTask::class.java).configureEach {
    testLogging {
        info.events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)

        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
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
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }

        val commonMain by getting {
            dependencies {
                api(libs.sqldelight.runtime)
            }
        }
        val commonTest by getting {
            dependencies {
                api(libs.sqldelight.runtime)
            }
        }
    }
}
