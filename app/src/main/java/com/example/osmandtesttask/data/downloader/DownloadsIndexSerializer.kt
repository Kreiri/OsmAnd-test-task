package com.example.osmandtesttask.data.downloader

import com.example.osmandtesttask.domain.downloader.DownloadedFileInfo
import com.example.osmandtesttask.domain.downloader.DownloadsIndex
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object DownloadsIndexSerializer {
    fun serialize(index: DownloadsIndex): String {
        return index.toJsonObject().toString()
    }

    fun deserialize(string: String): DownloadsIndex? {
        return try {
            val json = JSONObject(string)
            return json.toDownloadsIndex()
        } catch (e: JSONException) {
            null
        }
    }
}
private fun JSONObject.toDownloadsIndex(): DownloadsIndex {
    val downloadedFilesJson = getJSONArray("downloadedFiles")
    val downloadedFiles = mutableSetOf<DownloadedFileInfo>()
    for (i in 0 until downloadedFilesJson.length()) {
        val fileInfo = downloadedFilesJson.getJSONObject(i).toDownloadedFileInfo()
        downloadedFiles.add(fileInfo)
    }
    return DownloadsIndex(downloadedFiles.toSet())
}
private fun JSONObject.toDownloadedFileInfo(): DownloadedFileInfo {
    val filename = getString("filename")
    val downloadName = getString("downloadName")
    return DownloadedFileInfo(filename, downloadName)
}

private fun DownloadsIndex.toJsonObject(): JSONObject {
    val downloadedFilesJson = JSONArray()
    downloadedFiles.forEach { downloadedFilesJson.put(it.toJsonObject()) }
    return JSONObject().also { json ->
        json.put("downloadedFiles", downloadedFilesJson)
    }
}

private fun DownloadedFileInfo.toJsonObject(): JSONObject {
    return JSONObject().also { json ->
        json.put("filename", filename)
        json.put("downloadName", downloadName)
    }
}