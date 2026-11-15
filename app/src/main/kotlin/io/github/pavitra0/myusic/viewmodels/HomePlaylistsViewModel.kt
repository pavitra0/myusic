package io.github.pavitra0.myusic.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.github.pavitra0.myusic.database
import io.github.pavitra0.myusic.enums.PlaylistSortBy
import io.github.pavitra0.myusic.enums.SortOrder
import io.github.pavitra0.myusic.models.PlaylistPreview

class HomePlaylistsViewModel : ViewModel() {
    var items: List<PlaylistPreview> by mutableStateOf(emptyList())

    suspend fun loadArtists(
        sortBy: PlaylistSortBy,
        sortOrder: SortOrder
    ) {
        database
            .playlistPreviews(sortBy, sortOrder)
            .collect { items = it }
    }
}