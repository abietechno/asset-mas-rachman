package com.example.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = SyncPrefs(applicationContext)
        if (prefs.current().token == null) return Result.success() // mode offline / sudah logout

        return try {
            val result = SyncEngine(AppDatabase.getDatabase(applicationContext), prefs).sync()
            prefs.saveStatus(result.summary(), success = true)
            runCatching { com.example.util.TaxReminders.check(applicationContext) }
            Result.success()
        } catch (e: HttpException) {
            when {
                e.code() == 401 -> {
                    prefs.clearSession("Sesi berakhir, silakan login ulang untuk melanjutkan sinkronisasi.")
                    Result.failure()
                }
                e.code() >= 500 && runAttemptCount < MAX_ATTEMPTS -> {
                    prefs.saveStatus("Server bermasalah (${e.code()}), mencoba lagi nanti.", success = false)
                    Result.retry()
                }
                else -> {
                    prefs.saveStatus("Sinkronisasi gagal (HTTP ${e.code()}).", success = false)
                    Result.failure()
                }
            }
        } catch (e: IOException) {
            prefs.saveStatus("Tidak dapat terhubung ke server, mencoba lagi nanti.", success = false)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}

object SyncScheduler {
    private const val PERIODIC = "asset_sync_periodic"
    private const val NOW = "asset_sync_now"

    private val needsNetwork = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Sinkron berkala di latar belakang (minimum WorkManager 15 menit). Aman dipanggil berulang. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(needsNetwork)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Sinkron segera (mis. setelah simpan/hapus aset). Permintaan yang bertumpuk digabung. */
    fun syncNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(needsNetwork)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        WorkManager.getInstance(context).cancelUniqueWork(NOW)
    }
}
