package com.sreekumar.countryexplorer.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sreekumar.countryexplorer.data.CountryRepository
import com.sreekumar.countryexplorer.domain.Country
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One immutable snapshot of everything the list screen needs to draw itself.
 *
 * This single-state-object pattern is deliberate, and it is the same lesson as
 * `useReducer` over five separate `useState` calls: with one object you cannot
 * render an impossible combination like "loading AND showing an error", because
 * every change produces one new consistent value.
 */
data class CountryListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val query: String = "",
    val favoritesOnly: Boolean = false,
    val countries: List<Country> = emptyList(),
)

/** Internal bookkeeping, kept separate from the data streams. */
private data class LoadState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * ## Why ViewModel exists
 *
 * Rotate the device and Android *destroys and recreates* your Activity. A
 * ViewModel survives that. It is scoped to the screen's lifetime, not the
 * Activity instance's, so in-flight work and loaded data are not thrown away.
 *
 * There is no web equivalent, because the browser never rebuilds your
 * component tree out from under you.
 *
 * The rules that make this architecture work:
 *  - the ViewModel never imports anything from Compose or Android UI;
 *  - state flows down (`uiState`), events flow up (the `on...` functions);
 *  - the UI is a pure function of `uiState`.
 *
 * That is unidirectional data flow, and it is the same contract as a React
 * component reading from a store and dispatching actions.
 */
class CountryListViewModel(
    private val repository: CountryRepository,
) : ViewModel() {

    // Each piece of user input is its own stream, so a keystroke recomputes
    // only what depends on the query.
    private val query = MutableStateFlow("")
    private val favoritesOnly = MutableStateFlow(false)
    private val loadState = MutableStateFlow(LoadState())

    /**
     * The derived state the screen collects.
     *
     * `combine` of four streams: whenever any one changes, this block reruns and
     * emits a fresh snapshot. The filtering lives here rather than in the UI,
     * which is what keeps the Composable dumb and testable.
     *
     * `stateIn` converts the cold `Flow` into a hot `StateFlow` that always has
     * a current value:
     *  - `viewModelScope` ties the work to this ViewModel's life — when it is
     *    cleared, collection stops. No manual unsubscribe, ever.
     *  - `WhileSubscribed(5_000)` keeps the upstream alive for 5s after the last
     *    collector leaves, so a rotation does not trigger a reload, but
     *    genuinely leaving the screen does stop the work.
     *  - the third argument is the initial value, available synchronously on the
     *    very first frame.
     */
    val uiState: StateFlow<CountryListUiState> =
        combine(
            repository.countries,
            query,
            favoritesOnly,
            loadState,
        ) { countries, currentQuery, onlyFavorites, load ->
            CountryListUiState(
                isLoading = load.isLoading,
                isRefreshing = load.isRefreshing,
                errorMessage = load.errorMessage,
                query = currentQuery,
                favoritesOnly = onlyFavorites,
                countries = countries.filter { country ->
                    val matchesQuery = currentQuery.isBlank() ||
                        country.name.contains(currentQuery, ignoreCase = true) ||
                        country.capital?.contains(currentQuery, ignoreCase = true) == true
                    val matchesFavorites = !onlyFavorites || country.isFavorite
                    matchesQuery && matchesFavorites
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CountryListUiState(),
        )

    // `init` runs on construction — the ViewModel's "constructor body". Because
    // a ViewModel is created once per screen, this fires once, not on every
    // recomposition. (In React you would need useEffect with an empty dep array
    // to get this behaviour; here it is the default.)
    init {
        load(isUserInitiatedRefresh = false)
    }

    // --- Events from the UI. Named on* by convention, like React props. ------

    fun onQueryChange(newQuery: String) {
        // Filtering is local, so there is nothing to debounce. If this hit the
        // network per keystroke you would want `query.debounce(300)` — the
        // direct analogue of RxJS debounceTime.
        query.value = newQuery
    }

    fun onFavoritesOnlyChange(enabled: Boolean) {
        favoritesOnly.value = enabled
    }

    fun onRetry() = load(isUserInitiatedRefresh = false)

    fun onRefresh() = load(isUserInitiatedRefresh = true)

    fun onToggleFavorite(country: Country) {
        // `launch` starts a coroutine and returns immediately — fire and forget,
        // but *scoped*: if the ViewModel dies first, this is cancelled.
        viewModelScope.launch {
            repository.setFavorite(country.code, isFavorite = !country.isFavorite)
        }
    }

    private fun load(isUserInitiatedRefresh: Boolean) {
        viewModelScope.launch {
            loadState.value = LoadState(
                isLoading = !isUserInitiatedRefresh,
                isRefreshing = isUserInitiatedRefresh,
                errorMessage = null,
            )
            // try/catch around a suspend call reads exactly like try/catch
            // around `await` — one of coroutines' best ergonomic wins, since
            // async errors are ordinary exceptions again.
            try {
                repository.refresh()
                loadState.value = LoadState(isLoading = false, isRefreshing = false)
            } catch (error: Exception) {
                loadState.value = LoadState(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = error.message ?: "Something went wrong",
                )
            }
        }
    }

    companion object {
        /**
         * A ViewModel with constructor arguments needs a factory, because the
         * framework — not you — decides when to instantiate it.
         *
         * This is the seam that makes testing trivial: pass a fake repository.
         */
        fun factory(repository: CountryRepository) = viewModelFactory {
            initializer { CountryListViewModel(repository) }
        }
    }
}
