pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            name = "isolatedReviewedFrpBoyStaging"
            url = uri(rootDir.resolve(".qualification/maven"))
            content { includeGroup("io.github.frpboy.youtubedl-android") }
        }
        google()
        mavenCentral()
        mavenLocal()
    }
}
rootProject.name = "Seali"
include (":app")
include(":color")

include(":frpboy-mmkv")
project(":frpboy-mmkv").projectDir = file("dependencies/frpboy-mmkv")
include(":frpboy-mmkvannotation")
project(":frpboy-mmkvannotation").projectDir = file("dependencies/frpboy-mmkvannotation")
