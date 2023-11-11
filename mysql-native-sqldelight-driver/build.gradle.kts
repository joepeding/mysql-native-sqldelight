import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply true
}

tasks.withType(AbstractTestTask::class.java).configureEach {
    testLogging {
        info.events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)

        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStandardStreams = true
        showStackTraces = true
    }
}

kotlin {
    fun KotlinNativeTarget.registerCinterop() {
        compilations.named("main") {
            cinterops {
                register("mysql") {
                    defFile(project.file("src/nativeInterop/cinterop/mysql.def"))
                }
            }
        }
    }

    when (HostManager.host) {
        KonanTarget.LINUX_X64 -> linuxX64 { registerCinterop() }
        KonanTarget.LINUX_ARM64 -> linuxArm64 { registerCinterop() }
        KonanTarget.MACOS_ARM64 -> macosArm64 { registerCinterop() }
        KonanTarget.MACOS_X64 -> macosX64 { registerCinterop() }
        else -> error("Not supported")
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }

        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.datetime)
                implementation(libs.kermit)
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

