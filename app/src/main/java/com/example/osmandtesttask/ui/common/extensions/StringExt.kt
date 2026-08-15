package com.example.osmandtesttask.ui.common.extensions

import android.content.Context
import android.text.format.Formatter

fun Long.toReadableFileSize(context: Context): String {
    return Formatter.formatFileSize(context, this)
}