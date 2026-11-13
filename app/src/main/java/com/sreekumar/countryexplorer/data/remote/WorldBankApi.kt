package com.sreekumar.countryexplorer.data.remote

import kotlinx.serialization.json.JsonArray
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit turns this *interface* into a working HTTP client at runtime: you
 * describe the request with annotations, Retrofit writes the implementation.
 *
 * The closest web analogy is a typed API client generated from OpenAPI — except
 * you hand-write the signatures and get the implementation free.
 *
 * `suspend` is the keyword that matters most here. A suspend function can pause
 * without blocking its thread, and can only be called from a coroutine. Compare:
 *
 *   TS:      async function countries(): Promise<Country[]>
 *   Kotlin:  suspend fun countries(): List<Country>
 *
 * Note there is no `Promise`/`Deferred` wrapper in the return type — suspend
 * encodes the asynchrony, so you never `.then()` or `await` explicitly. The
 * call just looks synchronous and reads top to bottom.
 *
 * Why return a raw [JsonArray] rather than a typed model? Because the World
 * Bank wraps every response in a two-element array of *different* types:
 *
 *   [ { "page": 1, "pages": 1, ... },        <- metadata object
 *     [ { ...country }, { ...country } ] ]   <- the actual rows
 *
 * No single data class describes that, so we take the JSON tree and pull the
 * rows out ourselves in CountryRemoteDataSource. Real APIs are full of shapes
 * like this; handling them deliberately beats contorting your models.
 */
interface WorldBankApi {

    /** All countries (plus ~79 aggregate regions we filter out later). */
    @GET("country")
    suspend fun countries(
        @Query("format") format: String = "json",
        @Query("per_page") perPage: Int = 400,
    ): JsonArray

    /**
     * Latest population for every country in one request.
     * `mrv=1` = "most recent value", so we get one data point each.
     */
    @GET("country/all/indicator/SP.POP.TOTL")
    suspend fun populations(
        @Query("format") format: String = "json",
        @Query("mrv") mostRecentValues: Int = 1,
        @Query("per_page") perPage: Int = 400,
    ): JsonArray
}
