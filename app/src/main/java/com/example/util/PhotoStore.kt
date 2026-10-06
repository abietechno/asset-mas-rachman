package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Penyimpanan berkas foto aset di dalam folder aplikasi.
 *
 * Foto dari kamera dan galeri dikecilkan ke [MAX_SIDE] px pada sisi terpanjang lalu disimpan ulang sebagai JPEG,
 * supaya hemat kuota saat diunggah (server membatasi 5 MB) dan hemat penyimpanan HP.
 */
object PhotoStore {
    const val MAX_PHOTOS_PER_ASSET = 5
    private const val MAX_SIDE = 1280
    private const val QUALITY = 80

    private fun dir(context: Context): File =
        File(context.filesDir, "asset_photos").apply { if (!exists()) mkdirs() }

    /** Berkas kosong + URI yang bisa dipakai aplikasi kamera untuk menulis hasil jepretan. */
    fun newCameraTarget(context: Context): Pair<File, Uri> {
        val file = File(dir(context), "cam_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }

    /**
     * Mengecilkan [source] lalu menyimpannya sebagai foto aset. Mengembalikan path berkas hasil,
     * atau null bila gambar tidak bisa dibaca.
     */
    suspend fun importFrom(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeScaled(context, source) ?: return@runCatching null
            val rotated = applyOrientation(context, source, bitmap)
            val target = File(dir(context), "img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(target).use { out -> rotated.compress(Bitmap.CompressFormat.JPEG, QUALITY, out) }
            if (rotated !== bitmap) bitmap.recycle()
            rotated.recycle()
            target.absolutePath
        }.getOrNull()
    }

    /** Foto hasil kamera sudah berupa berkas kita sendiri: cukup dikecilkan di tempat. */
    suspend fun compressInPlace(context: Context, file: File): String? = withContext(Dispatchers.IO) {
        val path = importFrom(context, Uri.fromFile(file))
        file.delete()
        path
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    /** Membaca gambar dengan subsampling supaya tidak memuat berkas besar ke memori. */
    private fun decodeScaled(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_SIDE || bounds.outHeight / (sample * 2) >= MAX_SIDE) {
            sample *= 2
        }

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val longest = maxOf(decoded.width, decoded.height)
        if (longest <= MAX_SIDE) return decoded

        val scale = MAX_SIDE.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    /** Banyak kamera menyimpan foto mendatar dengan penanda rotasi; terapkan supaya tidak miring. */
    private fun applyOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val degrees = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)

        if (degrees == 0f) return bitmap
        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
