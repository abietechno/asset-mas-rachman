package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AssetViewModel
import com.example.ui.ThemeMode
import com.example.ui.components.CupertinoCard
import com.example.ui.components.CupertinoListTile
import com.example.ui.components.CupertinoSectionHeader
import com.example.ui.components.CupertinoSegmentedControl
import com.example.ui.components.CupertinoTopBar
import com.example.ui.theme.*
import com.example.util.TaxReminders
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/** Pengaturan: akun, sinkronisasi dashboard, tema, pengingat, dan laporan operasional. */
@Composable
fun SettingsScreen(
    viewModel: AssetViewModel,
    onOpenReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userName by viewModel.currentUserName.collectAsStateWithLifecycle()
    val userRole by viewModel.currentUserRole.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val sync by viewModel.syncStatus.collectAsStateWithLifecycle()

    var showLogoutConfirm by remember { mutableStateOf(false) }
    var reminderMessage by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        item { CupertinoTopBar(title = "Pengaturan", subtitle = "Akun, sinkronisasi & tampilan") }

        // Akun
        item {
            CupertinoCard(modifier = Modifier.padding(horizontal = Dimens.ScreenMargin, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CupertinoPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = CupertinoPrimary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(text = userRole, style = MaterialTheme.typography.bodySmall, color = CupertinoSecondaryLabel)
                    }
                }
            }
        }

        // Sinkronisasi
        item { CupertinoSectionHeader(title = "Sinkronisasi Dashboard") }
        item {
            CupertinoCard(modifier = Modifier.padding(horizontal = Dimens.ScreenMargin, vertical = 2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (sync.connected) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                        contentDescription = null,
                        tint = if (sync.connected) CupertinoGreen else CupertinoOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (sync.connected) "Terhubung" else "Mode offline",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = sync.serverUrl ?: "Data hanya tersimpan di perangkat ini",
                            style = MaterialTheme.typography.bodySmall,
                            color = CupertinoSecondaryLabel
                        )
                    }
                }

                if (sync.connected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (sync.pendingCount > 0) {
                        Text(
                            text = "${sync.pendingCount} perubahan menunggu dikirim",
                            style = MaterialTheme.typography.bodySmall,
                            color = CupertinoOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    sync.lastSyncAt?.let {
                        Text(
                            text = "Terakhir sinkron: " + SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = CupertinoSecondaryLabel
                        )
                    }
                    sync.message?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = CupertinoSecondaryLabel)
                    }
                    TextButton(
                        onClick = { viewModel.syncNow() },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("settings_sync_now")
                    ) {
                        Text("Sinkronkan Sekarang", color = CupertinoPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Tampilan
        item { CupertinoSectionHeader(title = "Tampilan") }
        item {
            CupertinoCard(modifier = Modifier.padding(horizontal = Dimens.ScreenMargin, vertical = 2.dp)) {
                CupertinoSegmentedControl(
                    items = ThemeMode.values().toList(),
                    selectedItem = themeMode,
                    onItemSelected = { viewModel.setThemeMode(it) },
                    itemLabel = { mode ->
                        when (mode) {
                            ThemeMode.SYSTEM -> "Sistem"
                            ThemeMode.LIGHT -> "Terang"
                            ThemeMode.DARK -> "Gelap"
                        }
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Mode gelap mengikuti pengaturan perangkat bila dipilih \"Sistem\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = CupertinoSecondaryLabel
                )
            }
        }

        // Lainnya
        item { CupertinoSectionHeader(title = "Lainnya") }
        item {
            CupertinoCard(
                modifier = Modifier.padding(horizontal = Dimens.ScreenMargin, vertical = 2.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                CupertinoListTile(
                    title = "Laporan Operasional",
                    subtitle = "Rekap inventaris, kondisi & kepatuhan pajak",
                    leadingIcon = Icons.Outlined.Assessment,
                    leadingIconTint = CupertinoPurple,
                    leadingIconBg = CupertinoPurple.copy(alpha = 0.12f),
                    onClick = onOpenReports
                )
                CupertinoListTile(
                    title = "Pengingat Otomatis",
                    subtitle = reminderMessage
                        ?: "Pajak, plat 5 tahunan & servis. Ketuk untuk memeriksa sekarang.",
                    leadingIcon = Icons.Outlined.NotificationsActive,
                    leadingIconTint = CupertinoOrange,
                    leadingIconBg = CupertinoOrange.copy(alpha = 0.12f),
                    showDivider = false,
                    onClick = {
                        scope.launch { reminderMessage = TaxReminders.check(context).message() }
                    }
                )
            }
        }

        // Keluar
        item {
            CupertinoCard(
                modifier = Modifier.padding(horizontal = Dimens.ScreenMargin, vertical = 10.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                CupertinoListTile(
                    title = "Keluar Akun",
                    leadingIcon = Icons.AutoMirrored.Outlined.Logout,
                    leadingIconTint = CupertinoRed,
                    leadingIconBg = CupertinoRed.copy(alpha = 0.12f),
                    showDivider = false,
                    onClick = { showLogoutConfirm = true }
                )
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Keluar dari akun?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = if (sync.pendingCount > 0) {
                        "${sync.pendingCount} perubahan akan dikirim dulu ke dashboard sebelum keluar."
                    } else {
                        "Anda perlu memasukkan email dan kata sandi lagi untuk masuk."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    viewModel.logout()
                }) {
                    Text("Keluar", color = CupertinoRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Batal") }
            }
        )
    }
}
