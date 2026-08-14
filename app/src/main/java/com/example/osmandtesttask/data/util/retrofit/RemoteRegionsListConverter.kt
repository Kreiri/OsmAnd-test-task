package com.example.osmandtesttask.data.util.retrofit

import com.example.osmandtesttask.data.remote.dto.RemoteRegionsList
import com.example.osmandtesttask.data.remote.parsers.RemoteRegionsListParser
import okhttp3.ResponseBody
import org.xmlpull.v1.XmlPullParserException
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class RemoteRegionsListConverter : Converter<ResponseBody, RemoteRegionsList> {
    override fun convert(value: ResponseBody): RemoteRegionsList? {
        return try {
            value.byteStream().use { inputStream ->
                RemoteRegionsListParser().parse(inputStream)
            }
        } catch (e: XmlPullParserException) {
            null
        }
    }

    class Factory : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type,
            annotations: Array<out Annotation?>,
            retrofit: Retrofit
        ): Converter<ResponseBody, RemoteRegionsList>? {
            if (type == RemoteRegionsList::class.java) {
                return RemoteRegionsListConverter()
            }
            return null
        }

        companion object {
            fun create() = Factory()
        }
    }
}