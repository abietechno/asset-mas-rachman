package com.example.sync

import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.model.CategoryEntity
import com.example.model.SyncState

data class SyncResult(
    val pushed: Int = 0,
    val pulled: Int = 0,
    /** Perubahan HP yang kalah karena server sudah berubah lebih dulu (versi server dipakai). */
    val conflicts: Int = 0,
    /** Perubahan yang ditolak server (validasi); tetap antre dan dilaporkan. */
    val rejected: Int = 0,
    val firstError: String? = null
) {
    fun summary(): String = buildString {
        append("Terkirim $pushed, diterima $pulled")
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
    private val apiFactory: (baseUrl: String, token: String?, device: DeviceInfo?) -> ApiService =
        { url, token, device -> ApiClient.create(url, token, device) }
) {
    private val dao = db.assetDao()

    suspend fun sync(): SyncResult {
        val session = prefs.current()
        val baseUrl = session.baseUrl ?: error("Server belum diatur")
        val token = session.token ?: error("Belum login")
        val api = apiFactory(baseUrl, token, prefs.deviceInfo())

        val push = push(api)
        val pulled = pull(api, session.lastPull)
        return push.copy(pulled = pulled)
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
                                snapshot.syncState == SyncState.DELETED && cur.syncState == SyncState.DELETED ->
                                    dao.deleteAssetById(cur.id)
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

    private suspend fun pull(api: ApiService, since: Long): Int {
        val response = api.pull(since)
        var applied = 0

        db.withTransaction {
            for (remote in response.data) {
                val local = remote.clientUuid?.let { dao.findByClientUuid(it) } ?: dao.findByServerId(remote.id)

                if (local != null && (local.syncState == SyncState.DIRTY || local.syncState == SyncState.DELETED)) continue

                if (remote.deleted) {
                    if (local != null) dao.deleteAssetById(local.id)
                } else {
                    dao.insertAsset(AssetMapper.toEntity(remote, local))
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
