val enableAndroid = gradle.startParameter.projectProperties["enableAndroid"]?.toBoolean() ?: true

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("org\\.jetbrains.*")
            }
        }
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.7.0"
        id("org.jetbrains.kotlin.android") version "1.9.22"
        id("com.google.dagger.hilt.android") version "2.51"
        id("com.google.devtools.ksp") version "1.9.22-1.0.17"
        id("com.google.gms.google-services") version "4.4.2"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Rivava"

if (enableAndroid) {
    include(":app")
} else {
    logger.lifecycle("Android module is disabled. Run with -PenableAndroid=true to include :app.")
}
