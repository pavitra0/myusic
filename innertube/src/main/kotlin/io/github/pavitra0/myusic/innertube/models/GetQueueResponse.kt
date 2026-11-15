package io.github.pavitra0.myusic.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class GetQueueResponse(
    val queueDatas: List<QueueData>?,
) {
    @Serializable
    data class QueueData(
        val content: NextResponse.MusicQueueRenderer.Content.PlaylistPanelRenderer.Content?
    )
}
