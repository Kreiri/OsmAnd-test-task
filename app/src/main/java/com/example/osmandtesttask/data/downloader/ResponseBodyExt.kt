package com.example.osmandtesttask.data.downloader

import com.example.osmandtesttask.domain.downloader.DownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import java.io.File
import kotlin.coroutines.cancellation.CancellationException


fun ResponseBody.toFileWithProgress(destination: File) = flow<DownloadState> {
    val totalBytes = contentLength()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var bytesRead = 0L

    emit(DownloadState.Progress(0, totalBytes))

    try {
        if (destination.exists()) destination.delete()
        destination.parentFile?.mkdirs()

        byteStream().buffered().use { inputStream ->
            destination.outputStream().buffered().use { outputStream ->
                while (true) {
                    val bufferBytesRead = inputStream.read(buffer)
                    if (bufferBytesRead == -1) break

                    outputStream.write(buffer, 0, bufferBytesRead)
                    bytesRead += bufferBytesRead

                    if (bytesRead == totalBytes) emit(DownloadState.Finished)
                    else emit(DownloadState.Progress(bytesRead, totalBytes))
                }
                outputStream.flush()
            }
        }
    } catch (e: Throwable) {
        if (destination.exists()) {
            destination.delete()
        }
        if (e is CancellationException) {
            emit(DownloadState.Cancelled)
            throw e
        }
        emit(DownloadState.Error(e))
    }
}.flowOn(Dispatchers.IO)