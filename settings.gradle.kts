// ============================================================================
//  settings.gradle.kts — defines WHICH modules exist and WHERE deps come from.
//  Think of it as the workspace/monorepo root config (like an Nx workspace file).
// ============================================================================
pluginManagement {
    // Where Gradle looks for *build plugins* (the tools that run the build).
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Where Gradle looks for *libraries your app code imports* (like npm registry).
    repositories {
        google()        // Google's Maven: AndroidX, Compose, Room
        mavenCentral()  // Everything else: Retrofit, Coil, kotlinx
    }
}

rootProject.name = "CountryExplorer"
include(":app")     // one module, named ":app". Big apps add :core, :feature-x, etc.
