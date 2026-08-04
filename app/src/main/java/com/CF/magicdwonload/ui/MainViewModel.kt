package com.CF.magicdwonload.ui

import android.app.Application
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.CF.magicdwonload.extractor.YoutubeDLExtractor
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _resultMessage = MutableLiveData<String>()
    val resultMessage: LiveData<String> get() = _resultMessage

    private val _downloadProgress = MutableLiveData<Int>()
    val downloadProgress: LiveData<Int> get() = _downloadProgress

    private val extractor = YoutubeDLExtractor()

    private val channelId = "magic_download_channel"
    private val notificationId = 888
    private val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val DOWNLOAD_PROCESS_ID = "MAGIC_DOWNLOAD_TASK"


    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Status Unduhan Media",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menampilkan progress unduhan dari aplikasi MagicDownload"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateSystemNotification(
        progress: Int,
        statusText: String,
        isFinished: Boolean = false,
        filePath: String? = null
    ) {
        val icon = if (isFinished) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_sys_download

        val builder = NotificationCompat.Builder(getApplication(), channelId)
            .setSmallIcon(icon)
            .setContentTitle("MagicDownload Manager")
            .setContentText(statusText)
            .setOngoing(!isFinished)
            .setOnlyAlertOnce(true)

        if (!isFinished) {
            if (progress >= 0) {
                builder.setProgress(100, progress, false)
            } else {
                // Fase Muxing (Progress tidak diketahui)
                builder.setProgress(100, 100, true)
            }
        } else {
            builder.setProgress(0, 0, false)
        }

        if (!filePath.isNullOrEmpty()) {
            try {

                // Arahkan notifikasi ke Download Manager bawaan sistem
                val intent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

                val pendingIntent = PendingIntent.getActivity(
                    getApplication(),
                    System.currentTimeMillis().toInt(),
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                builder.setContentIntent(pendingIntent)
                builder.setAutoCancel(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        notificationManager.notify(notificationId, builder.build())
    }

    fun startExtraction(url: String, quality: String) {
        if (!extractor.isUrlValid(url)) {
            _resultMessage.value = "Format URL salah! Masukkan tautan internet yang benar!"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _resultMessage.value = "Sedang Mengunduh..."
            _downloadProgress.value = 0

            val request = YoutubeDLRequest(url)
            val isAudio = quality.contains("kbps")
            val isMusicFormat = quality.contains("kbps", ignoreCase = true)
            val subFolderName = if (isAudio) "Music" else "Video"
            val baseDownloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            // Penggabungan path yang lebih rapi
            val targetFolder = File(File(baseDownloadFolder, "MagicDownload"), subFolderName)

            if (!targetFolder.exists() && !targetFolder.mkdirs()) {
                _resultMessage.value = "Gagal mengalokasikan ruang penyimpanan. Pastikan memori cukup!"
                _isLoading.value = false
                return@launch
            }

            if (isMusicFormat) {
                request.addOption("-x") // Ekstrak audio
                request.addOption("--audio-format", "mp3")

                request.addOption("--embed-thumbnail")

                if (quality.contains("320")) {
                    request.addOption("--audio-quality", "0") // Kualitas Terbaik
                } else {
                    request.addOption("--audio-quality", "5") // Kualitas Sedang
                }
            }

            updateSystemNotification(0, "Membuka Jalur unduhan...")

            val result = withContext(Dispatchers.IO) {
                extractor.downloadMedia(url, quality, targetFolder) { progress, eta, sizeStatus ->
                    val currentProgress = progress.toInt()
                    val timeRemaining = if (eta > 0 && currentProgress >= 0) " | Sisa: ${eta} dtk" else ""

                    // Wajib postValue karena berada di Background Thread
                    _downloadProgress.postValue(currentProgress)
                    Log.d("MAGIC_DEBUG", "Progress: $currentProgress%")

                    val friendlySizeStatus = sizeStatus
                        .replace("MiB", "MB")
                        .replace("KiB", "KB")
                        .replace("GiB", "GB")

                    updateSystemNotification(currentProgress, "$friendlySizeStatus$timeRemaining")
                }
            }

            if (result.startsWith("SUKSES")) {
                var downloadedFilePath = result.substringAfter("|")
                val trackedFile = File(downloadedFilePath)

                // Fallback pencarian file terbaru jika path asli meleset
                if (downloadedFilePath.isEmpty() || !trackedFile.exists()) {
                    val latestFile = targetFolder.listFiles()
                        ?.filter { it.isFile && it.name.contains("MagicDL_") }
                        ?.maxByOrNull { it.lastModified() }

                    if (latestFile != null) {
                        downloadedFilePath = latestFile.absolutePath
                    }
                }

                _resultMessage.value = "Unduhan Selesai!"

                if (downloadedFilePath.isNotEmpty() && File(downloadedFilePath).exists()) {
                    MediaScannerConnection.scanFile(getApplication(), arrayOf(downloadedFilePath), null, null)
                    updateSystemNotification(100, "Selesai! Ketuk untuk langsung memutar media.", true, downloadedFilePath)
                } else {
                    updateSystemNotification(100, "Selesai diunduh, tapi gagal dilacak! Buka folder manual.", true, null)
                }
            } else {
                _resultMessage.value = "Unduhan Gagal! Periksa koneksi internet atau validitas URL."
                notificationManager.cancel(notificationId)
            }

            _isLoading.value = false
        }
    }

    fun cancelDownload() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Membunuh proses berdasarkan ID
                com.yausername.youtubedl_android.YoutubeDL.getInstance().destroyProcessById(DOWNLOAD_PROCESS_ID)

                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _resultMessage.value = "Unduhan dibatalkan!"
                    // Matikan notifikasi sistem jika dibatalkan
                    notificationManager.cancel(notificationId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
