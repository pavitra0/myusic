package io.github.pavitra0.myusic.models

import androidx.compose.runtime.Immutable

@Immutable
data class PlaylistPreview(
    val id: Long,
    val name: String,
    val songCount: Int
) {
    val playlist by lazy {
        Playlist(
            id = id,
            name = name
        )
    }
}
