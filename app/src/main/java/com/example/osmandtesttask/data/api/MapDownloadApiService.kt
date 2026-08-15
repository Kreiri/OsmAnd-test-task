package com.example.osmandtesttask.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

interface MapDownloadApiService {
    @Streaming
    @GET("/download.php?standard=yes")
    suspend fun downloadMap(@Query("file") fileName: String): Response<ResponseBody>
}