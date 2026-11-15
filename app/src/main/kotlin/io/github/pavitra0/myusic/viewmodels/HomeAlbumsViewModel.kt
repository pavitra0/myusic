package io.github.pavitra0.myusic.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.github.pavitra0.myusic.database
import io.github.pavitra0.myusic.enums.AlbumSortBy
import io.github.pavitra0.myusic.enums.SortOrder
import io.github.pavitra0.myusic.models.Album

class HomeAlbumsViewModel : ViewModel() {
    var items: List<Album> by mutableStateOf(emptyList())

    suspend fun loadAlbums(
        sortBy: AlbumSortBy,
        sortOrder: SortOrder
    ) {
        database
            .albums(sortBy, sortOrder)
            .collect { items = it }
    }
}