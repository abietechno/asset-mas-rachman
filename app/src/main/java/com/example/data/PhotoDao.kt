package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    /** Foto yang ditampilkan untuk satu aset (yang menunggu dihapus disembunyikan). */
    @Query("SELECT * FROM asset_photos WHERE assetLocalId = :assetLocalId AND state != 2 ORDER BY id")
    fun observeForAsset(assetLocalId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM asset_photos WHERE assetLocalId = :assetLocalId AND state != 2 ORDER BY id")
    suspend fun forAsset(assetLocalId: Long): List<PhotoEntity>

    @Query("SELECT COUNT(*) FROM asset_photos WHERE assetLocalId = :assetLocalId AND state != 2")
    suspend fun countForAsset(assetLocalId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoEntity>)

    @Query("SELECT * FROM asset_photos WHERE state IN (1, 2) ORDER BY id")
    suspend fun pending(): List<PhotoEntity>

    @Query("SELECT COUNT(*) FROM asset_photos WHERE state IN (1, 2)")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE asset_photos SET serverId = :serverId, remoteUrl = :remoteUrl, state = 0 WHERE id = :id")
    suspend fun markUploaded(id: Long, serverId: Long, remoteUrl: String?)

    /** Foto yang belum pernah terunggah cukup dibuang; yang sudah ada di server ditandai untuk dihapus saat sync. */
    @Query("UPDATE asset_photos SET state = 2 WHERE id = :id")
    suspend fun markForDelete(id: Long)

    @Query("DELETE FROM asset_photos WHERE id = :id")
    suspend fun deleteRow(id: Long)

    @Query("DELETE FROM asset_photos WHERE assetLocalId = :assetLocalId")
    suspend fun deleteForAsset(assetLocalId: Long)

    /** Foto milik aset ini yang asalnya dari server (dipakai saat menyamakan daftar foto dari pull). */
    @Query("SELECT * FROM asset_photos WHERE assetLocalId = :assetLocalId AND serverId IS NOT NULL")
    suspend fun serverPhotosFor(assetLocalId: Long): List<PhotoEntity>

    @Query("DELETE FROM asset_photos")
    suspend fun deleteAll()
}
