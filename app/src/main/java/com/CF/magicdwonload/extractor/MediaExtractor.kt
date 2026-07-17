package com.CF.magicdwonload.extractor

import java.io.File

interface MediaExtractor {
    fun isUrlValid(url: String?): Boolean

    suspend fun downloadMedia(
        url: String,
        quality: String,
        outputDir: File,
        onProgressUpdate: (Float, Long, String) -> Unit
    ): String
}