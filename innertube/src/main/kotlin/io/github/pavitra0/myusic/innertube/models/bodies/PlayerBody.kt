package io.github.pavitra0.myusic.innertube.models.bodies

import io.github.pavitra0.myusic.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlayerBody(
    val context: Context,
    val videoId: String,
    val playlistId: String? = null
)