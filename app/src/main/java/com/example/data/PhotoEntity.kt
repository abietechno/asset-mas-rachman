package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Status unggah satu foto aset. */
object PhotoState {
    const val SYNCED = 0 // sudah ada di server
    const val PENDING_UPLOAD = 1 // diambil di HP, menunggu terkirim
    const val PENDING_DELETE = 2 // dihapus di HP, menunggu dihapus di server
}

/**
 * Foto aset. Foto yang diambil di lapangan disimpan sebagai berkas lokal dan diunggah saat ada internet;
 * foto dari dashboard hanya menyimpan URL-nya dan dimuat sesuai kebutuhan (Coil menyimpan salinannya).
 */
@Entity(tableName = "asset_photos", indices = [Index("assetLocalId")])
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Id aset di Room (bukan id server), supaya foto bisa diambil sebelum asetnya pernah tersinkron. */
    val assetLocalId: Long,
    /** Id AssetImage di server; null berarti belum pernah terunggah. */
    val serverId: Long? = null,
    /** Berkas di penyimpanan aplikasi; null untuk foto yang hanya ada di server. */
    val localPath: String? = null,
    val remoteUrl: String? = null,
    val state: Int = PhotoState.PENDING_UPLOAD,
    val createdAt: Long = System.currentTimeMillis()
)
