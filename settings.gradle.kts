pluginManagement {
    repositories {
        google() // Voeg expliciet de Google repository toe
        maven {
            url = uri("https://maven.google.com")
        }
        maven {
            url = uri("https://jcenter.bintray.com")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "TrackerMobilePrivate"
include(":app")
