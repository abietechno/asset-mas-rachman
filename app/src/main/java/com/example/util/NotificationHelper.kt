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
    private const val CHANNEL_NAME = "Jatuh Tempo Pajak Kendaraan"
    private const val CHANNEL_DESC = "Notifikasi peringatan masa berlaku STNK dan pajak tahunan kendaraan perusahaan"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendTaxReminderNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        assetId: Long? = null
    ): Boolean {
        try {
            initNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                if (assetId != null) {
                    putExtra("TARGET_ASSET_ID", assetId)
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val manager = NotificationManagerCompat.from(context)
            // If Android 13+ and notification permission is granted, notify
            manager.notify(notificationId, builder.build())
            return true
        } catch (e: SecurityException) {
            // Permission not granted or restricted
            return false
        } catch (e: Exception) {
            return false
        }
    }

    fun dispatchBatchTaxAlerts(context: Context, vehiclesNeedingAttention: List<AssetEntity>): Int {
        var dispatched = 0
        vehiclesNeedingAttention.forEachIndexed { index, vehicle ->
            val days = FormatUtils.getDaysUntil(vehicle.annualTaxDueDate) ?: 0
            val plate = vehicle.licensePlate ?: "-"
            val title = if (days < 0) {
                "🔴 Pajak Terlewat: ${vehicle.name} ($plate)"
            } else {
                "⚠️ Pengingat Pajak: ${vehicle.name} ($plate)"
            }
            val text = if (days < 0) {
                "Pajak tahunan telah kadaluarsa ${-days} hari yang lalu. Segera proses perpanjangan STNK."
            } else {
                "Masa berlaku pajak tahunan akan habis dalam $days hari (${FormatUtils.formatDate(vehicle.annualTaxDueDate)})."
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
        return dispatched
    }
}
