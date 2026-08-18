pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Requis pour libsu (com.github.topjohnwu.libsu)
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "FileRescue Libre"
include(":app")
