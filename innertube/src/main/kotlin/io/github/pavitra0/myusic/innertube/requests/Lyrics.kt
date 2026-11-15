package io.github.pavitra0.myusic.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.github.pavitra0.myusic.innertube.Innertube
import io.github.pavitra0.myusic.innertube.models.BrowseResponse
import io.github.pavitra0.myusic.innertube.models.NextResponse
import io.github.pavitra0.myusic.innertube.models.bodies.BrowseBody
import io.github.pavitra0.myusic.innertube.models.bodies.NextBody
import io.github.pavitra0.myusic.innertube.utils.runCatchingNonCancellable

suspend fun Innertube.lyrics(videoId: String): Result<String?>? = runCatchingNonCancellable {
    val nextResponse = client.post(NEXT) {
        setBody(NextBody(videoId = videoId))
        mask("contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.tabRenderer(endpoint,title)")
    }.body<NextResponse>()

    val browseId = nextResponse
        .contents
        ?.singleColumnMusicWatchNextResultsRenderer
        ?.tabbedRenderer
        ?.watchNextTabbedResultsRenderer
        ?.tabs
        ?.getOrNull(1)
        ?.tabRenderer
        ?.endpoint
        ?.browseEndpoint
        ?.browseId
        ?: return@runCatchingNonCancellable null

    val response = client.post(BROWSE) {
        setBody(BrowseBody(browseId = browseId))
        mask("contents.sectionListRenderer.contents.musicDescriptionShelfRenderer.description")
    }.body<BrowseResponse>()

    response.contents
        ?.sectionListRenderer
        ?.contents
        ?.firstOrNull()
        ?.musicDescriptionShelfRenderer
        ?.description
        ?.text
}