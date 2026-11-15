package io.github.pavitra0.myusic.kugou

import kotlin.coroutines.cancellation.CancellationException

internal fun <T> Result<T>.recoverIfCancelled(): Result<T>? {
    return when (exceptionOrNull()) {
        is CancellationException -> null
        else -> this
    }
}
