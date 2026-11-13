// ============================================================================
//  ROOT build file. Declares plugins for the whole build but applies none here
//  (`apply false`) — the :app module opts in. Keeps plugin versions in one place.
// ============================================================================

// ----------------------------------------------------------------------------
//  Why this buildscript block exists
// ----------------------------------------------------------------------------
//  As of AGP 9.0, the Android plugin compiles Kotlin itself and ships with its
//  own Kotlin Gradle Plugin (2.2.10). That version is too old for the AndroidX
//  libraries this project uses: Compose UI and core-ktx are now built with
//  Kotlin 2.4, and a 2.2 compiler cannot read 2.4 metadata — the build fails
//  with "Module was compiled with an incompatible version of Kotlin".
//
//  Putting a newer KGP on the buildscript classpath is Google's documented way
//  to raise it. See:
//  https://developer.android.com/build/releases/agp-9-0-0-release-notes
//
//  This is the kind of version-alignment friction that Android build files are
//  infamous for, and worth seeing once rather than being shielded from.
// ----------------------------------------------------------------------------
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:${libs.versions.ksp.get()}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
