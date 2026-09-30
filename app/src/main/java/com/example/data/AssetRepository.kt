package com.example.data

import com.example.model.AssetEntity
import com.example.model.AssetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

class AssetRepository(private val assetDao: AssetDao) {

    val allAssets: Flow<List<AssetEntity>> = assetDao.getAllAssets()

    fun getAssetsByType(type: AssetType): Flow<List<AssetEntity>> =
        assetDao.getAssetsByType(type)

    fun getAssetById(id: Long): Flow<AssetEntity?> =
        assetDao.getAssetById(id)

    suspend fun insertAsset(asset: AssetEntity): Long = withContext(Dispatchers.IO) {
        assetDao.insertAsset(asset)
    }

    suspend fun updateAsset(asset: AssetEntity) = withContext(Dispatchers.IO) {
        assetDao.updateAsset(asset.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteAsset(asset: AssetEntity) = withContext(Dispatchers.IO) {
        assetDao.deleteAsset(asset)
    }

    suspend fun deleteAssetById(id: Long) = withContext(Dispatchers.IO) {
        assetDao.deleteAssetById(id)
    }

    suspend fun renewVehicleTax(assetId: Long) = withContext(Dispatchers.IO) {
        val existing = assetDao.getAssetByIdOnce(assetId) ?: return@withContext
        val currentDue = existing.annualTaxDueDate ?: System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = if (currentDue < System.currentTimeMillis()) System.currentTimeMillis() else currentDue
            add(Calendar.YEAR, 1)
        }
        val updated = existing.copy(
            annualTaxDueDate = cal.timeInMillis,
            updatedAt = System.currentTimeMillis()
        )
        assetDao.updateAsset(updated)
    }

    suspend fun initializeSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = assetDao.getAssetCount()
        if (count == 0) {
            assetDao.insertAssets(SampleData.getInitialAssets())
        }
    }
}
