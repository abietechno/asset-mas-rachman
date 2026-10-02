package com.example.ui.dialogs

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import com.example.ui.AssetViewModel
import com.example.ui.components.CupertinoCard
import com.example.ui.theme.*
import com.example.util.FormatUtils
import java.util.Calendar

/** Pilihan kategori di form; [id] null = jenis bawaan saat belum ada data dari server. */
private data class CategoryOption(val id: Long?, val name: String, val kind: AssetType, val usefulLifeYears: Int?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAssetDialog(
    editingAsset: AssetEntity?,
    viewModel: AssetViewModel,
    onDismiss: () -> Unit
) {
    val isEditMode = editingAsset != null && editingAsset.id != 0L

    val serverCategories by viewModel.categories.collectAsStateWithLifecycle()
    // Tanpa data server (mode offline) form memakai 4 jenis bawaan; kategori kustom dari dashboard muncul setelah sync.
    val options = if (serverCategories.isEmpty()) {
        AssetType.values().map { CategoryOption(null, it.displayName, it, null) }
    } else {
        serverCategories.map { CategoryOption(it.id, it.name, it.kind, it.usefulLifeYears) }
    }

    var type by remember { mutableStateOf(editingAsset?.type ?: AssetType.KENDARAAN) }
    var categoryId by remember {
        mutableStateOf(editingAsset?.categoryId ?: serverCategories.firstOrNull { it.kind == type && it.isSystem }?.id)
    }
    var name by remember { mutableStateOf(editingAsset?.name ?: "") }

    // Pin lokasi (hanya Tanah / Rumah & Bangunan)
    var latitude by remember { mutableStateOf(editingAsset?.latitude) }
    var longitude by remember { mutableStateOf(editingAsset?.longitude) }
    var locAccuracy by remember { mutableStateOf(editingAsset?.locationAccuracyM) }
    var locCapturedAt by remember { mutableStateOf(editingAsset?.locationCapturedAt) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var code by remember {
        mutableStateOf(
            editingAsset?.code ?: "AST-${type.name.take(3)}-${(System.currentTimeMillis() % 10000)}"
        )
    }
    var acquisitionCostStr by remember {
        mutableStateOf(
            if (editingAsset != null && editingAsset.acquisitionCost > 0) editingAsset.acquisitionCost.toLong().toString() else ""
        )
    }
    var usefulLifeStr by remember {
        mutableStateOf(
            if (editingAsset != null) editingAsset.usefulLifeYears.toString()
            else if (type == AssetType.TANAH) "0" else if (type == AssetType.BANGUNAN) "20" else if (type == AssetType.KENDARAAN) "8" else "4"
        )
    }
    var salvageValueStr by remember {
        mutableStateOf(
            if (editingAsset != null && editingAsset.salvageValue > 0) editingAsset.salvageValue.toLong().toString() else "0"
        )
    }
    var location by remember { mutableStateOf(editingAsset?.location ?: "Kantor Pusat Jakarta") }
    var pic by remember { mutableStateOf(editingAsset?.pic ?: "General Affairs") }
    var status by remember { mutableStateOf(editingAsset?.status ?: AssetStatus.DIGUNAKAN) }
    var condition by remember { mutableStateOf(editingAsset?.condition ?: AssetCondition.BAIK) }
    var notes by remember { mutableStateOf(editingAsset?.notes ?: "") }

    // Vehicle fields
    var vehicleType by remember { mutableStateOf(editingAsset?.vehicleType ?: VehicleType.MOBIL) }
    var licensePlate by remember { mutableStateOf(editingAsset?.licensePlate ?: "") }
    var engineNumber by remember { mutableStateOf(editingAsset?.engineNumber ?: "") }
    var chassisNumber by remember { mutableStateOf(editingAsset?.chassisNumber ?: "") }
    var bpkbNumber by remember { mutableStateOf(editingAsset?.bpkbNumber ?: "") }
    var stnkNumber by remember { mutableStateOf(editingAsset?.stnkNumber ?: "") }
    var taxMonthsOffset by remember { mutableStateOf(12) } // default 12 months ahead

    // Property fields
    var certType by remember { mutableStateOf(editingAsset?.certificateType ?: "SHM") }
    var certNumber by remember { mutableStateOf(editingAsset?.certificateNumber ?: "") }
    var pbbNop by remember { mutableStateOf(editingAsset?.pbbNop ?: "") }
    var landAreaStr by remember { mutableStateOf(editingAsset?.landAreaM2?.toString() ?: "") }
    var buildingAreaStr by remember { mutableStateOf(editingAsset?.buildingAreaM2?.toString() ?: "") }

    // Inventory fields
    var brandModel by remember { mutableStateOf(editingAsset?.brandModel ?: "") }
    var serialNumber by remember { mutableStateOf(editingAsset?.serialNumber ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("add_edit_asset_dialog"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Cupertino Modal Top Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.5.dp, CupertinoSeparator)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Batal", color = CupertinoSecondaryLabel, style = MaterialTheme.typography.bodyLarge)
                        }

                        Text(
                            text = if (isEditMode) "Edit Data Aset" else "Tambah Aset Baru",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(
                            onClick = {
                                if (name.isBlank()) {
                                    errorMessage = "Nama aset wajib diisi."
                                    return@TextButton
                                }
                                val cost = acquisitionCostStr.toDoubleOrNull() ?: 0.0
                                val usefulYears = if (type == AssetType.TANAH) 0 else (usefulLifeStr.toIntOrNull() ?: 5)
                                val salvage = salvageValueStr.toDoubleOrNull() ?: 0.0

                                val cal = Calendar.getInstance()
                                val annualTaxDue = if (type == AssetType.KENDARAAN) {
                                    editingAsset?.annualTaxDueDate ?: Calendar.getInstance().apply {
                                        add(Calendar.MONTH, taxMonthsOffset)
                                    }.timeInMillis
                                } else null

                                val fiveYearPlateDue = if (type == AssetType.KENDARAAN) {
                                    editingAsset?.fiveYearPlateDueDate ?: Calendar.getInstance().apply {
                                        add(Calendar.YEAR, 5)
                                    }.timeInMillis
                                } else null

                                val assetToSave = AssetEntity(
                                    id = editingAsset?.id ?: 0L,
                                    name = name.trim(),
                                    code = code.trim(),
                                    type = type,
                                    categoryId = categoryId,
                                    latitude = if (type == AssetType.TANAH || type == AssetType.BANGUNAN) latitude else null,
                                    longitude = if (type == AssetType.TANAH || type == AssetType.BANGUNAN) longitude else null,
                                    locationAccuracyM = if (type == AssetType.TANAH || type == AssetType.BANGUNAN) locAccuracy else null,
                                    locationCapturedAt = if (type == AssetType.TANAH || type == AssetType.BANGUNAN) locCapturedAt else null,
                                    acquisitionCost = cost,
                                    acquisitionDate = editingAsset?.acquisitionDate ?: System.currentTimeMillis(),
                                    usefulLifeYears = usefulYears,
                                    salvageValue = salvage,
                                    location = location.trim(),
                                    pic = pic.trim(),
                                    status = status,
                                    condition = condition,
                                    notes = notes.trim(),
                                    vehicleType = if (type == AssetType.KENDARAAN) vehicleType else null,
                                    licensePlate = if (type == AssetType.KENDARAAN) licensePlate.trim().uppercase() else null,
                                    engineNumber = if (type == AssetType.KENDARAAN) engineNumber.trim().uppercase() else null,
                                    chassisNumber = if (type == AssetType.KENDARAAN) chassisNumber.trim().uppercase() else null,
                                    bpkbNumber = if (type == AssetType.KENDARAAN) bpkbNumber.trim() else null,
                                    stnkNumber = if (type == AssetType.KENDARAAN) stnkNumber.trim() else null,
                                    annualTaxDueDate = annualTaxDue,
                                    fiveYearPlateDueDate = fiveYearPlateDue,
                                    certificateType = if (type == AssetType.TANAH || type == AssetType.BANGUNAN) certType.trim() else null,
                                    certificateNumber = if (type == AssetType.TANAH || type == AssetType.BANGUNAN) certNumber.trim() else null,
                                    pbbNop = if (type == AssetType.TANAH || type == AssetType.BANGUNAN) pbbNop.trim() else null,
                                    landAreaM2 = landAreaStr.toDoubleOrNull(),
                                    buildingAreaM2 = buildingAreaStr.toDoubleOrNull(),
                                    brandModel = if (type == AssetType.INVENTARIS) brandModel.trim() else null,
                                    serialNumber = if (type == AssetType.INVENTARIS) serialNumber.trim() else null
                                )

                                viewModel.saveAsset(assetToSave)
                            }
                        ) {
                            Text("Simpan", color = CupertinoPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    if (errorMessage != null) {
                        item {
                            Surface(
                                color = CupertinoRedLight,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = CupertinoRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    // Kategori Selector
                    item {
                        Text(
                            text = "KATEGORI ASET",
                            style = MaterialTheme.typography.labelSmall,
                            color = CupertinoSecondaryLabel,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            options.forEach { o ->
                                val isSelected = if (o.id != null) o.id == categoryId else o.kind == type
                                val bg = if (isSelected) CupertinoPrimary else CupertinoFill
                                val textCol = if (isSelected) Color.White else CupertinoLabel

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(bg)
                                        .clickable {
                                            type = o.kind
                                            categoryId = o.id
                                            if (!isEditMode) {
                                                code = "AST-${o.kind.name.take(3)}-${(System.currentTimeMillis() % 10000)}"
                                                usefulLifeStr = (o.usefulLifeYears ?: when (o.kind) {
                                                    AssetType.TANAH -> 0
                                                    AssetType.BANGUNAN -> 20
                                                    AssetType.KENDARAAN -> 8
                                                    AssetType.INVENTARIS -> 4
                                                }).toString()
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = o.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = textCol
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Informasi Utama
                    item {
                        Text(
                            text = "INFORMASI UTAMA",
                            style = MaterialTheme.typography.labelSmall,
                            color = CupertinoSecondaryLabel,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        CupertinoCard {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nama Aset *") },
                                placeholder = { Text("contoh: Toyota Innova Zenix / Gudang Cikarang") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_asset_name")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it },
                                label = { Text("Kode Tag Aset") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = { Text("Lokasi Fisik / Gedung") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = pic,
                                onValueChange = { pic = it },
                                label = { Text("Penanggung Jawab (PIC)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Catatan Integrasi Web Admin
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CupertinoPrimary.copy(alpha = 0.08f),
                            border = BorderStroke(0.5.dp, CupertinoPrimary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = CupertinoPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Parameter keuangan, harga perolehan, dan metode depresiasi aset tersinkronisasi otomatis dengan Dashboard Web Admin.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CupertinoLabel
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Bidang Khusus KENDARAAN (Mobil / Motor)
                    if (type == AssetType.KENDARAAN) {
                        item {
                            Text(
                                text = "DETAIL KENDARAAN & PAJAK (MOBIL / MOTOR)",
                                style = MaterialTheme.typography.labelSmall,
                                color = CupertinoSecondaryLabel,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            CupertinoCard {
                                // Jenis Kendaraan
                                Text("Jenis Kendaraan", style = MaterialTheme.typography.labelSmall, color = CupertinoSecondaryLabel)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(VehicleType.MOBIL, VehicleType.MOTOR, VehicleType.TRUK).forEach { vt ->
                                        val isSel = vt == vehicleType
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { vehicleType = vt },
                                            label = { Text(vt.displayName) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = licensePlate,
                                    onValueChange = { licensePlate = it.uppercase() },
                                    label = { Text("Nomor Plat Polisi *") },
                                    placeholder = { Text("contoh: B 1234 ABC") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_license_plate")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = engineNumber,
                                    onValueChange = { engineNumber = it.uppercase() },
                                    label = { Text("Nomor Mesin") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = chassisNumber,
                                    onValueChange = { chassisNumber = it.uppercase() },
                                    label = { Text("Nomor Rangka (VIN)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = stnkNumber,
                                        onValueChange = { stnkNumber = it },
                                        label = { Text("Nomor STNK") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = bpkbNumber,
                                        onValueChange = { bpkbNumber = it },
                                        label = { Text("Nomor BPKB") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Jatuh Tempo Pajak Tahunan:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CupertinoSecondaryLabel
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(1 to "1 Bulan Lagi", 6 to "6 Bulan Lagi", 12 to "1 Tahun Lagi").forEach { (months, lbl) ->
                                        FilterChip(
                                            selected = taxMonthsOffset == months,
                                            onClick = { taxMonthsOffset = months },
                                            label = { Text(lbl) }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Bidang Khusus TANAH & BANGUNAN
                    if (type == AssetType.TANAH || type == AssetType.BANGUNAN) {
                        item {
                            Text(
                                text = "DETAIL LEGALITAS PROPERTI",
                                style = MaterialTheme.typography.labelSmall,
                                color = CupertinoSecondaryLabel,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            CupertinoCard {
                                OutlinedTextField(
                                    value = certType,
                                    onValueChange = { certType = it },
                                    label = { Text("Tipe Sertifikat (SHM / HGB / Girik)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = certNumber,
                                    onValueChange = { certNumber = it },
                                    label = { Text("Nomor Sertifikat") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = pbbNop,
                                    onValueChange = { pbbNop = it },
                                    label = { Text("Nomor Objek Pajak (NOP PBB)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = landAreaStr,
                                        onValueChange = { landAreaStr = it.filter { c -> c.isDigit() || c == '.' } },
                                        label = { Text("Luas Tanah (m²)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (type == AssetType.BANGUNAN) {
                                        OutlinedTextField(
                                            value = buildingAreaStr,
                                            onValueChange = { buildingAreaStr = it.filter { c -> c.isDigit() || c == '.' } },
                                            label = { Text("Luas Bangunan (m²)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Pin lokasi di peta (TANAH & BANGUNAN)
                    if (type == AssetType.TANAH || type == AssetType.BANGUNAN) {
                        item {
                            Text(
                                text = "LOKASI DI PETA",
                                style = MaterialTheme.typography.labelSmall,
                                color = CupertinoSecondaryLabel,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            CupertinoCard {
                                val lat = latitude
                                val lng = longitude
                                if (lat != null && lng != null) {
                                    Text(
                                        text = "%.6f, %.6f".format(lat, lng),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = locAccuracy?.let { "Akurasi GPS ±${it.toInt()} m" } ?: "Titik ditentukan manual",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CupertinoSecondaryLabel
                                    )
                                } else {
                                    Text(
                                        text = "Belum ada pin lokasi. Ambil dari GPS saat Anda berada di lokasi aset.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CupertinoSecondaryLabel
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { showLocationPicker = true },
                                        modifier = Modifier.weight(1f).testTag("btn_set_location")
                                    ) { Text(if (lat != null) "Ubah Pin" else "Ambil Lokasi") }
                                    if (lat != null) {
                                        TextButton(
                                            onClick = {
                                                latitude = null
                                                longitude = null
                                                locAccuracy = null
                                                locCapturedAt = null
                                            }
                                        ) { Text("Hapus Pin", color = CupertinoRed) }
                                    }
                                }
                            }

                            if (showLocationPicker) {
                                LocationPickerDialog(
                                    initialLat = latitude,
                                    initialLng = longitude,
                                    initialAccuracyM = locAccuracy,
                                    onDismiss = { showLocationPicker = false },
                                    onConfirm = { la, lo, acc, addr ->
                                        latitude = la
                                        longitude = lo
                                        locAccuracy = acc
                                        locCapturedAt = System.currentTimeMillis()
                                        // Alamat dari pencarian/peta mengisi kolom Lokasi bila masih kosong atau nilai awal bawaan.
                                        if (!addr.isNullOrBlank() && (location.isBlank() || location.startsWith("Kantor Pusat"))) location = addr
                                        showLocationPicker = false
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Bidang Khusus INVENTARIS
                    if (type == AssetType.INVENTARIS) {
                        item {
                            Text(
                                text = "DETAIL INVENTARIS KANTOR",
                                style = MaterialTheme.typography.labelSmall,
                                color = CupertinoSecondaryLabel,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            CupertinoCard {
                                OutlinedTextField(
                                    value = brandModel,
                                    onValueChange = { brandModel = it },
                                    label = { Text("Merk & Model Perangkat") },
                                    placeholder = { Text("contoh: MacBook Pro M3 / Server Dell") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = serialNumber,
                                    onValueChange = { serialNumber = it },
                                    label = { Text("Serial Number / Barcode") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Status & Kondisi
                    item {
                        Text(
                            text = "STATUS & KONDISI",
                            style = MaterialTheme.typography.labelSmall,
                            color = CupertinoSecondaryLabel,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        CupertinoCard {
                            Text("Status Operasional", style = MaterialTheme.typography.labelSmall, color = CupertinoSecondaryLabel)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AssetStatus.values().forEach { st ->
                                    FilterChip(
                                        selected = status == st,
                                        onClick = { status = st },
                                        label = { Text(st.displayName) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Kondisi Fisik", style = MaterialTheme.typography.labelSmall, color = CupertinoSecondaryLabel)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AssetCondition.values().forEach { cond ->
                                    FilterChip(
                                        selected = condition == cond,
                                        onClick = { condition = cond },
                                        label = { Text(cond.displayName) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Catatan Tambahan") },
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
