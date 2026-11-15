package io.github.pavitra0.myusic.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pavitra0.myusic.LocalPlayerPadding
import io.github.pavitra0.myusic.R
import io.github.pavitra0.myusic.enums.AlbumSortBy
import io.github.pavitra0.myusic.enums.SortOrder
import io.github.pavitra0.myusic.models.Album
import io.github.pavitra0.myusic.ui.components.HomeScaffold
import io.github.pavitra0.myusic.ui.components.SortingHeader
import io.github.pavitra0.myusic.ui.items.LocalAlbumItem
import io.github.pavitra0.myusic.utils.albumSortByKey
import io.github.pavitra0.myusic.utils.albumSortOrderKey
import io.github.pavitra0.myusic.utils.rememberPreference
import io.github.pavitra0.myusic.viewmodels.HomeAlbumsViewModel

@Composable
fun HomeAlbums(
    openSearch: () -> Unit,
    openSettings: () -> Unit,
    onAlbumClick: (Album) -> Unit
) {
    val playerPadding = LocalPlayerPadding.current

    var sortBy by rememberPreference(albumSortByKey, AlbumSortBy.Title)
    var sortOrder by rememberPreference(albumSortOrderKey, SortOrder.Ascending)

    val viewModel: HomeAlbumsViewModel = viewModel()

    LaunchedEffect(sortBy, sortOrder) {
        viewModel.loadAlbums(
            sortBy = sortBy,
            sortOrder = sortOrder
        )
    }

    HomeScaffold(
        title = R.string.albums,
        openSearch = openSearch,
        openSettings = openSettings
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                bottom = 16.dp + playerPadding
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(
                key = "header",
                span = { GridItemSpan(maxCurrentLineSpan) }
            ) {
                SortingHeader(
                    sortBy = sortBy,
                    changeSortBy = { sortBy = it },
                    sortByEntries = AlbumSortBy.entries.toList(),
                    sortOrder = sortOrder,
                    toggleSortOrder = { sortOrder = !sortOrder },
                    size = viewModel.items.size,
                    itemCountText = R.plurals.number_of_albums
                )
            }

            items(
                items = viewModel.items,
                key = Album::id
            ) { album ->
                LocalAlbumItem(
                    modifier = Modifier.animateItem(),
                    album = album,
                    onClick = { onAlbumClick(album) }
                )
            }
        }
    }
}