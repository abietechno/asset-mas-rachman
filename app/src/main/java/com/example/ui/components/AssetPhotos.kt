package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.PhotoEntity
import com.example.data.PhotoState
import com.example.ui.theme.*
import com.example.util.PhotoStore
import kotlinx.coroutines.launch
import java.io.File

/** Sumber gambar untuk satu foto: berkas lokal bila ada, selain itu URL dari dashboard. */
private fun PhotoEntity.model(): Any? = localPath?.let { File(it) } ?: remoteUrl

/**
 * Daftar foto aset yang bisa diubah: ketuk tambah untuk memotret atau memilih dari galeri,
 * ketuk tanda silang untuk menghapus. Foto yang belum terkirim diberi tanda awan.
 */
@Composable
fun AssetPhotoPicker(
    photos: List<PhotoEntity>,
    onPicked: (Uri) -> Unit,
    onRemove: (PhotoEntity) -> Unit,
    modifier: Modifier = Modifier,
    maxPhotos: Int = PhotoStore.MAX_PHOTOS_PER_ASSET
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSourceSheet by remember { mutableStateOf(false) }
    var cameraTarget by remember { mutableStateOf<Pair<File, Uri>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onPicked) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val target = cameraTarget
        cameraTarget = null
        if (ok && target != null) {
            scope.launch { onPicked(Uri.fromFile(target.first)) }
        } else {
            target?.first?.delete()
        }
    }

    fun openCamera() {
        val target = PhotoStore.newCameraTarget(context)
        cameraTarget = target
        cameraLauncher.launch(target.second)
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera() else message = "Izin kamera ditolak. Foto masih bisa dipilih dari galeri."
    }

    Column(modifier = modifier) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(photos, key = { it.id }) { photo ->
                Box {
                    AsyncImage(
                        model = photo.model(),
                        contentDescription = "Foto aset",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CupertinoFill)
                    )
                    // Tanda foto yang belum terkirim ke dashboard.
                    if (photo.state == PhotoState.PENDING_UPLOAD) {
                        Icon(
                            imageVector = Icons.Outlined.CloudUpload,
                            contentDescription = "Belum terkirim",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                                .size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(CupertinoRed)
                            .clickable { onRemove(photo) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Hapus foto",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            if (photos.size < maxPhotos) {
                item {
                    Column(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CupertinoFill)
                            .clickable { showSourceSheet = true }
                            .testTag("btn_add_photo"),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddAPhoto,
                            contentDescription = "Tambah foto",
                            tint = CupertinoPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tambah",
                            style = MaterialTheme.typography.labelSmall,
                            color = CupertinoPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Text(
            text = message ?: "${photos.size}/$maxPhotos foto. Foto dikecilkan otomatis sebelum dikirim.",
            style = MaterialTheme.typography.labelSmall,
            color = if (message != null) CupertinoOrange else CupertinoSecondaryLabel,
            modifier = Modifier.padding(top = 6.dp)
        )
    }

    if (showSourceSheet) {
        AlertDialog(
            onDismissRequest = { showSourceSheet = false },
            title = { Text("Tambah foto", fontWeight = FontWeight.Bold) },
            text = { Text("Pilih sumber foto aset.") },
            confirmButton = {
                TextButton(onClick = {
                    showSourceSheet = false
                    message = null
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) openCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
                }) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kamera", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSourceSheet = false
                    message = null
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galeri")
                }
            }
        )
    }
}

/** Galeri foto hanya-baca untuk halaman detail; ketuk foto untuk melihat versi besarnya. */
@Composable
fun AssetPhotoGallery(photos: List<PhotoEntity>, modifier: Modifier = Modifier) {
    var preview by remember { mutableStateOf<PhotoEntity?>(null) }

    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(photos, key = { it.id }) { photo ->
            Box {
                AsyncImage(
                    model = photo.model(),
                    contentDescription = "Foto aset",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CupertinoFill)
                        .clickable { preview = photo }
                )
                if (photo.state == PhotoState.PENDING_UPLOAD) {
                    Icon(
                        imageVector = Icons.Outlined.CloudUpload,
                        contentDescription = "Belum terkirim",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }

    preview?.let { photo ->
        Dialog(onDismissRequest = { preview = null }) {
            AsyncImage(
                model = photo.model(),
                contentDescription = "Foto aset",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black)
                    .clickable { preview = null }
            )
        }
    }
}
