package com.sreekumar.countryexplorer

import android.content.Context
import androidx.room.Room
import com.sreekumar.countryexplorer.data.CountryRepository
import com.sreekumar.countryexplorer.data.local.CountryExplorerDatabase
import com.sreekumar.countryexplorer.data.remote.WorldBankRemoteDataSource
import com.sreekumar.countryexplorer.data.remote.WorldBankApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * ## Dependency injection, by hand
 *
 * Angular hands you an injector and `@Injectable`. Android's equivalents are
 * Hilt and Koin — but doing it manually once teaches you what they automate:
 * build each object, pass its dependencies in, keep one instance alive.
 *
 * Everything here is `by lazy`, meaning "construct on first access, then reuse
 * forever". That is how you get singletons in Kotlin without a framework, and
 * it keeps app startup cheap: nothing is built until something asks for it.
 *
 * When this app grows, swapping this file for Hilt is a mechanical change —
 * which is exactly why starting here is safe.
 */
class AppContainer(private val context: Context) {

    /**
     * One shared JSON parser.
     *
     * - `ignoreUnknownKeys = true` is essential for real APIs: the World Bank
     *   sends fields we never declared (`adminregion`, `lendingType`). Without
     *   this, every unexpected key is a crash. The default is strict precisely
     *   so that you make this choice deliberately.
     * - `coerceInputValues = true` turns an unexpected `null` into the default
     *   value of a property instead of throwing.
     */
    private val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            // baseUrl MUST end in "/" — Retrofit resolves @GET paths against it.
            .baseUrl("https://api.worldbank.org/v2/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private val worldBankApi: WorldBankApi by lazy {
        // This is where the interface becomes a real HTTP client.
        retrofit.create(WorldBankApi::class.java)
    }

    private val database: CountryExplorerDatabase by lazy {
        Room.databaseBuilder(
            context = context,
            klass = CountryExplorerDatabase::class.java,
            name = "country-explorer.db",
        ).build()
    }

    /** The only thing the UI layer is allowed to know about. */
    val countryRepository: CountryRepository by lazy {
        CountryRepository(
            remoteDataSource = WorldBankRemoteDataSource(worldBankApi, json),
            favoriteDao = database.favoriteCountryDao(),
        )
    }
}
