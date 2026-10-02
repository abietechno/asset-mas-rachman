package com.example.util

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.model.AssetEntity
import com.example.model.AssetStatus
import com.example.model.AssetType
import com.example.model.SyncState
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Satu pengingat untuk satu kendaraan: pajak tahunan, plat 5 tahunan, atau servis. */
data class ReminderItem(
    /** Stabil selama tanggal jatuh temponya sama; berubah setelah diperpanjang sehingga pengingat mulai dari awal. */
    val key: String,
    val assetId: Long,
    val title: String,
    val message: String,
    /** 1 = <=30 hari, 2 = <=14 hari, 3 = <=7 hari, 4 = hari H / terlewat. */
    val level: Int
)

/**
 * Menentukan pengingat jatuh tempo kendaraan dan kapan boleh dikirim ulang. Murni (tanpa Android) agar bisa dites.
 *
 * Aturan servis: [SERVICE_INTERVAL_MONTHS] bulan sejak `lastServiceDate`. Kendaraan tanpa tanggal servis terakhir
 * tidak diingatkan (tidak ada dasar perhitungan).
 */
object ReminderPlanner {
    const val SERVICE_INTERVAL_MONTHS = 6
    private const val REPEAT_WHEN_OVERDUE_MS = 7L * 24 * 60 * 60 * 1000

    private fun startOfDay(ms: Long): Long = Calendar.getInstance().apply {
        timeInMillis = ms
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Selisih hari kalender (negatif = sudah lewat). */
    fun daysUntil(dueMs: Long, nowMs: Long): Int =
        Math.round((startOfDay(dueMs) - startOfDay(nowMs)).toDouble() / TimeUnit.DAYS.toMillis(1)).toInt()

    fun levelFor(days: Int): Int = when {
        days <= 0 -> 4
        days <= 7 -> 3
        days <= 14 -> 2
        days <= 30 -> 1
        else -> 0
    }

    private fun fmt(ms: Long) = SimpleDateFormat("d MMM yyyy", Locale("id", "ID")).format(ms)

    private fun sentence(what: String, days: Int, dueMs: Long): String = when {
        days < 0 -> "$what terlewat ${-days} hari (jatuh tempo ${fmt(dueMs)}). Segera diurus."
        days == 0 -> "$what jatuh tempo hari ini (${fmt(dueMs)})."
        days == 1 -> "$what jatuh tempo besok (${fmt(dueMs)})."
        else -> "$what jatuh tempo $days hari lagi (${fmt(dueMs)})."
    }

    fun plan(assets: List<AssetEntity>, nowMs: Long): List<ReminderItem> {
        val items = mutableListOf<ReminderItem>()
        for (a in assets) {
            if (a.type != AssetType.KENDARAAN) continue
            if (a.syncState == SyncState.DELETED || a.status == AssetStatus.DIHAPUSBUKUKAN) continue
            val title = "${a.name}${a.licensePlate?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""}"

            fun add(kind: String, label: String, dueMs: Long?) {
                if (dueMs == null || dueMs <= 0) return
                val days = daysUntil(dueMs, nowMs)
                val level = levelFor(days)
                if (level == 0) return
                items += ReminderItem("${a.id}|$kind|$dueMs", a.id, title, sentence(label, days, dueMs), level)
            }

            add("tax", "Pajak tahunan", a.annualTaxDueDate)
            add("plate", "Ganti plat 5 tahunan", a.fiveYearPlateDueDate)
            a.lastServiceDate?.takeIf { it > 0 }?.let { last ->
                val next = Calendar.getInstance().apply { timeInMillis = last; add(Calendar.MONTH, SERVICE_INTERVAL_MONTHS) }.timeInMillis
                add("service", "Servis berkala", next)
            }
        }
        return items
    }

    /** Kirim bila tingkat urgensi naik; yang sudah lewat jatuh tempo diulang tiap 7 hari. */
    fun shouldNotify(lastLevel: Int, lastAtMs: Long, level: Int, nowMs: Long): Boolean = when {
        level > lastLevel -> true
        level == 4 && lastLevel == 4 -> nowMs - lastAtMs >= REPEAT_WHEN_OVERDUE_MS
        else -> false
    }
}

object TaxReminders {
    private const val PREFS = "tax_reminders"
    private const val WORK_NAME = "tax_reminders_periodic"

    /** Memeriksa data lokal dan mengirim notifikasi yang perlu. Mengembalikan jumlah notifikasi terkirim. */
    suspend fun check(context: Context): Int {
        val app = context.applicationContext
        val assets = AppDatabase.getDatabase(app).assetDao().getAllAssets().first()
        val items = ReminderPlanner.plan(assets, System.currentTimeMillis())

        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val activeKeys = items.map { it.key }.toSet()
        val editor = prefs.edit()
        // Buang catatan kendaraan yang sudah beres (diperpanjang / dihapus) supaya tidak menumpuk.
        prefs.all.keys.filter { k -> k.drop(2) !in activeKeys }.forEach { editor.remove(it) }

        // Tanpa izin notifikasi: jangan catat sebagai terkirim, supaya dicoba lagi setelah izin diberikan.
        val canNotify = NotificationManagerCompat.from(app).areNotificationsEnabled()
        var sent = 0
        val now = System.currentTimeMillis()
        if (canNotify) {
            for (item in items) {
                val lastLevel = prefs.getInt("L:${item.key}", 0)
                val lastAt = prefs.getLong("T:${item.key}", 0L)
                if (!ReminderPlanner.shouldNotify(lastLevel, lastAt, item.level, now)) continue
                val ok = NotificationHelper.sendTaxReminderNotification(
                    context = app,
                    notificationId = item.key.hashCode() and 0x7fffffff,
                    title = item.title,
                    message = item.message,
                    assetId = item.assetId
                )
                if (ok) {
                    editor.putInt("L:${item.key}", item.level).putLong("T:${item.key}", now)
                    sent++
                }
            }
        }
        editor.apply()
        return sent
    }

    /** Pengecekan berkala di latar belakang (dua kali sehari), tanpa perlu aplikasi dibuka. Aman dipanggil berulang. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<TaxReminderWorker>(12, TimeUnit.HOURS)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

class TaxReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        TaxReminders.check(applicationContext)
        return Result.success()
    }
}
