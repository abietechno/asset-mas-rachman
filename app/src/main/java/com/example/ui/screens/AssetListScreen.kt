package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Landscape
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
import com.example.model.AssetCondition
import com.example.model.AssetStatus
import com.example.model.AssetType
import com.example.ui.AssetViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.FormatUtils

/** Ikon & warna per jenis bidang aset, dipakai bersama oleh daftar aset dan beranda. */
internal fun assetIcon(type: AssetType) = when (type) {
    AssetType.TANAH -> Icons.Outlined.Landscape
    AssetType.BANGUNAN -> Icons.Outlined.Apartment
    AssetType.KENDARAAN -> Icons.Outlined.DirectionsCar
    AssetType.INVENTARIS -> Icons.Outlined.Devices
}

internal fun assetColor(type: AssetType) = when (type) {
    AssetType.TANAH -> CupertinoGreen
    AssetType.BANGUNAN -> CupertinoPurple
    AssetType.KENDARAAN -> CupertinoPrimary
    AssetType.INVENTARIS -> CupertinoTeal
}

internal fun statusColor(status: AssetStatus) = when (status) {
    AssetStatus.DIGUNAKAN -> CupertinoGreen
    AssetStatus.STANDBY -> CupertinoPrimary
    AssetStatus.DISEWAKAN -> CupertinoTeal
    AssetStatus.DALAM_PERBAIKAN -> CupertinoOrange
    AssetStatus.DIHAPUSBUKUKAN -> CupertinoSecondaryLabelLight
}

internal fun conditionColor(condition: AssetCondition) = when (condition) {
    AssetCondition.SANGAT_BAIK -> CupertinoGreen
    AssetCondition.BAIK -> CupertinoPrimary
    AssetCondition.PERLU_PERBAIKAN -> CupertinoOrange
    AssetCondition.RUSAK -> CupertinoRed
}

/** Pil filter kecil untuk baris kategori (biru) dan status (netral). */
@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    strong: Boolean = true
) {
    val bg = when {
        selected && strong -> CupertinoPrimary
        selected -> CupertinoLabel
        else -> CupertinoFill
    }
    val fg = when {
        selected && strong -> Color.White
        selected -> CupertinoTheme.colors.surfaceBackground
        else -> CupertinoLabel
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = fg,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssetListScreen(
    viewModel: AssetViewModel,
    modifier: Modifier = Modifier
) {
    val assets by viewModel.filteredAssets.collectAsStateWithLifecycle()
    val searchQuery by viewModel.assetSearchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.assetCategoryFilter.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.assetCategoryIdFilter.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.assetStatusFilter.collectAsStateWithLifecycle()
    val serverCategories by viewModel.categories.collectAsStateWithLifecycle()
    val glass = LocalCupertinoGlass.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .testTag("asset_list_screen"),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        item {
            CupertinoTopBar(
                title = "Aset Perusahaan",
                subtitle = "${assets.size} aset tercatat"
            )
        }

        // Pencarian + filter kategori tetap menempel di atas saat daftar digulir.
        stickyHeader {
            Surface(
                color = glass.surface.copy(alpha = 0.97f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(0.5.dp, glass.hairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Box(modifier = Modifier.padding(horizontal = Dimens.ScreenMargin)) {
                        CupertinoSearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.assetSearchQuery.value = it },
                            placeholder = "Cari nama, kode, plat, PIC...",
                            modifier = Modifier.testTag("asset_search_bar")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Kategori dari dashboard bila sudah tersinkron; kalau belum, pakai 4 jenis bawaan.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = Dimens.ScreenMargin),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterPill(
                            label = "Semua",
                            selected = selectedCategory == null && selectedCategoryId == null,
                            onClick = {
                                viewModel.assetCategoryFilter.value = null
                                viewModel.assetCategoryIdFilter.value = null
                            }
                        )
                        if (serverCategories.isEmpty()) {
                            AssetType.values().forEach { t ->
                                FilterPill(
                                    label = t.displayName,
                                    selected = selectedCategoryId == null && selectedCategory == t,
                                    onClick = {
                                        viewModel.assetCategoryFilter.value = t
                                        viewModel.assetCategoryIdFilter.value = null
                                    }
                                )
                            }
                        } else {
                            serverCategories.forEach { c ->
                                FilterPill(
                                    label = c.name,
                                    selected = selectedCategoryId == c.id,
                                    onClick = {
                                        viewModel.assetCategoryIdFilter.value = c.id
                                        // Jenis bidang ikut diset agar tombol tambah memilih kategori yang sesuai.
                                        viewModel.assetCategoryFilter.value = c.kind
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Filter status ikut tergulir (lebih jarang dipakai daripada kategori).
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenMargin, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterPill(
                    label = "Semua Status",
                    selected = selectedStatus == null,
                    onClick = { viewModel.assetStatusFilter.value = null },
                    strong = false
                )
                AssetStatus.values().forEach { st ->
                    FilterPill(
                        label = st.displayName,
                        selected = selectedStatus == st,
                        onClick = { viewModel.assetStatusFilter.value = st },
                        strong = false
                    )
                }
            }
        }

        if (assets.isEmpty()) {
            item {
                val adaFilter = searchQuery.isNotBlank() || selectedStatus != null ||
                    selectedCategory != null || selectedCategoryId != null

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = CupertinoTertiaryLabel,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (adaFilter) "Tidak ada aset yang cocok" else "Belum ada aset",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CupertinoSecondaryLabel
                    )
                    Text(
                        text = if (adaFilter) "Coba ubah kata kunci atau filter." else "Ketuk tombol + untuk mencatat aset pertama.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CupertinoTertiaryLabel
                    )
                    if (adaFilter) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = {
                            viewModel.assetSearchQuery.value = ""
                            viewModel.assetStatusFilter.value = null
                            viewModel.assetCategoryFilter.value = null
                            viewModel.assetCategoryIdFilter.value = null
                        }) {
                            Text("Hapus semua filter", color = CupertinoPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            itemsIndexed(assets, key = { _, asset -> asset.id }) { index, asset ->
                val isVehicle = asset.type == AssetType.KENDARAAN
                val taxStatus = asset.annualTaxDueDate?.let { FormatUtils.getTaxStatus(it) }

                CupertinoAssetRow(
                    title = asset.name,
                    subtitle = if (isVehicle && !asset.licensePlate.isNullOrBlank()) {
                        listOfNotNull(asset.licensePlate, asset.pic.ifBlank { null }).joinToString(" - ")
                    } else {
                        listOfNotNull(asset.code.ifBlank { null }, asset.location.ifBlank { null }).joinToString(" - ")
                    },
                    icon = assetIcon(asset.type),
                    iconColor = assetColor(asset.type),
                    chipText = asset.status.displayName,
                    chipBackground = statusColor(asset.status).copy(alpha = 0.12f),
                    chipColor = statusColor(asset.status),
                    trailingNote = if (isVehicle && taxStatus != null) {
                        FormatUtils.getTaxStatusBadgeText(asset.annualTaxDueDate)
                    } else {
                        asset.condition.displayName
                    },
                    trailingNoteColor = if (isVehicle && taxStatus != null) {
                        FormatUtils.getTaxStatusColor(taxStatus)
                    } else {
                        conditionColor(asset.condition)
                    },
                    shape = groupedShape(index, assets.size),
                    showDivider = index < assets.size - 1,
                    onClick = { viewModel.openAssetDetail(asset) },
                    modifier = Modifier
                        .padding(horizontal = Dimens.ScreenMargin)
                        .testTag("asset_item_${asset.id}")
                )
            }
        }
    }
}
