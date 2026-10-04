package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.SyncStatus
import com.example.ui.theme.*

/**
 * Base Cupertino-themed Scaffold with an ambient glassmorphism-inspired backdrop.
 * It renders subtle ethereal mesh glows beneath translucent glass cards for true iOS depth.
 */
@Composable
fun CupertinoScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    contentWindowInsets: WindowInsets = WindowInsets.systemBars,
    content: @Composable (PaddingValues) -> Unit
) {
    val glass = LocalCupertinoGlass.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Ambient glassmorphic glow mesh in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-right subtle blue orb
            drawCircle(
                color = if (glass.isDark) Color(0x140A84FF) else Color(0x16007AFF),
                radius = width * 0.55f,
                center = Offset(width * 0.85f, height * 0.12f)
            )

            // Mid-left subtle purple/indigo orb
            drawCircle(
                color = if (glass.isDark) Color(0x105E5CE6) else Color(0x105856D6),
                radius = width * 0.65f,
                center = Offset(width * 0.05f, height * 0.42f)
            )

            // Bottom-right subtle teal/mint orb
            drawCircle(
                color = if (glass.isDark) Color(0x0C30D158) else Color(0x0F30B0C7),
                radius = width * 0.50f,
                center = Offset(width * 0.90f, height * 0.78f)
            )
        }

        // Scaffold Content Layer
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent, // Let the ambient glass mesh show through
            contentWindowInsets = contentWindowInsets,
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}

/**
 * Clean iOS Glassmorphic Card with frosted translucent surface,
 * subtle crystal border highlight, and rounded squircle corners.
 */
@Composable
fun CupertinoGlassCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 1.dp,
    shape: RoundedCornerShape = RoundedCornerShape(Dimens.CardRadius),
    contentPadding: PaddingValues = PaddingValues(Dimens.CardPadding),
    backgroundColor: Color = LocalCupertinoGlass.current.surface,
    borderColor: Color = LocalCupertinoGlass.current.border,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = CupertinoPrimary.copy(alpha = 0.15f)),
            onClick = onClick
        )
    } else Modifier

    Surface(
        shape = shape,
        color = backgroundColor,
        // Warna "glass" bukan bagian colorScheme, jadi contentColor harus eksplisit; tanpa ini semua Text
        // tanpa warna di dalam kartu jadi hitam dan tak terbaca pada mode gelap.
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = elevation,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(clickableModifier)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Backwards compatible alias for CupertinoCard using the glass styling.
 */
@Composable
fun CupertinoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = LocalCupertinoGlass.current.surface,
    elevation: Dp = 0.5.dp,
    shape: RoundedCornerShape = RoundedCornerShape(Dimens.CardRadius),
    borderColor: Color = LocalCupertinoGlass.current.border,
    contentPadding: PaddingValues = PaddingValues(Dimens.CardPadding),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    CupertinoGlassCard(
        modifier = modifier,
        elevation = elevation,
        shape = shape,
        contentPadding = contentPadding,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        onClick = onClick,
        content = content
    )
}

/**
 * Top bar ringkas (~56 dp): judul 20sp + subjudul satu baris di kiri, aksi di kanan. Menggantikan "large title"
 * 28sp yang memakan ~108 dp tinggi layar.
 */
@Composable
fun CupertinoTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val glass = LocalCupertinoGlass.current

    Surface(
        color = glass.surface.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(0.5.dp, glass.hairline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("cupertino_top_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.TopBarMinHeight)
                .padding(horizontal = Dimens.ScreenMargin, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigationIcon != null) {
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.CenterStart) {
                    navigationIcon()
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = CupertinoSecondaryLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (actions != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    actions()
                }
            }
        }
    }
}

/**
 * Kartu KPI ringkas (~72 dp): ikon 32 dp di kiri, angka 18sp + label di kanan, catatan kecil di bawah.
 */
