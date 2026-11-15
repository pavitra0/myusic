package io.github.pavitra0.myusic.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import io.github.pavitra0.myusic.innertube.Innertube
import io.github.pavitra0.myusic.innertube.requests.artistPage
import io.github.pavitra0.myusic.database
import io.github.pavitra0.myusic.models.Artist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ArtistViewModel : ViewModel() {
    var artist: Artist? by mutableStateOf(null)
    var artistPage: Innertube.ArtistPage? by mutableStateOf(null)

    suspend fun loadArtist(browseId: String, tabIndex: Int) {
        database
            .artist(browseId)
            .combine(snapshotFlow { tabIndex }.map { it != 4 }) { artist, mustFetch -> artist to mustFetch }
            .distinctUntilChanged()
            .collect { (currentArtist, mustFetch) ->
                artist = currentArtist

                if (artistPage == null && (currentArtist?.timestamp == null || mustFetch)) {
                    withContext(Dispatchers.IO) {
                        Innertube.artistPage(browseId = browseId)
                            ?.onSuccess { currentArtistPage ->
                                artistPage = currentArtistPage

                                database.upsert(
                                    Artist(
                                        id = browseId,
                                        name = currentArtistPage.name,
                                        thumbnailUrl = currentArtistPage.thumbnail?.url,
                                        timestamp = System.currentTimeMillis(),
                                        bookmarkedAt = currentArtist?.bookmarkedAt
                                    )
                                )
                            }
                    }
                }
            }
    }
}