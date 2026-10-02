package com.example.sync

import android.content.Context
import android.os.Build
import com.example.BuildConfig
import java.util.UUID
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.syncDataStore by preferencesDataStore(name = "sync_prefs")

data class SyncSnapshot(
    val baseUrl: String?,
    val token: String?,
    val userName: String?,
    val userRole: String?,
    /** "<baseUrl>|<userId>" pemilik data lokal; dipakai agar data satu akun tidak terkirim ke akun lain. */
    val dataOwner: String?,
    /** server_time (detik) dari pull terakhir yang tuntas. */
    val lastPull: Long,
    val lastSyncAt: Long?,
    val lastMessage: String?
)

/** Identitas perangkat yang dikirim ke dashboard (header X-Device-*) agar muncul di daftar perangkat online. */
data class DeviceInfo(val id: String, val name: String, val appVersion: String)

class SyncPrefs(private val context: Context) {

    /** ID dibuat sekali saat pertama dipakai dan bertahan selama aplikasi terpasang. */
    suspend fun deviceInfo(): DeviceInfo {
        var id = context.syncDataStore.data.first()[DEVICE_ID]
        if (id == null) {
            id = UUID.randomUUID().toString()
            context.syncDataStore.edit { it[DEVICE_ID] = id }
        }
        return DeviceInfo(id, "${Build.MANUFACTURER} ${Build.MODEL}".trim(), BuildConfig.VERSION_NAME)
    }


    val snapshot: Flow<SyncSnapshot> = context.syncDataStore.data.map { p ->
        SyncSnapshot(
            baseUrl = p[BASE_URL],
            token = p[TOKEN],
            userName = p[USER_NAME],
            userRole = p[USER_ROLE],
            dataOwner = p[DATA_OWNER],
            lastPull = p[LAST_PULL] ?: 0L,
            lastSyncAt = p[LAST_SYNC_AT],
            lastMessage = p[LAST_MESSAGE]
        )
    }

    suspend fun current(): SyncSnapshot = snapshot.first()

    suspend fun saveLogin(baseUrl: String, token: String, user: UserDto, dataOwner: String, resetPull: Boolean) {
        context.syncDataStore.edit { p ->
            p[BASE_URL] = baseUrl
            p[TOKEN] = token
            p[USER_NAME] = user.name
            p[USER_ROLE] = user.role ?: "Asset Manager"
            p[DATA_OWNER] = dataOwner
            if (resetPull) p[LAST_PULL] = 0L
            p.remove(LAST_MESSAGE)
        }
    }

    /** Menghapus sesi tapi mempertahankan URL server & pemilik data (untuk deteksi ganti akun). */
    suspend fun clearSession(message: String? = null) {
        context.syncDataStore.edit { p ->
            p.remove(TOKEN)
            if (message != null) p[LAST_MESSAGE] = message else p.remove(LAST_MESSAGE)
        }
    }

    suspend fun saveLastPull(serverTime: Long) {
        context.syncDataStore.edit { it[LAST_PULL] = serverTime }
    }

    suspend fun saveStatus(message: String, success: Boolean) {
        context.syncDataStore.edit { p ->
            if (success) p[LAST_SYNC_AT] = System.currentTimeMillis()
            p[LAST_MESSAGE] = message
        }
    }

    private companion object {
        val BASE_URL = stringPreferencesKey("base_url")
        val TOKEN = stringPreferencesKey("token")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_ROLE = stringPreferencesKey("user_role")
        val DATA_OWNER = stringPreferencesKey("data_owner")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val LAST_PULL = longPreferencesKey("last_pull")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val LAST_MESSAGE = stringPreferencesKey("last_message")
    }
}
