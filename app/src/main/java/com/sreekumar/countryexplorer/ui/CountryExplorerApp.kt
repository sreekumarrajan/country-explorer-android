package com.sreekumar.countryexplorer.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.sreekumar.countryexplorer.AppContainer
import com.sreekumar.countryexplorer.ui.detail.CountryDetailScreen
import com.sreekumar.countryexplorer.ui.detail.CountryDetailViewModel
import com.sreekumar.countryexplorer.ui.list.CountryListScreen
import com.sreekumar.countryexplorer.ui.list.CountryListViewModel
import kotlinx.serialization.Serializable

/**
 * ## Navigation
 *
 * Closest analogy: your Angular `RouterModule.forRoot(routes)` or React
 * Router's route config. One place that maps a destination to a screen.
 *
 * Routes are declared as @Serializable types rather than URL strings, which is
 * the modern type-safe approach (Navigation 2.8+). The win over
 * `"detail/{code}"` strings:
 *
 *   - the compiler catches a missing or misspelled argument;
 *   - no manual string building or parsing at the call site;
 *   - refactoring a route argument is a rename, not a hunt for string literals
 *     you hope you found all of.
 *
 * `object` for a route with no arguments, `data class` for one that has them.
 * (`object` declares a singleton — Kotlin's built-in, thread-safe one.)
 */
@Serializable
object CountryListRoute

@Serializable
data class CountryDetailRoute(val countryCode: String)

@Composable
fun CountryExplorerApp(appContainer: AppContainer) {

    // The NavController holds navigation state (including the back stack).
    // `remember` keeps it across recompositions — without it every redraw
    // would build a new one and reset history. `remember` is useMemo/useRef.
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = CountryListRoute,
    ) {
        // `composable<Route>` registers one destination; the lambda is the
        // screen — the equivalent of `element={<List/>}` in React Router.
        composable<CountryListRoute> {
            /**
             * `viewModel()` either creates the ViewModel or returns the
             * existing one for this navigation entry. That scoping is the whole
             * trick: navigate to detail and back, and the list ViewModel (and
             * its loaded data and scroll position) is still there; pop the
             * detail screen for good and *its* ViewModel is cleared.
             */
            val viewModel: CountryListViewModel = viewModel(
                factory = CountryListViewModel.factory(appContainer.countryRepository),
            )
            CountryListScreen(
                viewModel = viewModel,
                // Navigating is a plain function passed down as a prop.
                onCountryClick = { code -> navController.navigate(CountryDetailRoute(code)) },
            )
        }

        composable<CountryDetailRoute> { backStackEntry ->
            // `toRoute` decodes the typed route out of the back stack — the
            // type-safe replacement for `arguments?.getString("code")`.
            val route: CountryDetailRoute = backStackEntry.toRoute()

            val viewModel: CountryDetailViewModel = viewModel(
                factory = CountryDetailViewModel.factory(
                    repository = appContainer.countryRepository,
                    countryCode = route.countryCode,
                ),
            )
            CountryDetailScreen(
                viewModel = viewModel,
                // popBackStack() means "go back". Android also routes the
                // system back gesture here automatically, which is why you
                // rarely handle hardware back yourself.
                onBack = { navController.popBackStack() },
            )
        }
    }
}
