package com.example.osmandtesttask.domain

import java.io.InputStream
import java.util.Locale

typealias LocaleProvider = () -> Locale
typealias AssetProvider = (fileName: String) -> InputStream