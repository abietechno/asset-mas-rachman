package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AssetEntity
import com.example.model.AssetType
import com.example.model.TaxStatus
import com.example.model.VehicleType
import com.example.ui.AssetViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.FormatUtils
import com.example.util.NotificationHelper

@Composable
fun VehiclesTaxScreen(
    viewModel: AssetViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vehicles by viewModel.filteredVehicles.collectAsStateWithLifecycle()
    val searchQuery by viewModel.vehicleSearchQuery.collectAsStateWithLifecycle()
    val selectedType by viewModel.vehicleTypeFilter.collectAsStateWithLifecycle()
    val selectedUrgency by viewModel.vehicleTaxUrgencyFilter.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .testTag("vehicles_tax_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            CupertinoTopBar(
                title = "Kendaraan & Pajak",
                subtitle = "Monitoring Masa Berlaku STNK & Legalitas",
                actions = {
                    IconButton(
                        onClick = { viewModel.openAddAsset(AssetType.KENDARAAN) },
                        modifier = Modifier.testTag("add_vehicle_button")
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
                                contentDescription = "Tambah Kendaraan",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        }

        // Notification Action Banner
        item {
            CupertinoCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("tax_notification_action_banner"),
                backgroundColor = CupertinoPrimary.copy(alpha = 0.08f),
                borderColor = CupertinoPrimary.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CupertinoPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sistem Notifikasi Pajak Otomatis",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CupertinoPrimary
                        )
                        Text(
                            text = "Kirim pengingat real-time ke status bar untuk kendaraan yang mendekati atau melewati jatuh tempo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CupertinoLabel
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { viewModel.triggerTaxRemindersNow() },
                        colors = ButtonDefaults.buttonColors(containerColor = CupertinoPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("send_tax_reminder_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Uji Kirim Notifikasi Sistem",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Search Bar
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                CupertinoSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.vehicleSearchQuery.value = it },
                    placeholder = "Cari Plat Nomor, No Mesin, No Rangka...",
                    modifier = Modifier.testTag("vehicle_search_bar")
                )
            }
        }

        // Urgency Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All
                FilterPill(
                    label = "Semua Kendaraan",
                    isSelected = selectedUrgency == null && selectedType == null,
                    onClick = {
                        viewModel.vehicleTaxUrgencyFilter.value = null
                        viewModel.vehicleTypeFilter.value = null
                    }
                )

                // Critical / Expired
                FilterPill(
                    label = "🔴 Kritis / Lewat",
                    isSelected = selectedUrgency == TaxStatus.CRITICAL || selectedUrgency == TaxStatus.EXPIRED,
                    onClick = { viewModel.vehicleTaxUrgencyFilter.value = TaxStatus.CRITICAL }
                )

                // Warning (<30 days)
                FilterPill(
                    label = "🟡 < 30 Hari",
                    isSelected = selectedUrgency == TaxStatus.WARNING,
                    onClick = { viewModel.vehicleTaxUrgencyFilter.value = TaxStatus.WARNING }
                )

                // Safe
                FilterPill(
                    label = "🟢 Aman",
                    isSelected = selectedUrgency == TaxStatus.SAFE,
                    onClick = { viewModel.vehicleTaxUrgencyFilter.value = TaxStatus.SAFE }
                )

                // Mobil
                FilterPill(
                    label = "Mobil",
                    isSelected = selectedType == VehicleType.MOBIL,
                    onClick = { viewModel.vehicleTypeFilter.value = VehicleType.MOBIL }
                )

                // Motor
                FilterPill(
                    label = "Motor",
                    isSelected = selectedType == VehicleType.MOTOR,
                    onClick = { viewModel.vehicleTypeFilter.value = VehicleType.MOTOR }
                )
            }
        }

        // Vehicle Item Cards
        if (vehicles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada kendaraan yang sesuai filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CupertinoSecondaryLabel
                    )
                }
            }
        } else {
            items(vehicles, key = { it.id }) { vehicle ->
                val taxStatus = FormatUtils.getTaxStatus(vehicle.annualTaxDueDate)
                val badgeColor = FormatUtils.getTaxStatusColor(taxStatus)
                val badgeBg = when (taxStatus) {
                    TaxStatus.EXPIRED -> CupertinoRedLight
                    TaxStatus.CRITICAL -> CupertinoRedLight
                    TaxStatus.WARNING -> CupertinoOrangeLight
                    TaxStatus.SAFE -> CupertinoGreenLight
                }

                CupertinoCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("vehicle_card_${vehicle.id}"),
                    onClick = { viewModel.openAssetDetail(vehicle) }
                ) {
                    // Header: Plat Polisi & Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Indonesian Plate Display
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1C1C1E))
                                .border(1.dp, Color(0xFFE5E5EA), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = vehicle.licensePlate ?: "TANPA PLAT",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.5.sp
                            )
                        }

                        CupertinoBadge(
                            text = FormatUtils.getTaxStatusBadgeText(vehicle.annualTaxDueDate),
                            backgroundColor = badgeBg,
                            textColor = badgeColor,
                            icon = if (taxStatus != TaxStatus.SAFE) Icons.Default.Warning else Icons.Default.CheckCircle
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = vehicle.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Pengguna: ${vehicle.pic} • Lokasi: ${vehicle.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CupertinoSecondaryLabel,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Vehicle Technical Details Inset Group
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CupertinoSystemBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Row 1: Nomor Rangka & Mesin
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Nomor Mesin",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CupertinoSecondaryLabel
                                    )
                                    Text(
                                        text = vehicle.engineNumber ?: "-",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Nomor Rangka (VIN)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CupertinoSecondaryLabel
                                    )
                                    Text(
                                        text = vehicle.chassisNumber ?: "-",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = CupertinoSeparator, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 2: BPKB & STNK
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "No. BPKB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CupertinoSecondaryLabel
                                    )
                                    Text(
                                        text = vehicle.bpkbNumber ?: "-",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "No. STNK",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CupertinoSecondaryLabel
                                    )
                                    Text(
                                        text = vehicle.stnkNumber ?: "-",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = CupertinoSeparator, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 3: Tanggal Pajak 1 Thn & 5 Thn
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pajak Tahunan",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CupertinoSecondaryLabel
                                    )
                                    Text(
                                        text = FormatUtils.formatDate(vehicle.annualTaxDueDate),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "STNK 5 Tahunan (Plat)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CupertinoSecondaryLabel
                                    )
                                    Text(
                                        text = FormatUtils.formatDate(vehicle.fiveYearPlateDueDate),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons for this Vehicle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Renew tax +1 year button
                        OutlinedButton(
                            onClick = { viewModel.renewTaxForVehicle(vehicle) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CupertinoGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = CupertinoGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Perpanjang (+1 Thn)",
                                style = MaterialTheme.typography.labelSmall,
                                color = CupertinoGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Trigger Individual Notification
                        Button(
                            onClick = {
                                val success = NotificationHelper.sendTaxReminderNotification(
                                    context = context,
                                    notificationId = vehicle.id.toInt(),
                                    title = "Peringatan Pajak: ${vehicle.licensePlate}",
                                    message = "Pajak ${vehicle.name} (${vehicle.licensePlate}) jatuh tempo ${FormatUtils.formatDate(vehicle.annualTaxDueDate)}.",
                                    assetId = vehicle.id
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CupertinoFill),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = CupertinoPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Kirim Notif",
                                style = MaterialTheme.typography.labelSmall,
                                color = CupertinoPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) CupertinoPrimary else CupertinoFill
    val textColor = if (isSelected) Color.White else CupertinoLabel

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}
