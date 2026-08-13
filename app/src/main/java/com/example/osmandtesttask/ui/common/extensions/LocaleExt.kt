package com.example.osmandtesttask.ui.common.extensions

import android.content.Context
import java.util.Locale

fun Context.getCurrentLocale() : Locale {
    return resources.configuration.locales.get(0)
}