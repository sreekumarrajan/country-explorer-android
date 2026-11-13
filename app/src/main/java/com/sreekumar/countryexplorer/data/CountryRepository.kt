package com.sreekumar.countryexplorer.data

import com.sreekumar.countryexplorer.data.local.FavoriteCountryDao
import com.sreekumar.countryexplorer.data.local.FavoriteCountryEntity
import com.sreekumar.countryexplorer.data.remote.CountryRemoteDataSource
import com.sreekumar.countryexplorer.domain.Country
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The repository is the ViewModel's single source of truth. It owns the
 * decision "where does this data come from?" so no screen has to care.
 *
 * Here that means: countries come from the network, favourites come from the
 * database, and the two are *merged* into one stream the UI can render.
 *
 * Closest Angular analogy: a singleton service holding a BehaviorSubject that
 * components subscribe to. Same shape, enforced by types.
 *
 * An `interface` would be the textbook move (easy to fake in tests). We keep it
 * a class and fake the whole repository in tests instead — see the test folder.
 */
class CountryRepository(
    private val remoteDataSource: CountryRemoteDataSource,
    private val favoriteDao: FavoriteCountryDao,
) {

    /**
     * In-memory cache of the last network result.
     *
     * [MutableStateFlow] is a Flow that always holds exactly one current value
     * and replays it to every new collector. If you know RxJS, it *is*
     * BehaviorSubject. If you know React, it is the closest thing to a store.
     */
    private val cachedCountries = MutableStateFlow<List<Country>>(emptyList())

    // A coroutine-aware lock. Guards against two refreshes racing (e.g. the
    // user taps Retry while the initial load is still in flight). Unlike a
    // Java `synchronized` block, waiting on a Mutex suspends instead of
    // blocking a thread.
    private val refreshMutex = Mutex()

    /**
     * The stream the UI actually renders.
     *
     * `combine` re-emits whenever *either* input changes — RxJS combineLatest.
     * So: a fresh network load re-emits, and favouriting one country re-emits
     * with that country's `isFavorite` flipped. The UI just re-renders.
     */
    val countries: Flow<List<Country>> =
        combine(cachedCountries, favoriteDao.observeFavoriteCodes()) { countries, favoriteCodes ->
            // Set lookup is O(1); `in` on a List would be O(n) per country,
            // i.e. ~217 x 217 comparisons on every single emission.
            val favorites = favoriteCodes.toSet()
            countries.map { country ->
                country.copy(isFavorite = country.code in favorites)
            }
        }

    /** One country as a stream, for the detail screen. */
    fun country(code: String): Flow<Country?> =
        countries.map { list -> list.firstOrNull { it.code == code } }

    /** Hit the network and replace the cache. Throws on failure — callers decide. */
    suspend fun refresh() {
        refreshMutex.withLock {
            cachedCountries.value = remoteDataSource.fetchCountries()
        }
    }

    /** Load only if we have nothing yet (used when a screen opens). */
    suspend fun refreshIfEmpty() {
        if (cachedCountries.value.isEmpty()) refresh()
    }

    /**
     * Writes one row. Notice there is no "and then update the UI" step: the
     * write lands in SQLite, Room's Flow re-emits, `combine` above recomputes,
     * and the screen redraws. One-way data flow, exactly like Redux — the
     * database is the store.
     */
    suspend fun setFavorite(countryCode: String, isFavorite: Boolean) {
        if (isFavorite) {
            favoriteDao.insert(FavoriteCountryEntity(countryCode))
        } else {
            favoriteDao.delete(countryCode)
        }
    }
}
