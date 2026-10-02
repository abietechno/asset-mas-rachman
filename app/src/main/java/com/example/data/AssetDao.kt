package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.AssetEntity
import com.example.model.AssetType
import com.example.model.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {

    @Query("SELECT * FROM assets WHERE syncState != 2 ORDER BY id DESC")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE type = :type AND syncState != 2 ORDER BY id DESC")
    fun getAssetsByType(type: AssetType): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    fun getAssetById(id: Long): Flow<AssetEntity?>

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    suspend fun getAssetByIdOnce(id: Long): AssetEntity?

    @Query("SELECT COUNT(*) FROM assets")
    suspend fun getAssetCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Update
    suspend fun updateAsset(asset: AssetEntity)

    @Delete
    suspend fun deleteAsset(asset: AssetEntity)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteAssetById(id: Long)

    // --- Sinkronisasi (syncState: 0 SYNCED, 1 DIRTY, 2 DELETED, 3 LOCAL_ONLY) ---

    @Query("SELECT * FROM assets WHERE syncState IN (1, 2) ORDER BY id")
    suspend fun getPendingChanges(): List<AssetEntity>

    @Query("SELECT COUNT(*) FROM assets WHERE syncState IN (1, 2)")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM assets WHERE clientUuid = :uuid LIMIT 1")
    suspend fun findByClientUuid(uuid: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE serverId = :serverId LIMIT 1")
    suspend fun findByServerId(serverId: Long): AssetEntity?

    @Query("DELETE FROM assets WHERE syncState = 3")
    suspend fun deleteLocalOnly()

    @Query("DELETE FROM assets")
    suspend fun deleteAll()

    // --- Kategori (selalu dikirim penuh oleh server) ---

    @Query("SELECT * FROM categories ORDER BY isSystem DESC, id")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}
