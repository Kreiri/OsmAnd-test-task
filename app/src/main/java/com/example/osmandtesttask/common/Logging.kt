package com.example.osmandtesttask.common

import android.util.Log
import com.example.osmandtesttask.BuildConfig


object Logger {
    private const val DEFAULT_TAG = "OSMTT"
    fun d(subtag: String, message: String) {
        if (BuildConfig.DEBUG) {
            val tag = if (subtag.isEmpty()) DEFAULT_TAG else "$DEFAULT_TAG-$subtag"
            Log.d(tag, message)
        }
    }

    fun d(message: String) {
        d("", message)
    }
}