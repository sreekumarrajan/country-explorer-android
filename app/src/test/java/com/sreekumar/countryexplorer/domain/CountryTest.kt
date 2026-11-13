package com.sreekumar.countryexplorer.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-function tests: no coroutines, no fakes, no setup. When logic lives on a
 * plain data class like this, testing it costs nothing — which is the argument
 * for keeping computed values in the domain layer instead of inline in the UI.
 */
class CountryTest {

    private fun country(alpha2: String) = Country(
        code = "XXX", alpha2 = alpha2, name = "Test", capital = null,
        region = "R", incomeLevel = "I", latitude = null, longitude = null,
        population = null,
    )

    @Test
    fun `builds flag emoji from ISO alpha-2 code`() {
        assertEquals("🇯🇵", country("JP").flagEmoji)
        assertEquals("🇮🇳", country("IN").flagEmoji)
        assertEquals("🇰🇪", country("ke").flagEmoji)   // lowercase input works
    }

    @Test
    fun `falls back to a neutral flag for malformed codes`() {
        assertEquals("🏳️", country("").flagEmoji)
        assertEquals("🏳️", country("X").flagEmoji)
        assertEquals("🏳️", country("1A").flagEmoji)
    }

    @Test
    fun `builds a lowercase flag image url`() {
        assertEquals("https://flagcdn.com/w320/jp.png", country("JP").flagImageUrl)
    }
}
