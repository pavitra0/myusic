package io.github.pavitra0.myusic.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import io.github.pavitra0.myusic.innertube.Innertube
import io.github.pavitra0.myusic.innertube.utils.plus

class ItemsPageViewModel<T : Innertube.Item> : ViewModel() {
    var itemsMap: MutableMap<String, Innertube.ItemsPage<T>?> = mutableStateMapOf()

    fun setItems(
        tag: String,
        items: Innertube.ItemsPage<T>?
    ) {
        if (!itemsMap.containsKey(tag)) itemsMap[tag] = items
        else itemsMap[tag].plus(items!!)
    }
}