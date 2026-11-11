plugins {
 alias(libs.plugins.android.library)
 alias(libs.plugins.kotlin.android)
 alias(libs.plugins.kotlin.serialization)
}
val upstreamRuntime by configurations.creating { isTransitive = false }
val generatedRuntime = layout.buildDirectory.dir("generated/upstream-runtime")
val extractRuntime by tasks.registering(ExtractDownloaderRuntime::class) {
    archives.from(upstreamRuntime)
    expectedSha256.set("9d29b148187242d6e754fb59f126b62dde5c1c7fcadaa29d14dbe13659495e4c")
    destination.set(generatedRuntime)
}
tasks.named("preBuild") { dependsOn(extractRuntime) }
android {
 buildFeatures { buildConfig = true }
 namespace = "com.yausername.youtubedl_android"
 compileSdk = 36
 defaultConfig { minSdk = 24 }
 sourceSets.getByName("main") {
    res.srcDir(generatedRuntime.map { it.dir("res") })
    jniLibs.srcDir(generatedRuntime.map { it.dir("jni") })
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21; isCoreLibraryDesugaringEnabled = true }
}
kotlin { jvmToolchain(21) }
dependencies {
 upstreamRuntime("io.github.junkfood02.youtubedl-android:library:0.17.3@aar")
 testImplementation("junit:junit:4.13.2")
 implementation(libs.androidx.core.ktx)
 implementation(libs.kotlinx.serialization.json)
 implementation("io.github.junkfood02.youtubedl-android:common:0.17.3")
 implementation("commons-io:commons-io:2.15.1")
 coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
}
