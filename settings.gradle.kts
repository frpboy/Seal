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
        google()
        mavenCentral()
        mavenLocal()
    }
}
rootProject.name = "Seali"
include (":app")
include(":color")
include(":frpboy-common")
project(":frpboy-common").projectDir = file("dependencies/frpboy-common")
include(":frpboy-library")
project(":frpboy-library").projectDir = file("dependencies/frpboy-library")
include(":frpboy-ffmpeg")
project(":frpboy-ffmpeg").projectDir = file("dependencies/frpboy-ffmpeg")

include(":frpboy-mmkv")
project(":frpboy-mmkv").projectDir = file("dependencies/frpboy-mmkv")
include(":frpboy-mmkvannotation")
project(":frpboy-mmkvannotation").projectDir = file("dependencies/frpboy-mmkvannotation")
