package com.sreekumar.countryexplorer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sreekumar.countryexplorer.R
import com.sreekumar.countryexplorer.domain.Country
import com.sreekumar.countryexplorer.util.formatPopulation

/**
 * One row of the list.
 *
 * ## The Modifier parameter convention
 *
 * Every reusable Composable takes `modifier: Modifier = Modifier` as its *last*
 * parameter with a default. Modifier is Compose's styling and layout channel —
 * padding, size, click handling, background — and it is ordered: chaining
 * `.padding().clickable()` makes a different hit area than
 * `.clickable().padding()`, because each wraps the previous.
 *
 * Accepting one lets the *caller* decide layout, which is what makes a
 * component reusable — the equivalent of accepting a className/style prop.
 */
@Composable
fun CountryRow(
    country: Country,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // `clickable` before padding: the padded area is tappable too,
            // which is what you want for a list row (bigger touch target).
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        /**
         * AsyncImage downloads, decodes, caches and displays — Coil handles the
         * whole lifecycle, and cancels the request if the row scrolls away.
         * The iOS repo's SwiftUI `AsyncImage` is the direct counterpart.
         */
        AsyncImage(
            model = country.flagImageUrl,
            contentDescription = null,   // decorative: the name is right there
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 48.dp, height = 32.dp)
                .clip(RoundedCornerShape(4.dp)),
        )

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                // `weight(1f)` inside a Row = flex: 1. It takes the leftover
                // space after the fixed-size siblings are measured, which keeps
                // the heart button pinned right and lets long names truncate.
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "${country.flagEmoji}  ${country.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                // Kotlin string templates: "$var" and "${expression}".
                // `?: ` supplies a fallback when capital is null.
                text = buildString {
                    append(country.capital ?: country.region)
                    country.population?.let { append(" · ${formatPopulation(it)}") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (country.isFavorite) Icons.Filled.Favorite
                              else Icons.Filled.FavoriteBorder,
                // contentDescription is NOT optional here: it is what a screen
                // reader announces. Accessibility is a first-class API on
                // Android, not an afterthought — treat it like aria-label.
                contentDescription = stringResource(
                    if (country.isFavorite) R.string.remove_favorite
                    else R.string.add_favorite
                ),
                tint = if (country.isFavorite) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
