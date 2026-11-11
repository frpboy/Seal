import java.nio.file.Files
import java.security.MessageDigest

plugins { id("com.android.library") }
group = "com.frpboy.seali.dependencies"
version = "1.3.17-source-candidate"
android {
    namespace = "com.tencent.mmkv"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures { buildConfig = true; aidl = true }
    flavorDimensions += "stl_mode"
    productFlavors { create("DefaultCpp") { dimension = "stl_mode" } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
    packaging { jniLibs.keepDebugSymbols += "**/libmmkv.so" }
}
dependencies {
    implementation("androidx.annotation:annotation:1.9.1")
    compileOnly("org.jetbrains:annotations:13.0")
    compileOnly(project(":frpboy-mmkvannotation"))
}

// Separate dependency task runs even when AGP marks native merging NO-SOURCE.
val nativePayload = layout.projectDirectory.file("src/main/jniLibs/arm64-v8a/libmmkv.so").asFile
val verifyOwnedNative = tasks.register("verifyOwnedMmkvNative") {
    val native = nativePayload
    doLast {
        check(native.isFile && !Files.isSymbolicLink(native.toPath())) {
            "Source-built MMKV ARM64 payload missing. Follow SOURCE.md; do not ship a Java-only module."
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(native.readBytes()).joinToString("") { "%02x".format(it) }
        check(digest == "6544cd777e2b7bfdc450dcfbb22e1145062e9077296700f1251abcd03724fae0") {
            "MMKV payload differs from reviewed source build. Review/audit changed bytes before changing the pin."
        }
    }
}
tasks.configureEach {
    if (name.startsWith("merge") && (name.endsWith("NativeLibs") || name.endsWith("JniLibFolders"))) dependsOn(verifyOwnedNative)
}
