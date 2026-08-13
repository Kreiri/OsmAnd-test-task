package com.example.osmandtesttask.data.repository

import com.example.osmandtesttask.data.remote.parsers.RemoteRegionsListParser
import com.example.osmandtesttask.domain.errors.AppError
import com.example.osmandtesttask.domain.models.RegionsList
import com.example.osmandtesttask.domain.repository.IRegionRepository
import kotlinx.coroutines.delay
import java.io.ByteArrayInputStream
import kotlin.time.Duration.Companion.milliseconds

class DummyRepository : IRegionRepository {
    private val parser = RemoteRegionsListParser()
    private var data: RegionsList? = null

    override suspend fun getRegionsList(): Result<RegionsList> {
        delay(2.milliseconds)

        val cached = data
        if (cached != null) return Result.success(cached)

        val result = runCatching { parseDummyData() }
        data = if (result.isSuccess) {
            result.getOrThrow()
        } else {
            null
        }
        return result
    }

    private fun parseDummyData(): RegionsList {
        val config = try {
            val inputStream = ByteArrayInputStream(DUMMY_XML.toByteArray())
            val response = parser.parse(inputStream)
            response.toLocal()
        } catch (e: Exception) {
            throw AppError.General(e)
        }
        return config
    }
}


private val DUMMY_XML = $$"""<?xml version="1.0" encoding="utf-8"?>
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
			<region name="faroe-islands" translate="Faroe Islands;entity=node" lang="fo" poly_extract="north-europe"/>
			<region name="finland" inner_download_prefix="$name" srtm="no" lang="fi,sv" poly_extract="north-europe" join_map_files="yes">
				<region map="yes" name="aland" translate="name:fi=Ahvenanmaa;admin_level=3;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" name="eastern-finland" translate="Eastern Finland;entity=relation"/>
				<region name="northern-finland" map="yes" srtm="no" hillshade="no" wiki="no" translate="name:en=Northern Finland;entity=relation">
					<region type="srtm" name="lapland" translate="Lapland;entity=relation"/>
					<region type="srtm" name="northern-ostrobothnia" translate="name:en=Northern Ostrobothnia;entity=relation"/>
				</region>
				<region map="yes" srtm="yes" hillshade="no" name="southern-finland" translate="Southern Finland;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" name="western-finland" translate="Western Finland;entity=relation"/>
			</region>
			<region name="latvia" lang="lv" poly_extract="north-europe"/>
			<region name="lithuania" lang="lt" poly_extract="north-europe"/>
			<region name="norway" srtm="no" inner_download_prefix="$name" lang="nb,nn" poly_extract="north-europe" join_map_files="yes">
				<region srtm="yes" hillshade="no" wiki="no" name="akershus" translate="Akershus;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="astfold" translate="Østfold;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="aust-agder" translate="name:en=East Agder;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="buskerud" translate="Buskerud;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="finnmark" translate="Finnmark;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="hedmark" translate="Hedmark;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="hordaland" translate="Hordaland;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="more-og-romsdal" translate="Møre og Romsdal;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="nordland" translate="Nordland;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="oppland" translate="Oppland;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="oslo" translate="Oslo;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="rogaland" translate="Rogaland;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="sogn-og-fjordane" translate="Sogn og Fjordane;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="trondelag" translate="Trøndelag;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="telemark" translate="Telemark;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="troms" translate="Troms;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="vest-agder" translate="name:en=West Agder;entity=relation"/>
				<region srtm="yes" hillshade="no" wiki="no" name="vestfold" translate="Vestfold;entity=relation"/>
				<region name="svalbard-and-jan-mayen" translate="Svalbard;entity=relation"/>
			</region>
			<region name="sweden" inner_download_prefix="$name" srtm="no" lang="sv" poly_extract="north-europe" join_map_files="yes">
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="blekinge" translate="Blekinge län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="dalarna" translate="Dalecarlia;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="gavleborg" translate="Gävleborgs län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="gotland" translate="Gotlands län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="halland" translate="Hallands län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="jamtland" translate="Jämtlands län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="jonkoping" translate="Jönköpings län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="kalmar" translate="Kalmar län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="kronoberg" translate="Kronobergs län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="norrbotten" translate="Norrbottens län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="orebro" translate="Örebro län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="ostergotland" translate="Östergötlands län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="skane" translate="Skåne län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="sodermanland" translate="Södermanlands län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="stockholm" translate="Stockholms län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="uppsala" translate="Uppsala län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="varmland" translate="Värmlands län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="vasterbotten" translate="Västerbottens län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="vasternorrland" translate="Västernorrlands län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="vastmanland" translate="Västmanlands län;entity=relation"/>
				<region map="yes" srtm="yes" hillshade="no" wiki="no" name="vastra-gotaland" translate="Västra Götalands län;entity=relation"/>
			</region>

		<!-- east-europe -->
			<region name="belarus" lang="be,ru" poly_extract="east-europe" inner_download_prefix="$name" join_map_files="yes" translate="name:en=Belarus;entity=relation">
				<region type="map" name="brest" translate="name:en=Brest Region;entity=relation"/>
				<region type="map" name="gomel" translate="name:en=Homel region;entity=relation"/>
				<region type="map" name="hrodna" translate="name:en=Hrodna Region;entity=relation"/>
				<region type="map" name="magilyow" translate="name:en=Mahilyow Region;entity=relation"/>
				<region type="map" name="minsk" translate="name:en=Minsk;entity=relation"/>
				<region type="map" name="vitebsk" translate="name:en=Vitsebsk Region;entity=relation"/>
			</region>
			<region name="bulgaria" lang="bg" poly_extract="east-europe"/>
			<region name="czech-republic" inner_download_prefix="$name" translate="name:en=Czechia;entity=relation" lang="cs,sk" poly_extract="east-europe" join_map_files="yes">
				<region name="jihovychod" srtm="no" hillshade="no" wiki="no" translate="name:en=Southeast;ref=CZ06;entity=relation"/>
				<region name="jihozapad" srtm="no" hillshade="no" wiki="no" translate="name:en=Southwest;ref=CZ03;entity=relation"/>
				<region name="moravskoslezsko" srtm="no" hillshade="no" wiki="no" translate="name:en=Moravia-Silesia;entity=relation"/>
				<region name="praha" srtm="no" hillshade="no" wiki="no" translate="name:en=Prague;entity=relation"/>
				<region name="severovychod" srtm="no" hillshade="no" wiki="no" translate="name:en=Northeast;ref=CZ05;entity=relation"/>
				<region name="severozapad" srtm="no" hillshade="no" wiki="no" translate="name:en=Northwest;ref=CZ04;entity=relation"/>
				<region name="stredni-cechy" srtm="no" hillshade="no" wiki="no" translate="name:en=Central Bohemia;entity=relation"/>
				<region name="stredni-morava" srtm="no" hillshade="no" wiki="no" translate="name:en=Central Moravia;entity=relation"/>
			</region>
			<region name="hungary" lang="hu" poly_extract="east-europe"/>
			<region name="moldova" lang="ro" poly_extract="east-europe"/>
			<region name="poland" inner_download_prefix="$name" lang="pl" poly_extract="east-europe" map="no" roads="yes" join_map_files="yes">
				<region name="greater-poland" srtm="no" hillshade="no" wiki="no" translate="Greater Poland;entity=node"/>
				<region name="kuyavian-pomeranian" srtm="no" hillshade="no" wiki="no" translate="Kuyavian-Pomerania;entity=node"/>
				<region name="lesser-poland" srtm="no" hillshade="no" wiki="no" translate="Lesser Poland;entity=node"/>
				<region name="lodz" srtm="no" hillshade="no" wiki="no" translate="Łódź;entity=node"/>
				<region name="lower-silesian" srtm="no" hillshade="no" wiki="no" translate="Lower Silesia;entity=node"/>
				<region name="lublin" srtm="no" hillshade="no" wiki="no" translate="name:en=Lublin Voivodeship;entity=relation"/>
				<region name="lubusz" srtm="no" hillshade="no" wiki="no"/>
				<region name="masovian" srtm="no" hillshade="no" wiki="no" translate="Masovia;entity=node"/>
				<region name="opole" srtm="no" hillshade="no" wiki="no" translate="Opole Voivodeship;entity=node"/>
				<region name="podlachian" srtm="no" hillshade="no" wiki="no" translate="Podlachia;entity=node"/>
				<region name="pomeranian" srtm="no" hillshade="no" wiki="no" translate="Pomerania;entity=node"/>
				<region name="silesian" srtm="no" hillshade="no" wiki="no" translate="Silesia;entity=node"/>
				<region name="subcarpathian" srtm="no" hillshade="no" wiki="no" translate="Subcarpathia;entity=node"/>
				<region name="swietokrzyskie" srtm="no" hillshade="no" wiki="no" translate="Holy Cross;entity=node"/>
				<region name="warmian-masurian" srtm="no" hillshade="no" wiki="no" translate="Warmian-Masurian Voivodeship;entity=node"/>
				<region name="west-pomeranian" srtm="no" hillshade="no" wiki="no" translate="West Pomerania;entity=node"/>
			</region>
			<region name="romania" lang="ro" poly_extract="east-europe"/>
			<region name="slovakia" lang="sk" poly_extract="east-europe"/>
			<region name="transnistria" hillshade="no" lang="ru,uk,ro" poly_extract="east-europe"/>
			<region name="ukraine" inner_download_prefix="$name" lang="uk,ru" poly_extract="east-europe" join_map_files="yes">
				<region name="cherkasy" hillshade="no" srtm="no" wiki="no" translate="name:en=Cherkasy Oblast;entity=relation"/>
				<region name="chernihiv" hillshade="no" srtm="no" wiki="no" translate="name:en=Chernihiv Oblast;entity=relation"/>
				<region name="chernivtsi" hillshade="no" srtm="no" wiki="no" translate="name:en=Chernivtsi Oblast;entity=relation"/>
				<region name="crimea" hillshade="no" srtm="no" wiki="no" boundary="ukraine/crimea" translate="name:en=Autonomous Republic of Crimea;entity=relation"/>
				<region name="dnipropetrovsk" hillshade="no" srtm="no" wiki="no" translate="name:en=Dnipropetrovsk Oblast;entity=relation"/>
				<region name="donetsk" hillshade="no" srtm="no" wiki="no" translate="name:en=Donetsk Oblast;entity=relation"/>
				<region name="ivano-frankivsk" hillshade="no" srtm="no" wiki="no" translate="name:en=Ivano-Frankivsk Oblast;entity=relation"/>
				<region name="kharkiv" hillshade="no" srtm="no" wiki="no" translate="name:en=Kharkiv Oblast;entity=relation"/>
				<region name="kherson" hillshade="no" srtm="no" wiki="no" translate="name:en=Kherson Oblast;entity=relation"/>
				<region name="khmelnytskyy" hillshade="no" srtm="no" wiki="no" translate="name:en=Khmelnytskyi Oblast;entity=relation"/>
				<region name="kiev" hillshade="no" srtm="no" wiki="no" translate="name:en=Kyiv Oblast;entity=relation"/>
				<region name="kiev-city" hillshade="no" srtm="no" wiki="no" translate="name:en=Kyiv;entity=relation"/>
				<region name="kirovohrad" hillshade="no" srtm="no" wiki="no" translate="name:en=Kirovohrad Oblast;entity=relation"/>
				<region name="luhansk" hillshade="no" srtm="no" wiki="no" translate="name:en=Luhansk Oblast;entity=relation"/>
				<region name="lviv" hillshade="no" srtm="no" wiki="no" translate="name:en=Lviv Oblast;entity=relation"/>
				<region name="mykolayiv" hillshade="no" srtm="no" wiki="no" translate="name:en=Mykolaiv Oblast;entity=relation"/>
				<region name="odessa" hillshade="no" srtm="no" wiki="no" translate="name:en=Odesa Oblast;entity=relation"/>
				<region name="poltava" hillshade="no" srtm="no" wiki="no" translate="name:en=Poltava Oblast;entity=relation"/>
				<region name="rivne" hillshade="no" srtm="no" wiki="no" translate="name:en=Rivne Oblast;entity=relation"/>
				<region name="sumy" hillshade="no" srtm="no" wiki="no" translate="name:en=Sumy Oblast;entity=relation"/>
				<region name="ternopil" hillshade="no" srtm="no" wiki="no" translate="name:en=Ternopil Oblast;entity=relation"/>
				<region name="transcarpathia" hillshade="no" srtm="no" wiki="no" translate="name:en=Zakarpattia Oblast;entity=relation"/>
				<region name="vinnytsya" hillshade="no" srtm="no" wiki="no" translate="name:en=Vinnytsia Oblast;entity=relation"/>
				<region name="volyn" hillshade="no" srtm="no" wiki="no" translate="name:en=Volyn Oblast;entity=relation"/>
				<region name="zaporizhzhya" hillshade="no" srtm="no" wiki="no" translate="name:en=Zaporizhia Oblast;entity=relation"/>
				<region name="zhytomyr" hillshade="no" srtm="no" wiki="no" translate="name:en=Zhytomyr Oblast;entity=relation"/>
			</region>
		</region>
		<region name="World_basemap" type="map" roads="no" wiki="no" boundary="no" translate="=World basemap"/>
		<region name="World_seamarks" type="map" roads="no" wiki="no" boundary="no" translate="=World seamarks"/>
</regions_list>""".trimIndent()