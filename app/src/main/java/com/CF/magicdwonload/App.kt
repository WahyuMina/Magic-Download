package com.CF.magicdwonload

import android.app.Application
import android.util.Log
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        // Panggil fungsi inisialisasi agar onCreate tetap bersih
        initMediaEngines()
    }

    private fun initMediaEngines() {
        try {
            // Pemanggilan elegan karena sudah di-import di atas!
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Aria2c.getInstance().init(this)

            Log.d("MagicDownload", "Mesin Ekstraksi berhasil dinyalakan!")
        } catch (e: Exception) {
            Log.e("MagicDownload", "Gagal inisialisasi mesin utama: ${e.message}")
        }
    }
}