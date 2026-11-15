package io.github.pavitra0.myusic.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicTwoRowItemRenderer(
    val navigationEndpoint: NavigationEndpoint?,
    val thumbnailRenderer: ThumbnailRenderer?,
    val title: Runs?,
    val subtitle: Runs?,
)
