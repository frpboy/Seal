plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}
// Local project module, no public registry or account required.
group = "com.frpboy.seali.dependencies"
version = "0.1.0-alpha.7"
android {
    namespace = "com.yausername.youtubedl_common"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("commons-io:commons-io:2.5")
    implementation("org.apache.commons:commons-compress:1.12")
}
