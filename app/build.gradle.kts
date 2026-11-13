@file:Suppress("UnstableApiUsage")

import com.android.build.api.variant.FilterConfiguration
import java.io.FileInputStream
import java.util.Properties
import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.room)
    alias(libs.plugins.ktfmt.gradle)
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties: Properties = Properties()
if (keystorePropertiesFile.exists()) {
    FileInputStream(keystorePropertiesFile).use { input ->
        keystoreProperties.load(input)
    }
}

fun signingValue(environmentName: String, propertyName: String): String? =
    System.getenv(environmentName)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }

val uploadKeyAlias = signingValue("SEALI_UPLOAD_KEY_ALIAS", "keyAlias")
val uploadKeyPassword = signingValue("SEALI_UPLOAD_KEY_PASSWORD", "keyPassword")
val uploadStorePassword = signingValue("SEALI_UPLOAD_STORE_PASSWORD", "storePassword")
val uploadStoreFilePath = signingValue("SEALI_UPLOAD_KEYSTORE_FILE", "storeFile")
val uploadStoreType = signingValue("SEALI_UPLOAD_KEYSTORE_TYPE", "storeType") ?: "JKS"
val signingValues = listOf(uploadKeyAlias, uploadKeyPassword, uploadStorePassword, uploadStoreFilePath)
val hasAnySigningValue = signingValues.any { !it.isNullOrBlank() }
val signingConfigured = signingValues.all { !it.isNullOrBlank() }
val uploadKeystoreFile = uploadStoreFilePath?.let { rootProject.file(it) }

if (hasAnySigningValue && !signingConfigured) {
    throw GradleException(
        "Seali upload signing configuration is incomplete. Provide the alias, key password, store password, and keystore file."
    )
}

// Alpha7 ships only the reviewed source-built ARM64 payload set. Source compilation
// and unit tests are payload-free; APK packaging verifies staged input custody.
val splitApks = project.hasProperty("splitApks")

val abiFilterList = (properties["ABI_FILTERS"] as String).split(';')

val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)

val baseVersionName = currentVersion.name
val currentVersionCode = currentVersion.code.toInt()

val verifyAlpha7Inputs = tasks.register<Exec>("verifyAlpha7BuildInputs") {
    workingDir(rootProject.projectDir)
    // Native payloads are intentionally untracked. Always rehash before packaging.
    outputs.upToDateWhen { false }
}
gradle.projectsEvaluated {
    val nativeRoots = rootProject.allprojects.flatMap { owner ->
        owner.extensions.findByType(com.android.build.gradle.BaseExtension::class.java)
            ?.sourceSets?.flatMap { it.jniLibs.srcDirs } ?: emptyList()
    }.distinct().sortedBy { it.absolutePath }
    val args = nativeRoots.flatMap { listOf("--native-root", it.absolutePath) }
    val command = listOf("python3", "scripts/build-contract/verify-alpha7-inputs.py", "--root", rootProject.projectDir.absolutePath) + args
    verifyAlpha7Inputs.configure { commandLine(command) }
}
tasks.configureEach {
    if ((name.startsWith("merge") && (name.endsWith("NativeLibs") || name.endsWith("JniLibFolders") || name.endsWith("Assets"))) ||
        (name.startsWith("compress") && name.endsWith("Assets"))) {
        dependsOn(verifyAlpha7Inputs)
        val supportedAlpha7Abi = !splitApks && abiFilterList == listOf("arm64-v8a")
        doFirst {
            check(supportedAlpha7Abi) {
                "Alpha7 packaging supports the reviewed ARM64 payload set only; split/other ABI builds are not qualified."
            }
        }
    }
}

