package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.model.TaxStatus
import com.example.ui.theme.CupertinoGreen
import com.example.ui.theme.CupertinoOrange
import com.example.ui.theme.CupertinoRed
import com.example.ui.theme.CupertinoSecondaryLabel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object FormatUtils {

    private val indonesianLocale = Locale("id", "ID")

    fun formatRupiah(amount: Double): String {
        val symbols = DecimalFormatSymbols(indonesianLocale).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,##0", symbols)
        return "Rp ${formatter.format(amount)}"
    }

    fun formatCompactRupiah(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> {
                val valInM = amount / 1_000_000_000.0
                String.format(indonesianLocale, "Rp %.1f M", valInM)
            }
            amount >= 1_000_000 -> {
                val valInJt = amount / 1_000_000.0
                String.format(indonesianLocale, "Rp %.1f Jt", valInJt)
            }
            amount >= 1_000 -> {
                val valInRb = amount / 1_000.0
                String.format(indonesianLocale, "Rp %.0f Rb", valInRb)
            }
            else -> formatRupiah(amount)
        }
    }

    fun formatDate(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return "-"
        val sdf = SimpleDateFormat("dd MMM yyyy", indonesianLocale)
        return sdf.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return "-"
        val sdf = SimpleDateFormat("dd/MM/yyyy", indonesianLocale)
        return sdf.format(Date(timestamp))
    }

    fun getDaysUntil(dueDate: Long?): Long? {
        if (dueDate == null || dueDate <= 0) return null
        val diffMs = dueDate - System.currentTimeMillis()
        return TimeUnit.MILLISECONDS.toDays(diffMs)
    }

    fun getTaxStatus(dueDate: Long?): TaxStatus {
        val days = getDaysUntil(dueDate) ?: return TaxStatus.SAFE
        return when {
            days < 0 -> TaxStatus.EXPIRED
            days <= 14 -> TaxStatus.CRITICAL
            days <= 30 -> TaxStatus.WARNING
            else -> TaxStatus.SAFE
        }
    }

    fun getTaxStatusBadgeText(dueDate: Long?): String {
        val days = getDaysUntil(dueDate) ?: return "Tidak Terjadwal"
        return when {
            days < 0 -> "Lewat ${-days} hari!"
            days == 0L -> "Jatuh tempo Hari Ini!"
            days == 1L -> "Besok jatuh tempo"
            days <= 14 -> "$days hari lagi (Kritis)"
            days <= 30 -> "$days hari lagi"
            else -> "$days hari lagi"
        }
    }

    fun getTaxStatusColor(status: TaxStatus): Color {
        return when (status) {
            TaxStatus.EXPIRED -> CupertinoRed
            TaxStatus.CRITICAL -> CupertinoRed
            TaxStatus.WARNING -> CupertinoOrange
            TaxStatus.SAFE -> CupertinoGreen
        }
    }
}
