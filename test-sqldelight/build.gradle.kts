import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    alias(libs.plugins.sqldelight) apply true
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
    when (HostManager.host) {
        KonanTarget.LINUX_X64 -> linuxX64()
        KonanTarget.LINUX_ARM64 -> linuxArm64()
        KonanTarget.MACOS_ARM64 -> macosArm64()
        KonanTarget.MACOS_X64 -> macosX64()
        else -> error("Not supported")
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.mysqlNativeDriver)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.coroutines.test)
                implementation(libs.sqldelight.runtime)
                implementation(libs.stately.concurrency)
            }
        }
    }
}

sqldelight {
    databases.register("NativeMySQL") {
        dialect(projects.mysqlNativeDialect)
        packageName.set("nl.joepeding.sqldelight.testmysql")
        deriveSchemaFromMigrations.set(true)
    }
    linkSqlite.set(false)
}