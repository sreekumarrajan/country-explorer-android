package com.sreekumar.countryexplorer.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sreekumar.countryexplorer.R
import com.sreekumar.countryexplorer.ui.components.EmptyState
import com.sreekumar.countryexplorer.ui.components.LoadingState
import com.sreekumar.countryexplorer.util.formatPopulation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryDetailScreen(
    viewModel: CountryDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val country = uiState.country

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(country?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // AutoMirrored icons flip automatically in right-to-left
                        // locales (Arabic, Hebrew). Always use them for
                        // directional icons — free RTL support.
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (country != null) {
                        IconButton(onClick = viewModel::onToggleFavorite) {
                            Icon(
                                imageVector = if (country.isFavorite) Icons.Filled.Favorite
                                              else Icons.Filled.FavoriteBorder,
                                contentDescription = stringResource(
                                    if (country.isFavorite) R.string.remove_favorite
                                    else R.string.add_favorite
                                ),
                            )
                        }
                    }
                },
            )
        },
    ) { contentPadding ->
        when {
            uiState.isLoading -> LoadingState(Modifier.padding(contentPadding))
            country == null -> EmptyState(
                message = stringResource(R.string.error_generic),
                modifier = Modifier.padding(contentPadding),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    // A plain Column does not scroll; you opt in. Forgetting
                    // this is why content gets clipped on small screens.
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AsyncImage(
                    model = country.flagImageUrl,
                    contentDescription = "Flag of ${country.name}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )

                Text(
                    text = "${country.flagEmoji}  ${country.name}",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 12.dp),
                )

                FactRow("Capital", country.capital ?: "—")
                FactRow("Region", country.region)
                FactRow("Income level", country.incomeLevel)
                FactRow(
                    "Population",
                    country.population?.let(::formatPopulation) ?: "—",
                )
                FactRow("ISO codes", "${country.alpha2} · ${country.code}")
                FactRow(
                    "Coordinates",
                    if (country.latitude != null && country.longitude != null) {
                        // %.2f is printf-style formatting, same as C/Java.
                        "%.2f, %.2f".format(country.latitude, country.longitude)
                    } else "—",
                )
            }
        }
    }
}

/**
 * A small private Composable. Breaking the screen into named pieces is free —
 * Compose functions are not components with instances or mounting costs, they
 * are just function calls that emit UI.
 */
@Composable
private fun FactRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        HorizontalDivider()
    }
}
