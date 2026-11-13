package com.sreekumar.countryexplorer.ui.list

import app.cash.turbine.test
import com.sreekumar.countryexplorer.FakeFavoriteDao
import com.sreekumar.countryexplorer.FakeRemoteDataSource
import com.sreekumar.countryexplorer.country
import com.sreekumar.countryexplorer.data.CountryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ## Unit-testing a ViewModel
 *
 * These are plain JVM tests — `./gradlew test` runs them on your Mac in
 * seconds, no emulator, because the ViewModel imports nothing from Android UI.
 * Keeping that true is most of what "testable architecture" means in practice.
 *
 * Two coroutine-specific things to learn here:
 *
 *  1. `runTest` provides a *virtual clock*. Delays are skipped instantly, so a
 *     test of a 5-second timeout takes microseconds.
 *  2. `viewModelScope` dispatches to `Dispatchers.Main`, which does not exist
 *     off-device. `Dispatchers.setMain(...)` swaps in a test dispatcher. Forget
 *     it and every ViewModel test fails with "Module with the Main dispatcher
 *     had failed to initialize" — the single most common first-time error.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CountryListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // @Before runs before each test — the JUnit equivalent of beforeEach.
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()   // leaking this breaks later tests
    }

    private fun viewModelWith(
        remote: FakeRemoteDataSource,
    ): CountryListViewModel = CountryListViewModel(
        CountryRepository(remote, FakeFavoriteDao())
    )

    @Test
    fun `loads countries on init`() = runTest(testDispatcher) {
        // Backticked function names let a test read as a sentence. Allowed on
        // the JVM, idiomatic in Kotlin tests, and much better than
        // loadsCountriesOnInit().
        val remote = FakeRemoteDataSource(listOf(country(name = "Japan")))
        val viewModel = viewModelWith(remote)

        /**
         * Turbine's `test {}` collects the flow and lets you await emissions
         * one at a time. Needed because `uiState` is a *stream*: without it you
         * would read `.value` at an arbitrary moment and get a flaky test.
         */
        viewModel.uiState.test {
            // First emission is the synchronous initial value.
            assertTrue(awaitItem().isLoading)

            // Drain emissions until the load settles. A StateFlow built with
            // `combine` can emit intermediate states; asserting on the settled
            // one is what makes this robust.
            var state = awaitItem()
            while (state.isLoading || state.countries.isEmpty()) {
                state = awaitItem()
            }

            assertFalse(state.isLoading)
            assertEquals(listOf("Japan"), state.countries.map { it.name })
            assertEquals(1, remote.fetchCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `surfaces an error message when the network fails`() = runTest(testDispatcher) {
        val remote = FakeRemoteDataSource()
        remote.failWith(java.io.IOException("airplane mode"))
        val viewModel = viewModelWith(remote)

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.errorMessage == null) {
                state = awaitItem()
            }
            assertEquals("airplane mode", state.errorMessage)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search filters by name and capital, case-insensitively`() = runTest(testDispatcher) {
        val remote = FakeRemoteDataSource(
            listOf(
                country(code = "JPN", name = "Japan", capital = "Tokyo"),
                country(code = "IND", name = "India", capital = "New Delhi"),
                country(code = "KEN", name = "Kenya", capital = "Nairobi"),
            )
        )
        val viewModel = viewModelWith(remote)

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.countries.size != 3) state = awaitItem()

            // Matches a name, ignoring case.
            viewModel.onQueryChange("jap")
            state = awaitItem()
            while (state.query != "jap") state = awaitItem()
            assertEquals(listOf("Japan"), state.countries.map { it.name })

            // Matches a CAPITAL, not just a name — the behaviour worth pinning
            // down, since it is the part a refactor would quietly break.
            viewModel.onQueryChange("nairobi")
            state = awaitItem()
            while (state.query != "nairobi") state = awaitItem()
            assertEquals(listOf("Kenya"), state.countries.map { it.name })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggling a favorite updates the list and the favorites-only filter`() =
        runTest(testDispatcher) {
            val remote = FakeRemoteDataSource(
                listOf(
                    country(code = "JPN", name = "Japan"),
                    country(code = "IND", name = "India"),
                )
            )
            val viewModel = viewModelWith(remote)

            viewModel.uiState.test {
                var state = awaitItem()
                while (state.countries.size != 2) state = awaitItem()

                // Nothing is favourited yet.
                assertTrue(state.countries.none { it.isFavorite })

                val japan = state.countries.first { it.code == "JPN" }
                viewModel.onToggleFavorite(japan)

                // This is the end-to-end assertion that matters: the write goes
                // to the (fake) database, Room's Flow re-emits, the repository
                // re-merges, and the UI state arrives with isFavorite flipped.
                // No manual refresh anywhere in that chain.
                state = awaitItem()
                while (state.countries.none { it.isFavorite }) state = awaitItem()
                assertEquals(
                    listOf("JPN"),
                    state.countries.filter { it.isFavorite }.map { it.code },
                )

                // And the filter should now show exactly that one country.
                viewModel.onFavoritesOnlyChange(true)
                state = awaitItem()
                while (!state.favoritesOnly) state = awaitItem()
                assertEquals(listOf("Japan"), state.countries.map { it.name })

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `retry refetches after a failure`() = runTest(testDispatcher) {
        val remote = FakeRemoteDataSource()
        remote.failWith(java.io.IOException("offline"))
        val viewModel = viewModelWith(remote)

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.errorMessage == null) state = awaitItem()
            assertNotNull(state.errorMessage)

            // The network comes back, the user taps Retry.
            remote.succeedWith(listOf(country(name = "Japan")))
            viewModel.onRetry()

            state = awaitItem()
            while (state.countries.isEmpty()) state = awaitItem()
            assertEquals(listOf("Japan"), state.countries.map { it.name })
            assertEquals(null, state.errorMessage)
            assertEquals(2, remote.fetchCount)   // initial failure + retry

            cancelAndIgnoreRemainingEvents()
        }
    }
}
