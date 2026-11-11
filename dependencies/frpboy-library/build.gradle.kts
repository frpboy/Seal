plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}
// Local project module, no public registry or account required.
group = "com.frpboy.seali.dependencies"
version = "0.1.0-alpha.7"
android {
    namespace = "com.yausername.youtubedl_android"
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
    implementation(project(":frpboy-common"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.11.1")
    implementation("commons-io:commons-io:2.5")
}

dependencies { testImplementation(libs.junit4) }
