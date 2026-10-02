package com.example.ui.dialogs

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssetEntity
import com.example.model.AssetType
import com.example.model.TaxStatus
import com.example.ui.AssetViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.DepreciationCalculator
import com.example.util.FormatUtils
import com.example.util.NotificationHelper

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
    val schedule = remember(asset) { DepreciationCalculator.generateYearlySchedule(asset) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
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
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // Header: Code, Category, Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = asset.code,
                            style = MaterialTheme.typography.labelMedium,
                            color = CupertinoSecondaryLabel,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = CupertinoPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                onDismiss()
                                viewModel.openEditAsset(asset)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CupertinoFill)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Aset",
                                tint = CupertinoPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CupertinoRedLight)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Aset",
                                tint = CupertinoRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CupertinoBadge(
                        text = "Status: ${asset.status.displayName}",
                        backgroundColor = CupertinoPrimary.copy(alpha = 0.12f),
                        textColor = CupertinoPrimary
                    )

                    CupertinoBadge(
                        text = "Kondisi: ${asset.condition.displayName}",
                        backgroundColor = CupertinoGreenLight,
                        textColor = CupertinoGreen
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Operational Status Card (No financial & depreciation info)
            item {
                Text(
                    text = "STATUS & IDENTITAS OPERASIONAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = CupertinoSecondaryLabel,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                CupertinoCard {
                    DetailInfoRow("Kode Tag Aset", asset.code)
                    DetailInfoRow("Kategori Aset", categoryName)
                    DetailInfoRow("Status Operasional", asset.status.displayName)
                    DetailInfoRow("Kondisi Fisik", asset.condition.displayName)
                    DetailInfoRow("Lokasi Penempatan", asset.location)
                    DetailInfoRow("Penanggung Jawab (PIC)", asset.pic)
                    DetailInfoRow("Terdata Sejak", FormatUtils.formatDate(asset.acquisitionDate))

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CupertinoPrimary.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = CupertinoPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Valuasi finansial, perolehan, dan buku penyusutan dikelola terpusat pada Dashboard Admin Web.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CupertinoLabel
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Vehicle Specific Details Section
            if (asset.type == AssetType.KENDARAAN) {
                item {
                    val taxStatus = FormatUtils.getTaxStatus(asset.annualTaxDueDate)
                    val badgeColor = FormatUtils.getTaxStatusColor(taxStatus)
                    val badgeBg = when (taxStatus) {
                        TaxStatus.EXPIRED -> CupertinoRedLight
                        TaxStatus.CRITICAL -> CupertinoRedLight
                        TaxStatus.WARNING -> CupertinoOrangeLight
                        TaxStatus.SAFE -> CupertinoGreenLight
                    }

                    Text(
                        text = "INFORMASI KENDARAAN & SURAT-SURAT",
                        style = MaterialTheme.typography.labelSmall,
                        color = CupertinoSecondaryLabel,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    CupertinoCard {
                        // License plate banner
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
                                    text = asset.licensePlate ?: "-",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            CupertinoBadge(
                                text = FormatUtils.getTaxStatusBadgeText(asset.annualTaxDueDate),
                                backgroundColor = badgeBg,
                                textColor = badgeColor
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = CupertinoSeparator, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        DetailInfoRow("Nomor Mesin", asset.engineNumber ?: "-")
                        DetailInfoRow("Nomor Rangka (VIN)", asset.chassisNumber ?: "-")
                        DetailInfoRow("Nomor BPKB", asset.bpkbNumber ?: "-")
                        DetailInfoRow("Nomor STNK", asset.stnkNumber ?: "-")
                        DetailInfoRow("Jatuh Tempo Pajak Tahunan", FormatUtils.formatDate(asset.annualTaxDueDate))
                        DetailInfoRow("Masa Berlaku STNK 5 Tahun", FormatUtils.formatDate(asset.fiveYearPlateDueDate))
                        if (asset.annualTaxAmount != null) {
                            DetailInfoRow("Estimasi Nominal PKB", FormatUtils.formatRupiah(asset.annualTaxAmount))
                        }
                        if (asset.lastServiceDate != null) {
                            DetailInfoRow("Servis Terakhir", FormatUtils.formatDate(asset.lastServiceDate))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Vehicle Actions: Renew tax & Send push notification
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.renewTaxForVehicle(asset) },
                                colors = ButtonDefaults.buttonColors(containerColor = CupertinoGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Perpanjang (+1 Thn)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    NotificationHelper.sendTaxReminderNotification(
                                        context = context,
                                        notificationId = asset.id.toInt(),
                                        title = "Peringatan Pajak: ${asset.licensePlate}",
                                        message = "Pajak tahunan ${asset.name} jatuh tempo ${FormatUtils.formatDate(asset.annualTaxDueDate)}.",
                                        assetId = asset.id
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CupertinoFill),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = CupertinoPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kirim Notif", style = MaterialTheme.typography.labelSmall, color = CupertinoPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Real Estate (Land / Building) Details
            if (asset.type == AssetType.TANAH || asset.type == AssetType.BANGUNAN) {
                item {
                    Text(
                        text = "LEGALITAS & PROPERTI",
                        style = MaterialTheme.typography.labelSmall,
                        color = CupertinoSecondaryLabel,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    CupertinoCard {
                        DetailInfoRow("Jenis Sertifikat", asset.certificateType ?: "-")
                        DetailInfoRow("Nomor Sertifikat", asset.certificateNumber ?: "-")
                        DetailInfoRow("NOP PBB", asset.pbbNop ?: "-")
                        if (asset.landAreaM2 != null) {
                            DetailInfoRow("Luas Tanah", "${asset.landAreaM2} m²")
                        }
                        if (asset.buildingAreaM2 != null) {
                            DetailInfoRow("Luas Bangunan", "${asset.buildingAreaM2} m²")
                        }
                        if (asset.numberOfFloors != null) {
                            DetailInfoRow("Jumlah Lantai", "${asset.numberOfFloors} Lantai")
                        }
                        if (!asset.pbgNumber.isNullOrBlank()) {
                            DetailInfoRow("No. IMB / PBG", asset.pbgNumber)
                        }
                        if (asset.latitude != null && asset.longitude != null) {
                            DetailInfoRow("Koordinat", "%.6f, %.6f".format(asset.latitude, asset.longitude))
                            DetailInfoRow(
                                "Akurasi Lokasi",
                                asset.locationAccuracyM?.let { "±${it.toInt()} m (GPS)" } ?: "Titik manual"
                            )
                            TextButton(
                                onClick = {
                                    // geo: membuka aplikasi peta apa pun; label muncul sebagai nama pin.
                                    val label = android.net.Uri.encode(asset.name)
                                    val uri = android.net.Uri.parse("geo:${asset.latitude},${asset.longitude}?q=${asset.latitude},${asset.longitude}($label)")
                                    runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri)) }
                                },
                                modifier = Modifier.testTag("btn_open_in_maps")
                            ) { Text("Buka di Google Maps", fontWeight = FontWeight.Bold) }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Inventory Details
            if (asset.type == AssetType.INVENTARIS) {
                item {
                    Text(
                        text = "SPESIFIKASI INVENTARIS",
                        style = MaterialTheme.typography.labelSmall,
                        color = CupertinoSecondaryLabel,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    CupertinoCard {
                        DetailInfoRow("Merk & Model", asset.brandModel ?: "-")
                        DetailInfoRow("Serial Number", asset.serialNumber ?: "-")
                        DetailInfoRow("Departemen", asset.department ?: "-")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // General Info (Location, PIC, Notes)
            item {
                Text(
                    text = "LOKASI & PENANGGUNG JAWAB",
                    style = MaterialTheme.typography.labelSmall,
                    color = CupertinoSecondaryLabel,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                CupertinoCard {
                    DetailInfoRow("Penanggung Jawab (PIC)", asset.pic)
                    DetailInfoRow("Lokasi Fisik", asset.location)
                    if (asset.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Catatan:",
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

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Hapus Aset?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Aset '${asset.name}' akan dihapus secara permanen dari sistem. Tindakan ini tidak dapat dibatalkan.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteAsset(asset)
                    }
                ) {
                    Text("Hapus", color = CupertinoRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = CupertinoSecondaryLabel
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
