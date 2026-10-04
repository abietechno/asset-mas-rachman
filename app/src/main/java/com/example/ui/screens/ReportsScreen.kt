package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AssetCondition
import com.example.model.AssetEntity
import com.example.model.AssetStatus
import com.example.model.AssetType
import com.example.ui.AssetViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReportFilter(val label: String) {
    SEMUA("Semua Aset"),
    KENDARAAN("Kendaraan & Pajak"),
    INVENTARIS("Inventaris Kantor & IT"),
    PROPERTI("Tanah & Bangunan"),
    PERLU_TINDAKAN("Perlu Servis / Kritis")
}

@Composable
fun ReportsScreen(
    viewModel: AssetViewModel,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allAssets by viewModel.allAssets.collectAsStateWithLifecycle()
    val analytics by viewModel.dashboardAnalytics.collectAsStateWithLifecycle()

    var activeReportFilter by remember { mutableStateOf(ReportFilter.SEMUA) }

    val filteredList = remember(allAssets, activeReportFilter) {
        when (activeReportFilter) {
            ReportFilter.SEMUA -> allAssets
            ReportFilter.KENDARAAN -> allAssets.filter { it.type == AssetType.KENDARAAN }
            ReportFilter.INVENTARIS -> allAssets.filter { it.type == AssetType.INVENTARIS }
            ReportFilter.PROPERTI -> allAssets.filter { it.type == AssetType.TANAH || it.type == AssetType.BANGUNAN }
            ReportFilter.PERLU_TINDAKAN -> allAssets.filter {
                it.status == AssetStatus.DALAM_PERBAIKAN ||
                        it.condition == AssetCondition.PERLU_PERBAIKAN ||
                        it.condition == AssetCondition.RUSAK ||
                        (it.type == AssetType.KENDARAAN && FormatUtils.getTaxStatus(it.annualTaxDueDate) != com.example.model.TaxStatus.SAFE)
            }
        }
    }

    val reportTimestamp = remember {
        SimpleDateFormat("dd MMMM yyyy, HH:mm 'WIB'", Locale("id", "ID")).format(Date())
    }

    fun generateShareableReportText(): String {
        val sb = StringBuilder()
        sb.append("📋 LAPORAN OPERASIONAL ASET PERUSAHAAN\n")
        sb.append("Waktu Penarikan Data: $reportTimestamp\n")
        sb.append("=========================================\n\n")
        sb.append("RINGKASAN OPERASIONAL:\n")
        sb.append("• Total Jumlah Aset: ${analytics.totalAssetsCount} Unit\n")
        sb.append("• Aktif Digunakan: ${analytics.activeAssetsCount} Unit\n")
        sb.append("• Standby di Pool: ${analytics.standbyAssetsCount} Unit\n")
        sb.append("• Butuh Servis/Tindakan: ${analytics.maintenanceAssetsCount} Unit\n")
        sb.append("• Kendaraan Butuh Perhatian Pajak: ${analytics.vehiclesWithTaxWarningCount} Unit\n\n")
        sb.append("=========================================\n")
        sb.append("DETAIL INVENTARIS OPERASIONAL:\n")

        filteredList.forEachIndexed { idx, asset ->
            sb.append("${idx + 1}. [${asset.code}] ${asset.name}\n")
            sb.append("   - Kategori: ${asset.type.displayName}\n")
            sb.append("   - Status: ${asset.status.displayName} | Kondisi: ${asset.condition.displayName}\n")
            sb.append("   - Lokasi: ${asset.location}\n")
            sb.append("   - PIC: ${asset.pic}\n")
            if (asset.type == AssetType.KENDARAAN) {
                sb.append("   - Plat Nomor: ${asset.licensePlate ?: "-"} | Pajak: ${FormatUtils.formatDate(asset.annualTaxDueDate)}\n")
                sb.append("   - No. Mesin: ${asset.engineNumber ?: "-"} | No. Rangka: ${asset.chassisNumber ?: "-"}\n")
                sb.append("   - STNK: ${asset.stnkNumber ?: "-"} | BPKB: ${asset.bpkbNumber ?: "-"}\n")
            } else if (asset.type == AssetType.INVENTARIS) {
                sb.append("   - Model: ${asset.brandModel ?: "-"} | SN: ${asset.serialNumber ?: "-"}\n")
            } else {
                sb.append("   - Sertifikat: ${asset.certificateType ?: "-"} (${asset.certificateNumber ?: "-"})\n")
            }
            sb.append("\n")
        }
        sb.append("=========================================\n")
        sb.append("Catatan: Valuasi finansial & depresiasi terintegrasi di Dashboard Web Admin.")
        return sb.toString()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .testTag("reports_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            CupertinoTopBar(
                title = "Laporan Operasional",
                subtitle = "Per $reportTimestamp",
                navigationIcon = onClose?.let {
                    {
                        IconButton(onClick = it, modifier = Modifier.testTag("close_report_button")) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = CupertinoSecondaryLabel
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val text = generateShareableReportText()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Laporan Operasional Aset Perusahaan")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan Operasional"))
                        },
                        modifier = Modifier.testTag("share_report_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CupertinoPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Bagikan Laporan",
                                tint = CupertinoPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )
        }

        // Executive Operational Summary Card
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "REKAPITULASI STATUS OPERASIONAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = CupertinoSecondaryLabel,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )

                CupertinoCard {
                    ReportMetricRow(
                        label = "Total Unit Aset Terdata",
                        value = "${analytics.totalAssetsCount} Unit",
                        valueColor = CupertinoLabel,
                        isBold = true
                    )
                    HorizontalDivider(color = CupertinoSeparator, thickness = 0.5.dp)
                    ReportMetricRow(
                        label = "Aset Aktif Digunakan",
                        value = "${analytics.activeAssetsCount} Unit",
                        valueColor = CupertinoGreen
                    )
                    HorizontalDivider(color = CupertinoSeparator, thickness = 0.5.dp)
                    ReportMetricRow(
                        label = "Unit Standby (Siap Pakai)",
                        value = "${analytics.standbyAssetsCount} Unit",
                        valueColor = CupertinoPurple
                    )
                    HorizontalDivider(color = CupertinoSeparator, thickness = 0.5.dp)
                    ReportMetricRow(
                        label = "Unit Butuh Servis / Perbaikan",
                        value = "${analytics.maintenanceAssetsCount} Unit",
                        valueColor = CupertinoOrange
                    )
                    HorizontalDivider(color = CupertinoSeparator, thickness = 0.5.dp)
                    ReportMetricRow(
                        label = "Kendaraan Perlu Perpanjangan Pajak",
                        value = "${analytics.vehiclesWithTaxWarningCount} Unit",
                        valueColor = if (analytics.vehiclesWithTaxWarningCount > 0) CupertinoRed else CupertinoGreen
                    )
                }
            }
        }

        // Report Filter Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportFilter.values().forEach { filter ->
                    val isSelected = filter == activeReportFilter
                    val bg = if (isSelected) CupertinoLabel else CupertinoFill
                    val textCol = if (isSelected) Color.White else CupertinoSecondaryLabel

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .clickable { activeReportFilter = filter }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textCol
                        )
                    }
                }
            }
        }

        // Detailed Asset Report Cards
        items(filteredList, key = { it.id }) { asset ->
            CupertinoCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .clickable { viewModel.openAssetDetail(asset) },
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = asset.code,
                                style = MaterialTheme.typography.labelSmall,
                                color = CupertinoSecondaryLabel,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${asset.type.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CupertinoPrimary
                            )
                        }
                        Text(
                            text = asset.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    CupertinoBadge(
                        text = asset.status.displayName,
                        backgroundColor = CupertinoFill,
                        textColor = CupertinoLabel
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = CupertinoSeparator, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Penanggung Jawab (PIC)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CupertinoSecondaryLabel
                        )
                        Text(
                            text = asset.pic,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Kondisi Fisik",
                            style = MaterialTheme.typography.labelSmall,
                            color = CupertinoSecondaryLabel
                        )
                        Text(
                            text = asset.condition.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when (asset.condition) {
                                AssetCondition.SANGAT_BAIK -> CupertinoGreen
                                AssetCondition.BAIK -> CupertinoPrimary
                                AssetCondition.PERLU_PERBAIKAN -> CupertinoOrange
                                AssetCondition.RUSAK -> CupertinoRed
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (asset.type == AssetType.KENDARAAN && !asset.licensePlate.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Plat: ${asset.licensePlate} • Mesin: ${asset.engineNumber ?: "-"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CupertinoSecondaryLabel
                        )
                        Text(
                            text = "Pajak: ${FormatUtils.formatDate(asset.annualTaxDueDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = FormatUtils.getTaxStatusColor(FormatUtils.getTaxStatus(asset.annualTaxDueDate))
                        )
                    }
                } else {
                    Text(
                        text = "Lokasi: ${asset.location}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CupertinoSecondaryLabel
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportMetricRow(
    label: String,
    value: String,
    valueColor: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else CupertinoSecondaryLabel,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}
