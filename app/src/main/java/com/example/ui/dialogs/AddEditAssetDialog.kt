package com.example.ui.dialogs

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
import androidx.compose.material.icons.outlined.Place
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.model.AssetCondition
import com.example.model.AssetEntity
import com.example.model.AssetStatus
import com.example.model.AssetType
import com.example.model.VehicleType
import com.example.ui.AssetViewModel
import com.example.data.PhotoEntity
import com.example.data.PhotoState
import com.example.ui.components.AssetPhotoPicker
import com.example.ui.components.CupertinoCard
import com.example.ui.components.CupertinoDateField
import com.example.ui.components.CupertinoTextField
import com.example.ui.theme.*
import com.example.util.PhotoStore
import com.example.util.FormatUtils
import java.util.Calendar

/** Pilihan kategori di form; [id] null = jenis bawaan saat belum ada data dari server. */
private data class CategoryOption(val id: Long?, val name: String, val kind: AssetType, val usefulLifeYears: Int?)

private fun defaultLife(kind: AssetType) = when (kind) {
    AssetType.TANAH -> 0
    AssetType.BANGUNAN -> 20
    AssetType.KENDARAAN -> 8
    AssetType.INVENTARIS -> 4
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

/** Isian angka (digit saja) dengan pratinjau rupiah di bawahnya. */
@Composable
private fun MoneyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    CupertinoTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit)) },
        label = label,
        placeholder = "0",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        supportingText = value.toDoubleOrNull()?.takeIf { it > 0 }?.let { FormatUtils.formatRupiah(it) },
        modifier = (testTag?.let { modifier.testTag(it) } ?: modifier)
    )
}

