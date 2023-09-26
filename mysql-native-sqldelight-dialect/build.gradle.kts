plugins {
    alias(libs.plugins.kotlin.jvm) apply true
    alias(libs.plugins.grammarKitComposer) apply true
}

dependencies {
    implementation(libs.kotlin.datetime)
    api(libs.sqldelight.dialect.api)
    api(libs.sqldelight.mysql.dialect)
    compileOnly(libs.sqldelight.compiler.env)
}

kotlin {
    jvmToolchain(17)
}