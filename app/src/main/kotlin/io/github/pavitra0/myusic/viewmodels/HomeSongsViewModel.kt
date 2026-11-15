package io.github.pavitra0.myusic.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.github.pavitra0.myusic.database
import io.github.pavitra0.myusic.enums.SongSortBy
import io.github.pavitra0.myusic.enums.SortOrder
import io.github.pavitra0.myusic.models.Song

class HomeSongsViewModel : ViewModel() {
    var items: List<Song> by mutableStateOf(emptyList())

    suspend fun loadSongs(
        sortBy: SongSortBy,
        sortOrder: SortOrder
    ) {
        database
            .songs(sortBy, sortOrder)
            .collect { items = it }
    }
}