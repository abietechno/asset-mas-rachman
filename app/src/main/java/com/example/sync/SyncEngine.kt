package com.example.sync

import android.content.Context
import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.PhotoEntity
import com.example.data.PhotoState
import com.example.model.CategoryEntity
import com.example.model.SyncState
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File

data class SyncResult(
    val pushed: Int = 0,
    val pulled: Int = 0,
    /** Perubahan HP yang kalah karena server sudah berubah lebih dulu (versi server dipakai). */
    val conflicts: Int = 0,
    /** Perubahan yang ditolak server (validasi); tetap antre dan dilaporkan. */
    val rejected: Int = 0,
    val photosUploaded: Int = 0,
    val firstError: String? = null
) {
    fun summary(): String = buildString {
        append("Terkirim $pushed, diterima $pulled")
        if (photosUploaded > 0) append(", $photosUploaded foto terkirim")
        if (conflicts > 0) append(", $conflicts konflik (versi server dipakai)")
        if (rejected > 0) append(", $rejected ditolak server: $firstError")
    }
}

/**
 * Push perubahan HP lalu pull perubahan server. Urutan ini penting: perubahan lokal yang belum terkirim
 * tidak boleh ditimpa data server, jadi baris DIRTY/DELETED dilewati saat pull.
 * Kegagalan jaringan / HTTP dilempar ke pemanggil (SyncWorker) yang menentukan retry.
 */
