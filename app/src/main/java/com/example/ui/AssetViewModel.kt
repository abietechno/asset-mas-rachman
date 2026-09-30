package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AssetRepository
import com.example.model.AssetCondition
import com.example.model.AssetEntity
import com.example.model.AssetStatus
import com.example.model.AssetType
import com.example.model.TaxStatus
import com.example.model.VehicleType
import com.example.util.DepreciationCalculator
import com.example.util.FormatUtils
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CupertinoTab(val label: String) {
    DASHBOARD("Ringkasan"),
    ASSETS("Semua Aset"),
    VEHICLES("Kendaraan & Pajak"),
    REPORTS("Laporan Real-Time")
}

data class DashboardAnalytics(
    val totalAssetsCount: Int = 0,
    val activeAssetsCount: Int = 0,
    val standbyAssetsCount: Int = 0,
    val maintenanceAssetsCount: Int = 0,
    val totalVehiclesCount: Int = 0,
    val vehiclesWithTaxWarningCount: Int = 0,
    val urgentTaxVehicles: List<AssetEntity> = emptyList(),
    val categoryStats: Map<AssetType, CategoryStat> = emptyMap()
)

data class CategoryStat(
    val type: AssetType,
    val count: Int,
    val activeCount: Int = 0,
    val standbyCount: Int = 0,
    val maintenanceCount: Int = 0
)

enum class ThemeMode(val label: String) {
    SYSTEM("Ikuti Sistem Perangkat"),
    LIGHT("Mode Terang (Light)"),
    DARK("Mode Gelap (Dark)")
}

class AssetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AssetRepository

    // Theme Mode
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    // Authentication State
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserName = MutableStateFlow("Bambang Sudiro")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    private val _currentUserRole = MutableStateFlow("Asset Manager")
    val currentUserRole: StateFlow<String> = _currentUserRole.asStateFlow()

    fun login(identifier: String, role: String) {
        _currentUserName.value = identifier
        _currentUserRole.value = role
        _isLoggedIn.value = true
        viewModelScope.launch {
            _userMessage.emit("Selamat datang, $identifier! Anda masuk sebagai $role.")
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentTab.value = CupertinoTab.DASHBOARD
        viewModelScope.launch {
            _userMessage.emit("Anda telah keluar dari sistem.")
        }
    }

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AssetRepository(db.assetDao())
        viewModelScope.launch {
            repository.initializeSampleDataIfEmpty()
        }
        NotificationHelper.initNotificationChannel(application)
    }

    // Navigation state
    private val _currentTab = MutableStateFlow(CupertinoTab.DASHBOARD)
    val currentTab: StateFlow<CupertinoTab> = _currentTab.asStateFlow()

    // Filters for Assets list
    val assetSearchQuery = MutableStateFlow("")
    val assetCategoryFilter = MutableStateFlow<AssetType?>(null) // null = Semua
    val assetStatusFilter = MutableStateFlow<AssetStatus?>(null)

    // Filters for Vehicle list
    val vehicleSearchQuery = MutableStateFlow("")
    val vehicleTypeFilter = MutableStateFlow<VehicleType?>(null)
    val vehicleTaxUrgencyFilter = MutableStateFlow<TaxStatus?>(null) // null = Semua

    // Selected asset for Detail bottom sheet
    private val _selectedAsset = MutableStateFlow<AssetEntity?>(null)
    val selectedAsset: StateFlow<AssetEntity?> = _selectedAsset.asStateFlow()

    // Add / Edit asset modal state
    private val _isAddEditOpen = MutableStateFlow(false)
    val isAddEditOpen: StateFlow<Boolean> = _isAddEditOpen.asStateFlow()

    private val _editingAsset = MutableStateFlow<AssetEntity?>(null)
    val editingAsset: StateFlow<AssetEntity?> = _editingAsset.asStateFlow()

    // User message toast / banner feedback
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // All raw assets from Room
    val allAssets: StateFlow<List<AssetEntity>> = repository.allAssets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Derived Analytics for Dashboard
    val dashboardAnalytics: StateFlow<DashboardAnalytics> = allAssets
        .combine(MutableStateFlow(Unit)) { assets, _ ->
            computeDashboardAnalytics(assets)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardAnalytics()
        )

    // Filtered Assets list
    val filteredAssets: StateFlow<List<AssetEntity>> = combine(
        allAssets,
        assetSearchQuery,
        assetCategoryFilter,
        assetStatusFilter
    ) { assets, query, cat, status ->
        assets.filter { asset ->
            val matchQuery = query.isBlank() ||
                    asset.name.contains(query, ignoreCase = true) ||
                    asset.code.contains(query, ignoreCase = true) ||
                    asset.location.contains(query, ignoreCase = true) ||
                    asset.pic.contains(query, ignoreCase = true) ||
                    (asset.licensePlate?.contains(query, ignoreCase = true) == true)
            val matchCat = cat == null || asset.type == cat
            val matchStatus = status == null || asset.status == status
            matchQuery && matchCat && matchStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered Vehicles list
    val filteredVehicles: StateFlow<List<AssetEntity>> = combine(
        allAssets,
        vehicleSearchQuery,
        vehicleTypeFilter,
        vehicleTaxUrgencyFilter
    ) { assets, query, vType, urgency ->
        assets.filter { it.type == AssetType.KENDARAAN }
            .filter { vehicle ->
                val matchQuery = query.isBlank() ||
                        vehicle.name.contains(query, ignoreCase = true) ||
                        (vehicle.licensePlate?.contains(query, ignoreCase = true) == true) ||
                        (vehicle.chassisNumber?.contains(query, ignoreCase = true) == true) ||
                        (vehicle.engineNumber?.contains(query, ignoreCase = true) == true) ||
                        vehicle.pic.contains(query, ignoreCase = true)
                val matchType = vType == null || vehicle.vehicleType == vType
                val matchUrgency = urgency == null || FormatUtils.getTaxStatus(vehicle.annualTaxDueDate) == urgency
                matchQuery && matchType && matchUrgency
            }
            .sortedBy { it.annualTaxDueDate ?: Long.MAX_VALUE }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectTab(tab: CupertinoTab) {
        _currentTab.value = tab
    }

    fun openAssetDetail(asset: AssetEntity) {
        _selectedAsset.value = asset
    }

    fun closeAssetDetail() {
        _selectedAsset.value = null
    }

    fun openAddAsset(defaultType: AssetType? = null) {
        _editingAsset.value = if (defaultType != null) {
            AssetEntity(
                name = "",
                code = "AST-${defaultType.name.take(3)}-${System.currentTimeMillis() % 1000}",
                type = defaultType,
                acquisitionCost = 0.0,
                acquisitionDate = System.currentTimeMillis(),
                usefulLifeYears = if (defaultType == AssetType.TANAH) 0 else 5,
                location = "Kantor Pusat",
                pic = "Departemen Terkait"
            )
        } else null
        _isAddEditOpen.value = true
    }

    fun openEditAsset(asset: AssetEntity) {
        _editingAsset.value = asset
        _isAddEditOpen.value = true
    }

    fun closeAddEdit() {
        _isAddEditOpen.value = false
        _editingAsset.value = null
    }

    fun saveAsset(asset: AssetEntity) {
        viewModelScope.launch {
            if (asset.id == 0L) {
                repository.insertAsset(asset)
                _userMessage.emit("Aset baru '${asset.name}' berhasil disimpan.")
            } else {
                repository.updateAsset(asset)
                _userMessage.emit("Data aset '${asset.name}' berhasil diperbarui.")
            }
            // Update selected asset if currently viewed
            if (_selectedAsset.value?.id == asset.id) {
                _selectedAsset.value = asset
            }
            closeAddEdit()
        }
    }

    fun deleteAsset(asset: AssetEntity) {
        viewModelScope.launch {
            repository.deleteAsset(asset)
            _userMessage.emit("Aset '${asset.name}' telah dihapus.")
            closeAssetDetail()
        }
    }

    fun renewTaxForVehicle(vehicle: AssetEntity) {
        viewModelScope.launch {
            repository.renewVehicleTax(vehicle.id)
            val updated = vehicle.copy(
                annualTaxDueDate = (vehicle.annualTaxDueDate ?: System.currentTimeMillis()) + (365L * 24 * 60 * 60 * 1000)
            )
            if (_selectedAsset.value?.id == vehicle.id) {
                _selectedAsset.value = updated
            }
            _userMessage.emit("Pajak tahunan untuk ${vehicle.licensePlate ?: vehicle.name} berhasil diperpanjang +1 tahun.")
        }
    }

    fun triggerTaxRemindersNow() {
        viewModelScope.launch {
            val vehicles = allAssets.value.filter { it.type == AssetType.KENDARAAN }
            val urgent = vehicles.filter {
                val status = FormatUtils.getTaxStatus(it.annualTaxDueDate)
                status == TaxStatus.EXPIRED || status == TaxStatus.CRITICAL || status == TaxStatus.WARNING
            }

            if (urgent.isEmpty()) {
                _userMessage.emit("Seluruh kendaraan memiliki masa berlaku pajak yang masih aman.")
                return@launch
            }

            val count = NotificationHelper.dispatchBatchTaxAlerts(getApplication(), urgent)
            if (count > 0) {
                _userMessage.emit("$count notifikasi pengingat pajak berhasil dikirim ke perangkat.")
            } else {
                _userMessage.emit("Ditemukan ${urgent.size} kendaraan jatuh tempo. (Aktifkan izin notifikasi pada perangkat).")
            }
        }
    }

    private fun computeDashboardAnalytics(assets: List<AssetEntity>): DashboardAnalytics {
        var activeCount = 0
        var standbyCount = 0
        var maintenanceCount = 0

        val catMap = mutableMapOf<AssetType, MutableList<AssetEntity>>()
        AssetType.values().forEach { catMap[it] = mutableListOf() }

        val urgentVehicles = mutableListOf<AssetEntity>()
        var vehiclesCount = 0
        var vehiclesWarningCount = 0

        for (asset in assets) {
            when (asset.status) {
                AssetStatus.DIGUNAKAN -> activeCount++
                AssetStatus.STANDBY -> standbyCount++
                AssetStatus.DALAM_PERBAIKAN -> maintenanceCount++
                else -> {}
            }
            if (asset.condition == AssetCondition.PERLU_PERBAIKAN || asset.condition == AssetCondition.RUSAK) {
                if (asset.status != AssetStatus.DALAM_PERBAIKAN) {
                    maintenanceCount++
                }
            }

            catMap[asset.type]?.add(asset)

            if (asset.type == AssetType.KENDARAAN) {
                vehiclesCount++
                val taxStatus = FormatUtils.getTaxStatus(asset.annualTaxDueDate)
                if (taxStatus != TaxStatus.SAFE) {
                    vehiclesWarningCount++
                    urgentVehicles.add(asset)
                }
            }
        }

        val categoryStats = catMap.mapValues { (type, list) ->
            var cActive = 0
            var cStandby = 0
            var cMaint = 0
            list.forEach { a ->
                if (a.status == AssetStatus.DIGUNAKAN) cActive++
                if (a.status == AssetStatus.STANDBY) cStandby++
                if (a.status == AssetStatus.DALAM_PERBAIKAN || a.condition == AssetCondition.PERLU_PERBAIKAN || a.condition == AssetCondition.RUSAK) cMaint++
            }
            CategoryStat(
                type = type,
                count = list.size,
                activeCount = cActive,
                standbyCount = cStandby,
                maintenanceCount = cMaint
            )
        }

        urgentVehicles.sortBy { it.annualTaxDueDate ?: Long.MAX_VALUE }

        return DashboardAnalytics(
            totalAssetsCount = assets.size,
            activeAssetsCount = activeCount,
            standbyAssetsCount = standbyCount,
            maintenanceAssetsCount = maintenanceCount,
            totalVehiclesCount = vehiclesCount,
            vehiclesWithTaxWarningCount = vehiclesWarningCount,
            urgentTaxVehicles = urgentVehicles,
            categoryStats = categoryStats
        )
    }
}
