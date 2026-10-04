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
import com.example.model.CategoryEntity
import com.example.model.TaxStatus
import com.example.model.VehicleType
import com.example.sync.ApiClient
import com.example.sync.LoginRequest
import com.example.sync.SyncEngine
import com.example.sync.SyncPrefs
import com.example.sync.SyncScheduler
import com.example.sync.serverMessage
import retrofit2.HttpException
import java.io.IOException
import com.example.util.DepreciationCalculator
import com.example.util.FormatUtils
import com.example.util.NotificationHelper
import com.example.util.TaxReminders
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext

enum class CupertinoTab(val label: String) {
    DASHBOARD("Ringkasan"),
    ASSETS("Semua Aset"),
    VEHICLES("Kendaraan & Pajak"),
    SETTINGS("Pengaturan")
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

/** Ringkasan sinkronisasi untuk UI pengaturan. */
data class SyncStatus(
    val connected: Boolean = false, // login ke dashboard (punya token)
    val serverUrl: String? = null,
    val pendingCount: Int = 0,
    val lastSyncAt: Long? = null,
    val message: String? = null
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

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.assetDao()
    private val prefs = SyncPrefs(application)
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

    fun logout() {
        viewModelScope.launch {
            val session = prefs.current()
            if (session.token != null && session.baseUrl != null) {
                // Kirim perubahan yang tertunda sebelum token dicabut; gagal pun tetap logout (data tetap di HP).
                runCatching { SyncEngine(db, prefs).sync() }
                runCatching { ApiClient.create(session.baseUrl, session.token, prefs.deviceInfo()).logout() }
                prefs.clearSession()
                SyncScheduler.cancelAll(getApplication())
            }
            _isLoggedIn.value = false
            _currentTab.value = CupertinoTab.DASHBOARD
            _userMessage.emit("Anda telah keluar dari sistem.")
        }
    }

    // --- Login ke dashboard & sinkronisasi ---

    private val _loginLoading = MutableStateFlow(false)
    val loginLoading: StateFlow<Boolean> = _loginLoading.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    /** URL server terakhir yang dipakai, untuk mengisi form login. */
    private val _savedServerUrl = MutableStateFlow("")
    val savedServerUrl: StateFlow<String> = _savedServerUrl.asStateFlow()

    fun loginToServer(serverUrl: String, email: String, password: String) {
        viewModelScope.launch {
            _loginLoading.value = true
            _loginError.value = null
            try {
                val baseUrl = ApiClient.normalizeBaseUrl(serverUrl)
                val response = ApiClient.create(baseUrl, null, prefs.deviceInfo()).login(LoginRequest(email.trim(), password))
                val data = response.data ?: throw IllegalStateException(response.message ?: "Respons server tidak valid")

                val owner = "$baseUrl|${data.user.id}"
                val previousOwner = prefs.current().dataOwner
                withContext(Dispatchers.IO) {
                    // Akun berbeda: data lokal milik akun lain dibuang agar tidak terkirim ke akun ini.
                    if (previousOwner != null && previousOwner != owner) {
                        dao.deleteAll()
                        dao.deleteAllCategories()
                    } else {
                        dao.deleteLocalOnly()
                    }
                }
                prefs.saveLogin(baseUrl, data.token, data.user, owner, resetPull = previousOwner != owner)

                _currentUserName.value = data.user.name
                _currentUserRole.value = data.user.role ?: "Asset Manager"
                _savedServerUrl.value = baseUrl
                _isLoggedIn.value = true
                SyncScheduler.schedulePeriodic(getApplication())
                SyncScheduler.syncNow(getApplication())
                _userMessage.emit("Selamat datang, ${data.user.name}! Menyinkronkan data aset...")
            } catch (e: HttpException) {
                _loginError.value = e.serverMessage()
                    ?: if (e.code() == 404) "Alamat server tidak ditemukan (HTTP 404)." else "Login gagal (HTTP ${e.code()})."
            } catch (e: IOException) {
                _loginError.value = "Tidak dapat terhubung ke server. Periksa alamat dan koneksi."
            } catch (e: IllegalStateException) {
                _loginError.value = e.message
            } finally {
                _loginLoading.value = false
            }
        }
    }

    /**
     * Denyut tiap menit selama aplikasi terbuka (dipanggil dari MainActivity hanya saat STARTED), supaya
     * dashboard menampilkan perangkat ini online. Gagal jaringan diabaikan; berikutnya dicoba lagi.
     */
    suspend fun heartbeatLoop() {
        var api: com.example.sync.ApiService? = null
        var apiKey: Pair<String, String>? = null
        while (true) {
            val session = prefs.current()
            if (session.token != null && session.baseUrl != null) {
                val key = session.baseUrl to session.token
                if (key != apiKey) {
                    api = ApiClient.create(session.baseUrl, session.token, prefs.deviceInfo())
                    apiKey = key
                }
                try {
                    api?.heartbeat()
                } catch (e: HttpException) {
                    if (e.code() == 401) prefs.clearSession("Sesi berakhir, silakan login ulang untuk melanjutkan sinkronisasi.")
                } catch (e: IOException) {
                    // offline: dicoba lagi pada denyut berikutnya
                }
            }
            kotlinx.coroutines.delay(60_000)
        }
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun syncNow() {
        viewModelScope.launch {
            if (prefs.current().token == null) {
                _userMessage.emit("Masuk ke server terlebih dahulu untuk sinkronisasi.")
            } else {
                SyncScheduler.syncNow(getApplication())
                _userMessage.emit("Sinkronisasi dimulai...")
            }
        }
    }

    private fun requestSync() {
        viewModelScope.launch {
            if (prefs.current().token != null) SyncScheduler.syncNow(getApplication())
        }
    }

    init {
        repository = AssetRepository(dao)
        viewModelScope.launch {
            val session = prefs.current()
            _savedServerUrl.value = session.baseUrl.orEmpty()
            if (session.token != null) {
                // Sesi server masih ada: langsung masuk dan lanjutkan sinkronisasi berkala.
                _currentUserName.value = session.userName ?: _currentUserName.value
                _currentUserRole.value = session.userRole ?: _currentUserRole.value
                _isLoggedIn.value = true
                SyncScheduler.schedulePeriodic(application)
            }
        }
        NotificationHelper.initNotificationChannel(application)
        // Pengingat pajak/plat/servis berjalan otomatis di latar belakang; cek sekali saat aplikasi dibuka.
        TaxReminders.schedule(application)
        viewModelScope.launch { TaxReminders.check(application) }
    }

    /** Kategori dari dashboard; kosong di mode offline (form memakai 4 jenis bawaan). */
    val categories: StateFlow<List<CategoryEntity>> =
        repository.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val syncStatus: StateFlow<SyncStatus> = combine(prefs.snapshot, dao.observePendingCount()) { snap, pending ->
        SyncStatus(
            connected = snap.token != null,
            serverUrl = snap.baseUrl,
            pendingCount = pending,
            lastSyncAt = snap.lastSyncAt,
            message = snap.lastMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncStatus())

    // Navigation state
    private val _currentTab = MutableStateFlow(CupertinoTab.DASHBOARD)
    val currentTab: StateFlow<CupertinoTab> = _currentTab.asStateFlow()

    // Filters for Assets list
    val assetSearchQuery = MutableStateFlow("")
    val assetCategoryFilter = MutableStateFlow<AssetType?>(null) // null = Semua
    // Filter per kategori kustom dari dashboard (mis. "Alat Berat"). Lebih spesifik daripada jenis bidang:
    // dua kategori bisa punya jenis bidang sama, jadi filter ini dipakai lebih dulu bila terisi.
    val assetCategoryIdFilter = MutableStateFlow<Long?>(null)
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
        assetStatusFilter,
        assetCategoryIdFilter
    ) { assets, query, cat, status, categoryId ->
        assets.filter { asset ->
            val matchQuery = query.isBlank() ||
                    asset.name.contains(query, ignoreCase = true) ||
                    asset.code.contains(query, ignoreCase = true) ||
                    asset.location.contains(query, ignoreCase = true) ||
                    asset.pic.contains(query, ignoreCase = true) ||
                    (asset.licensePlate?.contains(query, ignoreCase = true) == true)
            val matchCat = when {
                categoryId != null -> asset.categoryId == categoryId
                cat != null -> asset.type == cat
                else -> true
            }
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
            requestSync()
        }
    }

    fun deleteAsset(asset: AssetEntity) {
        viewModelScope.launch {
            repository.deleteAsset(asset)
            _userMessage.emit("Aset '${asset.name}' telah dihapus.")
            closeAssetDetail()
            requestSync()
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
            requestSync()
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
