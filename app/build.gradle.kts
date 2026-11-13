// ============================================================================
//  :app module build file — the closest thing to your package.json + tsconfig
//  + webpack config, all in one, written in Kotlin (a type-safe DSL).
// ============================================================================
plugins {
    alias(libs.plugins.android.application)   // builds an Android APP *and*,
                                              // since AGP 9, compiles Kotlin itself
    alias(libs.plugins.kotlin.compose)        // Compose compiler plugin
    alias(libs.plugins.kotlin.serialization)  // generates JSON (de)serializers
    alias(libs.plugins.ksp)                   // runs annotation processors (Room)
}

android {
    // `namespace` is the Kotlin package that owns the generated R class.
    namespace = "com.sreekumar.countryexplorer"

    // compileSdk: which Android API level you COMPILE against — you can call any
    // API up to this level. Always the latest stable.
    //
    // This is not a free choice: AndroidX libraries declare a MINIMUM compileSdk
    // their consumers must use, and the build fails with an explicit error if
    // you are below it. Compose UI 1.12 and core-ktx 1.19 both require 37.
    compileSdk = 37

    defaultConfig {
        // applicationId is the app's unique identity on the device and in Play.
        // It can differ from `namespace`, but keeping them equal is conventional.
        applicationId = "com.sreekumar.countryexplorer"

        // minSdk: the OLDEST Android version you support. Lower = more devices,
        // more compatibility work. 26 (Android 8, 2017) covers ~95% of devices.
        minSdk = 26

        // targetSdk: "I have tested against this version's behaviour changes."
        // Android uses it to decide whether to apply newer runtime behaviours.
        //
        // These three move independently: compileSdk is which APIs you can call,
        // targetSdk is which runtime behaviours you opt into, and minSdk is which
        // devices can install the app. Bumping compileSdk alone is always safe;
        // bumping targetSdk needs testing.
        targetSdk = 37

        versionCode = 1        // integer Play uses to order builds; must increase
        versionName = "1.0"    // human-readable string users see
    }

    buildTypes {
        release {
            // R8 shrinking/obfuscation. Off here so stack traces stay readable
            // while you're learning; a real release build would turn this on.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        // Java bytecode level for any Java sources / desugaring.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true     // enable Jetpack Compose in this module
    }
}

kotlin {
    compilerOptions {
        // Kotlin must emit the same bytecode level as compileOptions above.
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // ---- Platform basics ---------------------------------------------------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // ---- Compose ------------------------------------------------------------
    // A BOM ("bill of materials") pins a mutually-compatible set of versions,
    // so the Compose lines below deliberately carry no version numbers.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)   // debug builds only

    // ---- Architecture -------------------------------------------------------
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    // ---- Networking ---------------------------------------------------------
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)

    // ---- Images -------------------------------------------------------------
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)   // lets Coil fetch http(s) URLs

    // ---- Local database -----------------------------------------------------
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)           // `ksp` = run this as codegen

    // ---- Unit tests (run on your Mac's JVM, no device needed) --------------
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
