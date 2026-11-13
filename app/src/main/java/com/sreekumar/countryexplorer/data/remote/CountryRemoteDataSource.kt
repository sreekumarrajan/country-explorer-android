package com.sreekumar.countryexplorer.data.remote

import com.sreekumar.countryexplorer.domain.Country
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
// This import is what makes `json.decodeFromJsonElement<T>(element)`
// resolve to the single-argument reified overload. Without it the
// compiler finds only `decodeFromJsonElement(deserializer, element)`
// and reports a baffling "No value passed for parameter 'element'".
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray

/** Thrown when the server replies with a shape we can't make sense of. */
class CountryDataException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * ## Program to an interface
 *
 * The repository depends on this *interface*, never on the class below it. That
 * one indirection buys two things:
 *
 *  - tests swap in a fake with three lines of code and no mocking framework;
 *  - replacing the World Bank with a different provider touches one file.
 *
 * Kotlin makes this cheap: an interface with one suspend function. (Note that
 * `suspend` is part of the contract — an implementation may do I/O, so callers
 * must already be in a coroutine.)
 *
 * Angular's DI does the same job with an abstract class and a provider token;
 * here the seam is just a type.
 */
interface CountryRemoteDataSource {
    suspend fun fetchCountries(): List<Country>
}

/**
 * Owns everything about talking to the World Bank: firing the requests,
 * unwrapping the odd envelope, stitching two responses together, and handing
 * back clean [Country] objects.
 *
 * The ViewModel never learns that population lives at a different URL. That is
 * the whole point of a data source: one lie-free boundary.
 */
class WorldBankRemoteDataSource(
    private val api: WorldBankApi,
    private val json: Json,
) : CountryRemoteDataSource {

    /**
     * ## The structured-concurrency lesson
     *
     * The country list and the population numbers are independent, so they
     * should go out *together*, not one after the other.
     *
     *   React/Angular:  const [a, b] = await Promise.all([getA(), getB()])
     *   Kotlin:         val a = async { getA() }; val b = async { getB() }
     *                   a.await() to b.await()
     *
     * `coroutineScope { }` is the part with no JS equivalent, and it is the
     * good part. It does not return until every coroutine started inside it has
     * finished, and it guarantees:
     *
     *   - if either request throws, the *other is cancelled* automatically and
     *     the exception propagates out of the block (Promise.all only rejects —
     *     the loser keeps running and leaks);
     *   - if whoever called this gets cancelled (user left the screen), both
     *     requests are cancelled too.
     *
     * That property — children can never outlive their scope — is called
     * structured concurrency, and it is why Kotlin apps rarely leak work.
     */
    override suspend fun fetchCountries(): List<Country> = coroutineScope {
        val countriesResponse = async { api.countries() }
        val populationsResponse = async { api.populations() }

        val countryDtos = decodeRows<CountryDto>(countriesResponse.await())
        val populationRows = decodeRows<PopulationDto>(populationsResponse.await())

        // associate / associateBy build a Map — think Object.fromEntries or a
        // reduce into a lookup table. We index population by ISO3 code so the
        // join below is O(1) per country instead of a nested scan.
        val populationByCode: Map<String, Long> = populationRows
            .mapNotNull { row ->
                // mapNotNull = map + filter(Boolean) in one pass: nulls vanish.
                val value = row.value
                if (value == null || row.countryiso3code.isBlank()) null
                else row.countryiso3code to value   // `to` builds a Pair
            }
            .toMap()

        countryDtos
            // The World Bank mixes ~79 aggregates ("World", "Euro area") into
            // the country list and marks them with region "Aggregates".
            .filter { it.region.value.isNotBlank() && it.region.value != AGGREGATE_REGION }
            .map { dto -> dto.toDomain(population = populationByCode[dto.id]) }
            .sortedBy { it.name }
    }

    /**
     * Pulls the row array out of the World Bank's `[metadata, [rows]]` envelope
     * and decodes it into a list of [T].
     *
     * Two Kotlin features in one signature:
     *  - `inline`: the compiler pastes this function body into each call site.
     *  - `reified T`: because it is inlined, the real type survives to runtime,
     *    so `serializer<T>()` can be resolved. Without `reified`, the JVM erases
     *    generics and the decoder would not know what to build. This pairing is
     *    how Kotlin libraries offer `decodeFromString<Foo>(json)` with no
     *    `Foo::class` argument — compare `JSONDecoder().decode(Foo.self, ...)`
     *    in Swift, where you must pass the type explicitly.
     */
    private inline fun <reified T> decodeRows(envelope: JsonArray): List<T> {
        // The `?:` elvis operator: "use the left side, or if null, the right".
        // Here the right side throws, which also smart-casts away the null for
        // everything below.
        val rows = envelope.getOrNull(1)
            ?: throw CountryDataException(
                "Expected [metadata, rows] but got ${envelope.size} element(s)"
            )
        return try {
            json.decodeFromJsonElement(rows.jsonArray)
        } catch (error: Exception) {
            throw CountryDataException("Could not decode API rows", error)
        }
    }

    private companion object {
        // `companion object` holds members shared by all instances — Kotlin has
        // no `static`, so constants live here. This is a private one, visible
        // only inside the class.
        const val AGGREGATE_REGION = "Aggregates"
    }
}
