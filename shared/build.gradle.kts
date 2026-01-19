import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.maven.publish)
}

kotlin {
    explicitApi()

    androidLibrary {
        namespace = "io.github.speak2me.kmp.curl.logging"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        compilerOptions
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    iosArm64()
    iosSimulatorArm64()
    
    jvm()
    
    js {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    sourceSets {
        commonMain.dependencies {
            // put your Multiplatform dependencies here
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.test.host)
            implementation(libs.ktor.client.tests)
        }
        jvmTest.dependencies {
            implementation(libs.ktor.serialization.jackson)
            implementation(libs.ktor.client.encoding)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}
