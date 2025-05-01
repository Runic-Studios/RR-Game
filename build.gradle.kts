import com.ncorti.ktfmt.gradle.tasks.KtfmtCheckTask
import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ktfmt)
}

group = "com.runicrealms.game.plugin"

version = "3.0"

repositories { mavenCentral() }

subprojects {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://nexus.runicrealms.com/repository/maven-public/")
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")

    kotlin { compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") } }

    java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

    dependencies {
        // Kotlin
        implementation(rootProject.libs.kotlin.stdlib)
        implementation(rootProject.libs.kotlin.test)

        // Paper
        compileOnly(rootProject.libs.paper.api)

        // Configuration and Injection
        implementation(rootProject.libs.guice)
        implementation(rootProject.libs.jackson.kotlin)
        implementation(rootProject.libs.jackson.databind)

        // Trove
        implementation(rootProject.libs.trove.client)
        implementation(rootProject.libs.grpc.kotlinstub)
        implementation(rootProject.libs.grpc.netty)
        implementation(rootProject.libs.grpc.protobuf)
        implementation(rootProject.libs.protobuf.java)

        // Coroutines
        implementation(rootProject.libs.mccoroutine.api)
        implementation(rootProject.libs.mccoroutine.core)
        implementation(rootProject.libs.kotlinx.coroutine)

        // Velagones
        implementation(rootProject.libs.velagones.paper)
    }
}

ktfmt {
    kotlinLangStyle()
    srcSetPathExclusionPattern = Regex(".*generated.*")
}

tasks.withType<KtfmtFormatTask>().configureEach {
    source = project.fileTree(rootDir)
    include("**/*.kt")
    exclude("**/generated/**")
}

tasks.withType<KtfmtCheckTask>().configureEach {
    source = project.fileTree(rootDir)
    include("**/*.kt")
    exclude("**/generated/**")
}
