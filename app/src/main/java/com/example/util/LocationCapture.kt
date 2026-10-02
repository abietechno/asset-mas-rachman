package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

sealed interface CaptureResult {
    /** [reachedTarget] false = waktu habis, ini titik terbaik yang sempat didapat (akurasi mungkin kurang). */
    data class Fix(val latitude: Double, val longitude: Double, val accuracyM: Double, val reachedTarget: Boolean) : CaptureResult

    /** Hanya titik palsu (mock location) yang diterima; ditolak agar lokasi aset tidak bisa dipalsukan. */
    data object MockLocation : CaptureResult

    /** Tidak ada titik sama sekali (GPS mati, di dalam ruangan, atau gagal meminta lokasi). */
    data object Unavailable : CaptureResult
}

object LocationCapture {

    private const val MAX_AGE_NS = 30_000_000_000L // abaikan titik lama dari cache > 30 detik

    private fun isMock(l: Location) =
        if (Build.VERSION.SDK_INT >= 31) l.isMock else @Suppress("DEPRECATION") l.isFromMockProvider

    /**
     * Mengumpulkan titik GPS beberapa detik dan memakai yang paling akurat: berhenti begitu akurasi mencapai
     * [targetAccuracyM] atau saat [timeoutMs] habis. Satu fix pertama biasanya masih puluhan meter; menunggu
     * beberapa detik memberi hasil jauh lebih rapat. Izin lokasi harus sudah diberikan pemanggil.
     */
    @SuppressLint("MissingPermission")
    suspend fun capture(
        context: Context,
        targetAccuracyM: Float = 8f,
        timeoutMs: Long = 25_000L,
        onProgress: (accuracyM: Float) -> Unit = {}
    ): CaptureResult = suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        val handler = Handler(Looper.getMainLooper())
        val done = AtomicBoolean(false)
        var best: Location? = null
        var sawMock = false
        lateinit var callback: LocationCallback

        fun finish(reached: Boolean) {
            if (!done.compareAndSet(false, true)) return
            client.removeLocationUpdates(callback)
            handler.removeCallbacksAndMessages(null)
            val b = best
            cont.resume(
                when {
                    b != null -> CaptureResult.Fix(b.latitude, b.longitude, b.accuracy.toDouble(), reached)
                    sawMock -> CaptureResult.MockLocation
                    else -> CaptureResult.Unavailable
                }
            )
        }

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (l in result.locations) {
                    if (isMock(l)) { sawMock = true; continue }
                    if (SystemClock.elapsedRealtimeNanos() - l.elapsedRealtimeNanos > MAX_AGE_NS) continue
                    if (!l.hasAccuracy()) continue
                    if (best == null || l.accuracy < best!!.accuracy) best = l
                }
                best?.let {
                    onProgress(it.accuracy)
                    if (it.accuracy <= targetAccuracyM) finish(reached = true)
                }
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMinUpdateIntervalMillis(500L)
            .setWaitForAccurateLocation(true)
            .build()

        cont.invokeOnCancellation {
            if (done.compareAndSet(false, true)) {
                client.removeLocationUpdates(callback)
                handler.removeCallbacksAndMessages(null)
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener { finish(reached = false) }
        handler.postDelayed({ finish(reached = false) }, timeoutMs)
    }
}
