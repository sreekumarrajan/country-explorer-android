package com.sreekumar.countryexplorer

import com.sreekumar.countryexplorer.domain.Country

/**
 * Test fixtures. A factory with default arguments beats repeating the full
 * constructor in every test: each test overrides only the field it cares about,
 * so a test reads as "a country named X" instead of ten lines of noise.
 */
fun country(
    code: String = "JPN",
    alpha2: String = "JP",
    name: String = "Japan",
    capital: String? = "Tokyo",
    region: String = "East Asia & Pacific",
    incomeLevel: String = "High income",
    population: Long? = 123_366_734,   // underscores are legal in numeric literals
    isFavorite: Boolean = false,
) = Country(
    code = code,
    alpha2 = alpha2,
    name = name,
    capital = capital,
    region = region,
    incomeLevel = incomeLevel,
    latitude = 35.67,
    longitude = 139.77,
    population = population,
    isFavorite = isFavorite,
)