@Composable
fun AddEditAssetDialog(
    editingAsset: AssetEntity?,
    viewModel: AssetViewModel,
    onDismiss: () -> Unit
) {
    val isEditMode = editingAsset != null && editingAsset.id != 0L
    val serverCategories by viewModel.categories.collectAsStateWithLifecycle()

    // Tanpa data server (mode offline) form memakai 4 jenis bawaan; kategori kustom muncul setelah sync.
    val options = if (serverCategories.isEmpty()) {
        AssetType.values().map { CategoryOption(null, it.displayName, it, null) }
    } else {
        serverCategories.map { CategoryOption(it.id, it.name, it.kind, it.usefulLifeYears) }
    }

    var type by remember { mutableStateOf(editingAsset?.type ?: AssetType.KENDARAAN) }
    var categoryId by remember {
        mutableStateOf(editingAsset?.categoryId ?: serverCategories.firstOrNull { it.kind == type && it.isSystem }?.id)
    }

    // Informasi utama
    var name by remember { mutableStateOf(editingAsset?.name ?: "") }
    var code by remember {
        mutableStateOf(editingAsset?.code ?: "AST-${type.name.take(3)}-${System.currentTimeMillis() % 10000}")
    }
    var costStr by remember {
        mutableStateOf(editingAsset?.acquisitionCost?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var acquisitionDate by remember { mutableStateOf(editingAsset?.acquisitionDate ?: System.currentTimeMillis()) }
    var usefulLifeStr by remember {
        mutableStateOf((editingAsset?.usefulLifeYears ?: defaultLife(type)).toString())
    }
    var salvageStr by remember {
        mutableStateOf(editingAsset?.salvageValue?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var location by remember { mutableStateOf(editingAsset?.location ?: "") }
    var pic by remember { mutableStateOf(editingAsset?.pic ?: "") }
    var notes by remember { mutableStateOf(editingAsset?.notes ?: "") }
    var status by remember { mutableStateOf(editingAsset?.status ?: AssetStatus.DIGUNAKAN) }
    var condition by remember { mutableStateOf(editingAsset?.condition ?: AssetCondition.BAIK) }

    // Kendaraan
    var vehicleType by remember { mutableStateOf(editingAsset?.vehicleType ?: VehicleType.MOBIL) }
    var licensePlate by remember { mutableStateOf(editingAsset?.licensePlate ?: "") }
    var brand by remember { mutableStateOf(editingAsset?.brand ?: "") }
    var vehicleModel by remember { mutableStateOf(editingAsset?.vehicleModel ?: "") }
    var yearStr by remember { mutableStateOf(editingAsset?.yearManufacture?.toString() ?: "") }
    var engineNumber by remember { mutableStateOf(editingAsset?.engineNumber ?: "") }
    var chassisNumber by remember { mutableStateOf(editingAsset?.chassisNumber ?: "") }
    var bpkbNumber by remember { mutableStateOf(editingAsset?.bpkbNumber ?: "") }
    var stnkNumber by remember { mutableStateOf(editingAsset?.stnkNumber ?: "") }
    var annualTaxDue by remember {
        mutableStateOf(editingAsset?.annualTaxDueDate ?: Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.timeInMillis)
    }
    var platePlateDue by remember { mutableStateOf(editingAsset?.fiveYearPlateDueDate) }
    var lastServiceDate by remember { mutableStateOf(editingAsset?.lastServiceDate) }

    // Tanah / Bangunan
    var certType by remember { mutableStateOf(editingAsset?.certificateType ?: "SHM") }
    var certNumber by remember { mutableStateOf(editingAsset?.certificateNumber ?: "") }
    var pbbNop by remember { mutableStateOf(editingAsset?.pbbNop ?: "") }
    var landAreaStr by remember { mutableStateOf(editingAsset?.landAreaM2?.toString() ?: "") }
    var buildingAreaStr by remember { mutableStateOf(editingAsset?.buildingAreaM2?.toString() ?: "") }

    // Pin lokasi (Tanah / Bangunan)
    var latitude by remember { mutableStateOf(editingAsset?.latitude) }
    var longitude by remember { mutableStateOf(editingAsset?.longitude) }
    var locAccuracy by remember { mutableStateOf(editingAsset?.locationAccuracyM) }
    var locCapturedAt by remember { mutableStateOf(editingAsset?.locationCapturedAt) }
    var showLocationPicker by remember { mutableStateOf(false) }

    // Inventaris
    var brandModel by remember { mutableStateOf(editingAsset?.brandModel ?: "") }
    var serialNumber by remember { mutableStateOf(editingAsset?.serialNumber ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Aset yang sudah tersimpan: foto langsung masuk daftar. Aset baru: ditahan dulu karena belum punya id.
    val savedPhotos by (if (isEditMode) viewModel.photosFor(editingAsset!!.id) else null)
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptyList<PhotoEntity>()) }
    var draftPhotoPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()

    val shownPhotos = if (isEditMode) {
        savedPhotos
    } else {
        draftPhotoPaths.mapIndexed { i, path ->
            PhotoEntity(id = -(i + 1).toLong(), assetLocalId = 0, localPath = path, state = PhotoState.PENDING_UPLOAD)
        }
    }

    val isVehicle = type == AssetType.KENDARAAN
    val isProperty = type == AssetType.TANAH || type == AssetType.BANGUNAN

    fun save() {
        if (name.isBlank()) {
            errorMessage = "Nama aset wajib diisi."
            return
        }
        if (isVehicle && licensePlate.isBlank()) {
            errorMessage = "Plat nomor wajib diisi untuk kendaraan."
            return
        }

        viewModel.saveAsset(
            newPhotoPaths = draftPhotoPaths,
            asset = AssetEntity(
                id = editingAsset?.id ?: 0L,
                name = name.trim(),
                code = code.trim(),
                type = type,
                categoryId = categoryId,
                acquisitionCost = costStr.toDoubleOrNull() ?: 0.0,
                acquisitionDate = acquisitionDate,
                usefulLifeYears = if (type == AssetType.TANAH) 0 else (usefulLifeStr.toIntOrNull() ?: defaultLife(type)),
                salvageValue = salvageStr.toDoubleOrNull() ?: 0.0,
                location = location.trim(),
                pic = pic.trim(),
                status = status,
                condition = condition,
                notes = notes.trim(),
                // Kendaraan
                vehicleType = if (isVehicle) vehicleType else null,
                brand = if (isVehicle) brand.trim().ifBlank { null } else null,
                vehicleModel = if (isVehicle) vehicleModel.trim().ifBlank { null } else null,
                yearManufacture = if (isVehicle) yearStr.toIntOrNull() else null,
                licensePlate = if (isVehicle) licensePlate.trim().uppercase() else null,
                engineNumber = if (isVehicle) engineNumber.trim().uppercase() else null,
                chassisNumber = if (isVehicle) chassisNumber.trim().uppercase() else null,
                bpkbNumber = if (isVehicle) bpkbNumber.trim().ifBlank { null } else null,
                stnkNumber = if (isVehicle) stnkNumber.trim().ifBlank { null } else null,
                annualTaxDueDate = if (isVehicle) annualTaxDue else null,
                fiveYearPlateDueDate = if (isVehicle) platePlateDue else null,
                lastServiceDate = if (isVehicle) lastServiceDate else null,
                // Tanah / Bangunan
                certificateType = if (isProperty) certType.trim().ifBlank { null } else null,
                certificateNumber = if (isProperty) certNumber.trim().ifBlank { null } else null,
                pbbNop = if (isProperty) pbbNop.trim().ifBlank { null } else null,
                landAreaM2 = if (isProperty) landAreaStr.toDoubleOrNull() else null,
                buildingAreaM2 = if (type == AssetType.BANGUNAN) buildingAreaStr.toDoubleOrNull() else null,
                latitude = if (isProperty) latitude else null,
                longitude = if (isProperty) longitude else null,
                locationAccuracyM = if (isProperty) locAccuracy else null,
                locationCapturedAt = if (isProperty) locCapturedAt else null,
                // Inventaris (field Merk & Model perangkat memakai kolom brandModel)
                brandModel = if (type == AssetType.INVENTARIS) brandModel.trim().ifBlank { null } else editingAsset?.brandModel,
                serialNumber = if (type == AssetType.INVENTARIS) serialNumber.trim().ifBlank { null } else null
            )
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("add_edit_asset_dialog"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header ringkas
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(0.5.dp, CupertinoSeparator)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Batal", color = CupertinoSecondaryLabel)
                        }
                        Text(
                            text = if (isEditMode) "Edit Aset" else "Tambah Aset",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { save() }, modifier = Modifier.testTag("save_asset_button")) {
                            Text("Simpan", color = CupertinoPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    errorMessage?.let { msg ->
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CupertinoRed.copy(alpha = 0.10f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CupertinoRed,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // ── Kategori ──
                    item {
                        SectionTitle("KATEGORI ASET")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            options.forEach { o ->
                                val isSelected = if (o.id != null) o.id == categoryId else o.kind == type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) CupertinoPrimary else CupertinoFill)
                                        .clickable {
                                            type = o.kind
                                            categoryId = o.id
                                            if (!isEditMode) {
                                                code = "AST-${o.kind.name.take(3)}-${System.currentTimeMillis() % 10000}"
                                                usefulLifeStr = (o.usefulLifeYears ?: defaultLife(o.kind)).toString()
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = o.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else CupertinoLabel
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // ── Informasi utama ──
                    item {
                        SectionTitle("INFORMASI UTAMA")
                        CupertinoCard {
                            CupertinoTextField(
                                value = name,
                                onValueChange = { name = it; errorMessage = null },
                                label = "Nama Aset *",
                                placeholder = "contoh: Toyota Innova Zenix",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_asset_name")
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CupertinoTextField(
                                    value = code,
                                    onValueChange = { code = it },
                                    label = "Kode Tag",
                                    modifier = Modifier.weight(1f)
                                )
                                MoneyField(
                                    label = "Nilai Perolehan",
                                    value = costStr,
                                    onValueChange = { costStr = it },
                                    modifier = Modifier.weight(1f),
                                    testTag = "input_acquisition_cost"
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            CupertinoDateField(
                                label = "Tanggal Perolehan",
                                value = acquisitionDate,
                                onChange = { acquisitionDate = it ?: System.currentTimeMillis() },
                                clearable = false,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (type != AssetType.TANAH) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CupertinoTextField(
                                        value = usefulLifeStr,
                                        onValueChange = { usefulLifeStr = it.filter(Char::isDigit).take(3) },
                                        label = "Masa Manfaat (thn)",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    MoneyField(
                                        label = "Nilai Residu",
                                        value = salvageStr,
                                        onValueChange = { salvageStr = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CupertinoTextField(
                                    value = location,
                                    onValueChange = { location = it },
                                    label = "Lokasi Fisik",
                                    modifier = Modifier.weight(1f)
                                )
                                CupertinoTextField(
                                    value = pic,
                                    onValueChange = { pic = it },
                                    label = "Penanggung Jawab",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // ── Kendaraan ──
                    if (isVehicle) {
                        item {
                            SectionTitle("DETAIL KENDARAAN & PAJAK")
                            CupertinoCard {
                                Text(
                                    text = "Jenis Kendaraan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CupertinoSecondaryLabel,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    VehicleType.values().forEach { vt ->
                                        FilterChip(
                                            selected = vt == vehicleType,
                                            onClick = { vehicleType = vt },
                                            label = { Text(vt.displayName, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                CupertinoTextField(
                                    value = licensePlate,
                                    onValueChange = { licensePlate = it.uppercase(); errorMessage = null },
                                    label = "Plat Nomor *",
                                    placeholder = "B 1234 ABC",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_license_plate")
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CupertinoTextField(
                                        value = brand,
                                        onValueChange = { brand = it },
                                        label = "Merk",
                                        placeholder = "Toyota",
                                        modifier = Modifier.weight(1f)
                                    )
                                    CupertinoTextField(
                                        value = vehicleModel,
                                        onValueChange = { vehicleModel = it },
                                        label = "Model",
                                        placeholder = "Innova Zenix",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CupertinoTextField(
                                        value = yearStr,
                                        onValueChange = { yearStr = it.filter(Char::isDigit).take(4) },
                                        label = "Tahun",
                                        placeholder = "2024",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    CupertinoTextField(
                                        value = chassisNumber,
                                        onValueChange = { chassisNumber = it.uppercase() },
                                        label = "No. Rangka (VIN)",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CupertinoTextField(
                                        value = engineNumber,
                                        onValueChange = { engineNumber = it.uppercase() },
                                        label = "No. Mesin",
                                        modifier = Modifier.weight(1f)
                                    )
                                    CupertinoTextField(
                                        value = bpkbNumber,
                                        onValueChange = { bpkbNumber = it },
                                        label = "No. BPKB",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                CupertinoTextField(
                                    value = stnkNumber,
                                    onValueChange = { stnkNumber = it },
                                    label = "No. STNK",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CupertinoDateField(
                                        label = "Jatuh Tempo Pajak",
                                        value = annualTaxDue,
                                        onChange = { annualTaxDue = it ?: annualTaxDue },
                                        clearable = false,
                                        modifier = Modifier.weight(1f)
                                    )
                                    CupertinoDateField(
                                        label = "Plat 5 Tahunan",
                                        value = platePlateDue,
                                        onChange = { platePlateDue = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                CupertinoDateField(
                                    label = "Servis Terakhir (dasar pengingat servis)",
                                    value = lastServiceDate,
                                    onChange = { lastServiceDate = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // ── Tanah / Bangunan ──
                    if (isProperty) {
                        item {
                            SectionTitle("LEGALITAS PROPERTI")
                            CupertinoCard {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CupertinoTextField(
                                        value = certType,
                                        onValueChange = { certType = it },
                                        label = "Tipe Sertifikat",
                                        placeholder = "SHM / HGB",
                                        modifier = Modifier.weight(1f)
                                    )
                                    CupertinoTextField(
                                        value = certNumber,
                                        onValueChange = { certNumber = it },
                                        label = "No. Sertifikat",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                CupertinoTextField(
                                    value = pbbNop,
                                    onValueChange = { pbbNop = it },
                                    label = "NOP PBB",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CupertinoTextField(
                                        value = landAreaStr,
                                        onValueChange = { landAreaStr = it.filter { c -> c.isDigit() || c == '.' } },
                                        label = "Luas Tanah (m²)",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (type == AssetType.BANGUNAN) {
                                        CupertinoTextField(
                                            value = buildingAreaStr,
                                            onValueChange = { buildingAreaStr = it.filter { c -> c.isDigit() || c == '.' } },
                                            label = "Luas Bangunan (m²)",
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Text(
                                    text = "Jumlah lantai, No. IMB/PBG, dan alamat lengkap diisi dari dashboard web.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CupertinoSecondaryLabel,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // ── Pin lokasi ──
                        item {
                            SectionTitle("LOKASI DI PETA")
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
                                        text = "Belum ada pin lokasi. Cari alamat, ambil dari GPS, atau ketuk peta.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CupertinoSecondaryLabel
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { showLocationPicker = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_set_location")
                                    ) {
                                        Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (lat != null) "Ubah Pin" else "Pilih Lokasi")
                                    }
                                    if (lat != null) {
                                        TextButton(onClick = {
                                            latitude = null
                                            longitude = null
                                            locAccuracy = null
                                            locCapturedAt = null
                                        }) { Text("Hapus", color = CupertinoRed) }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // ── Inventaris ──
                    if (type == AssetType.INVENTARIS) {
                        item {
                            SectionTitle("DETAIL INVENTARIS")
                            CupertinoCard {
                                CupertinoTextField(
                                    value = brandModel,
                                    onValueChange = { brandModel = it },
                                    label = "Merk & Model Perangkat",
                                    placeholder = "MacBook Pro M3",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                CupertinoTextField(
                                    value = serialNumber,
                                    onValueChange = { serialNumber = it },
                                    label = "Nomor Seri / Barcode",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // ── Foto ──
                    item {
                        SectionTitle("FOTO ASET")
                        CupertinoCard {
                            AssetPhotoPicker(
                                photos = shownPhotos,
                                onPicked = { uri ->
                                    scope.launch {
                                        val path = viewModel.importPhoto(uri)
                                        if (path == null) {
                                            errorMessage = "Foto tidak bisa dibaca. Coba foto lain."
                                        } else if (isEditMode) {
                                            viewModel.addPhoto(editingAsset!!.id, path)
                                        } else {
                                            draftPhotoPaths = draftPhotoPaths + path
                                        }
                                    }
                                },
                                onRemove = { photo ->
                                    if (photo.id < 0) {
                                        // Foto aset baru yang belum tersimpan.
                                        PhotoStore.delete(photo.localPath)
                                        draftPhotoPaths = draftPhotoPaths.filterNot { it == photo.localPath }
                                    } else {
                                        viewModel.removePhoto(photo)
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // ── Status & kondisi ──
                    item {
                        SectionTitle("STATUS & KONDISI")
                        CupertinoCard {
                            Text(
                                text = "Status Operasional",
                                style = MaterialTheme.typography.labelSmall,
                                color = CupertinoSecondaryLabel,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AssetStatus.values().forEach { st ->
                                    FilterChip(
                                        selected = status == st,
                                        onClick = { status = st },
                                        label = { Text(st.displayName, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Kondisi Fisik",
                                style = MaterialTheme.typography.labelSmall,
                                color = CupertinoSecondaryLabel,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AssetCondition.values().forEach { cond ->
                                    FilterChip(
                                        selected = condition == cond,
                                        onClick = { condition = cond },
                                        label = { Text(cond.displayName, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            CupertinoTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = "Catatan Aset",
                                singleLine = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
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
                // Alamat dari pencarian/peta mengisi kolom Lokasi bila masih kosong.
                if (!addr.isNullOrBlank() && location.isBlank()) location = addr
                showLocationPicker = false
            }
        )
    }
}
