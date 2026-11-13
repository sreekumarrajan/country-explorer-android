package com.sreekumar.countryexplorer.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sreekumar.countryexplorer.data.CountryRepository
import com.sreekumar.countryexplorer.domain.Country
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CountryDetailUiState(
    val country: Country? = null,
    val isLoading: Boolean = true,
)

/**
 * The detail screen's ViewModel. Note what it does *not* do: it never receives
 * a `Country` object from the list screen.
 *
 * It is handed only a country code and reads that country from the repository.
 * Passing the whole object would be faster to write and immediately wrong:
 * favourite that country here and the list screen would show stale data.
 * Reading both screens from the same source means they cannot disagree.
 *
 * Same principle as "pass an id, select from the store" in Redux rather than
 * passing a snapshot down through props.
 */
class CountryDetailViewModel(
    private val repository: CountryRepository,
    private val countryCode: String,
) : ViewModel() {

    val uiState: StateFlow<CountryDetailUiState> =
        repository.country(countryCode)
            .map { country -> CountryDetailUiState(country = country, isLoading = false) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CountryDetailUiState(),
            )

    init {
        // Covers the cold-start case: if the process was killed and restored
        // straight onto this screen, the in-memory cache is empty.
        viewModelScope.launch {
            runCatching { repository.refreshIfEmpty() }
        }
    }

    fun onToggleFavorite() {
        val current = uiState.value.country ?: return
        viewModelScope.launch {
            repository.setFavorite(current.code, isFavorite = !current.isFavorite)
        }
    }

    companion object {
        fun factory(repository: CountryRepository, countryCode: String) = viewModelFactory {
            initializer { CountryDetailViewModel(repository, countryCode) }
        }
    }
}
