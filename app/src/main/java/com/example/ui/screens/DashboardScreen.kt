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
import com.example.util.FormatUtils

@Composable
fun DashboardScreen(
    viewModel: AssetViewModel,
    onNavigateTab: (CupertinoTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val analytics by viewModel.dashboardAnalytics.collectAsStateWithLifecycle()
    val allAssets by viewModel.allAssets.collectAsStateWithLifecycle()
    val currentUserName by viewModel.currentUserName.collectAsStateWithLifecycle()
    val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()

    var showLogoutDialog by remember { mutableStateOf(false) }
    val sync by viewModel.syncStatus.collectAsStateWithLifecycle()

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
                    // Profile & Logout Button
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.testTag("user_profile_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CupertinoFill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Profil & Keluar",
                                tint = CupertinoLabel,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Quick Add Button
                    IconButton(
                        onClick = { viewModel.openAddAsset() },
                        modifier = Modifier.testTag("top_add_asset_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CupertinoPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah Aset",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
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
                    onClick = { viewModel.logout() }
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
                                "Ketuk untuk kembali ke layar login, isi alamat server, lalu masuk dengan akun dashboard.",
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerTaxRemindersNow() },
                            colors = ButtonDefaults.buttonColors(containerColor = CupertinoRed),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kirim Notifikasi",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { onNavigateTab(CupertinoTab.VEHICLES) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CupertinoRed),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "Periksa Detail",
                                style = MaterialTheme.typography.labelSmall,
                                color = CupertinoRed,
                                fontWeight = FontWeight.Bold
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
                        title = "Total Unit Aset",
                        value = "${analytics.totalAssetsCount} Unit",
                        subtitle = "Seluruh Kategori",
                        icon = Icons.Outlined.Inventory2,
                        iconColor = CupertinoPrimary,
                        iconBgColor = CupertinoPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f)
                    )

                    CupertinoMetricCard(
                        title = "Aktif Digunakan",
                        value = "${analytics.activeAssetsCount} Unit",
                        subtitle = "Sedang Operasional",
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
                        title = "Siap Pakai / Standby",
                        value = "${analytics.standbyAssetsCount} Unit",
                        subtitle = "Tersedia di Pool",
                        icon = Icons.Outlined.Schedule,
                        iconColor = CupertinoPurple,
                        iconBgColor = CupertinoPurpleLight,
                        modifier = Modifier.weight(1f)
                    )

                    CupertinoMetricCard(
                        title = "Butuh Servis / Perbaikan",
                        value = "${analytics.maintenanceAssetsCount} Unit",
                        subtitle = "Perlu Ditangani",
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

        // Section: Akses Cepat Lapangan
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "AKSES CEPAT OPERASIONAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = CupertinoSecondaryLabel,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CupertinoActionButton(
                        text = "+ Catat Aset",
                        onClick = { viewModel.openAddAsset() },
                        icon = Icons.Default.AddCircleOutline,
                        backgroundColor = CupertinoPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    CupertinoActionButton(
                        text = "Laporan Lapangan",
                        onClick = { onNavigateTab(CupertinoTab.REPORTS) },
                        icon = Icons.Default.Assessment,
                        backgroundColor = CupertinoPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Profile & Theme Settings Dialog
    if (showLogoutDialog) {
        val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
        val sync by viewModel.syncStatus.collectAsStateWithLifecycle()

        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CupertinoPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = CupertinoPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = currentUserName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentUserRole,
                            style = MaterialTheme.typography.bodySmall,
                            color = CupertinoSecondaryLabel
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SINKRONISASI DASHBOARD",
                        style = MaterialTheme.typography.labelSmall,
                        color = CupertinoSecondaryLabel,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = if (sync.connected) "Terhubung ke ${sync.serverUrl}" else "Mode offline: data hanya ada di perangkat ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (sync.connected) {
                        if (sync.pendingCount > 0) {
                            Text(
                                text = "${sync.pendingCount} perubahan menunggu dikirim",
                                style = MaterialTheme.typography.bodySmall,
                                color = CupertinoOrange
                            )
                        }
                        sync.lastSyncAt?.let {
                            Text(
                                text = "Terakhir sinkron: " + java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID")).format(it),
                                style = MaterialTheme.typography.bodySmall,
                                color = CupertinoSecondaryLabel
                            )
                        }
                    }
                    sync.message?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = CupertinoSecondaryLabel)
                    }
                    if (sync.connected) {
                        TextButton(onClick = { viewModel.syncNow() }) {
                            Text("Sinkronkan Sekarang", color = CupertinoPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "TEMA TAMPILAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = CupertinoSecondaryLabel,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Theme selector buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            com.example.ui.ThemeMode.SYSTEM to "📱 Sistem",
                            com.example.ui.ThemeMode.LIGHT to "☀️ Terang",
                            com.example.ui.ThemeMode.DARK to "🌙 Gelap"
                        ).forEach { (mode, title) ->
                            val isSelected = currentThemeMode == mode
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) CupertinoPrimary else CupertinoFill,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setThemeMode(mode) }
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Mode Terang memberikan tampilan bersih dan cerah. Pada Mode Gelap, warna font otomatis disesuaikan menjadi putih terang.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CupertinoSecondaryLabel
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    }
                ) {
                    Text("Keluar Akun", color = CupertinoRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}
