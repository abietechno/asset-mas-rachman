package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.components.CupertinoActionButton
import com.example.ui.components.CupertinoGlassCard
import com.example.ui.theme.CupertinoPrimary
import com.example.ui.theme.CupertinoRed
import com.example.ui.theme.CupertinoSecondaryLabel
import com.example.ui.theme.CupertinoSeparator
import com.example.ui.theme.CupertinoSystemBackground

/** Login ke dashboard: hanya email dan kata sandi. Alamat server diatur lewat konfigurasi build (DEFAULT_SERVER_URL). */
@Composable
fun LoginScreen(
    onLogin: (email: String, password: String) -> Unit,
    isLoading: Boolean = false,
    serverError: String? = null,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val shownError = localError ?: serverError

    fun submit() {
        when {
            isLoading -> Unit
            email.isBlank() -> localError = "Mohon masukkan email."
            password.isEmpty() -> localError = "Mohon masukkan kata sandi."
            else -> onLogin(email, password)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CupertinoPrimary,
        unfocusedBorderColor = CupertinoSeparator,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = CupertinoSystemBackground
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("login_screen")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color(0x33007AFF), radius = size.width * 0.70f, center = Offset(size.width * 0.85f, size.height * 0.15f))
            drawCircle(Color(0x285856D6), radius = size.width * 0.75f, center = Offset(size.width * 0.10f, size.height * 0.50f))
            drawCircle(Color(0x2430B0C7), radius = size.width * 0.60f, center = Offset(size.width * 0.90f, size.height * 0.85f))
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
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF5856D6))))
                    .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = "Asset Management",
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Asset Management",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(28.dp))

            CupertinoGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_form_card"),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                if (shownError != null) {
                    Text(
                        text = shownError,
                        style = MaterialTheme.typography.bodySmall,
                        color = CupertinoRed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("login_error")
                    )
                }

                Text(
                    text = "EMAIL",
                    style = MaterialTheme.typography.labelSmall,
                    color = CupertinoSecondaryLabel,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        localError = null
                    },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CupertinoPrimary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    placeholder = { Text("nama@perusahaan.co.id") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_login_identifier")
                )

                Spacer(modifier = Modifier.height(14.dp))

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
                        localError = null
                    },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CupertinoPrimary) },
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
                    colors = fieldColors,
                    placeholder = { Text("Masukkan kata sandi") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        submit()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_login_password")
                )

                Spacer(modifier = Modifier.height(20.dp))

                CupertinoActionButton(
                    text = if (isLoading) "Memproses..." else "Masuk",
                    onClick = { submit() },
                    icon = Icons.Default.Login,
                    backgroundColor = CupertinoPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_submit_button")
                )
            }
        }
    }
}
