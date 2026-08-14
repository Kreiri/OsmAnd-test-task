package com.example.osmandtesttask.common

import java.util.Locale

fun String.capitalizeFirstChar(languageCode: String): String {
    val locale = Locale.forLanguageTag(languageCode)
    return capitalizeFirstChar(locale)
}

fun String.capitalizeFirstChar(locale: Locale): String {
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

/**
 * Format value as percentage. 100 is considered 100%
 */
fun Float.formatAsPercentage100(): String {
    val format = if (this == 0f || this >= 100) {
        "%.0f"
    } else {
        "%.2f"
    }
    val percentString = format.format(this)
    return percentString
}

/**
 * Format value as percentage. 1 is considered 100%
 */
fun Float.formatAsPercentage1(): String {
    val percentage = this * 100
    return percentage.formatAsPercentage100()
}