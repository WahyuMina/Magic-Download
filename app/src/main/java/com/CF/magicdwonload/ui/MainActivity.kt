package com.CF.magicdwonload.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.CF.magicdwonload.R
import com.CF.magicdwonload.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.security.DrbgParameters

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    // Opsi kualitas media
    private var videoQualities = arrayOf("1080p", "720p", "480p", "360p")
    private var musicQualities = arrayOf("320kbps (Tinggi)", "128kbps (Sedang)")

    // Launcher perizinan notifikasi (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Mohon izinkan notifikasi untuk melihat status unduhan", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkNotificationPermission()
        updateYoutubeDLExtractor()
        setupQualitySpinner(videoQualities) // Baku awal: Video
        setupListeners()
        observeViewModel()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupQualitySpinner(qualities: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, qualities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerQuality.adapter = adapter
    }

    private fun setupListeners() {
        // Menarik thumbnail otomatis saat URL valid dimasukkan
        binding.etUrl.addTextChangedListener { text ->
            val url = text.toString().trim()
            val isValidUrl = android.util.Patterns.WEB_URL.matcher(url).matches()

            if (isValidUrl) {
                binding.layoutOptions.visibility = View.VISIBLE
                fetchThumbnailPreview(url) // Fungsi menarik gambar
            } else {
                binding.layoutOptions.visibility = View.GONE
                binding.thumbnailPreview.visibility = View.GONE
            }
        }
        // Aksi saat tombol batal
        binding.btnCancel.setOnClickListener {
            viewModel.cancelDownload()
            binding.progressDownload.progress = 0
        }


        // Pembaruan dinamis Spinner berdasarkan tipe media
        binding.rgFormat.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == binding.rbVideo.id) {
                setupQualitySpinner(videoQualities)
            } else {
                setupQualitySpinner(musicQualities)
            }
        }

        binding.btnDownload.setOnClickListener {
            val urlInput = binding.etUrl.text.toString().trim()
            val selectedQuality = binding.spinnerQuality.selectedItem.toString()

            if (urlInput.isEmpty()) {
                binding.urlInputLayout.error = "Hei! Jangan suruh aku mengunduh udara kosong!"
            } else if (!android.util.Patterns.WEB_URL.matcher(urlInput).matches()) {
                binding.urlInputLayout.error = "Itu bukan tautan! Masukkan URL yang valid!"
            } else {
                binding.urlInputLayout.error = null
                viewModel.startExtraction(urlInput, selectedQuality)
            }
        }
    }

    private  fun fetchThumbnailPreview(videoUrl : String) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
            // Tarik metadata dari YoutubeDL
                val streamInfo = com.yausername.youtubedl_android.YoutubeDL.getInstance().getInfo(videoUrl)
                val thumbnailUrl = streamInfo.thumbnail

                val videoDuration = streamInfo.duration

            // menampung ukuran file
                var s1080 = 0L; var s720 = 0L; var s480 = 0L; var s360 = 0L
                var sAudioHigh = 0L; var sAudioLow = 0L

            //membaca ukuran setiap format yang tersedia
                streamInfo.formats?.forEach { format ->
                    val size = if (format.fileSize > 0L) {
                        format.fileSize
                    } else if (format.tbr > 0 && videoDuration > 0) {
                        (format.tbr * 1024 / 8 * videoDuration).toLong()
                    } else {
                        0L// panggil balik dari else
                    }

                    if (size > 0L) {
                        val h = format.height

                        if (h >= 1000) {
                            s1080 = maxOf(s1080, size) // Menangkap 1080p dan resolusi vertikal besar
                        } else if (h >= 700) {
                            s720 = maxOf(s720, size)   // Menangkap 720p, 718p, dll
                        } else if (h >= 400) {
                            s480 = maxOf(s480, size)   // Menangkap 480p, 540p, 640p
                        } else if (h > 0) {
                            s360 = maxOf(s360, size)   // Menangkap 360p ke bawah
                        }

                        if (format.acodec != "none" && format.vcodec == "none") {
                            sAudioHigh = maxOf(sAudioHigh, size)
                            if (sAudioLow == 0L) sAudioLow = size else sAudioLow = minOf(sAudioLow, size)
                        }
                    }
                }

            // Kembail ke UI Thread untuk menampilkan tempilan
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (!thumbnailUrl.isNullOrEmpty()) {
                        binding.thumbnailPreview.visibility = View.VISIBLE

                        // Memuat gambar
                        com.bumptech.glide.Glide.with(this@MainActivity)
                            .load(thumbnailUrl)
                            .into(binding.thumbnailPreview)
                    }
                    fun formatMB(bytes: Long): String {
                        if (bytes <= 0L) return "MB" // jk server menyembunyikan ukuran
                        return  String.format("%.1f MB", bytes / (1024.0 * 1024.0))
                    }

                    // menampilkan ukuran file video
                    videoQualities = arrayOf(
                        "1080p (${formatMB(s1080)})",
                        "720p (${formatMB(s720)})",
                        "480p (${formatMB(s480)})",
                        "360p (${formatMB(s360)})"

                    )
                    // menampilkan ukuran file audio
                    musicQualities = arrayOf(
                        "320kbps (Tinggi) (${formatMB(sAudioHigh)})",
                        "128kbps (Sedang) (${formatMB(sAudioLow)})"
                    )

                    if (binding.rbVideo.isChecked) {
                        setupQualitySpinner(videoQualities)
                    } else {
                        setupQualitySpinner(musicQualities)
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    binding.thumbnailPreview.visibility = View.GONE
                }
            }
        }
    }

    private fun observeViewModel() {
        // Konfigurasi awal batas progress bar
        binding.progressDownload.max = 100

        // Menangkap status persentase unduhan
        viewModel.downloadProgress.observe(this) { currentProgress ->
            binding.progressDownload.progress = currentProgress
        }

        // Menangani visibilitas UI selama proses loading
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressDownload.visibility = if (isLoading) View.VISIBLE else View.GONE

            binding.btnCancel.visibility = if (isLoading) View.VISIBLE else View.GONE // Tampil tombol batal jika sedang loading

            binding.btnDownload.isEnabled = !isLoading // Cegah spam klik tombol
            binding.etUrl.isEnabled = !isLoading // Cegah spam input URL
        }

        // Menampilkan pesan hasil ekstraksi
        viewModel.resultMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateYoutubeDLExtractor() {
        // Berjalan di background thread
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Update YoutubeDL
                val status = com.yausername.youtubedl_android.YoutubeDL.getInstance()
                    .updateYoutubeDL(applicationContext, com.yausername.youtubedl_android.YoutubeDL.UpdateChannel._STABLE)

                // Kembail ke UI Thread untuk menampilkan pesan
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (status == com.yausername.youtubedl_android.YoutubeDL.UpdateStatus.DONE) {
                        Toast.makeText(this@MainActivity, "Mesin Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Mesin Sudah Versi Terbaru.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Gagal Memperbarui Mesin: Cek Koneksi!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}