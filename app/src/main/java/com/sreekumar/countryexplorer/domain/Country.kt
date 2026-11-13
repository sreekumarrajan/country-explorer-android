package com.sreekumar.countryexplorer.domain

/**
 * The app's own model of a country — the shape the UI wants, not the shape any
 * API happens to return.
 *
 * Coming from TypeScript, read `data class` as "interface + generated runtime":
 * Kotlin writes equals(), hashCode(), toString() and copy() for you.
 *
 *   TS:      const next = { ...country, isFavorite: true }
 *   Kotlin:  val next = country.copy(isFavorite = true)
 *
 * `val` means immutable (TS `readonly`); `var` would be mutable. Default to
 * `val` everywhere — Compose, like React, relies on new objects rather than
 * mutated ones to know something changed.
 *
 * `String?` is a *nullable* String. Kotlin's biggest safety win over TS: the
 * compiler forces you to handle null, and there is no `any` escape hatch.
 */
data class Country(
    val code: String,          // ISO 3166-1 alpha-3 ("JPN") — our stable identity
    val alpha2: String,        // ISO 3166-1 alpha-2 ("JP")  — for flags
    val name: String,
    val capital: String?,      // nullable: a few territories have no capital
    val region: String,
    val incomeLevel: String,
    val latitude: Double?,
    val longitude: Double?,
    val population: Long?,     // nullable: the World Bank lacks data for a few
    val isFavorite: Boolean = false,   // default value = optional constructor arg
) {
    /**
     * `get()` with no backing field is a *computed property* — identical in
     * spirit to a TS getter or an Angular getter in a component class. It is
     * recalculated on each read and stores nothing.
     */
    val flagImageUrl: String
        get() = "https://flagcdn.com/w320/${alpha2.lowercase()}.png"

    /**
     * Turns "JP" into 🇯🇵. Flag emoji are just two "regional indicator symbols",
     * which sit at Unicode 0x1F1E6 ('A') through 0x1F1FF ('Z').
     *
     * The identical algorithm appears in the iOS repo — a good place to compare
     * the two standard libraries side by side.
     */
    val flagEmoji: String
        get() {
            val letters = alpha2.uppercase()
            if (letters.length != 2 || letters.any { it !in 'A'..'Z' }) return "🏳️"
            // `map` here is the same map you know; `joinToString("")` is .join("")
            return letters
                .map { letter -> Character.toChars(0x1F1E6 + (letter - 'A')).concatToString() }
                .joinToString("")
        }
}
