package com.sreekumar.countryexplorer.ui.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sreekumar.countryexplorer.R
import com.sreekumar.countryexplorer.ui.components.CountryRow
import com.sreekumar.countryexplorer.ui.components.EmptyState
import com.sreekumar.countryexplorer.ui.components.ErrorState
import com.sreekumar.countryexplorer.ui.components.LoadingState

/**
 * ## The list screen
 *
 * Read this as a React function component. It takes a ViewModel and a
 * navigation callback, reads state, and returns a description of the UI.
 *
 * Why does the screen receive `onCountryClick` instead of a NavController?
 * Because a Composable that does not know how to navigate can be previewed,
 * tested and reused anywhere. Navigation is injected from above. This is the
 * "lift behaviour to the parent" rule you already follow in React.
 */
@OptIn(ExperimentalMaterial3Api::class)   // TopAppBar is still marked experimental
@Composable
fun CountryListScreen(
    viewModel: CountryListViewModel,
    onCountryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    /**
     * `collectAsStateWithLifecycle` subscribes to the StateFlow and turns each
     * emission into Compose state, so a new value triggers recomposition.
     *
     * The `WithLifecycle` part matters: it stops collecting when the app goes
     * to the background and resumes on return. The plain `collectAsState` keeps
     * collecting while the screen is invisible, burning battery — this is the
     * single most common mistake in Compose apps.
     *
     * `by` is property delegation: it unwraps State<T> so you write
     * `uiState.countries` instead of `uiState.value.countries`.
     */
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        // Scaffold lays out the Material skeleton: top bar, bottom bar, FAB,
        // snackbars — and critically, it computes the insets for you.
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = viewModel::onRefresh) {
                        // `::` is a method reference — `viewModel.onRefresh`
                        // as a value, not a call. Same as passing the function
                        // itself rather than invoking it.
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.retry))
                    }
                },
            )
        },
    ) { contentPadding ->
        // Scaffold hands you padding that accounts for the top bar AND the
        // system status bar. Ignore it and your content draws underneath them.
        Column(modifier = Modifier.padding(contentPadding)) {

            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Search,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            /**
             * ## The most important idea in Compose
             *
             * This text field is *stateless*. It does not remember what you
             * typed — it renders `uiState.query` and reports keystrokes upward.
             * The ViewModel owns the value, and the new value comes back down.
             *
             * That is a controlled component, exactly as in React:
             *   <input value={query} onChange={e => setQuery(e.target.value)} />
             *
             * The pattern has a name here: *state hoisting*. If a Composable
             * owns no state, it cannot drift out of sync with your model.
             */

            FilterChip(
                selected = uiState.favoritesOnly,
                onClick = { viewModel.onFavoritesOnlyChange(!uiState.favoritesOnly) },
                label = { Text(stringResource(R.string.favorites_only)) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            // Bind the nullable field to a local `val` before the `when`.
            //
            // Kotlin normally "smart casts": after `if (x != null)` it treats x
            // as non-null. That cannot apply to `uiState.errorMessage`, because
            // `uiState` is a *delegated* property (the `by` above) — the
            // compiler cannot prove two reads return the same value. Copying to
            // a local, which nothing else can change, restores the smart cast.
            //
            // You will hit this constantly with `by` properties. The fix is
            // always the same: assign to a local first.
            val errorMessage = uiState.errorMessage

            // `when` is Kotlin's switch, but it is an *expression* and the
            // compiler checks exhaustiveness. Here it replaces the nested
            // ternaries / early returns you would write in JSX.
            when {
                uiState.isLoading -> LoadingState(Modifier.fillMaxSize())

                errorMessage != null -> ErrorState(
                    // Smart cast works on the local: the compiler knows it is
                    // non-null in this branch, so no `!!` is needed.
                    message = errorMessage,
                    onRetry = viewModel::onRetry,
                )

                uiState.countries.isEmpty() -> EmptyState(
                    message = stringResource(
                        if (uiState.favoritesOnly) R.string.empty_favorites
                        else R.string.empty_search
                    ),
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    /**
                     * LazyColumn only composes the rows actually on screen —
                     * it is FlatList / virtual scrolling, not a plain map over
                     * 200 items. A regular `Column` would build all 217 rows.
                     *
                     * `key = { it.code }` is the same contract as React's
                     * `key` prop: it lets Compose match items across updates
                     * so scroll position and animations survive a data change.
                     */
                    items(
                        items = uiState.countries,
                        key = { country -> country.code },
                    ) { country ->
                        CountryRow(
                            country = country,
                            onClick = { onCountryClick(country.code) },
                            onToggleFavorite = { viewModel.onToggleFavorite(country) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
