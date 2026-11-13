package com.sreekumar.countryexplorer

import android.app.Application

/**
 * The Application object: created once, before any screen, and alive for the
 * whole process. It is the natural home for app-scoped singletons.
 *
 * Registered in AndroidManifest.xml via `android:name=".CountryExplorerApplication"` —
 * declaring it in the manifest is what makes Android use your subclass. Forget
 * that line and this file is silently never run.
 *
 * Mental model: `main.ts` in Angular, where you bootstrap the root providers.
 */
class CountryExplorerApplication : Application() {

    // `lateinit` = "non-null, but assigned later than the constructor". Reading
    // it before assignment throws a clear error rather than giving you null.
    // `private set` makes it read-only from outside, writable inside. Kotlin's
    // version of a public getter with a private setter.
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}
