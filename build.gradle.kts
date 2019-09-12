import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin library project to get you started.
 */

group = "com.dc2f"
version = "0.0.1-SNAPSHOT"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=enable")
}

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.3.41"

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://jitpack.io")

}

val jacksonVersion = "2.9.9"


dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("io.github.microutils:kotlin-logging:1.4.9")
    implementation("com.github.ajalt:clikt:2.1.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.9")

    implementation("com.swoval:file-tree-views:2.1.3")

    api("com.dc2f:dc2f:0.1.3-SNAPSHOT")
    api("com.dc2f:dc2f-edit-api:+")

    implementation("com.github.vishna:watchservice-ktx:master-SNAPSHOT")


    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}