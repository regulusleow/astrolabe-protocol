pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "astrolabe-protocol"
include(":AstrolabeProtocolKotlin")
project(":AstrolabeProtocolKotlin").projectDir = file("Implementations/Kotlin")
