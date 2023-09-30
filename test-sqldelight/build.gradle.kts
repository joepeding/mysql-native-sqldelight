import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

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
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosArm64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.mysqlNativeSqldelightDriver)
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
        dialect(projects.mysqlNativeSqldelightDialect)
        packageName.set("nl.joepeding.sqldelight.testmysql")
        deriveSchemaFromMigrations.set(true)
    }
    linkSqlite.set(false)
}