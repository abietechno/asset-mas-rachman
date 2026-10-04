package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AssetEntity
import com.example.model.AssetType
import com.example.model.TaxStatus
import com.example.ui.AssetViewModel
import com.example.ui.CupertinoTab
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.DepreciationCalculator
import com.example.util.FormatUtils

@Composable
fun DashboardScreen(
    viewModel: AssetViewModel,
    onNavigateTab: (CupertinoTab) -> Unit,
    onOpenReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val analytics by viewModel.dashboardAnalytics.collectAsStateWithLifecycle()
    val allAssets by viewModel.allAssets.collectAsStateWithLifecycle()
    val currentUserName by viewModel.currentUserName.collectAsStateWithLifecycle()
    val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()

    val sync by viewModel.syncStatus.collectAsStateWithLifecycle()

    // Nilai perolehan vs nilai buku (garis lurus, tanah tidak menyusut) - dihitung ulang hanya saat data berubah.
    val valuation = remember(allAssets) {
        val cost = allAssets.sumOf { it.acquisitionCost }
        val book = allAssets.sumOf { DepreciationCalculator.calculate(it).currentBookValue }
        Triple(cost, book, (cost - book).coerceAtLeast(0.0))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Cupertino Top Bar
        item {
            CupertinoTopBar(
                title = "Asset Management",
                subtitle = "$currentUserName • $currentUserRole",
                actions = {
                    SyncStatusIcon(
                        status = sync,
                        onClick = {
                            if (sync.connected) viewModel.syncNow() else onNavigateTab(CupertinoTab.SETTINGS)
                        }
                    )
                }
            )
        }

        // Mode offline: aset di HP ini belum tersambung ke dashboard, jadi tidak akan muncul di sana.
        if (!sync.connected) {
            item {
                CupertinoCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("offline_banner"),
                    backgroundColor = CupertinoOrange.copy(alpha = 0.10f),
                    borderColor = CupertinoOrange.copy(alpha = 0.30f),
                    onClick = { onNavigateTab(CupertinoTab.SETTINGS) }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Mode offline",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CupertinoOrange
                        )
                        Text(
                            text = "Aset di layar ini hanya ada di HP dan belum tersambung ke dashboard. " +
                                "Ketuk untuk membuka Pengaturan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CupertinoSecondaryLabel
                        )
                    }
                }
            }
        }

        // Critical Tax Alert Banner (If any vehicle is expiring or expired)
        if (analytics.vehiclesWithTaxWarningCount > 0) {
            item {
                CupertinoCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("tax_alert_banner"),
                    backgroundColor = CupertinoRed.copy(alpha = 0.10f),
                    borderColor = CupertinoRed.copy(alpha = 0.30f),
                    onClick = { onNavigateTab(CupertinoTab.VEHICLES) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CupertinoRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Perhatian Pajak Kendaraan!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CupertinoRed
                            )
                            Text(
                                text = "${analytics.vehiclesWithTaxWarningCount} kendaraan memiliki masa pajak/STNK yang kritis atau telah habis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CupertinoLabel
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = CupertinoRed
                        )
                    }

                }
            }
        }

        // Nilai aset: total perolehan + nilai buku setelah penyusutan
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                CupertinoGlassCard {
                    Text(
                        text = "TOTAL NILAI ASET",
                        style = MaterialTheme.typography.labelSmall,
                        color = CupertinoSecondaryLabel,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FormatUtils.formatRupiah(valuation.first),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "Nilai perolehan • ${analytics.totalAssetsCount} unit aset",
                        style = MaterialTheme.typography.bodySmall,
                        color = CupertinoSecondaryLabel
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = CupertinoSeparator, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nilai buku kini",
                                style = MaterialTheme.typography.labelSmall,
                                color = CupertinoSecondaryLabel
                            )
                            Text(
                                text = FormatUtils.formatCompactRupiah(valuation.second),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CupertinoGreen,
                                maxLines = 1
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Akumulasi penyusutan",
                                style = MaterialTheme.typography.labelSmall,
                                color = CupertinoSecondaryLabel,
                                maxLines = 1
                            )
                            Text(
                                text = FormatUtils.formatCompactRupiah(valuation.third),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CupertinoOrange,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Section: KPI Metrik Operasional Aset (Focused on Operations, no financial clutter)
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "STATUS OPERASIONAL ASET",
                    style = MaterialTheme.typography.labelSmall,
                    color = CupertinoSecondaryLabel,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )

                // 2x2 Grid of Operational Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CupertinoMetricCard(
                        title = "Total Aset",
                        value = "${analytics.totalAssetsCount} Unit",
                        subtitle = "",
                        icon = Icons.Outlined.Inventory2,
                        iconColor = CupertinoPrimary,
                        iconBgColor = CupertinoPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f)
                    )

                    CupertinoMetricCard(
                        title = "Digunakan",
                        value = "${analytics.activeAssetsCount} Unit",
                        subtitle = "",
                        icon = Icons.Outlined.CheckCircle,
                        iconColor = CupertinoGreen,
                        iconBgColor = CupertinoGreenLight,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CupertinoMetricCard(
                        title = "Standby",
                        value = "${analytics.standbyAssetsCount} Unit",
                        subtitle = "",
                        icon = Icons.Outlined.Schedule,
                        iconColor = CupertinoPurple,
                        iconBgColor = CupertinoPurpleLight,
                        modifier = Modifier.weight(1f)
                    )

                    CupertinoMetricCard(
                        title = "Perlu Servis",
                        value = "${analytics.maintenanceAssetsCount} Unit",
                        subtitle = "",
                        icon = Icons.Outlined.Build,
                        iconColor = CupertinoOrange,
                        iconBgColor = CupertinoOrangeLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section: Breakdown Kategori Aset
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KATEGORI ASET LAPANGAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = CupertinoSecondaryLabel,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Lihat Semua",
                        style = MaterialTheme.typography.labelSmall,
                        color = CupertinoPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateTab(CupertinoTab.ASSETS) }
                    )
                }

                CupertinoCard {
                    analytics.categoryStats.values.forEachIndexed { index, stat ->
                        val icon = when (stat.type) {
                            AssetType.TANAH -> Icons.Outlined.Landscape
                            AssetType.BANGUNAN -> Icons.Outlined.Apartment
                            AssetType.KENDARAAN -> Icons.Outlined.DirectionsCar
                            AssetType.INVENTARIS -> Icons.Outlined.Devices
                        }
                        val iconColor = when (stat.type) {
                            AssetType.TANAH -> CupertinoGreen
                            AssetType.BANGUNAN -> CupertinoPurple
                            AssetType.KENDARAAN -> CupertinoPrimary
                            AssetType.INVENTARIS -> CupertinoTeal
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.assetCategoryFilter.value = stat.type
                                    onNavigateTab(CupertinoTab.ASSETS)
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(iconColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stat.type.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${stat.activeCount} Aktif • ${stat.standbyCount} Standby" +
                                            if (stat.maintenanceCount > 0) " • ${stat.maintenanceCount} Servis" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CupertinoSecondaryLabel
                                )
                            }

                            CupertinoBadge(
                                text = "${stat.count} Unit",
                                backgroundColor = iconColor.copy(alpha = 0.12f),
                                textColor = iconColor
                            )
                        }

                        if (index < analytics.categoryStats.size - 1) {
                            HorizontalDivider(
                                color = CupertinoSeparator,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 54.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section: Kendaraan Memerlukan Perhatian (Tax Expiry Status)
        if (analytics.urgentTaxVehicles.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STATUS PAJAK & LEGALITAS KENDARAAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = CupertinoSecondaryLabel,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Kelola Pajak",
                            style = MaterialTheme.typography.labelSmall,
                            color = CupertinoPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onNavigateTab(CupertinoTab.VEHICLES) }
                        )
                    }

                    analytics.urgentTaxVehicles.take(3).forEach { vehicle ->
                        val taxStatus = FormatUtils.getTaxStatus(vehicle.annualTaxDueDate)
                        val badgeColor = FormatUtils.getTaxStatusColor(taxStatus)
                        val badgeBg = when (taxStatus) {
                            TaxStatus.EXPIRED -> CupertinoRed.copy(alpha = 0.12f)
                            TaxStatus.CRITICAL -> CupertinoRed.copy(alpha = 0.12f)
                            TaxStatus.WARNING -> CupertinoOrange.copy(alpha = 0.12f)
                            TaxStatus.SAFE -> CupertinoGreen.copy(alpha = 0.12f)
                        }

                        CupertinoCard(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.openAssetDetail(vehicle) },
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(badgeBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = vehicle.licensePlate ?: "-",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        CupertinoBadge(
                                            text = FormatUtils.getTaxStatusBadgeText(vehicle.annualTaxDueDate),
                                            backgroundColor = badgeBg,
                                            textColor = badgeColor
                                        )
                                    }
                                    Text(
                                        text = "${vehicle.name} • ${vehicle.pic}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CupertinoSecondaryLabel,
                                        maxLines = 1
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = CupertinoSecondaryLabel
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Akses Cepat (ikon, supaya label tidak terpotong seperti tombol lebar sebelumnya)
        item {
            CupertinoSectionHeader(title = "Akses Cepat")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CupertinoQuickAction(
                    label = "Catat Aset",
                    icon = Icons.Outlined.AddCircle,
                    color = CupertinoPrimary,
                    onClick = { viewModel.openAddAsset() },
                    modifier = Modifier.weight(1f)
                )
                CupertinoQuickAction(
                    label = "Cari Aset",
                    icon = Icons.Outlined.Search,
                    color = CupertinoTeal,
                    onClick = { onNavigateTab(CupertinoTab.ASSETS) },
                    modifier = Modifier.weight(1f)
                )
                CupertinoQuickAction(
                    label = "Pajak",
                    icon = Icons.Outlined.DirectionsCar,
                    color = CupertinoOrange,
                    onClick = { onNavigateTab(CupertinoTab.VEHICLES) },
                    modifier = Modifier.weight(1f),
                    badge = analytics.vehiclesWithTaxWarningCount.takeIf { it > 0 }?.toString()
                )
                CupertinoQuickAction(
                    label = "Laporan",
                    icon = Icons.Outlined.Assessment,
                    color = CupertinoPurple,
                    onClick = onOpenReports,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
