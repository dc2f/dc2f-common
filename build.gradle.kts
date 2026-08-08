import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val dc2fVersion = "0.0.2"

group = "com.dc2f"
if (version == "unspecified") {
    version = dc2fVersion
}

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    kotlin("jvm") version "2.4.10"

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    `maven-publish`
    signing
}

repositories {
    // jcenter() dropped: shut down in 2021 and removed in Gradle 9.
    mavenCentral()
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.1")

    implementation("io.github.microutils:kotlin-logging:1.4.9")
    implementation("com.github.ajalt:clikt:2.2.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.9")

    // 2.1.3's bundled dylib is x86_64 only, so `serve` died with an
    // UnsatisfiedLinkError on Apple Silicon. 2.1.12 ships a universal binary
    // (still under native/x86_64/, misleadingly, but it contains arm64).
    implementation("com.swoval:file-tree-views:2.1.12")

    // rss feeds for blog
    implementation("com.rometools:rome:1.12.2")

    api("com.dc2f:dc2f:$version")
    api("com.dc2f:dc2f-edit-api:$version")

//    implementation("com.github.vishna:watchservice-ktx:master-SNAPSHOT")


    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

tasks.withType<KotlinCompile> {
    // kotlinOptions was removed in Kotlin 2.2; compilerOptions replaces it, and
    // -Xjvm-default became -jvm-default ("no-compatibility" == the old "all").
    // "-Xuse-experimental" went in 1.6 and kotlin.Experimental itself in 1.9 --
    // RequiresOptIn is stable now, so that flag stays dropped.
    compilerOptions {
        freeCompilerArgs.add("-jvm-default=no-compatibility")
    }
}

// Pins both the Java and Kotlin targets together. Setting only
// compilerOptions.jvmTarget lets compileJava follow whatever JDK is running and
// Gradle then rejects the mismatch. Gradle 9 requires JDK 17 to run anyway.
kotlin {
    jvmToolchain(17)
}


val secretConfig = file("../dc2f.kt/_tools/secrets/build_secrets.gradle.kts")
if (secretConfig.exists()) {
    apply { from(secretConfig) }
    allprojects {
        extra["signing.secretKeyRingFile"] = "../dc2f.kt/" + extra["signing.secretKeyRingFile"]
    }
} else {
    println("Warning: Secrets do not exist, maven publish will not be possible.")
}

tasks.register<Jar>("sourcesJar") {
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}
tasks.register<Jar>("javadocJar") {
    from(tasks.javadoc)
    archiveClassifier.set("javadoc")
}

val repoName = "dc2f-common"
val projectName = repoName

publishing {
    publications {
        create<MavenPublication>("mavenCentralJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
//            artifact(sourcesJar.get())
            artifact(tasks["javadocJar"])


            pom {
                name.set(projectName)
                description.set("Type safe static website generator")
                url.set("https://github.com/dc2f/$repoName/")
//                properties.set(mapOf(
//                    "myProp" to "value",
//                    "prop.with.dots" to "anotherValue"
//                ))
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("hpoul")
                        name.set("Herbert Poul")
                        email.set("herbert@poul.at")
                    }
                }
                scm {
                    connection.set("scm:git:http://github.com/dc2f/$repoName.git")
                    developerConnection.set("scm:git:ssh://github.com/dc2f/$repoName.git")
                    url.set("https://github.com/dc2f/$repoName")
                }
            }

        }
    }
    repositories {
        maven {
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            println("using $url")
            credentials {
                username = project.properties["ossrhUsername"] as? String
                password = project.properties["ossrhPassword"] as? String
            }

        }
//        maven {
//            name = "github"
//            url = uri("https://maven.pkg.github.com/dc2f")
//            credentials {
//                username = project.properties["dc2f.github.username"] as? String
//                password = project.properties["dc2f.github.password"] as? String
//            }
//        }
    }
}

signing {
    sign(publishing.publications["mavenCentralJava"])
}
