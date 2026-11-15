package io.github.pavitra0.myusic.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class ButtonRenderer(
    val navigationEndpoint: NavigationEndpoint?
)
