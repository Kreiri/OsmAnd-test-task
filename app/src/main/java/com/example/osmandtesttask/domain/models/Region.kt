package com.example.osmandtesttask.domain.models

import com.example.osmandtesttask.common.capitalizeFirstChar
import com.example.osmandtesttask.domain.LocaleProvider
import java.text.Collator

data class Region(
    val name: String,
    val downloadName: String,
    val type: RegionType?,
    val map: Boolean,
    val translate: String?,
    val regions: List<Region>
) {
    private val translations = RegionTranslations.parse(translate ?: "")

    fun getTranslation(field: String, locale: String): String? {
        return translations.getTranslation(field, locale)
    }

    fun getLocalizedName(locale: String): String {
        return getTranslation("name", locale)
            ?: getTranslation("", locale)
            ?: name.capitalizeFirstChar(locale)
    }
}

enum class RegionType {
    MAP, CONTINENT, SRTM, HILLSHADE, UNKNOWN
}

fun regionsComparator(localeProvider: LocaleProvider): Comparator<Region> {
    val locale = localeProvider.invoke()
    val language = locale.language
    val collator = Collator.getInstance(locale).apply {
        strength = Collator.SECONDARY
    }
    return Comparator { o1, o2 ->
        if (o1.type == RegionType.CONTINENT && o2.type != RegionType.CONTINENT) {
            -1
        } else if (o1.type != RegionType.CONTINENT && o2.type == RegionType.CONTINENT) {
            1
        } else {
            val name1 = o1.getLocalizedName(language)
            val name2 = o2.getLocalizedName(language)
            collator.compare(name1, name2)
        }
    }
}