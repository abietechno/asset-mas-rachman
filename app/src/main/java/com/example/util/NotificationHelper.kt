package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.model.AssetEntity

object NotificationHelper {

    const val CHANNEL_ID = "asset_tax_channel"
    private const val CHANNEL_NAME = "Pengingat Pajak & Servis Kendaraan"
    private const val CHANNEL_DESC =
        "Pengingat jatuh tempo pajak tahunan, plat 5 tahunan, dan servis berkala kendaraan perusahaan"

    /** Notifikasi dikelompokkan agar Android menampilkannya rapi. */
    private const val GROUP_KEY = "com.example.REMINDERS"
    private const val SUMMARY_ID = 1

    /**
     * Id notifikasi untuk satu pengingat. Tetap sama selama jatuh temponya sama, sehingga memasang
     * ulang hanya memperbarui notifikasi yang itu juga (tidak menumpuk). Mulai dari 2 agar tidak
     * bertabrakan dengan [SUMMARY_ID].
     */
    fun notificationIdFor(reminderKey: String): Int =
        (reminderKey.hashCode() and 0x7fffffff).coerceAtLeast(2)

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = CHANNEL_DESC
            enableVibration(true)
            setShowBadge(true) // tanda pada ikon aplikasi di layar utama
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun openAppIntent(context: Context, notificationId: Int, assetId: Long?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (assetId != null) putExtra("TARGET_ASSET_ID", assetId)
        }
        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Mengirim satu pengingat. Dipasang ulang setiap pemeriksaan selama jatuh temponya belum beres,
     * supaya tetap terlihat di panel notifikasi dan penanda (badge) ikon aplikasi tidak hilang.
     *
     * [alert] true hanya saat tingkat urgensinya naik; false membuatnya diperbarui tanpa bunyi/getar
     * lagi agar tidak mengganggu.
     *
     * Mengembalikan false bila izin notifikasi dimatikan pengguna, sehingga pemanggil tahu pengingat
     * itu belum tersampaikan dan bisa mencoba lagi nanti.
     */
    fun sendTaxReminderNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        assetId: Long? = null,
        alert: Boolean = true
    ): Boolean {
        return try {
            initNotificationChannel(context)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(if (alert) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(openAppIntent(context, notificationId, assetId))
                .setGroup(GROUP_KEY)
                .setOnlyAlertOnce(!alert)
                .setAutoCancel(true)

            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            true
        } catch (e: SecurityException) {
            false // izin POST_NOTIFICATIONS dicabut
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Notifikasi ringkasan yang menampung semua pengingat. Hanya dipakai bila ada minimal dua
     * pengingat: ringkasan tanpa anggota sering dibuang sendiri oleh Android/MIUI sehingga justru
     * tidak terlihat. [count] juga dipakai sebagai angka pada ikon aplikasi di peluncur yang
     * mendukungnya.
     */
    fun showSummary(context: Context, count: Int): Boolean {
        if (count < 2) return false
        return try {
            initNotificationChannel(context)

            val text = "$count kendaraan perlu perhatian (pajak, plat, atau servis)"
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Pengingat kendaraan")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(openAppIntent(context, SUMMARY_ID, null))
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setNumber(count) // angka badge
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)

            NotificationManagerCompat.from(context).notify(SUMMARY_ID, builder.build())
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Dipakai saat ringkasan tidak lagi relevan (pengingat tinggal satu atau sudah beres). */
    fun clearSummary(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(SUMMARY_ID) }
    }

    /** Menarik satu pengingat yang sudah beres, supaya penanda di ikon aplikasi ikut turun. */
    fun cancelReminder(context: Context, notificationId: Int) {
        runCatching { NotificationManagerCompat.from(context).cancel(notificationId) }
    }

    fun dispatchBatchTaxAlerts(context: Context, vehiclesNeedingAttention: List<AssetEntity>): Int {
        var dispatched = 0
        vehiclesNeedingAttention.forEachIndexed { index, vehicle ->
            val days = FormatUtils.getDaysUntil(vehicle.annualTaxDueDate) ?: 0
            val plate = vehicle.licensePlate ?: "-"
            val title = if (days < 0) {
                "Pajak terlewat: ${vehicle.name} ($plate)"
            } else {
                "Pengingat pajak: ${vehicle.name} ($plate)"
            }
            val text = if (days < 0) {
                "Pajak tahunan telah lewat ${-days} hari. Segera proses perpanjangan STNK."
            } else {
                "Masa berlaku pajak tahunan habis dalam $days hari (${FormatUtils.formatDate(vehicle.annualTaxDueDate)})."
            }
            val success = sendTaxReminderNotification(
                context = context,
                notificationId = 1000 + index,
                title = title,
                message = text,
                assetId = vehicle.id
            )
            if (success) dispatched++
        }
        if (dispatched > 0) showSummary(context, dispatched)
        return dispatched
    }
}
