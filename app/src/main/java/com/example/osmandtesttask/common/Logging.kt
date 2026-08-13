package com.example.osmandtesttask.common

import android.util.Log
import com.example.osmandtesttask.BuildConfig


object Logs {
    private const val DEFAULT_TAG = "OSMTestTask"
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