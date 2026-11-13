package com.sreekumar.countryexplorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sreekumar.countryexplorer.ui.CountryExplorerApp
import com.sreekumar.countryexplorer.ui.theme.CountryExplorerTheme

/**
 * ## What an Activity is
 *
 * An Activity is one screen-sized host that the OS can start, stop, rotate and
 * kill. It is NOT a React component: it has a lifecycle driven by the system,
 * and it can be destroyed and recreated at any time (rotation, low memory,
 * language change) while your app is still "running".
 *
 * That recreation is the single biggest difference from web development, and it
 * is why ViewModels exist — they survive it.
 *
 * Modern apps use ONE Activity and navigate between Composables inside it (the
 * single-page-app model). `setContent` is the handoff from the Android world
 * into Compose — the equivalent of `createRoot(el).render(<App/>)`.
 */
class MainActivity : ComponentActivity() {

    // `savedInstanceState` is a small bundle Android hands back when it
    // recreates this Activity after killing it. Nullable: null on a fresh start.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw behind the status/navigation bars (the modern Android look).
        enableEdgeToEdge()

        // Reach into the Application for our hand-rolled DI container.
        // `as` is a cast; it throws if the type is wrong, which is fine here —
        // if this is wrong, the manifest is wrong and nothing can work.
        val container = (application as CountryExplorerApplication).container

        setContent {
            // Everything from here down is declarative UI. Compose, like React,
            // re-runs these functions when state changes and diffs the result.
            CountryExplorerTheme {
                CountryExplorerApp(appContainer = container)
            }
        }
    }
}
