package com.github.musicyou.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.innertube.Innertube
import com.github.innertube.requests.relatedPage
import com.github.musicyou.database
import com.github.musicyou.enums.QuickPicksSource
import com.github.musicyou.models.Song
import kotlinx.coroutines.flow.distinctUntilChanged

class QuickPicksViewModel : ViewModel() {
    var trending: Song? by mutableStateOf(null)
    var relatedPageResult: Result<Innertube.RelatedPage?>? by mutableStateOf(null)

    suspend fun loadQuickPicks(quickPicksSource: QuickPicksSource) {
        val flow = when (quickPicksSource) {
            QuickPicksSource.Trending -> database.trending()
            QuickPicksSource.LastPlayed -> database.lastPlayed()
            QuickPicksSource.Random -> database.randomSong()
        }

        flow.distinctUntilChanged().collect { song ->
            if (quickPicksSource == QuickPicksSource.Random && song != null && trending != null) return@collect

            if (relatedPageResult == null || trending?.id != song?.id || relatedPageResult?.isSuccess != true) {
                val videoId = song?.id ?: "fJ9rUzIMcZQ"

                // Try the actual video, then a known-good fallback
                val res = Innertube.relatedPage(videoId = videoId)

                relatedPageResult = when {
                    // Success with songs
                    res != null && res.isSuccess && !res.getOrNull()?.songs.isNullOrEmpty() -> res

                    // Try fallback video
                    else -> {
                        val fallback = Innertube.relatedPage(videoId = "kJQP7kiw5Fk")
                        when {
                            fallback != null && fallback.isSuccess -> fallback
                            fallback != null -> fallback // propagate the error Result
                            res != null -> res // use original (may be error or empty)
                            else -> Result.failure(Exception("Failed to load quick picks"))
                        }
                    }
                }
            }

            trending = song
        }
    }
}