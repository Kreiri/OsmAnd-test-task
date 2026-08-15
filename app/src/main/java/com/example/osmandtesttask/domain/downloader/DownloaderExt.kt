package com.example.osmandtesttask.domain.downloader

import com.example.osmandtesttask.domain.models.Region

fun DownloadTask.matches(region: Region) = this.downloadName == region.downloadName
fun DownloaderState.Processing.hasActive(region: Region) = this.activeDownload.matches(region)
fun DownloaderState.Processing.hasQueued(region: Region) =
    this.enqueuedDownloads.any { it.matches(region) }

fun DownloadedFileInfo.matches(region: Region) = this.downloadName == region.downloadName
fun Collection<DownloadedFileInfo>.hasFileForRegion(region: Region) =
    this.any { it.matches(region) }