package com.sreekumar.countryexplorer.data.remote

import kotlinx.serialization.Serializable
import com.sreekumar.countryexplorer.domain.Country

/**
 * DTOs = Data Transfer Objects: the *wire* shape, mirroring the JSON exactly.
 *
 * Why not decode straight into `Country`? Because then every API quirk leaks
 * into your whole app. Keeping DTOs separate and mapping once (see `toDomain`)
 * means an API change touches one file. This is the same instinct as mapping an
 * HTTP response into a view model in an Angular service.
 *
 * `@Serializable` makes the Kotlin compiler *generate* the JSON parser at build
 * time — no reflection, no runtime surprises. TS has nothing equivalent built
 * in; it is closest to generating a zod schema from a type automatically.
 */
@Serializable
data class CountryDto(
    val id: String,              // "JPN"
    val iso2Code: String,        // "JP"
    val name: String,            // "Japan"
    val region: CodedValueDto,
    val incomeLevel: CodedValueDto,
    // A default value makes the field optional in the JSON. Without a default,
    // a missing key is a hard parse error — which is usually what you want.
    val capitalCity: String = "",
    // NOTE: the World Bank sends coordinates as *strings*, sometimes empty ("").
    // Decode them exactly as sent, then convert in the mapper below. Lying to
    // the decoder about wire types is how you get runtime crashes.
    val longitude: String = "",
    val latitude: String = "",
)

@Serializable
data class CodedValueDto(
    val id: String = "",
    val value: String = "",
)

/** One row of the population indicator response. */
@Serializable
data class PopulationDto(
    val countryiso3code: String = "",
    val date: String = "",
    val value: Long? = null,     // genuinely null for countries without data
)

/**
 * The single place where wire shape becomes app shape.
 *
 * This is an *extension function*: it adds a method to CountryDto without
 * subclassing or touching the class. Kotlin's most distinctive feature, and the
 * reason you'll see `"foo".capitalize()`-style helpers everywhere. Think of it
 * as a standalone function with nicer call syntax.
 */
fun CountryDto.toDomain(population: Long?): Country = Country(
    code = id,
    alpha2 = iso2Code,
    name = name,
    // `.ifBlank { null }` — if the string is empty/whitespace, use null instead.
    // Normalising "absent" to a single representation (null) early saves you
    // from `if (x != "" && x != null)` checks all over the UI.
    capital = capitalCity.ifBlank { null },
    region = region.value,
    incomeLevel = incomeLevel.value,
    // toDoubleOrNull() returns null instead of throwing — the safe parse.
    latitude = latitude.toDoubleOrNull(),
    longitude = longitude.toDoubleOrNull(),
    population = population,
)
