package com.CF.magicdwonload.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.CF.magicdwonload.BuildConfig
import com.CF.magicdwonload.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi ViewBinding!
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Menarik data versi secara dinamis dari Gradle
        binding.tvVersion.text = "Versi ${BuildConfig.VERSION_NAME}"
        binding.tvBranding.text = "Made by WahyuMina✨"

        // Transisi ke layar utama setelah 2 detik
        lifecycleScope.launch {
            delay(2000)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }
}