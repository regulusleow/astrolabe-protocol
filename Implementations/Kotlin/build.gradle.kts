import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
}

group = "io.github.regulusleow"
version = providers.gradleProperty("astrolabeVersion").get()

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
}

sourceSets {
    test {
        resources.srcDir(rootProject.layout.projectDirectory.dir("Contract"))
    }
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral()
    if (!providers.gradleProperty("signingInMemoryKey").orNull.isNullOrBlank()) {
        signAllPublications()
    }
    coordinates(
        groupId = project.group.toString(),
        artifactId = "astrolabe-protocol-kotlin",
        version = project.version.toString()
    )

    pom {
        name.set("Astrolabe Protocol Kotlin")
        description.set("Platform-neutral Kotlin implementation of the Astrolabe Wire Protocol.")
        inceptionYear.set("2026")
        url.set("https://github.com/regulusleow/astrolabe-protocol")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("regulusleow")
                name.set("Regulus Leow")
                url.set("https://github.com/regulusleow")
            }
        }

        scm {
            url.set("https://github.com/regulusleow/astrolabe-protocol")
            connection.set("scm:git:https://github.com/regulusleow/astrolabe-protocol.git")
            developerConnection.set("scm:git:ssh://git@github.com/regulusleow/astrolabe-protocol.git")
        }
    }
}

val requiredCentralPublishingProperties = listOf(
    "mavenCentralUsername",
    "mavenCentralPassword",
    "signingInMemoryKey"
)

tasks.matching { it.name.contains("MavenCentral") }.configureEach {
    doFirst {
        val missingProperties = requiredCentralPublishingProperties.filterNot {
            !providers.gradleProperty(it).orNull.isNullOrBlank()
        }
        if (missingProperties.isNotEmpty()) {
            throw GradleException(
                "Missing Maven Central publishing properties: ${missingProperties.joinToString()}"
            )
        }
    }
}
