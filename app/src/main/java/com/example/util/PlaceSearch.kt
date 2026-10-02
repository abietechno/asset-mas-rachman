package com.example.util

import android.content.Context
import android.location.Geocoder
import com.example.BuildConfig
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

data class PlaceSuggestion(val placeId: String, val primary: String, val secondary: String)

data class PlaceResult(val name: String?, val address: String?, val latitude: Double, val longitude: Double)

/**
 * Pencarian alamat / nama tempat (Google Places) dengan saran saat mengetik, mis. "Jl. Basuki Rahmat" atau
 * "PT Eternal Sukses Bersama". Satu sesi pencarian = beberapa ketikan + satu pemilihan; token sesi dibuat ulang
 * setelah pemilihan supaya penagihan Google dihitung per sesi, bukan per ketikan.
 * Membutuhkan "Places API (New)" aktif di project Google Cloud yang sama dengan API key peta.
 */
/** Key khusus Places API (New); jika belum diisi (nilai bawaan), pakai key peta. */
private fun placesKey(): String =
    BuildConfig.PLACES_API_KEY.takeUnless { it.isBlank() || it == "DEFAULT_API_KEY" } ?: BuildConfig.MAPS_API_KEY

class PlaceSearch(context: Context) {
    private val client = run {
        val app = context.applicationContext
        if (!Places.isInitialized()) Places.initializeWithNewPlacesApiEnabled(app, placesKey())
        Places.createClient(app)
    }
    private var token = AutocompleteSessionToken.newInstance()

    /** Saran untuk [query] (dibatasi Indonesia). Melempar exception jika API belum aktif / tanpa koneksi. */
    suspend fun suggest(query: String): List<PlaceSuggestion> {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setSessionToken(token)
            .setCountries("ID")
            .build()
        return client.findAutocompletePredictions(request).await().autocompletePredictions.map {
            PlaceSuggestion(
                placeId = it.placeId,
                primary = it.getPrimaryText(null).toString(),
                secondary = it.getSecondaryText(null).toString()
            )
        }
    }

    /** Detail (koordinat + alamat) dari saran yang dipilih; mengakhiri sesi pencarian. */
    suspend fun details(s: PlaceSuggestion): PlaceResult {
        val fields = listOf(Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS, Place.Field.LOCATION)
        val place = client.fetchPlace(FetchPlaceRequest.builder(s.placeId, fields).setSessionToken(token).build()).await().place
        token = AutocompleteSessionToken.newInstance()
        val loc = place.location ?: error("Tempat ini tidak punya koordinat")
        return PlaceResult(place.displayName, place.formattedAddress, loc.latitude, loc.longitude)
    }
}

/** Alamat perkiraan dari koordinat (Geocoder bawaan Android, tanpa kunci); null jika tidak tersedia. */
suspend fun reverseGeocode(context: Context, point: LatLng): String? = withContext(Dispatchers.IO) {
    runCatching {
        if (!Geocoder.isPresent()) return@runCatching null
        @Suppress("DEPRECATION")
        Geocoder(context, Locale("id", "ID")).getFromLocation(point.latitude, point.longitude, 1)
            ?.firstOrNull()?.getAddressLine(0)
    }.getOrNull()
}
