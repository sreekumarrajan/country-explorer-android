package com.sreekumar.countryexplorer.util

import java.text.NumberFormat
import java.util.Locale

/**
 * A top-level function — Kotlin does not require a class to hold it, unlike
 * Java. The file becomes `FormattingKt` on the JVM, but from Kotlin you just
 * import `formatPopulation`.
 *
 * `Locale.getDefault()` means an Indian user sees 1,23,45,678 and a US user
 * sees 12,345,678. Never hand-roll thousands separators.
 */
fun formatPopulation(value: Long): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)
