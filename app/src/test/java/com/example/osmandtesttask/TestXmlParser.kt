package com.example.osmandtesttask

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.osmandtesttask.data.remote.RemoteMapConfigParser
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class TestXmlParser {
    private val testXml = $$"""<?xml version="1.0" encoding="utf-8"?>
		<regions_list>
		<!-- boundary:
		1. boundary="no" - no boundary for this region
		2. boundary omit - boundary will be looked up by name
		3. boundary="$FILENAME" boundary will be looked up by $FILENAME
			in case FILENAME duplicates i.e. 'georgia', it will be accessible in 2 ways
			$PARENT_FOLDER/$FILENAME - 'us/georgia' and 'asia/georgia'
		-->
		<!-- type, map, wiki, roads, srtm, hillshade
			1. by default map=yes, srtm=yes, hillshade=yes (in case 'type' not specified)
			2. in case wiki/roads not specified, wiki/roads same as maps
			3. if map, wiki, roads, srtm, hillshade specified, it sets to value specified
			4. in case type specified it takes precedence and sets all flags = no
				Examples
				type=srtm: map=no, srtm=yes, hillshade=no
				type=hillshade: map=no, srtm=no, hillshade=yes
				type=continent: map=no, srtm=no, hillshade=no
		-->
		<!--
			download_suffix, inner_download_suffix, download_prefix, inner_download_prefix
			1. In case download_suffix, download_prefix specified, then
				download_name = download_prefix + "_" + name + "_" + download_suffix
			   In case only download_suffix speicfied
			   	download_name =  name + "_" + download_suffix
			   	In case only download_prefix speicfied
			   	download_name =  download_prefix + "_" + name
			2. If download_suffix is not specified it is taken from parent region or from parent parent region ...
			2.1 If parent region has inner_download_suffix, then download_suffix=inner_download_suffix
		-->
		<region type="continent" name="europe"
			translate="=Europe" inner_download_suffix="europe" boundary="no" poly_extract="europe">

		<!-- north-europe -->
			<region name="denmark" lang="da" poly_extract="north-europe" inner_download_prefix="$name" join_map_files="yes">
				<region type="map" name="capital-region" translate="name:en=Capital Region of Denmark;entity=relation"/>
				<region type="map" name="central-region" translate="name:en=Central Denmark Region;entity=relation"/>
				<region type="map" name="north-region" translate="name:en=North Denmark Region;entity=relation"/>
				<region type="map" name="southern-region" translate="name:en=Region of Southern Denmark;entity=relation"/>
				<region type="map" name="zealand-region" translate="name:en=Region Zealand;entity=relation"/>
			</region>
			<region name="estonia" lang="et" poly_extract="north-europe"/>
			<region name="iceland" lang="is" poly_extract="north-europe"/>
		</region>
	</regions_list>""".trimIndent()


    @Test
    fun testXml() {
        val inputStream = ByteArrayInputStream(testXml.toByteArray())
        val mapConfigParser = RemoteMapConfigParser()
        val result = mapConfigParser.parse(inputStream)
        assertNotNull(result)
        val mapConfigR = result!!

        val regionsR = mapConfigR.regions
        assertEquals(1, regionsR.size)

        val europeR = regionsR.first()
        assertEquals(3, europeR.regions.size)
        assertEquals("europe", europeR.innerDownloadSuffix)

        val denmarkR = europeR.regions.first()
        assertEquals(5, denmarkR.regions.size)

        val mapConfig = mapConfigR.toLocal()

        val regions = mapConfig.regions
        assertEquals(1, regions.size)

        val europe = regions.first()
        assertEquals(3, europe.regions.size)

        val denmark = europe.regions.first()
        assertEquals(5, denmark.regions.size)

        val denmarkCapital = denmark.regions.first()
        assertEquals("denmark_capital-region_europe", denmarkCapital.downloadName)
    }
}