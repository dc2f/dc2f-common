import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Shared with dc2f.kt and dc2f-edit-api: the api() dependencies below pin both
// siblings to this module's own `$version`, so a release only resolves if all
// three carry the same number. 0.3.0 is the first one ahead of the 0.2.1 that
// has been on Central since 2019. A release overrides it: -Pversion=0.3.0.
val dc2fVersion = "0.3.0-SNAPSHOT"

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
    // Was oss.sonatype.org's public group, which 404s since OSSRH's EOL. Its
    // half that mavenCentral() does not already cover is the snapshot repo.
    maven("https://central.sonatype.com/repository/maven-snapshots/")
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
            // Central validates the POM without resolving BOMs, so every
            // dependency has to carry its own version. The kotlin-bom platform
            // moves them into dependencyManagement instead, which failed the
            // first 0.3.0 deployment on kotlin-stdlib-jdk8, kotlin-reflect and
            // the bom import. This writes the versions Gradle actually resolved.
            versionMapping {
                usage("java-api") { fromResolutionResult() }
                usage("java-runtime") { fromResolutionResult() }
            }
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
            name = "central"
            // OSSRH was decommissioned on 2025-06-30; this is Sonatype's
            // replacement for a maven-publish setup -- the same deploy layout
            // in front of the Central Portal, authenticated with a Portal user
            // token. See dc2f.kt's build.gradle.kts and _tools/central-publish.sh:
            // a release staged here is invisible in the Portal until that script
            // posts it, from the same IP that uploaded.
            val releasesRepoUrl = "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://central.sonatype.com/repository/maven-snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            println("using $url")
            credentials {
                username = (project.findProperty("centralUsername")
                    ?: System.getenv("CENTRAL_TOKEN_USERNAME")
                    ?: project.findProperty("ossrhUsername")) as? String
                password = (project.findProperty("centralPassword")
                    ?: System.getenv("CENTRAL_TOKEN_PASSWORD")
                    ?: project.findProperty("ossrhPassword")) as? String
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
    // Away from this checkout there is no gpg keyring and no decrypted
    // build_secrets.gradle.kts next door; the armoured key arrives in
    // SIGNING_KEY instead.
    val signingKey = System.getenv("SIGNING_KEY")
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, System.getenv("SIGNING_PASSWORD"))
    }
    sign(publishing.publications["mavenCentralJava"])
}
