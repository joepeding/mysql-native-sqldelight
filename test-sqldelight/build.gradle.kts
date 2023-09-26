plugins {
    alias(libs.plugins.sqldelight) apply true
    alias(libs.plugins.kotlin.multiplatform) apply true
}
//dependencies {
//    commonMainImplementation(project(mapOf("path" to ":mysql-native-sqldelight-driver")))
//}

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