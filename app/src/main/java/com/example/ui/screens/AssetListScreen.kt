package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.model.AssetCondition
import com.example.model.AssetEntity
import com.example.model.AssetStatus
import com.example.model.AssetType
import com.example.ui.AssetViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.DepreciationCalculator
import com.example.util.FormatUtils

@Composable
fun AssetListScreen(
    viewModel: AssetViewModel,
    modifier: Modifier = Modifier
) {
    val assets by viewModel.filteredAssets.collectAsStateWithLifecycle()
    val searchQuery by viewModel.assetSearchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.assetCategoryFilter.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.assetStatusFilter.collectAsStateWithLifecycle()

    val categories = listOf<AssetType?>(null) + AssetType.values().toList()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .testTag("asset_list_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            CupertinoTopBar(
                title = "Aset Perusahaan",
                subtitle = "Total ${assets.size} item tercatat",
                actions = {
                    IconButton(
                        onClick = { viewModel.openAddAsset(selectedCategory) },
                        modifier = Modifier.testTag("add_asset_button")
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

        // Search Bar
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                CupertinoSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.assetSearchQuery.value = it },
                    placeholder = "Cari nama aset, kode tag, plat, PIC...",
                    modifier = Modifier.testTag("asset_search_bar")
                )
            }
        }

        // Filter Category Tabs (Horizontal Scrollable Cupertino Pills)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    val label = cat?.displayName ?: "Semua Aset"
                    val bg = if (isSelected) CupertinoPrimary else CupertinoFill
                    val textColor = if (isSelected) Color.White else CupertinoLabel

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(bg)
                            .clickable { viewModel.assetCategoryFilter.value = cat }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = textColor
                        )
                    }
                }
            }
        }

        // Status Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val statuses = listOf<AssetStatus?>(null) + AssetStatus.values().toList()
                statuses.forEach { st ->
                    val isSelected = st == selectedStatus
                    val label = st?.displayName ?: "Semua Status"
                    val bg = if (isSelected) CupertinoLabel else Color.Transparent
                    val textColor = if (isSelected) Color.White else CupertinoSecondaryLabel
                    val border = if (isSelected) null else BorderStroke(0.5.dp, CupertinoSeparator)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = bg,
                        border = border,
                        modifier = Modifier.clickable { viewModel.assetStatusFilter.value = st }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Asset List Items
        if (assets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = CupertinoSecondaryLabel,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tidak Ada Aset Ditemukan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CupertinoSecondaryLabel
                        )
                        Text(
                            text = "Coba ubah kata kunci atau filter pencarian Anda",
                            style = MaterialTheme.typography.bodySmall,
                            color = CupertinoTertiaryLabel
                        )
                    }
                }
            }
        } else {
            items(assets, key = { it.id }) { asset ->
                val dep = DepreciationCalculator.calculate(asset)
                val catIcon = when (asset.type) {
                    AssetType.TANAH -> Icons.Outlined.Landscape
                    AssetType.BANGUNAN -> Icons.Outlined.Apartment
                    AssetType.KENDARAAN -> Icons.Outlined.DirectionsCar
                    AssetType.INVENTARIS -> Icons.Outlined.Devices
                }
                val catColor = when (asset.type) {
                    AssetType.TANAH -> CupertinoGreen
                    AssetType.BANGUNAN -> CupertinoPurple
                    AssetType.KENDARAAN -> CupertinoPrimary
                    AssetType.INVENTARIS -> CupertinoTeal
                }

                CupertinoCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("asset_item_${asset.id}"),
                    onClick = { viewModel.openAssetDetail(asset) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(catColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = catIcon,
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = asset.code,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CupertinoSecondaryLabel,
                                    fontWeight = FontWeight.Bold
                                )

                                CupertinoBadge(
                                    text = asset.status.displayName,
                                    backgroundColor = when (asset.status) {
                                        AssetStatus.DIGUNAKAN -> CupertinoGreenLight
                                        AssetStatus.STANDBY -> CupertinoPrimary.copy(alpha = 0.12f)
                                        AssetStatus.DALAM_PERBAIKAN -> CupertinoOrangeLight
                                        else -> CupertinoFill
                                    },
                                    textColor = when (asset.status) {
                                        AssetStatus.DIGUNAKAN -> CupertinoGreen
                                        AssetStatus.STANDBY -> CupertinoPrimary
                                        AssetStatus.DALAM_PERBAIKAN -> CupertinoOrange
                                        else -> CupertinoLabel
                                    }
                                )
                            }

                            Text(
                                text = asset.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            // Specific tags
                            if (asset.type == AssetType.KENDARAAN && !asset.licensePlate.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                ) {
                                    Surface(
                                        color = CupertinoLabel,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = asset.licensePlate,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "PIC: ${asset.pic}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CupertinoSecondaryLabel,
                                        maxLines = 1
                                    )
                                }
                            } else {
                                Text(
                                    text = "${asset.location} • PIC: ${asset.pic}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CupertinoSecondaryLabel,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = CupertinoSeparator, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Operational details row (No financial & depreciation info)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Kondisi: ",
                                style = MaterialTheme.typography.labelSmall,
                                color = CupertinoSecondaryLabel
                            )
                            CupertinoBadge(
                                text = asset.condition.displayName,
                                backgroundColor = when (asset.condition) {
                                    AssetCondition.SANGAT_BAIK -> CupertinoGreen.copy(alpha = 0.12f)
                                    AssetCondition.BAIK -> CupertinoPrimary.copy(alpha = 0.12f)
                                    AssetCondition.PERLU_PERBAIKAN -> CupertinoOrange.copy(alpha = 0.12f)
                                    AssetCondition.RUSAK -> CupertinoRed.copy(alpha = 0.12f)
                                },
                                textColor = when (asset.condition) {
                                    AssetCondition.SANGAT_BAIK -> CupertinoGreen
                                    AssetCondition.BAIK -> CupertinoPrimary
                                    AssetCondition.PERLU_PERBAIKAN -> CupertinoOrange
                                    AssetCondition.RUSAK -> CupertinoRed
                                }
                            )
                        }

                        if (asset.type == AssetType.KENDARAAN && asset.annualTaxDueDate != null) {
                            val taxStatus = FormatUtils.getTaxStatus(asset.annualTaxDueDate)
                            Text(
                                text = "Pajak: ${FormatUtils.getTaxStatusBadgeText(asset.annualTaxDueDate)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = FormatUtils.getTaxStatusColor(taxStatus)
                            )
                        } else {
                            Text(
                                text = asset.location,
                                style = MaterialTheme.typography.labelSmall,
                                color = CupertinoSecondaryLabel,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
