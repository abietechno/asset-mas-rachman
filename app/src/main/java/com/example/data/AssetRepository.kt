package com.example.data

import com.example.model.AssetEntity
import com.example.model.AssetType
import com.example.model.CategoryEntity
import com.example.model.SyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

class AssetRepository(private val assetDao: AssetDao) {

    val allAssets: Flow<List<AssetEntity>> = assetDao.getAllAssets()

    val categories: Flow<List<CategoryEntity>> = assetDao.observeCategories()

    fun getAssetsByType(type: AssetType): Flow<List<AssetEntity>> =
        assetDao.getAssetsByType(type)

    fun getAssetById(id: Long): Flow<AssetEntity?> =
        assetDao.getAssetById(id)

    suspend fun insertAsset(asset: AssetEntity): Long = withContext(Dispatchers.IO) {
        assetDao.insertAsset(asset)
    }

    /**
     * Form edit membuat AssetEntity baru, jadi identitas sinkronisasi (serverId, clientUuid, serverVersion)
     * dan field yang tidak ada di form diambil dari baris yang tersimpan.
     */
    suspend fun updateAsset(asset: AssetEntity) = withContext(Dispatchers.IO) {
        val existing = assetDao.getAssetByIdOnce(asset.id)
        val merged = if (existing == null) asset else asset.copy(
            serverId = existing.serverId,
            clientUuid = existing.clientUuid,
            serverVersion = existing.serverVersion,
            createdAt = existing.createdAt,
            // Field yang hanya bisa diisi dari dashboard: pertahankan nilainya saat aset disimpan dari HP.
            annualTaxAmount = asset.annualTaxAmount ?: existing.annualTaxAmount,
            numberOfFloors = asset.numberOfFloors ?: existing.numberOfFloors,
            pbgNumber = asset.pbgNumber ?: existing.pbgNumber,
            address = asset.address ?: existing.address,
            department = asset.department ?: existing.department
        )
        assetDao.updateAsset(merged.copy(updatedAt = System.currentTimeMillis(), syncState = dirtyState(existing ?: merged)))
    }

    /** Aset yang sudah ada di server dihapus lewat tombstone agar penghapusan ikut ter-sync. */
    suspend fun deleteAsset(asset: AssetEntity) = deleteAssetById(asset.id)

    suspend fun deleteAssetById(id: Long) = withContext(Dispatchers.IO) {
        val existing = assetDao.getAssetByIdOnce(id) ?: return@withContext
        if (existing.serverId == null || existing.syncState == SyncState.LOCAL_ONLY) {
            assetDao.deleteAssetById(id)
        } else {
            assetDao.updateAsset(existing.copy(syncState = SyncState.DELETED, updatedAt = System.currentTimeMillis()))
        }
    }

    private fun dirtyState(asset: AssetEntity) =
        if (asset.syncState == SyncState.LOCAL_ONLY) SyncState.LOCAL_ONLY else SyncState.DIRTY

    suspend fun renewVehicleTax(assetId: Long) = withContext(Dispatchers.IO) {
        val existing = assetDao.getAssetByIdOnce(assetId) ?: return@withContext
        val currentDue = existing.annualTaxDueDate ?: System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = if (currentDue < System.currentTimeMillis()) System.currentTimeMillis() else currentDue
            add(Calendar.YEAR, 1)
        }
        val updated = existing.copy(
            annualTaxDueDate = cal.timeInMillis,
            updatedAt = System.currentTimeMillis(),
            syncState = dirtyState(existing)
        )
        assetDao.updateAsset(updated)
    }

    suspend fun initializeSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = assetDao.getAssetCount()
        if (count == 0) {
            assetDao.insertAssets(SampleData.getInitialAssets().map { it.copy(syncState = SyncState.LOCAL_ONLY) })
        }
    }
}