class SyncEngine(
    private val db: AppDatabase,
    private val prefs: SyncPrefs,
    /** Dibutuhkan untuk menghapus berkas foto setelah terkirim; null saat dipakai di tes. */
    private val context: Context? = null,
    private val apiFactory: (baseUrl: String, token: String?, device: DeviceInfo?) -> ApiService =
        { url, token, device -> ApiClient.create(url, token, device) }
) {
    private val dao = db.assetDao()
    private val photoDao = db.photoDao()

    suspend fun sync(): SyncResult {
        val session = prefs.current()
        val baseUrl = session.baseUrl ?: error("Server belum diatur")
        val token = session.token ?: error("Belum login")
        val api = apiFactory(baseUrl, token, prefs.deviceInfo())

        val push = push(api)
        // Foto menyusul setelah asetnya terkirim, karena unggah memerlukan id aset di server.
        val photos = syncPhotos(api)
        val pulled = pull(api, session.lastPull)
        return push.copy(pulled = pulled, photosUploaded = photos)
    }

    private suspend fun push(api: ApiService): SyncResult {
        val pending = dao.getPendingChanges()
        var result = SyncResult()

        for (batch in pending.chunked(PUSH_BATCH)) {
            val sent = batch.associateBy { it.clientUuid }
            val response = api.push(PushRequest(batch.map(AssetMapper::toChange)))

            for (r in response.results) {
                val snapshot = sent[r.clientUuid] ?: continue
                val remote = r.asset
                when (r.status) {
                    "applied" -> {
                        db.withTransaction {
                            val cur = dao.findByClientUuid(snapshot.clientUuid) ?: return@withTransaction
                            when {
                                snapshot.syncState == SyncState.DELETED && cur.syncState == SyncState.DELETED -> {
                                    photoDao.deleteForAsset(cur.id)
                                    dao.deleteAssetById(cur.id)
                                }
                                remote == null -> Unit
                                // Diubah lagi di HP saat request berjalan: catat versi server, biarkan DIRTY agar terkirim lagi.
                                cur.updatedAt != snapshot.updatedAt ->
                                    dao.updateAsset(cur.copy(serverId = remote.id, serverVersion = remote.version))
                                else -> dao.updateAsset(AssetMapper.toEntity(remote, cur))
                            }
                        }
                        result = result.copy(pushed = result.pushed + 1)
                    }
                    "conflict" -> {
                        db.withTransaction {
                            val cur = dao.findByClientUuid(snapshot.clientUuid)
                            when {
                                remote == null -> Unit
                                remote.deleted -> cur?.let { dao.deleteAssetById(it.id) }
                                else -> dao.insertAsset(AssetMapper.toEntity(remote, cur))
                            }
                        }
                        result = result.copy(conflicts = result.conflicts + 1)
                    }
                    else -> result = result.copy(
                        rejected = result.rejected + 1,
                        firstError = result.firstError ?: r.message ?: "ditolak"
                    )
                }
            }
        }
        return result
    }

    /**
     * Mengirim foto baru dan menghapus foto yang dibuang di HP. Foto milik aset yang belum pernah terkirim
     * dilewati dulu (belum punya id server) dan akan ikut pada sinkronisasi berikutnya.
     */
    private suspend fun syncPhotos(api: ApiService): Int {
        var uploaded = 0

        for (photo in photoDao.pending()) {
            val asset = dao.getAssetByIdOnce(photo.assetLocalId)

            when {
                photo.state == PhotoState.PENDING_DELETE -> {
                    val serverId = photo.serverId
                    if (serverId == null) {
                        photoDao.deleteRow(photo.id)
                    } else {
                        val response = api.deletePhoto(serverId)
                        // 404 berarti sudah tidak ada di server; perlakukan sebagai selesai.
                        if (response.isSuccessful || response.code() == 404) {
                            photoDao.deleteRow(photo.id)
                            com.example.util.PhotoStore.delete(photo.localPath)
                        }
                    }
                }

                photo.state == PhotoState.PENDING_UPLOAD -> {
                    val serverAssetId = asset?.serverId ?: continue
                    val file = photo.localPath?.let(::File)
                    if (file == null || !file.exists()) {
                        photoDao.deleteRow(photo.id) // berkasnya hilang; tidak ada yang bisa dikirim
                        continue
                    }

                    val part = MultipartBody.Part.createFormData(
                        "image",
                        file.name,
                        file.asRequestBody("image/jpeg".toMediaType())
                    )
                    try {
                        val result = api.uploadPhoto(serverAssetId, part)
                        val image = result.image
                        if (image != null) {
                            photoDao.markUploaded(photo.id, image.id, image.url)
                            uploaded++
                        }
                    } catch (e: HttpException) {
                        // Ditolak server (mis. bukan gambar / terlalu besar): buang supaya tidak terus dicoba.
                        if (e.code() == 422) {
                            photoDao.deleteRow(photo.id)
                            com.example.util.PhotoStore.delete(photo.localPath)
                        } else {
                            throw e
                        }
                    }
                }
            }
        }
        return uploaded
    }

    /** Menyamakan daftar foto milik satu aset dengan yang ada di server (foto lokal yang menunggu tidak disentuh). */
    private suspend fun syncPhotosFromServer(assetLocalId: Long, remote: List<RemotePhoto>) {
        val known = photoDao.serverPhotosFor(assetLocalId).associateBy { it.serverId }
        val remoteIds = remote.map { it.id }.toSet()

        // Foto yang sudah tidak ada di server dibuang dari HP.
        known.values.filter { it.serverId !in remoteIds && it.state == PhotoState.SYNCED }
            .forEach {
                photoDao.deleteRow(it.id)
                com.example.util.PhotoStore.delete(it.localPath)
            }

        val baru = remote.filter { it.id !in known.keys }.map {
            PhotoEntity(
                assetLocalId = assetLocalId,
                serverId = it.id,
                remoteUrl = it.url,
                state = PhotoState.SYNCED
            )
        }
        if (baru.isNotEmpty()) photoDao.insertAll(baru)
    }

    private suspend fun pull(api: ApiService, since: Long): Int {
        val response = api.pull(since)
        var applied = 0

        db.withTransaction {
            for (remote in response.data) {
                val local = remote.clientUuid?.let { dao.findByClientUuid(it) } ?: dao.findByServerId(remote.id)

                if (local != null && (local.syncState == SyncState.DIRTY || local.syncState == SyncState.DELETED)) continue

                if (remote.deleted) {
                    if (local != null) {
                        photoDao.deleteForAsset(local.id)
                        dao.deleteAssetById(local.id)
                    }
                } else {
                    val localId = dao.insertAsset(AssetMapper.toEntity(remote, local))
                    // insertAsset mengembalikan 0 bila baris lama yang diganti; pakai id lama bila ada.
                    syncPhotosFromServer(local?.id ?: localId, remote.photos)
                    applied++
                }
            }

            // Kategori dikelola di dashboard; daftarnya selalu lengkap sehingga cukup diganti seluruhnya.
            if (response.categories.isNotEmpty()) {
                dao.deleteAllCategories()
                dao.upsertCategories(
                    response.categories.filter { !it.deleted }.mapNotNull { c ->
                        AssetMapper.kindOf(c.kind)?.let { CategoryEntity(c.id, c.name, it, c.usefulLifeYears, c.isSystem) }
                    }
                )
            }
        }

        // Disimpan setelah semua tertulis; pull berikutnya memakai `>=` sehingga tumpang tindih 1 detik aman.
        prefs.saveLastPull(response.serverTime)
        return applied
    }

    private companion object {
        const val PUSH_BATCH = 100 // batas server 200 per request
    }
}