@Composable
fun CupertinoMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    modifier: Modifier = Modifier
) {
    CupertinoGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = CupertinoSecondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = iconColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Glassmorphic Segmented Control
 */
@Composable
fun <T> CupertinoSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: (T) -> String = { it.toString() }
) {
    val glass = LocalCupertinoGlass.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(glass.surfaceSecondary)
            .border(0.5.dp, glass.border, RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                val targetBg = if (isSelected) {
                    if (glass.isDark) Color(0xFF2C2C2E) else Color.White
                } else Color.Transparent
                val animatedBg by animateColorAsState(targetBg, label = "segment_bg")
                val textColor = if (isSelected) MaterialTheme.colorScheme.onSurface else CupertinoSecondaryLabel

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(animatedBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onItemSelected(item)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemLabel(item),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun CupertinoBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(0.5.dp, textColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier
                    .size(12.dp)
                    .padding(end = 4.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
fun CupertinoActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    backgroundColor: Color = CupertinoPrimary,
    contentColor: Color = Color.White,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = CupertinoFill,
            disabledContentColor = CupertinoSecondaryLabel
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 3.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier
            .height(Dimens.MinTouch)
            .testTag("cupertino_action_button")
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun CupertinoSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val glass = LocalCupertinoGlass.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(glass.surfaceSecondary)
            .border(0.5.dp, glass.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(CupertinoPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CupertinoSecondaryLabel
                        )
                    }
                    innerTextField()
                }
            )

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Hapus Pencarian",
                        tint = CupertinoSecondaryLabel,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CupertinoListTile(
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = CupertinoPrimary,
    leadingIconBg: Color = CupertinoPrimary.copy(alpha = 0.12f),
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = CupertinoPrimary.copy(alpha = 0.15f)),
            onClick = onClick
        )
    } else Modifier

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(clickableModifier)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(leadingIconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = leadingIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = CupertinoSecondaryLabel,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = CupertinoSecondaryLabel,
                    fontWeight = FontWeight.Medium
                )
            }

            if (trailingContent != null) {
                trailingContent()
            }
        }

        if (showDivider) {
            HorizontalDivider(
                color = CupertinoSeparator,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = if (leadingIcon != null) 64.dp else 16.dp)
            )
        }
    }
}


// ───────────────────────── Komponen padat (kepadatan ala marketplace, nuansa iOS) ─────────────────────────

/** Bentuk sudut baris dalam satu grup (inset grouped ala iOS): hanya baris pertama/terakhir yang membulat. */
fun groupedShape(index: Int, count: Int, radius: Dp = Dimens.CardRadius): RoundedCornerShape = when {
    count <= 1 -> RoundedCornerShape(radius)
    index == 0 -> RoundedCornerShape(topStart = radius, topEnd = radius)
    index == count - 1 -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
    else -> RoundedCornerShape(0.dp)
}

/** Judul bagian kecil (11sp, huruf besar) dengan aksi opsional seperti "Lihat semua". */
@Composable
fun CupertinoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenMargin + 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = CupertinoSecondaryLabel,
            fontWeight = FontWeight.Bold
        )
        if (actionText != null && onAction != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelSmall,
                color = CupertinoPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

/**
 * Baris aset padat (~60 dp): ikon 40 dp, nama satu baris, subjudul satu baris, dan chip status + catatan kecil di kanan.
 * Dipakai di daftar, beranda ("perlu perhatian"), dll. Gunakan [groupedShape] + [showDivider] untuk daftar bergrup.
 */
@Composable
fun CupertinoAssetRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    chipText: String? = null,
    chipBackground: Color = CupertinoFill,
    chipColor: Color = CupertinoLabel,
    trailingNote: String? = null,
    trailingNoteColor: Color = CupertinoSecondaryLabel,
    shape: RoundedCornerShape = RoundedCornerShape(Dimens.CardRadius),
    showDivider: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val glass = LocalCupertinoGlass.current

    Surface(
        shape = shape,
        color = glass.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                    .padding(horizontal = Dimens.CardPadding, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.RowIconTile)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = CupertinoSecondaryLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (chipText != null || trailingNote != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        if (chipText != null) {
                            CupertinoBadge(text = chipText, backgroundColor = chipBackground, textColor = chipColor)
                        }
                        if (trailingNote != null) {
                            Text(
                                text = trailingNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = trailingNoteColor,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.padding(top = if (chipText != null) 3.dp else 0.dp)
                            )
                        }
                    }
                }
            }

            if (showDivider) {
                HorizontalDivider(
                    color = CupertinoSeparator,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = Dimens.CardPadding + Dimens.RowIconTile + 12.dp)
                )
            }
        }
    }
}