android {
    compileSdk = 36

    if (signingConfigured) {
        signingConfigs {
            create("githubPublish") {
                keyAlias = uploadKeyAlias!!
                keyPassword = uploadKeyPassword!!
                storeFile = uploadKeystoreFile!!
                storePassword = uploadStorePassword!!
                storeType = uploadStoreType
            }
        }
    }

    buildFeatures { buildConfig = true }

    defaultConfig {
        applicationId = "com.frpboy.seali"
        minSdk = 26
        targetSdk = 36
        versionCode = 200_000_170
        check(versionCode == currentVersionCode)

        versionName = baseVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        if (splitApks) {
            splits {
                abi {
                    isEnable = true
                    reset()
                    include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                    isUniversalApk = true
                }
            }
        } else {
            ndk { abiFilters.addAll(abiFilterList) }
        }
    }

    room { schemaDirectory("$projectDir/schemas") }
    ksp { arg("room.incremental", "true") }

    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                val name =
                    if (splitApks) {
                        output.filters
                            .find { it.filterType == FilterConfiguration.FilterType.ABI }
                            ?.identifier
                    } else {
                        abiFilterList.firstOrNull()
                    }

                val baseAbiCode = abiCodes[name]

                if (baseAbiCode != null) {
                    output.versionCode.set(baseAbiCode + (output.versionCode.get() ?: 0))
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (signingConfigured) {
                signingConfig = signingConfigs.getByName("githubPublish")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "Seali Debug")
        }
    }

    flavorDimensions += "publishChannel"

    productFlavors {
        create("generic") {
            dimension = "publishChannel"
            isDefault = true
        }

        create("githubPreview") {
            dimension = "publishChannel"
            applicationIdSuffix = ".preview"
            resValue("string", "app_name", "Seali Preview")
        }

        create("fdroid") {
            dimension = "publishChannel"
            versionName = "$baseVersionName-(F-Droid)"
        }

        create("play") {
            dimension = "publishChannel"
            // Google Play build: no sideload updater, no runtime yt-dlp
            // downloads, no all-files-access permission.
        }
    }

    lint { disable.addAll(listOf("MissingTranslation", "ExtraTranslation", "MissingQuantity")) }

    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "Seali-${defaultConfig.versionName}-${name}.apk"
        }
    }

    kotlinOptions { freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn" }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        jniLibs.useLegacyPackaging = true
        jniLibs.keepDebugSymbols += "**/*.so" // Do not mutate independently reviewed payload bytes.
    }
    androidResources { generateLocaleConfig = true }

    namespace = "com.frpboy.seali"
}

tasks.configureEach {
    if (
            name in
                setOf(
                    "packageGenericRelease",
                    "signGenericReleaseBundle",
                    "bundleGenericRelease",
                    "packagePlayRelease",
                    "signPlayReleaseBundle",
                    "bundlePlayRelease",
                )
        ) {
        // Capture the signing preconditions as lambda-local values. A task
        // action that references build-script members captures the Gradle
        // script object, which the configuration cache cannot serialize.
        val releaseSigningConfigured = signingConfigured
        val releaseKeystorePresent = uploadKeystoreFile?.isFile == true
        doFirst {
            if (!releaseSigningConfigured || !releaseKeystorePresent) {
                throw GradleException(
                    "Generic release artifacts must be signed. Configure a valid Seali upload keystore before building."
                )
            }
        }
    }
}

ktfmt { kotlinLangStyle() }

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":color"))

    implementation(libs.bundles.core)

    implementation(libs.androidx.lifecycle.runtimeCompose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.androidxCompose)
    implementation(libs.bundles.accompanist)

    implementation(libs.coil.kt.compose)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.okhttp)

    implementation("io.github.frpboy.youtubedl-android:library:0.1.0-alpha.7-staging.4")
    implementation("io.github.frpboy.youtubedl-android:ffmpeg:0.1.0-alpha.7-staging.4")
    implementation("io.github.frpboy.youtubedl-android:common:0.1.0-alpha.7-staging.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.8")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.21")
    implementation("commons-io:commons-io:2.22.0")
    implementation("org.apache.commons:commons-compress:1.28.0")

    implementation(project(":frpboy-mmkv"))

    testImplementation(libs.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
    implementation(libs.androidx.compose.ui.tooling)
}

// Local isolated Maven qualification. Actual per-variant selected AAR bytes gate packaging.
androidComponents {
    onVariants { variant ->
        val upperName = variant.name.replaceFirstChar { it.uppercaseChar() }
        val ownedAars = configurations.getByName("${variant.name}RuntimeClasspath").incoming.artifactView {
            componentFilter {
                it is org.gradle.api.artifacts.component.ModuleComponentIdentifier &&
                    it.group == "io.github.frpboy.youtubedl-android"
            }
            attributes {
                attribute(org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "aar")
            }
        }.files
        val verifyOwned = tasks.register<VerifyAlpha7MavenTask>("verify${upperName}ReviewedMaven") {
            selectedAars.from(ownedAars)
            artifactLock.set(rootProject.layout.projectDirectory.file("scripts/build-contract/alpha7-maven-lock.json"))
            verifierScript.set(rootProject.layout.projectDirectory.file("scripts/build-contract/verify-alpha7-maven.py"))
            repositoryRoot.set(rootProject.layout.projectDirectory)
        }
        tasks.configureEach {
            if ((name.startsWith("merge${upperName}") &&
                    (name.endsWith("Assets") || name.endsWith("NativeLibs") || name.endsWith("JniLibFolders"))) ||
                name == "compress${upperName}Assets") {
                dependsOn(verifyOwned)
            }
        }
    }
}
