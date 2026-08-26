# MagicDownload✨

<div align="center">
  <img src="art/Logo.png" alt="Logo MagicDownload" widht="10%"/>
</div>

MagicDownload adalah aplikasi ekstraksi media berbasis Android yang dikembangkan menggunakan Kotlin. Aplikasi ini memanfaatkan mesin `yt-dlp` untuk mengekstrak dan mengunduh media dari berbagai platform web yang didukung.

## Dukung Pengembang🍵
Aplikasi ini 100% gratis dan bebas iklan. Namun, jika *MagicDownload* membantumu dan kamu ingin mentraktir saya segelas kopi agar mesin ini terus mendapatkan *update*, kamu bisa memberikan dukungan melalui:

https://saweria.co/WahyuMina

## Fitur Utama
* **Unduh dari Banyak Platform:** Mendukung pengunduhan video dan musik dari berbagai situs web dan media sosial populer secara langsung.
* **Bebas Pilih Kualitas:** Tersedia pilihan resolusi video (mulai dari ukuran hemat kuota hingga 1080p) dan kualitas audio jernih (MP3).
* **Otomatis Rapi:** Video dan suara digabungkan secara otomatis dengan kualitas terbaik, siap diputar tanpa aplikasi tambahan.
* **Unduh di Latar Belakang:** Proses unduhan tetap berjalan lancar meskipun kamu sedang membuka aplikasi lain, lengkap dengan pantauan progres di notifikasi HP.
* **Langsung Tersimpan:** Hasil unduhan otomatis masuk ke folder `Downloads` di HP-mu tanpa harus repot mengatur perizinan penyimpanan yang membingungkan.

## Stack Teknis
* **Bahasa:** Kotlin
* **Arsitektur:** MVVM (Model-View-ViewModel)
* **Konkurensi:** Kotlin Coroutines, LiveData
* **Implementasi UI:** ViewBinding
* **Dependensi Utama:** [youtubedl-android](https://github.com/junkfood02/youtubedl-android)

## Petunjuk Build
1. Klon repositori ini ke mesin lokal Anda.
2. Buka proyek di **Android Studio** (disarankan menggunakan AGP 8.0+).
3. Pastikan `buildConfig = true` telah diaktifkan di dalam blok `buildFeatures` pada file `build.gradle.kts` tingkat modul Anda.
4. Lakukan Gradle Sync dan jalankan aplikasi pada emulator atau perangkat fisik (API Level 26+).

## Cara install
1. Unduh file `MagicDownload-v1.3.3.apk` yang ada di bagian **[Releases](https://github.com/WahyuMina/Magic-Download/releases/tag/V.1.3.3)**.
2. Buka file APK yang sudah diunduh.
3. Jika muncul peringatan *"Install from Unknown Sources"* (Instal dari Sumber Tidak Dikenal), berikan izin pada peramban/file manager-mu.
4. Klik **Install** dan tunggu hingga selesai. MagicDownload siap digunakan!

## Apa Yang Baru?
* Mengatasi progress thumbnail tidak keluar

## License & Disclaimer
This project is licensed under the MIT License.

