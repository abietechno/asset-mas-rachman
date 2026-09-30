package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.CupertinoActionButton
import com.example.ui.components.CupertinoBadge
import com.example.ui.components.CupertinoGlassCard
import com.example.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: (identifier: String, role: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var loginType by remember { mutableStateOf(0) } // 0: NIK / ID, 1: Email
    var identifier by remember { mutableStateOf("EMP-8821") }
    var password by remember { mutableStateOf("••••••••") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("login_screen")
    ) {
        // Ambient glassmorphism glowing mesh orbs in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            drawCircle(
                color = Color(0x33007AFF),
                radius = width * 0.70f,
                center = Offset(width * 0.85f, height * 0.15f)
            )

            drawCircle(
                color = Color(0x285856D6),
                radius = width * 0.75f,
                center = Offset(width * 0.10f, height * 0.50f)
            )

            drawCircle(
                color = Color(0x2430B0C7),
                radius = width * 0.60f,
                center = Offset(width * 0.90f, height * 0.85f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Icon with iOS rounded squircle & subtle luminous glass glow
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF007AFF),
                                Color(0xFF5856D6)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Color.White.copy(alpha = 0.45f),
                        RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = "Asset Management Logo",
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Header titles in Plus Jakarta Sans
            Text(
                text = "Asset Management",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Portal Operasional & Pelaporan Aset",
                style = MaterialTheme.typography.bodyMedium,
                color = CupertinoSecondaryLabel,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            CupertinoBadge(
                text = "Mobile Field Operations",
                backgroundColor = CupertinoPrimary.copy(alpha = 0.12f),
                textColor = CupertinoPrimary,
                icon = Icons.Default.Shield
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Main Glassmorphic Login Card
            CupertinoGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_form_card"),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                // Segmented tab: ID Karyawan vs Email
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CupertinoFill)
                        .padding(2.dp)
                ) {
                    val tabs = listOf("ID Karyawan / NIK", "Email Perusahaan")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = loginType == index
                        val bg = if (isSelected) Color.White else Color.Transparent
                        val textCol = if (isSelected) CupertinoLabel else CupertinoSecondaryLabel
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable {
                                    loginType = index
                                    identifier = if (index == 0) "EMP-8821" else "admin.aset@perusahaan.co.id"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textCol
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Error alert
                if (errorMessage != null) {
                    Surface(
                        color = CupertinoRedLight,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = CupertinoRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = CupertinoRed
                            )
                        }
                    }
                }

                // Field 1: Identifier
                Text(
                    text = if (loginType == 0) "ID KARYAWAN / NIK" else "EMAIL PERUSAHAAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = CupertinoSecondaryLabel,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = identifier,
                    onValueChange = {
                        identifier = it
                        errorMessage = null
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (loginType == 0) Icons.Default.Badge else Icons.Default.Email,
                            contentDescription = null,
                            tint = CupertinoPrimary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CupertinoPrimary,
                        unfocusedBorderColor = CupertinoSeparator,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = CupertinoSystemBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_login_identifier"),
                    placeholder = {
                        Text(if (loginType == 0) "contoh: EMP-8821 / 3201xxxx" else "nama@perusahaan.co.id")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (loginType == 0) KeyboardType.Text else KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Field 2: Password
                Text(
                    text = "KATA SANDI",
                    style = MaterialTheme.typography.labelSmall,
                    color = CupertinoSecondaryLabel,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CupertinoPrimary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Sembunyikan" else "Tampilkan",
                                tint = CupertinoSecondaryLabel
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CupertinoPrimary,
                        unfocusedBorderColor = CupertinoSeparator,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = CupertinoSystemBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_login_password"),
                    placeholder = { Text("Masukkan kata sandi") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (identifier.isNotBlank()) {
                                onLoginSuccess(identifier, "Asset Manager")
                            } else {
                                errorMessage = "ID atau Email tidak boleh kosong"
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Row: Ingat Saya & Lupa Sandi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                    ) {
                        Switch(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CupertinoPrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = CupertinoFill
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ingat Saya",
                            style = MaterialTheme.typography.bodySmall,
                            color = CupertinoSecondaryLabel
                        )
                    }

                    Text(
                        text = "Lupa Sandi?",
                        style = MaterialTheme.typography.bodySmall,
                        color = CupertinoPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            errorMessage = "Silakan hubungi IT Helpdesk untuk reset kredensial."
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Login Button
                CupertinoActionButton(
                    text = "Masuk ke Sistem",
                    onClick = {
                        if (identifier.isBlank()) {
                            errorMessage = "Mohon masukkan ID Karyawan atau Email."
                        } else {
                            onLoginSuccess(identifier, "Asset Manager")
                        }
                    },
                    icon = Icons.Default.Login,
                    backgroundColor = CupertinoPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_submit_button")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Face ID / Biometric Button
                OutlinedButton(
                    onClick = {
                        onLoginSuccess("Bambang Sudiro", "Lead Asset Operations")
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CupertinoSeparator),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("biometric_login_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = CupertinoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Masuk Cepat via Biometrik / Face ID",
                        style = MaterialTheme.typography.labelMedium,
                        color = CupertinoLabel,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Demo Roles Selection
            Text(
                text = "AKSES CEPAT PERAN OPERASIONAL (DEMO)",
                style = MaterialTheme.typography.labelSmall,
                color = CupertinoSecondaryLabel,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CupertinoPrimary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, CupertinoPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onLoginSuccess("Bambang Sudiro", "Kepala Divisi Aset")
                        }
                        .testTag("quick_login_manager")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = CupertinoPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Asset Manager",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CupertinoPrimary
                        )
                        Text(
                            text = "Akses Penuh",
                            style = MaterialTheme.typography.labelSmall,
                            color = CupertinoSecondaryLabel
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CupertinoGreen.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, CupertinoGreen.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onLoginSuccess("Ahmad Fadhil", "Petugas Lapangan")
                        }
                        .testTag("quick_login_field_officer")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = CupertinoGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tim Lapangan",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CupertinoGreen
                        )
                        Text(
                            text = "Inspeksi & Kendaraan",
                            style = MaterialTheme.typography.labelSmall,
                            color = CupertinoSecondaryLabel
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = CupertinoTertiaryLabel,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Valuasi finansial & depresiasi terintegrasi di Web Admin",
                    style = MaterialTheme.typography.labelSmall,
                    color = CupertinoTertiaryLabel
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
