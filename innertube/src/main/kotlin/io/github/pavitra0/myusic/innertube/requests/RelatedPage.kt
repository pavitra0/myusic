package io.github.pavitra0.myusic.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.github.pavitra0.myusic.innertube.Innertube
import io.github.pavitra0.myusic.innertube.models.BrowseResponse
import io.github.pavitra0.myusic.innertube.models.MusicCarouselShelfRenderer
import io.github.pavitra0.myusic.innertube.models.NextResponse
import io.github.pavitra0.myusic.innertube.models.bodies.BrowseBody
import io.github.pavitra0.myusic.innertube.models.bodies.NextBody
import io.github.pavitra0.myusic.innertube.utils.findSectionByStrapline
import io.github.pavitra0.myusic.innertube.utils.findSectionByTitle
import io.github.pavitra0.myusic.innertube.utils.from
import io.github.pavitra0.myusic.innertube.utils.runCatchingNonCancellable

suspend fun Innertube.relatedPage(videoId: String) = runCatchingNonCancellable {
    val nextResponse = client.post(NEXT) {
        setBody(NextBody(videoId = videoId))
        mask("contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.tabRenderer(endpoint,title)")
    }.body<NextResponse>()

    val tabs = nextResponse
        .contents
        ?.singleColumnMusicWatchNextResultsRenderer
        ?.tabbedRenderer
        ?.watchNextTabbedResultsRenderer
        ?.tabs

    val browseId = tabs
        ?.find { tab ->
            val id = tab.tabRenderer?.endpoint?.browseEndpoint?.browseId
            val titleText = tab.tabRenderer?.title
            id?.startsWith("FMrp") == true ||
            (titleText != null && (titleText.contains("related", ignoreCase = true) || titleText.contains("you might", ignoreCase = true)))
        }
        ?.tabRenderer
        ?.endpoint
        ?.browseEndpoint
        ?.browseId
        ?: tabs?.getOrNull(2)?.tabRenderer?.endpoint?.browseEndpoint?.browseId
        ?: tabs?.getOrNull(1)?.tabRenderer?.endpoint?.browseEndpoint?.browseId
        ?: return@runCatchingNonCancellable null

    val response = client.post(BROWSE) {
        setBody(
            BrowseBody(
                localized = false,
                browseId = browseId
            )
        )
        mask("contents.sectionListRenderer.contents.musicCarouselShelfRenderer(header.musicCarouselShelfBasicHeaderRenderer(title,strapline),contents($MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK,$MUSIC_TWO_ROW_ITEM_RENDERER_MASK))")
    }.body<BrowseResponse>()

    val sectionListRenderer = response
        .contents
        ?.sectionListRenderer

    val songsSection = sectionListRenderer?.findSectionByTitle("You might also like")
        ?: sectionListRenderer?.contents?.firstOrNull { content ->
            content.musicCarouselShelfRenderer?.contents?.any { it.musicResponsiveListItemRenderer != null } == true
        }

    val playlistsSection = sectionListRenderer?.findSectionByTitle("Recommended playlists")
        ?: sectionListRenderer?.contents?.find { content ->
            val titleText = content.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text
            titleText?.contains("playlist", ignoreCase = true) == true
        }

    val albumsSection = sectionListRenderer?.findSectionByStrapline("MORE FROM")
        ?: sectionListRenderer?.findSectionByTitle("Albums")
        ?: sectionListRenderer?.contents?.find { content ->
            val titleText = content.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text
            titleText?.contains("album", ignoreCase = true) == true
        }

    val artistsSection = sectionListRenderer?.findSectionByTitle("Similar artists")
        ?: sectionListRenderer?.contents?.find { content ->
            val titleText = content.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text
            titleText?.contains("artist", ignoreCase = true) == true
        }

    Innertube.RelatedPage(
        songs = songsSection
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
            ?.mapNotNull(Innertube.SongItem::from),
        playlists = playlistsSection
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.PlaylistItem::from)
            ?.sortedByDescending { it.channel?.name == "YouTube Music" },
        albums = albumsSection
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.AlbumItem::from),
        artists = artistsSection
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.ArtistItem::from),
    )
}