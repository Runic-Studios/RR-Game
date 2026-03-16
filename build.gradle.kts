import com.ncorti.ktfmt.gradle.tasks.KtfmtCheckTask
import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktfmt)
}

group = "com.runicrealms.game.plugin"

version = "0.1.0"

repositories { mavenCentral() }

subprojects {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://reposilite.runicrealms.com/releases/")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://repo.aikar.co/content/groups/aikar/")
        maven("https://repo.fancyinnovations.com/releases")
        maven("https://mvn.lumine.io/repository/maven-public/")
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    kotlin { compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") } }

    java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

    dependencies {
        // Kotlin
        implementation(rootProject.libs.kotlin.stdlib)
        implementation(rootProject.libs.kotlin.test)
        implementation(rootProject.libs.kotlinx.serialization.core)
        implementation(rootProject.libs.kotlinx.serialization.cbor)

        // Paper
        compileOnly(rootProject.libs.paper.api)

        // Configuration and Injection
        implementation(rootProject.libs.guice)
        implementation(rootProject.libs.guice.assistedinject)
        implementation(rootProject.libs.jackson.kotlin)
        implementation(rootProject.libs.jackson.databind)
        implementation(rootProject.libs.jackson.dataformat.yaml)

        // MongoDB
        implementation(rootProject.libs.mongodb.driver)
        implementation(rootProject.libs.mongodb.bson)

        // Coroutines
        implementation(rootProject.libs.mccoroutine.api)
        implementation(rootProject.libs.mccoroutine.core)
        implementation(rootProject.libs.kotlinx.coroutine)

        // Shaded dependencies
        implementation(rootProject.libs.adventure.legacy)
        implementation(rootProject.libs.aikar.commands)
        implementation(rootProject.libs.odalitamenus)

        // Plugin dependencies
        compileOnly(rootProject.libs.velagones.paper)
        compileOnly(rootProject.libs.nbtapi)
        compileOnly(rootProject.libs.protocollib)
        compileOnly(rootProject.libs.fancyHolograms)
        compileOnly(rootProject.libs.mythicMobs)
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
