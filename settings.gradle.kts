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
        maven { url=uri("https://jitpack.io")}
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = "sk.eyJ1IjoiaXByb2JhYmlsaXNzaW1pMyIsImEiOiJjbTRsOXM1cDkxMGhiMmtyM3N1MHJjNHgyIn0.BbTuFcHNuFteXvY7GFXUrw"
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url=uri("https://jitpack.io")}
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = "sk.eyJ1IjoiaXByb2JhYmlsaXNzaW1pMyIsImEiOiJjbTRsOXM1cDkxMGhiMmtyM3N1MHJjNHgyIn0.BbTuFcHNuFteXvY7GFXUrw"
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

rootProject.name = "MobAlert"
include(":app")
 