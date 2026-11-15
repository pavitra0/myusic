package io.github.pavitra0.myusic

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.crossfade
import io.github.pavitra0.myusic.innertube.Innertube
import io.github.pavitra0.myusic.innertube.requests.visitorData
import io.github.pavitra0.myusic.database.DatabaseInitializer
import io.github.pavitra0.myusic.enums.CoilDiskCacheMaxSize
import io.github.pavitra0.myusic.utils.coilDiskCacheMaxSizeKey
import io.github.pavitra0.myusic.utils.getEnum
import io.github.pavitra0.myusic.utils.preferences
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainApplication : Application(), SingletonImageLoader.Factory {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        database = DatabaseInitializer.newInstance(context = applicationContext)

        GlobalScope.launch {
            if (Innertube.visitorData.isNullOrBlank()) Innertube.visitorData =
                Innertube.visitorData().getOrNull()
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .diskCache(
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil"))
                    .maxSizeBytes(
                        preferences.getEnum(
                            coilDiskCacheMaxSizeKey,
                            CoilDiskCacheMaxSize.`128MB`
                        ).bytes
                    )
                    .build()
            )
            .build()
    }
}