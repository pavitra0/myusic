package io.github.pavitra0.myusic.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.github.pavitra0.myusic.database
import io.github.pavitra0.myusic.enums.ArtistSortBy
import io.github.pavitra0.myusic.enums.SortOrder
import io.github.pavitra0.myusic.models.Artist

class HomeArtistsViewModel : ViewModel() {
    var items: List<Artist> by mutableStateOf(emptyList())

    suspend fun loadArtists(
        sortBy: ArtistSortBy,
        sortOrder: SortOrder
    ) {
        database
            .artists(sortBy, sortOrder)
            .collect { items = it }
    }
}