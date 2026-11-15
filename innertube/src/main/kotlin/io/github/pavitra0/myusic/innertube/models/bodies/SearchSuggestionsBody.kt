package io.github.pavitra0.myusic.innertube.models.bodies

import io.github.pavitra0.myusic.innertube.models.Context
import io.github.pavitra0.myusic.innertube.models.YouTubeClient
import kotlinx.serialization.Serializable

@Serializable
data class SearchSuggestionsBody(
    val context: Context = YouTubeClient.WEB_REMIX.toContext(),
    val input: String
)
