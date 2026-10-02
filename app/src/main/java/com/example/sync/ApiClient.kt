package com.example.sync

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface ApiService {
    @POST("api/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/heartbeat")
    suspend fun heartbeat(): Response<Unit>

    @POST("api/logout")
    suspend fun logout(): Response<Unit>

    @GET("api/sync/assets")
    suspend fun pull(@Query("since") since: Long): PullResponse

    @POST("api/sync/assets")
    suspend fun push(@Body body: PushRequest): PushResponse
}

object ApiClient {

    /** "192.168.1.9/dash/public" -> "http://192.168.1.9/dash/public/" (Retrofit mewajibkan akhiran "/"). */
    fun normalizeBaseUrl(raw: String): String {
        var url = raw.trim()
        if (url.isEmpty()) return url
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
        return if (url.endsWith("/")) url else "$url/"
    }

    // Header HTTP hanya boleh ASCII yang dapat dicetak; nama perangkat dari pabrikan bisa berisi karakter lain.
    private fun ascii(s: String) = s.filter { it.code in 32..126 }

    fun create(baseUrl: String, token: String?, device: DeviceInfo? = null): ApiService {
        val auth = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .apply {
                    if (token != null) header("Authorization", "Bearer $token")
                    if (device != null) {
                        header("X-Device-Id", ascii(device.id))
                        header("X-Device-Name", ascii(device.name))
                        header("X-App-Version", ascii(device.appVersion))
                    }
                }
                .build()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(auth)
            .apply {
                // BASIC saja: level BODY akan mencetak token & kata sandi ke logcat.
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()).withNullSerialization())
            .build()
            .create(ApiService::class.java)
    }
}

/** Pesan `message` dari respons error Laravel (mis. "Login gagal..."), atau null. */
fun HttpException.serverMessage(): String? = runCatching {
    JSONObject(response()?.errorBody()?.string().orEmpty()).optString("message")
}.getOrNull()?.takeIf { it.isNotBlank() }