/** Tombol aksi cepat: ikon 44 dp dalam tile berwarna + label 11sp (+ badge angka opsional). */
@Composable
fun CupertinoQuickAction(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    Column(
        modifier = modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(Dimens.MinTouch)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(CupertinoRed)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = badge, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Isian teks padat (~46 dp, label mengambang) pengganti OutlinedTextField (min 56 dp). Warna latar mengikuti tema
 * terang/gelap lewat token dinamis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CupertinoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CupertinoPrimary,
        unfocusedBorderColor = CupertinoSeparator,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = CupertinoFillSecondary
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(CupertinoPrimary),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        interactionSource = interaction,
        decorationBox = { inner ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = inner,
                enabled = enabled,
                singleLine = singleLine,
                visualTransformation = visualTransformation,
                interactionSource = interaction,
                isError = isError,
                label = { Text(label) },
                placeholder = placeholder?.let { { Text(it) } },
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                supportingText = supportingText?.let { { Text(it) } },
                colors = colors,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interaction,
                        colors = colors,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            )
        }
    )
}

/** Ikon status sinkronisasi untuk top bar: awan hijau (sinkron), biru + angka (antrean), oranye (belum login). */
@Composable
fun SyncStatusIcon(
    status: SyncStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when {
        !status.connected -> Icons.Outlined.CloudOff to CupertinoOrange
        status.pendingCount > 0 -> Icons.Outlined.CloudUpload to CupertinoPrimary
        else -> Icons.Outlined.CloudDone to CupertinoGreen
    }
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("sync_status_icon"),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = "Status sinkronisasi", tint = tint, modifier = Modifier.size(22.dp))
        if (status.connected && status.pendingCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 0.dp)
                    .clip(CircleShape)
                    .background(CupertinoPrimary)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (status.pendingCount > 99) "99+" else status.pendingCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


/**
 * Isian tanggal: tampil seperti field, diketuk membuka pemilih tanggal. Nilai disimpan sebagai jam 12 siang
 * waktu lokal supaya tanggalnya tidak bergeser saat diubah ke format "yyyy-MM-dd" untuk server.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CupertinoDateField(
    label: String,
    value: Long?,
    onChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    clearable: Boolean = true
) {
    var showPicker by remember { mutableStateOf(false) }
    val text = value?.let { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID")).format(it) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CupertinoSecondaryLabel,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = CupertinoFillSecondary,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, CupertinoSeparator),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.FieldMinHeight)
                .clickable { showPicker = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = CupertinoPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = text ?: "Pilih tanggal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (text != null) MaterialTheme.colorScheme.onSurface else CupertinoSecondaryLabel,
                    modifier = Modifier.weight(1f)
                )
                if (clearable && value != null) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Hapus tanggal",
                        tint = CupertinoSecondaryLabel,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onChange(null) }
                    )
                }
            }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value?.let(::localDateToUtcMillis))
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(state.selectedDateMillis?.let(::utcMillisToLocalNoon))
                    showPicker = false
                }) { Text("Pilih", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = state, title = { Text(label, modifier = Modifier.padding(start = 24.dp, top = 16.dp)) })
        }
    }
}

/** Pemilih tanggal Material bekerja di UTC tengah malam; ubah ke jam 12 siang lokal agar harinya tetap sama. */
private fun utcMillisToLocalNoon(utc: Long): Long {
    val src = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
    return java.util.Calendar.getInstance().apply {
        clear()
        set(
            src.get(java.util.Calendar.YEAR),
            src.get(java.util.Calendar.MONTH),
            src.get(java.util.Calendar.DAY_OF_MONTH),
            12, 0, 0
        )
    }.timeInMillis
}

private fun localDateToUtcMillis(local: Long): Long {
    val src = java.util.Calendar.getInstance().apply { timeInMillis = local }
    return java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            src.get(java.util.Calendar.YEAR),
            src.get(java.util.Calendar.MONTH),
            src.get(java.util.Calendar.DAY_OF_MONTH),
            0, 0, 0
        )
    }.timeInMillis
}
