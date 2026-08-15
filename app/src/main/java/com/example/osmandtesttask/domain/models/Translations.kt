package com.example.osmandtesttask.domain.models

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

data class RegionTranslations(val fieldTranslations: Map<String, FieldTranslations>) {

    fun getTranslation(field: String, locale: String): String? {
        return fieldTranslations[field]?.let {
            it.get(locale) ?: it.getDefault() ?: it.getFirst()
        }
    }

    companion object {
        fun parse(translate: String): RegionTranslations {
            val translations = parseTranslation(translate)
            return RegionTranslations(translations)
        }
    }
}

private fun parseTranslation(translate: String): Map<String, FieldTranslations> {
    val parsed = mutableListOf<ParsedTranslation>()
    val chunks = translate.split(";")
    chunks.forEachIndexed { index, chunk ->
        val parts = chunk.split("=", limit = 2)
        if (parts.size == 2) {
            val keys = parts[0].split(":")
            val fieldName = keys.first().takeIf { it.isNotEmpty() } ?: "name"
            val locale = keys.getOrNull(1) ?: ""
            val value = parts[1]
            val t = ParsedTranslation(fieldName, locale, value)
            parsed.add(t)
        } else if (parts.size == 1 && index == 0) {
            val candidate = parts.first()
            if (candidate.isNotBlank()) {
                parsed.add(ParsedTranslation("", "", candidate))
            }
        }
    }
    val translations = parsed.groupBy({
        it.fieldName
    }, {
        it.locale to it.value
    }).mapValues { (_, pairs) -> FieldTranslations(pairs.toMap()) }
    return translations
}