pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
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
        maven { url = uri("https://jitpack.io") }
        maven {
            url =uri ("https://maven.speedcheckerapi.com/artifactory/libs-release")
            credentials {
                username = providers.gradleProperty("speedchecker.username").getOrElse("defaultUsername")
                password = providers.gradleProperty("speedchecker.password").getOrElse("defaultPassword")
            }
        }
        maven { url = uri("https://repo.maven.apache.org/maven2") }
    }
}

rootProject.name = "MongoFS Terminal"
include(":app")
