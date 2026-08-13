package com.example.osmandtesttask.domain.models

import com.example.osmandtesttask.common.capitalizeFirstChar

data class Region(
    val name: String,
    val downloadName: String,
    val type: RegionType?,
    val map: Boolean,
    val translate: String?,
    val regions: List<Region>
) {
    private val translations: Map<String, FieldTranslations>
    init {
        val parsed = mutableListOf<ParsedTranslation>()
        if (translate != null) {
            val chunks = translate.split(";")
            for (chunk in chunks) {
                val parts = chunk.split("=", limit = 2)
                if (parts.size != 2) continue
                val keys = parts[0].split(":")
                val fieldName = keys.first().takeIf { it.isNotEmpty() } ?: "name"
                val locale = keys.getOrNull(1) ?: ""
                val value = parts[1]
                val t = ParsedTranslation(fieldName, locale, value)
                parsed.add(t)
            }
        }
        translations = parsed.groupBy({
            it.fieldName
        }, {
            it.locale to it.value
        }).mapValues { (_, pairs) -> FieldTranslations(pairs.toMap()) }
    }

    fun getTranslation(field: String, locale: String): String? {
        return translations[field]?.let {
            it.get(locale) ?: it.getDefault() ?: it.getFirst()
        }
    }

    fun getLocalizedName(locale: String): String {
        return getTranslation("name", locale) ?: name.capitalizeFirstChar(locale)
    }
}
data class ParsedTranslation(val fieldName: String, val locale: String, val value: String)
data class FieldTranslations(val localized: Map<String, String>) {
    fun getDefault(): String? {
        return localized[""] ?: localized["en"]
    }

    fun get(locale: String): String? {
        return localized[locale]
    }

    fun getFirst(): String? {
        return localized.values.firstOrNull()
    }
}

enum class RegionType {
    MAP, CONTINENT, SRTM, HILLSHADE, UNKNOWN
}