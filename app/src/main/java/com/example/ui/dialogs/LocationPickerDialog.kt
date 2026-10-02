package com.example.ui.dialogs

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.CupertinoGreen
import com.example.ui.theme.CupertinoOrange
import com.example.ui.theme.CupertinoPrimary
import com.example.ui.theme.CupertinoRed
import com.example.ui.theme.CupertinoSecondaryLabel
import com.example.util.CaptureResult
import com.example.util.LocationCapture
import com.example.util.PlaceSearch
import com.example.util.PlaceSuggestion
import com.example.util.reverseGeocode
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.DragState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class PinSource { NONE, GPS, SEARCH, MANUAL }

/**
 * Layar penuh untuk menentukan lokasi aset. Tiga cara, bisa dikombinasikan:
 * 1. Cari alamat / nama tempat di kolom atas lalu pilih dari saran (Google Places);
 * 2. Ambil dari GPS (titik terbaik beberapa detik, dengan akurasi ±m);
 * 3. Ketuk peta atau geser pin untuk koreksi.
 * Hasil membawa alamat (dari pencarian atau perkiraan dari koordinat) yang bisa mengisi kolom Lokasi aset.
 */
@Composable
fun LocationPickerDialog(
    initialLat: Double?,
    initialLng: Double?,
    initialAccuracyM: Double?,
    onDismiss: () -> Unit,
    onConfirm: (latitude: Double, longitude: Double, accuracyM: Double?, address: String?) -> Unit
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val placeSearch = remember { runCatching { PlaceSearch(context) }.getOrNull() }

    val hasInitialPin = initialLat != null && initialLng != null
    var pin by remember { mutableStateOf(if (hasInitialPin) LatLng(initialLat!!, initialLng!!) else null) }
    var source by remember { mutableStateOf(if (hasInitialPin) (if (initialAccuracyM != null) PinSource.GPS else PinSource.MANUAL) else PinSource.NONE) }
    var accuracyM by remember { mutableStateOf(initialAccuracyM) }
    var address by remember { mutableStateOf<String?>(null) }
    var addressFor by remember { mutableStateOf<LatLng?>(null) } // koordinat yang alamatnya sudah pasti (dari pencarian)

    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    var capturing by remember { mutableStateOf(false) }
    var liveAccuracy by remember { mutableStateOf<Float?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var satellite by remember { mutableStateOf(true) }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(pin ?: LatLng(-2.5, 118.0), if (pin != null) 18f else 4.5f)
    }
    val markerState = rememberMarkerState(position = pin ?: LatLng(0.0, 0.0))

    LaunchedEffect(pin) { pin?.let { markerState.position = it } }
    LaunchedEffect(markerState.dragState) {
        if (markerState.dragState == DragState.END) {
            pin = markerState.position
            accuracyM = null
            source = PinSource.MANUAL
        }
    }

    // Alamat perkiraan dari koordinat (kecuali alamatnya sudah dari hasil pencarian untuk titik yang sama).
    LaunchedEffect(pin) {
        val p = pin ?: return@LaunchedEffect
        if (p == addressFor) return@LaunchedEffect
        delay(500)
        address = reverseGeocode(context, p)
    }

    // Saran muncul setelah berhenti mengetik sebentar.
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 3 || placeSearch == null) {
            suggestions = emptyList()
            searchError = if (q.length >= 3 && placeSearch == null) "Pencarian belum siap (API key peta?)." else null
            return@LaunchedEffect
        }
        delay(350)
        searching = true
        searchError = null
        try {
            suggestions = placeSearch.suggest(q)
            if (suggestions.isEmpty()) searchError = "Tidak ada hasil untuk \"$q\"."
        } catch (e: Exception) {
            suggestions = emptyList()
            searchError = "Pencarian gagal: ${e.message ?: "periksa koneksi, atau aktifkan Places API (New) di Google Cloud"}"
        } finally {
            searching = false
        }
    }

    fun choose(s: PlaceSuggestion) {
        focus.clearFocus()
        suggestions = emptyList()
        query = s.primary
        scope.launch {
            searching = true
            try {
                val r = placeSearch!!.details(s)
                val p = LatLng(r.latitude, r.longitude)
                pin = p
                addressFor = p
                address = r.address ?: s.secondary.ifBlank { s.primary }
                accuracyM = null
                source = PinSource.SEARCH
                message = null
                camera.animate(CameraUpdateFactory.newLatLngZoom(p, 18f))
            } catch (e: Exception) {
                searchError = "Gagal mengambil lokasi: ${e.message ?: "coba lagi"}"
            } finally {
                searching = false
            }
        }
    }

    fun startCapture() {
        if (capturing) return
        scope.launch {
            capturing = true
            liveAccuracy = null
            message = "Mengambil lokasi GPS… tunggu beberapa detik di tempat terbuka."
            when (val r = LocationCapture.capture(context, onProgress = { liveAccuracy = it })) {
                is CaptureResult.Fix -> {
                    val p = LatLng(r.latitude, r.longitude)
                    pin = p
                    accuracyM = r.accuracyM
                    source = PinSource.GPS
                    message = if (r.reachedTarget) null
                    else "Akurasi masih ±${r.accuracyM.roundToInt()} m. Coba lagi di tempat lebih terbuka atau geser pin secara manual."
                    camera.animate(CameraUpdateFactory.newLatLngZoom(p, 19f))
                }
                CaptureResult.MockLocation -> message = "Lokasi palsu (mock location) terdeteksi dan ditolak. Matikan aplikasi lokasi palsu."
                CaptureResult.Unavailable -> message = "Lokasi tidak tersedia. Aktifkan GPS dan coba lagi di tempat terbuka."
            }
            capturing = false
            liveAccuracy = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (hasPermission) startCapture() else message = "Izin lokasi ditolak. Anda masih bisa mencari alamat atau mengetuk peta."
    }

    fun requestCapture() {
        if (hasPermission) startCapture()
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Batal", color = CupertinoRed, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onDismiss() })
                    Text("Pilih Lokasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (satellite) "Peta" else "Satelit",
                        color = CupertinoPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { satellite = !satellite }
                    )
                }

                // Kolom pencarian
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cari alamat atau nama tempat") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CupertinoPrimary) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = ""; suggestions = emptyList(); searchError = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus", tint = CupertinoSecondaryLabel)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = {
                        suggestions.firstOrNull()?.let { choose(it) } ?: focus.clearFocus()
                    }),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Peta + saran (menimpa peta) + panel bawah
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = camera,
                        properties = MapProperties(
                            mapType = if (satellite) MapType.HYBRID else MapType.NORMAL,
                            isMyLocationEnabled = hasPermission
                        ),
                        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false, mapToolbarEnabled = false),
                        onMapClick = {
                            focus.clearFocus()
                            suggestions = emptyList()
                            pin = it
                            accuracyM = null
                            source = PinSource.MANUAL
                        }
                    ) {
                        if (pin != null) {
                            Marker(state = markerState, draggable = true, title = "Lokasi aset")
                            accuracyM?.let {
                                Circle(
                                    center = pin!!,
                                    radius = it,
                                    strokeColor = CupertinoPrimary,
                                    strokeWidth = 2f,
                                    fillColor = CupertinoPrimary.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }

                    // Daftar saran
                    if (suggestions.isNotEmpty() || searchError != null || (searching && query.trim().length >= 3)) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 8.dp
                        ) {
                            if (suggestions.isEmpty()) {
                                Text(
                                    text = searchError ?: "Mencari…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (searchError != null) CupertinoOrange else CupertinoSecondaryLabel,
                                    modifier = Modifier.padding(14.dp)
                                )
                            } else {
                                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                    items(suggestions, key = { it.placeId }) { s ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { choose(s) }.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Place, contentDescription = null, tint = CupertinoPrimary)
                                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                                Text(s.primary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                if (s.secondary.isNotBlank()) {
                                                    Text(s.secondary, style = MaterialTheme.typography.bodySmall, color = CupertinoSecondaryLabel, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }

                    // Panel bawah
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 6.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val p = pin
                            if (p != null) {
                                Text(
                                    text = address ?: "%.6f, %.6f".format(p.latitude, p.longitude),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (address != null) {
                                    Text("%.6f, %.6f".format(p.latitude, p.longitude), style = MaterialTheme.typography.bodySmall, color = CupertinoSecondaryLabel)
                                }
                                val acc = accuracyM
                                val (label, color) = when {
                                    source == PinSource.SEARCH -> "Titik dari pencarian alamat; geser pin untuk menyesuaikan" to CupertinoSecondaryLabel
                                    acc == null -> "Titik ditentukan manual" to CupertinoSecondaryLabel
                                    acc <= 10 -> "Akurasi GPS ±${acc.roundToInt()} m (baik)" to CupertinoGreen
                                    acc <= 30 -> "Akurasi GPS ±${acc.roundToInt()} m (cukup)" to CupertinoOrange
                                    else -> "Akurasi GPS ±${acc.roundToInt()} m (rendah)" to CupertinoRed
                                }
                                Text(label, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.SemiBold)
                            } else {
                                Text("Belum ada pin", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "Cari alamat di atas, ambil dari GPS, atau ketuk peta.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CupertinoSecondaryLabel
                                )
                            }

                            val live = liveAccuracy
                            if (capturing) {
                                Text(
                                    text = if (live != null) "GPS: ±${live.roundToInt()} m, menunggu lebih akurat…" else (message ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CupertinoPrimary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else if (message != null) {
                                Text(message!!, style = MaterialTheme.typography.bodySmall, color = CupertinoOrange, modifier = Modifier.padding(top = 4.dp))
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { requestCapture() },
                                    enabled = !capturing,
                                    modifier = Modifier.weight(1f)
                                ) { Text(if (capturing) "Mengambil…" else "Lokasi Saya (GPS)") }
                                Button(
                                    onClick = { pin?.let { onConfirm(it.latitude, it.longitude, accuracyM, address) } },
                                    enabled = pin != null && !capturing,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Pakai Titik Ini") }
                            }
                        }
                    }
                }
            }
        }
    }
}
