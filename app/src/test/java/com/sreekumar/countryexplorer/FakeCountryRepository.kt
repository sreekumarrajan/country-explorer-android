package com.sreekumar.countryexplorer

import com.sreekumar.countryexplorer.data.CountryRepository
import com.sreekumar.countryexplorer.data.local.FavoriteCountryDao
import com.sreekumar.countryexplorer.data.local.FavoriteCountryEntity
import com.sreekumar.countryexplorer.data.remote.CountryRemoteDataSource
import com.sreekumar.countryexplorer.domain.Country
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * ## Test doubles without a mocking framework
 *
 * No Mockito, no MockK — just small classes implementing the same interfaces.
 * Hand-written fakes usually beat mocks: they read like real code, the compiler
 * updates you when a signature changes, and they can't tempt you into
 * asserting on implementation details.
 *
 * Both fakes below exist only because the production code depends on
 * *interfaces* (`FavoriteCountryDao`, `CountryRemoteDataSource`). That is the
 * practical payoff of those seams.
 */

/** An in-memory stand-in for Room. The Flow makes it behave like the real DAO. */
class FakeFavoriteDao : FavoriteCountryDao {
    private val codes = MutableStateFlow<List<String>>(emptyList())

    override fun observeFavoriteCodes(): Flow<List<String>> = codes

    override suspend fun insert(favorite: FavoriteCountryEntity) {
        codes.value = (codes.value + favorite.countryCode).distinct()
    }

    override suspend fun delete(code: String) {
        codes.value = codes.value - code
    }
}

/** A network layer we fully control: returns a list, or throws on demand. */
class FakeRemoteDataSource(
    private var countries: List<Country> = emptyList(),
) : CountryRemoteDataSource {

    private var error: Exception? = null
    var fetchCount = 0
        private set

    fun succeedWith(countries: List<Country>) {
        this.countries = countries
        error = null
    }

    fun failWith(error: Exception) {
        this.error = error
    }

    override suspend fun fetchCountries(): List<Country> {
        fetchCount++
        // `?.let { throw it }` — if error is non-null, throw it.
        error?.let { throw it }
        return countries
    }
}

/**
 * Builds a REAL repository wired to fakes.
 *
 * Deliberate choice: we are testing our own merge/filter logic, so the
 * repository under test is the production one. Only the boundaries (network,
 * database) are faked. Faking the repository too would test nothing.
 */
fun testRepository(
    countries: List<Country> = listOf(country()),
): Pair<CountryRepository, FakeRemoteDataSource> {
    val remote = FakeRemoteDataSource(countries)
    val repository = CountryRepository(remote, FakeFavoriteDao())
    // `to` builds a Pair; the call site destructures it as `val (a, b) = ...`
    return repository to remote
}
