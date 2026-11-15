package io.github.pavitra0.myusic.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Release(
    val draft: Boolean,
    @SerialName("tag_name") val tagName: String,
    val prerelease: Boolean
)
