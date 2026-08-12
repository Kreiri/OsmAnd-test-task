package com.example.osmandtesttask.data.remote

import android.util.Xml
import com.example.osmandtesttask.util.xml.BaseTreeXmlParser
import com.example.osmandtesttask.util.xml.TagContext
import com.example.osmandtesttask.util.xml.readStringAttribute
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.InputStream

class RemoteMapConfigParser : BaseTreeXmlParser() {
    fun parse(inputStream: InputStream): RemoteMapListConfig? {
        return try {
            inputStream.use { stream ->
                val parser = Xml.newPullParser().apply {
                    setInput(stream, null)
                }
                readRegionsList(parser)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun readRegionsList(parser: XmlPullParser): RemoteMapListConfig {
        val rootContext = MapListConfigBuilder("regions_list")
        parse(parser, rootContext)
        val config = rootContext.data
        return config
    }
}

class MapListConfigBuilder(override val contextTagName: String): TagContext {
    val regions = mutableListOf<RemoteRegion>()
    private var mapConfig: RemoteMapListConfig? = null
    override val data: RemoteMapListConfig
        get() = mapConfig ?: throw RuntimeException("Tag data has not finished being read")

    override fun readAttributes(parser: XmlPullParser) {}
    override fun onText(text: String) {}

    override fun onEndTag() {
        this.mapConfig = RemoteMapListConfig(regions.toList())
    }

    override fun createChildContext(tagName: String): TagContext? {
        return when(tagName) {
            "region" -> RemoteRegionBuilder(tagName)
            else -> null
        }
    }

    override fun onChildContextCompleted(child: TagContext) {
        if (child is RemoteRegionBuilder) {
            val region = child.data
            regions.add(region)
        }
    }
}
class RemoteRegionBuilder(override val contextTagName: String) : TagContext {
    var name: String = ""
    var translate: String? = null
    var downloadSuffix: String? = null
    var innerDownloadSuffix: String? = null
    var downloadPrefix: String? = null
    var innerDownloadPrefix: String? = null
    var type: String? = null
    var map: Boolean? = null
    val children = mutableListOf<RemoteRegion>()

    private var region: RemoteRegion? = null

    override val data: RemoteRegion
        get() = region ?: throw RuntimeException("Tag data has not finished being read")

    override fun readAttributes(parser: XmlPullParser) {
        readRegion(parser)
    }

    override fun createChildContext(tagName: String): TagContext? {
        return when(tagName) {
            "region" -> RemoteRegionBuilder(tagName)
            else -> null
        }
    }

    override fun onEndTag() {
       this.region = RemoteRegion(
           name = name,
           translate = translate,
           downloadSuffix = downloadSuffix,
           innerDownloadSuffix = innerDownloadSuffix,
           downloadPrefix = downloadPrefix,
           innerDownloadPrefix = innerDownloadPrefix,
           type = type,
           map = map,
           regions = children.toList()
       )
    }

    override fun onChildContextCompleted(child: TagContext) {
        if (child is RemoteRegionBuilder) {
            val childRegion = child.data
            children.add(childRegion)
        }
    }


    override fun onText(text: String) {}

    private fun readRegion(parser: XmlPullParser) {
        this.name = readStringAttribute(parser, "name")
            ?: throw XmlPullParserException("region.name must be present")
        this.translate = readStringAttribute(parser, "translate")
        this.downloadSuffix = readStringAttribute(parser, "download_suffix")
        this.innerDownloadSuffix = readStringAttribute(parser, "inner_download_suffix")
        this.downloadPrefix = readStringAttribute(parser, "download_prefix")
        this.innerDownloadPrefix = readStringAttribute(parser, "inner_download_prefix")
        this.type = readStringAttribute(parser, "type")
        this.map = readStringAttribute(parser, "map")?.parseAsBoolean()

    }

}

private fun String.parseAsBoolean(): Boolean {
    return this.toBoolean() || "yes".equals(this, ignoreCase = true)
}