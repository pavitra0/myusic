package io.github.pavitra0.myusic.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val contents: Contents?,
) {
    @Serializable
    data class Contents(
        val tabbedSearchResultsRenderer: Tabs?
    )
}
