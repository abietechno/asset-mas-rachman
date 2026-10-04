package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AssetEntity
import com.example.model.AssetType
import com.example.ui.AssetViewModel
import com.example.ui.components.*
import com.example.ui.screens.assetColor
import com.example.ui.screens.assetIcon
import com.example.ui.screens.conditionColor
import com.example.ui.screens.statusColor
import com.example.ui.theme.*
import com.example.util.DepreciationCalculator
import com.example.util.FormatUtils

/** Baris label-nilai yang padat; nilai boleh turun ke baris kedua (mis. alamat panjang). */
@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    valueColor: Color? = null,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = CupertinoSecondaryLabel,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                maxLines = 2,
                modifier = Modifier.weight(1.3f)
            )
        }
        if (showDivider) {
            HorizontalDivider(color = CupertinoSeparator, thickness = 0.5.dp)
        }
    }
}

/** Tombol aksi kecil (ikon + label) di bawah header detail. */
@Composable
private fun DetailAction(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.10f),
        contentColor = color,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = CupertinoSecondaryLabel,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailSheet(
    asset: AssetEntity,
    viewModel: AssetViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val categoryName = categories.firstOrNull { it.id == asset.categoryId }?.name ?: asset.type.displayName
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dep = remember(asset) { DepreciationCalculator.calculate(asset) }
    val isVehicle = asset.type == AssetType.KENDARAAN
    val isProperty = asset.type == AssetType.TANAH || asset.type == AssetType.BANGUNAN
    val hasPin = asset.latitude != null && asset.longitude != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(CupertinoTertiaryLabel)
            )
        },
        modifier = Modifier.testTag("asset_detail_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenMargin),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Header ringkas: ikon kategori, nama, kode, chip status & kondisi, baris aksi ──
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(assetColor(asset.type).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = assetIcon(asset.type),
                            contentDescription = null,
                            tint = assetColor(asset.type),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = asset.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                        Text(
                            text = listOfNotNull(asset.code.ifBlank { null }, categoryName).joinToString(" - "),
                            style = MaterialTheme.typography.bodySmall,
                            color = CupertinoSecondaryLabel,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CupertinoBadge(
                        text = asset.status.displayName,
                        backgroundColor = statusColor(asset.status).copy(alpha = 0.12f),
                        textColor = statusColor(asset.status)
                    )
                    CupertinoBadge(
                        text = asset.condition.displayName,
                        backgroundColor = conditionColor(asset.condition).copy(alpha = 0.12f),
                        textColor = conditionColor(asset.condition)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailAction(
                        label = "Edit",
                        icon = Icons.Outlined.Edit,
                        color = CupertinoPrimary,
                        onClick = {
                            onDismiss()
                            viewModel.openEditAsset(asset)
                        }
                    )
                    if (isVehicle) {
                        DetailAction(
                            label = "Perpanjang Pajak",
                            icon = Icons.Outlined.EventAvailable,
                            color = CupertinoGreen,
                            onClick = { viewModel.renewTaxForVehicle(asset) }
                        )
                    }
                    if (hasPin) {
                        DetailAction(
                            label = "Buka Peta",
                            icon = Icons.Outlined.Map,
                            color = CupertinoTeal,
                            onClick = {
                                // geo: membuka aplikasi peta apa pun; label muncul sebagai nama pin.
                                val label = android.net.Uri.encode(asset.name)
                                val uri = android.net.Uri.parse(
                                    "geo:${asset.latitude},${asset.longitude}?q=${asset.latitude},${asset.longitude}($label)"
                                )
                                runCatching {
                                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                                }
                            },
                            modifier = Modifier.testTag("btn_open_in_maps")
                        )
                    }
                    DetailAction(
                        label = "Hapus",
                        icon = Icons.Outlined.Delete,
                        color = CupertinoRed,
                        onClick = { showDeleteConfirm = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Nilai & penyusutan ──
            item {
                SectionTitle("NILAI & PENYUSUTAN")
                CupertinoCard {
                    if (asset.acquisitionCost <= 0.0) {
                        Text(
                            text = "Nilai perolehan belum diisi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CupertinoSecondaryLabel
                        )
                        Text(
                            text = "Ketuk Edit untuk mengisinya agar total nilai aset ikut terhitung.",
                            style = MaterialTheme.typography.labelSmall,
                            color = CupertinoTertiaryLabel,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    } else {
                        DetailInfoRow("Nilai Perolehan", FormatUtils.formatRupiah(dep.acquisitionCost))
                        DetailInfoRow("Tanggal Perolehan", FormatUtils.formatDate(asset.acquisitionDate))

                        if (asset.type == AssetType.TANAH || dep.usefulLifeYears <= 0) {
                            DetailInfoRow(
                                label = "Penyusutan",
                                value = if (asset.type == AssetType.TANAH) "Tanah tidak disusutkan" else "Tidak disusutkan",
                                showDivider = false
                            )
                        } else {
                            DetailInfoRow(
                                "Nilai Buku Saat Ini",
                                FormatUtils.formatRupiah(dep.currentBookValue),
                                valueColor = CupertinoGreen
                            )
                            DetailInfoRow(
                                "Akumulasi Penyusutan",
                                FormatUtils.formatRupiah(dep.accumulatedDepreciation),
                                valueColor = CupertinoOrange
                            )
                            DetailInfoRow("Penyusutan / Tahun", FormatUtils.formatRupiah(dep.annualDepreciation))
                            if (dep.salvageValue > 0) {
                                DetailInfoRow("Nilai Residu", FormatUtils.formatRupiah(dep.salvageValue))
                            }
                            DetailInfoRow(
                                label = "Masa Manfaat",
                                value = "${dep.usefulLifeYears} tahun - sisa ${dep.remainingMonths} bulan",
                                showDivider = false
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { dep.depreciationPercentage },
                                color = CupertinoOrange,
                                trackColor = CupertinoFill,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Text(
                                text = "${(dep.depreciationPercentage * 100).toInt()}% masa manfaat terpakai",
                                style = MaterialTheme.typography.labelSmall,
                                color = CupertinoSecondaryLabel,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Kendaraan ──
            if (isVehicle) {
                item {
                    val taxStatus = FormatUtils.getTaxStatus(asset.annualTaxDueDate)
                    val taxColor = FormatUtils.getTaxStatusColor(taxStatus)

                    SectionTitle("KENDARAAN & SURAT-SURAT")
                    CupertinoCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1C1C1E))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = asset.licensePlate ?: "TANPA PLAT",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            CupertinoBadge(
                                text = FormatUtils.getTaxStatusBadgeText(asset.annualTaxDueDate),
                                backgroundColor = taxColor.copy(alpha = 0.12f),
                                textColor = taxColor
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        asset.vehicleType?.let { DetailInfoRow("Jenis Kendaraan", it.displayName) }
                        if (!asset.brand.isNullOrBlank() || !asset.vehicleModel.isNullOrBlank()) {
                            DetailInfoRow(
                                "Merk & Model",
                                listOfNotNull(asset.brand, asset.vehicleModel).joinToString(" ")
                            )
                        }
                        asset.yearManufacture?.let { DetailInfoRow("Tahun Pembuatan", it.toString()) }
                        DetailInfoRow("Nomor Mesin", asset.engineNumber ?: "-")
                        DetailInfoRow("Nomor Rangka (VIN)", asset.chassisNumber ?: "-")
                        DetailInfoRow("Nomor BPKB", asset.bpkbNumber ?: "-")
                        DetailInfoRow("Nomor STNK", asset.stnkNumber ?: "-")
                        DetailInfoRow(
                            "Jatuh Tempo Pajak",
                            FormatUtils.formatDate(asset.annualTaxDueDate),
                            valueColor = taxColor
                        )
                        DetailInfoRow("Plat 5 Tahunan", FormatUtils.formatDate(asset.fiveYearPlateDueDate))
                        asset.annualTaxAmount?.let {
                            DetailInfoRow("Estimasi Pajak Tahunan", FormatUtils.formatRupiah(it))
                        }
                        DetailInfoRow(
                            label = "Servis Terakhir",
                            value = asset.lastServiceDate?.let { FormatUtils.formatDate(it) } ?: "Belum dicatat",
                            showDivider = false
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ── Tanah / Bangunan ──
            if (isProperty) {
                item {
                    SectionTitle("LEGALITAS & PROPERTI")
                    CupertinoCard {
                        DetailInfoRow("Jenis Sertifikat", asset.certificateType ?: "-")
                        DetailInfoRow("Nomor Sertifikat", asset.certificateNumber ?: "-")
                        DetailInfoRow("NOP PBB", asset.pbbNop ?: "-")
                        asset.landAreaM2?.let { DetailInfoRow("Luas Tanah", "$it m2") }
                        asset.buildingAreaM2?.let { DetailInfoRow("Luas Bangunan", "$it m2") }
                        asset.numberOfFloors?.let { DetailInfoRow("Jumlah Lantai", "$it lantai") }
                        if (!asset.pbgNumber.isNullOrBlank()) DetailInfoRow("No. IMB / PBG", asset.pbgNumber)
                        if (!asset.address.isNullOrBlank()) DetailInfoRow("Alamat", asset.address)

                        if (hasPin) {
                            DetailInfoRow("Koordinat", "%.6f, %.6f".format(asset.latitude, asset.longitude))
                            DetailInfoRow(
                                label = "Akurasi Lokasi",
                                value = asset.locationAccuracyM?.let { "+/- ${it.toInt()} m (GPS)" } ?: "Titik manual",
                                showDivider = false
                            )
                        } else {
                            DetailInfoRow(label = "Lokasi di Peta", value = "Belum ada pin", showDivider = false)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ── Inventaris ──
            if (asset.type == AssetType.INVENTARIS) {
                item {
                    SectionTitle("SPESIFIKASI INVENTARIS")
                    CupertinoCard {
                        DetailInfoRow("Merk & Model", asset.brandModel ?: "-")
                        DetailInfoRow("Nomor Seri", asset.serialNumber ?: "-")
                        DetailInfoRow("Departemen", asset.department ?: "-", showDivider = false)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ── Penempatan & catatan ──
            item {
                SectionTitle("PENEMPATAN")
                CupertinoCard {
                    DetailInfoRow("Lokasi Fisik", asset.location.ifBlank { "-" })
                    DetailInfoRow(
                        label = "Penanggung Jawab",
                        value = asset.pic.ifBlank { "-" },
                        showDivider = asset.notes.isNotBlank()
                    )
                    if (asset.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Catatan",
                            style = MaterialTheme.typography.labelSmall,
                            color = CupertinoSecondaryLabel
                        )
                        Text(
                            text = asset.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus aset?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Aset \"${asset.name}\" akan dihapus dari aplikasi dan dari dashboard saat sinkronisasi berikutnya.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteAsset(asset)
                }) { Text("Hapus", color = CupertinoRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
            }
        )
    }
}
