package com.CF.magicdwonload.extractor

import android.util.Patterns
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// KELAS INI PADA KONTRAK MEDIAEXTRACTOR!
class YoutubeDLExtractor : MediaExtractor {

    override fun isUrlValid(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false

        val isStandardUrl = Patterns.WEB_URL.matcher(url).matches()
        val hasDangerousChar = url.contains(";") || url.contains("|") || url.contains("&")
        val isSecure = url.startsWith("https://")

        return isStandardUrl && !hasDangerousChar && isSecure
    }
// All Platfrom Download
    override suspend fun downloadMedia(
        url: String,
        quality: String,
        outputDir: File,
        onProgressUpdate: (Float, Long, String) -> Unit
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val sanitizedQuality = quality.replace(" ", "_").replace("(", "").replace(")", "")
                val outputTemplate = "${outputDir.absolutePath}/MagicDL_%(title)s_${sanitizedQuality}.%(ext)s"

                val request = YoutubeDLRequest(url).apply {
                    addOption("--no-update")
                    addOption("--no-warnings")
                    addOption("--rm-cache-dir")
                    addOption("--extractor-args", "youtube:player_client=android,web")
                    addOption("-o", outputTemplate)
                    addOption("--merge-output-format", "mkv")
                    addOption("--embed-thumbnail") // Gambar ke dalam file
                    addOption("--add-metadata") // Menambahkan informasi judul, artis, dll

                    when (quality) {
                        "1080p" -> addOption("-f", "bestvideo[height<=1080]+bestaudio/best")
                        "720p"  -> addOption("-f", "bestvideo[height<=720]+bestaudio/best")
                        "480p"  -> addOption("-f", "bestvideo[height<=480]+bestaudio/best")
                        "360p"  -> addOption("-f", "best")
                        "320kbps (Tinggi)" -> {
                            addOption("-f", "bestaudio/best")
                            addOption("--extract-audio")
                            addOption("--audio-format", "mp3")
                            addOption("--audio-quality", "320k")
                        }
                        "128kbps (Sedang)" -> {
                            addOption("-f", "bestaudio/best")
                            addOption("--extract-audio")
                            addOption("--audio-format", "mp3")
                            addOption("--audio-quality", "128k")
                        }
                        else -> addOption("-f", "best")
                    }
                }

                var downloadStatus = "Mempersiapkan jalur unduhan..."
                var finalFilePath = ""

                val response = YoutubeDL.getInstance().execute(request) { progress, eta, line ->
                    if (!line.isNullOrEmpty()) {

                        if (line.contains("[download]")) {
                            val regex = Regex("([0-9.]+%)\\s+of\\s+([~0-9.a-zA-Z]+)")
                            val match = regex.find(line)
                            if (match != null) {
                                downloadStatus = "Mengunduh: ${match.groupValues[1]} dari ${match.groupValues[2]}"
                            }
                        }

                        if (line.contains("Merging formats") || line.contains("[Merger]")) {
                            downloadStatus = "Menyatukan Video & Audio (Muxing)..."
                        } else if (line.contains("[VideoConvertor]") || line.contains("Extracting audio")) {
                            downloadStatus = "Mengonversi berkas ke MP3..."
                        }

                        if (line.contains("Destination:") && !line.contains(".f") && !line.contains(".part")) {
                            finalFilePath = line.substringAfter("Destination: ").trim()
                        } else if (line.contains("Merging formats into")) {
                            finalFilePath = line.substringAfter("into \"").substringBeforeLast("\"")
                        } else if (line.contains("has already been downloaded")) {
                            finalFilePath = line.substringAfter("]").substringBefore(" has already").trim()
                        }
                    }
                    onProgressUpdate(progress, eta, downloadStatus)
                }

                if (response.exitCode == 0) "SUKSES|$finalFilePath" else "ERROR_SERVER: Kode ${response.exitCode}"
            } catch (e: Exception) {
                "ERROR_SYSTEM: ${e.message}"
            }
        }
    }
}