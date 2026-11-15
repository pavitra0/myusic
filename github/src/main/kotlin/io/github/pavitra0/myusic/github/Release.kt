package io.github.pavitra0.myusic.github

import kotlinx.serialization.Serializable

@Serializable
data class Release(
    val draft: Boolean,
    val name: String,
    val prerelease: Boolean
)