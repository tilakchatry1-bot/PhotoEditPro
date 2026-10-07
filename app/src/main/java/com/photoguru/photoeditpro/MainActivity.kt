package com.photoguru.photoeditpro

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Blur
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Checkmark
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExportNotes
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhotoEditProTheme {
                PhotoEditorScreen()
            }
        }
    }
}

private enum class EditMode {
    HOME, CROP, FILTERS, ADJUSTMENTS, BEAUTY, TEXT, STICKERS, EFFECTS
}

private data class TextOverlay(
    val id: Long,
    val text: String,
    val color: Color,
    val size: Float,
    val x: Float,
    val y: Float,
)

private data class StickerOverlay(
    val id: Long,
    val emoji: String,
    val x: Float,
    val y: Float,
    val rotation: Float = 0f,
    val scale: Float = 1f,
)

@Composable
fun PhotoEditorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var editMode by remember { mutableStateOf(EditMode.HOME) }

    var brightness by remember { mutableFloatStateOf(1f) }
    var contrast by remember { mutableFloatStateOf(1.1f) }
    var saturation by remember { mutableFloatStateOf(1.2f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var blur by remember { mutableFloatStateOf(0f) }
    var beauty by remember { mutableFloatStateOf(0f) }

    var customText by remember { mutableStateOf("Add Text") }
    var textColor by remember { mutableStateOf(Color.White) }
    var filterTint by remember { mutableStateOf(Color(0xFF7C3AED)) }
    var textOverlays by remember { mutableStateOf(listOf<TextOverlay>()) }
    var stickerOverlays by remember { mutableStateOf(listOf<StickerOverlay>()) }

    val filters = listOf(
        "Original" to Color(0xFFFFFFFF),
        "Vivid" to Color(0xFF7C3AED),
        "Warm" to Color(0xFFF59E0B),
        "Rose" to Color(0xFFEC4899),
        "Mint" to Color(0xFF10B981),
        "Sky" to Color(0xFF3B82F6),
        "Noir" to Color(0xFF1F2937),
    )

    val beautyFilters = listOf(
        "Normal" to 0f,
        "Glow" to 0.3f,
        "Smooth" to 0.6f,
        "Radiant" to 1f,
    )

    val stickerList = listOf("✨", "🔥", "❤️", "⭐", "🌈", "🎨", "🎉", "😍")

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            selectedImage = uri
            if (uri != null) editMode = EditMode.CROP
        }
    )

    when (editMode) {
        EditMode.HOME -> {
            HomeScreen {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }

        EditMode.CROP -> {
            if (selectedImage != null) {
                CropEditScreen(
                    imageUri = selectedImage!!,
                    onBack = { editMode = EditMode.HOME; selectedImage = null },
                    onNext = { editMode = EditMode.FILTERS }
                )
            }
        }

        EditMode.FILTERS -> {
            EditingScreen(
                imageUri = selectedImage!!,
                currentMode = EditMode.FILTERS,
                brightness = brightness,
                contrast = contrast,
                saturation = saturation,
                rotation = rotation,
                zoom = zoom,
                blur = blur,
                beauty = beauty,
                filterTint = filterTint,
                textOverlays = textOverlays,
                stickerOverlays = stickerOverlays,
                onModeChange = { editMode = it },
                onBrightnessChange = { brightness = it },
                onContrastChange = { contrast = it },
                onSaturationChange = { saturation = it },
                onRotationChange = { rotation = it },
                onZoomChange = { zoom = it },
                onBlurChange = { blur = it },
                onBeautyChange = { beauty = it },
                onFilterChange = { filterTint = it },
                onTextAdd = {
                    textOverlays = textOverlays + TextOverlay(
                        id = System.currentTimeMillis(),
                        text = customText,
                        color = textColor,
                        size = 36f,
                        x = 0.5f,
                        y = 0.5f
                    )
                },
                onStickerAdd = { emoji ->
                    stickerOverlays = stickerOverlays + StickerOverlay(
                        id = System.currentTimeMillis(),
                        emoji = emoji,
                        x = 0.5f,
                        y = 0.5f
                    )
                },
                filters = filters,
                beautyFilters = beautyFilters,
                stickerList = stickerList,
                onExport = {
                    scope.launch {
                        val savedFile = withContext(Dispatchers.IO) {
                            exportEditedPhoto(
                                context = context,
                                sourceUri = selectedImage!!,
                                brightness = brightness,
                                contrast = contrast,
                                saturation = saturation,
                                blur = blur,
                                beauty = beauty,
                                tint = filterTint,
                                textOverlays = textOverlays,
                                stickers = stickerOverlays,
                            )
                        }
                        Toast.makeText(
                            context,
                            if (savedFile != null) "Saved to Pictures/PhotoEditPro" else "Export failed",
                            Toast.LENGTH_LONG
                        ).show()
                        editMode = EditMode.HOME
                        selectedImage = null
                    }
                }
            )
        }

        else -> {
            if (selectedImage != null) {
                EditingScreen(
                    imageUri = selectedImage!!,
                    currentMode = editMode,
                    brightness = brightness,
                    contrast = contrast,
                    saturation = saturation,
                    rotation = rotation,
                    zoom = zoom,
                    blur = blur,
                    beauty = beauty,
                    filterTint = filterTint,
                    textOverlays = textOverlays,
                    stickerOverlays = stickerOverlays,
                    onModeChange = { editMode = it },
                    onBrightnessChange = { brightness = it },
                    onContrastChange = { contrast = it },
                    onSaturationChange = { saturation = it },
                    onRotationChange = { rotation = it },
                    onZoomChange = { zoom = it },
                    onBlurChange = { blur = it },
                    onBeautyChange = { beauty = it },
                    onFilterChange = { filterTint = it },
                    onTextAdd = {
                        textOverlays = textOverlays + TextOverlay(
                            id = System.currentTimeMillis(),
                            text = customText,
                            color = textColor,
                            size = 36f,
                            x = 0.5f,
                            y = 0.5f
                        )
                    },
                    onStickerAdd = { emoji ->
                        stickerOverlays = stickerOverlays + StickerOverlay(
                            id = System.currentTimeMillis(),
                            emoji = emoji,
                            x = 0.5f,
                            y = 0.5f
                        )
                    },
                    filters = filters,
                    beautyFilters = beautyFilters,
                    stickerList = stickerList,
                    onExport = {
                        scope.launch {
                            val savedFile = withContext(Dispatchers.IO) {
                                exportEditedPhoto(
                                    context = context,
                                    sourceUri = selectedImage!!,
                                    brightness = brightness,
                                    contrast = contrast,
                                    saturation = saturation,
                                    blur = blur,
                                    beauty = beauty,
                                    tint = filterTint,
                                    textOverlays = textOverlays,
                                    stickers = stickerOverlays,
                                )
                            }
                            Toast.makeText(
                                context,
                                if (savedFile != null) "Saved to Pictures/PhotoEditPro" else "Export failed",
                                Toast.LENGTH_LONG
                            ).show()
                            editMode = EditMode.HOME
                            selectedImage = null
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(onPickPhoto: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = Color(0xFF7C3AED),
                        shape = RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "PhotoEditPro",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Professional Photo Editing at Your Fingertips",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onPickPhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Start Editing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun CropEditScreen(
    imageUri: Uri,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text("Crop", color = Color.White, fontWeight = FontWeight.Bold)
            IconButton(onClick = onNext) {
                Icon(Icons.Default.Done, contentDescription = null, tint = Color(0xFF7C3AED))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CropButton("1:1", Modifier.weight(1f)) {}
            CropButton("16:9", Modifier.weight(1f)) {}
            CropButton("9:16", Modifier.weight(1f)) {}
        }
    }
}

@Composable
fun CropButton(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun EditingScreen(
    imageUri: Uri,
    currentMode: EditMode,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    rotation: Float,
    zoom: Float,
    blur: Float,
    beauty: Float,
    filterTint: Color,
    textOverlays: List<TextOverlay>,
    stickerOverlays: List<StickerOverlay>,
    onModeChange: (EditMode) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onRotationChange: (Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    onBlurChange: (Float) -> Unit,
    onBeautyChange: (Float) -> Unit,
    onFilterChange: (Color) -> Unit,
    onTextAdd: () -> Unit,
    onStickerAdd: (String) -> Unit,
    filters: List<Pair<String, Color>>,
    beautyFilters: List<Pair<String, Float>>,
    stickerList: List<String>,
    onExport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onModeChange(EditMode.HOME) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text("Edit Photo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.ExportNotes, contentDescription = null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export", fontSize = 12.sp)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = rotation
                        scaleX = zoom
                        scaleY = zoom
                    }
                    .blur(blur.dp),
                colorFilter = if (filterTint != Color(0xFFFFFFFF)) {
                    ColorFilter.tint(
                        color = filterTint.copy(alpha = 0.25f),
                        blendMode = BlendMode.SoftLight
                    )
                } else null
            )

            textOverlays.forEach { overlay ->
                Text(
                    text = overlay.text,
                    color = overlay.color,
                    fontSize = overlay.size.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(
                        x = (overlay.x * 300).dp,
                        y = (overlay.y * 260).dp
                    )
                )
            }

            stickerOverlays.forEach { sticker ->
                Text(
                    text = sticker.emoji,
                    fontSize = 48.sp,
                    modifier = Modifier
                        .offset(
                            x = (sticker.x * 260).dp,
                            y = (sticker.y * 220).dp
                        )
                        .graphicsLayer {
                            rotationZ = sticker.rotation
                            scaleX = sticker.scale
                            scaleY = sticker.scale
                        }
                )
            }
        }

        EditToolBar(
            currentMode = currentMode,
            onModeChange = onModeChange
        )

        when (currentMode) {
            EditMode.FILTERS -> {
                FiltersPanel(
                    filters = filters,
                    currentTint = filterTint,
                    onFilterChange = onFilterChange
                )
            }

            EditMode.ADJUSTMENTS -> {
                AdjustmentsPanel(
                    brightness = brightness,
                    contrast = contrast,
                    saturation = saturation,
                    rotation = rotation,
                    zoom = zoom,
                    onBrightnessChange = onBrightnessChange,
                    onContrastChange = onContrastChange,
                    onSaturationChange = onSaturationChange,
                    onRotationChange = onRotationChange,
                    onZoomChange = onZoomChange
                )
            }

            EditMode.BEAUTY -> {
                BeautyPanel(
                    blur = blur,
                    beauty = beauty,
                    beautyFilters = beautyFilters,
                    onBlurChange = onBlurChange,
                    onBeautyChange = onBeautyChange
                )
            }

            EditMode.TEXT -> {
                TextPanel(onTextAdd = onTextAdd)
            }

            EditMode.STICKERS -> {
                StickersPanel(stickerList = stickerList, onStickerAdd = onStickerAdd)
            }

            else -> {}
        }
    }
}

@Composable
fun EditToolBar(
    currentMode: EditMode,
    onModeChange: (EditMode) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            listOf(
                Pair(Icons.Default.FilterAlt, "Filters") to EditMode.FILTERS,
                Pair(Icons.Default.Tune, "Adjust") to EditMode.ADJUSTMENTS,
                Pair(Icons.Default.Face, "Beauty") to EditMode.BEAUTY,
                Pair(Icons.Default.Edit, "Text") to EditMode.TEXT,
                Pair(Icons.Default.Palette, "Stickers") to EditMode.STICKERS,
            )
        ) { (icon, label) ->
            val mode = when (label) {
                "Filters" -> EditMode.FILTERS
                "Adjust" -> EditMode.ADJUSTMENTS
                "Beauty" -> EditMode.BEAUTY
                "Text" -> EditMode.TEXT
                "Stickers" -> EditMode.STICKERS
                else -> EditMode.HOME
            }

            EditToolButton(
                icon = icon.first,
                label = label,
                isActive = currentMode == mode,
                onClick = { onModeChange(mode) }
            )
        }
    }
}

@Composable
fun EditToolButton(
    icon: androidx.compose.material.icons.filled.FilterAlt,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF7C3AED) else Color(0xFF334155),
        animationSpec = tween(300),
        label = "toolButtonBg"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .height(44.dp)
            .border(
                width = if (isActive) 2.dp else 0.dp,
                color = if (isActive) Color(0xFFA78BFA) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(icon, contentDescription = null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun FiltersPanel(
    filters: List<Pair<String, Color>>,
    currentTint: Color,
    onFilterChange: (Color) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(filters) { (label, tint) ->
            FilterCard(
                label = label,
                isActive = currentTint == tint,
                onClick = { onFilterChange(tint) }
            )
        }
    }
}

@Composable
fun FilterCard(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .size(90.dp)
            .clickable(onClick = onClick)
            .border(
                width = if (isActive) 3.dp else 0.dp,
                color = if (isActive) Color(0xFF7C3AED) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(color = label.hashCode().toLong().toInt().let { Color(0xFF000000L or (it.toLong() and 0xFFFFFF)) }, shape = RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(8.dp))
                Text(label, fontSize = 10.sp, color = Color(0xFFCBD5E1), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun AdjustmentsPanel(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    rotation: Float,
    zoom: Float,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onRotationChange: (Float) -> Unit,
    onZoomChange: (Float) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SliderCard("Brightness", brightness, 0.5f, 1.8f, onBrightnessChange)
        }
        item {
            SliderCard("Contrast", contrast, 0.5f, 2f, onContrastChange)
        }
        item {
            SliderCard("Saturation", saturation, 0.5f, 2f, onSaturationChange)
        }
        item {
            SliderCard("Rotation", rotation, -45f, 45f, onRotationChange)
        }
        item {
            SliderCard("Zoom", zoom, 0.7f, 2.5f, onZoomChange)
        }
    }
}

@Composable
fun SliderCard(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${(value * 100).toInt()}%", color = Color(0xFF7C3AED), fontSize = 12.sp)
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = min..max,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BeautyPanel(
    blur: Float,
    beauty: Float,
    beautyFilters: List<Pair<String, Float>>,
    onBlurChange: (Float) -> Unit,
    onBeautyChange: (Float) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SliderCard("Blur", blur, 0f, 10f, onBlurChange)
        }
        item {
            SliderCard("Beauty", beauty, 0f, 1f, onBeautyChange)
        }
        item {
            Text("Beauty Presets", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(beautyFilters) { (label, value) ->
                    ElevatedButton(
                        onClick = { onBeautyChange(value) },
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (beauty == value) Color(0xFF7C3AED) else Color(0xFF334155)
                        )
                    ) {
                        Text(label, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TextPanel(onTextAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var text by remember { mutableStateOf("") }
        var textColor by remember { mutableStateOf(Color.White) }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Enter text") },
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(Color.White, Color.Red, Color.Yellow, Color.Cyan, Color(0xFF7C3AED)).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { textColor = color }
                        .border(2.dp, if (textColor == color) Color.White else Color.Transparent, CircleShape)
                )
            }
        }

        Button(
            onClick = onTextAdd,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Text")
        }
    }
}

@Composable
fun StickersPanel(
    stickerList: List<String>,
    onStickerAdd: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stickerList) { emoji ->
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
                    .clickable { onStickerAdd(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 48.sp)
            }
        }
    }
}

private fun exportEditedPhoto(
    context: Context,
    sourceUri: Uri,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    blur: Float,
    beauty: Float,
    tint: Color,
    textOverlays: List<TextOverlay>,
    stickers: List<StickerOverlay>,
): File? {
    val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
    val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

    val outputBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(outputBitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val colorMatrix = ColorMatrix().apply {
        set(createAdjustMatrix(brightness, contrast, saturation))
    }
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(originalBitmap, 0f, 0f, paint)

    val fileName = "photoeditpro_${System.currentTimeMillis()}.jpg"
    val imageOut: OutputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoEditPro")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null
        context.contentResolver.openOutputStream(uri) ?: return null
    } else {
        val folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val appFolder = File(folder, "PhotoEditPro")
        if (!appFolder.exists()) appFolder.mkdirs()
        val targetFile = File(appFolder, fileName)
        FileOutputStream(targetFile)
    }

    outputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, imageOut)
    imageOut.flush()
    imageOut.close()
    return File(fileName)
}

private fun createAdjustMatrix(brightness: Float, contrast: Float, saturation: Float): FloatArray {
    val brightnessFactor = brightness
    val contrastFactor = contrast
    val saturationFactor = saturation

    val colorMatrix = ColorMatrix()
    colorMatrix.setScale(brightnessFactor, brightnessFactor, brightnessFactor, 1f)
    colorMatrix.postConcat(ColorMatrix().also {
        it.setScale(contrastFactor, contrastFactor, contrastFactor, 1f)
    })
    colorMatrix.postConcat(ColorMatrix().also {
        val array = FloatArray(20)
        it.get(array)
        array[0] = 0.213f * saturationFactor + 0.787f
        array[1] = 0.715f * saturationFactor - 0.715f
        array[2] = 0.072f * saturationFactor - 0.072f
        array[5] = 0.213f * saturationFactor - 0.213f
        array[6] = 0.715f * saturationFactor + 0.285f
        array[7] = 0.072f * saturationFactor - 0.072f
        array[10] = 0.213f * saturationFactor - 0.213f
        array[11] = 0.715f * saturationFactor - 0.715f
        array[12] = 0.072f * saturationFactor + 0.928f
        it.set(array)
    })

    val combined = FloatArray(20)
    colorMatrix.get(combined)
    return combined
}

@SuppressLint("ComposableNaming")
@Composable
fun rememberLauncherForActivityResult(
    contract: ActivityResultContracts.PickVisualMedia,
    onResult: (Uri?) -> Unit,
): androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest> {
    val context = LocalContext.current
    val activity = (context as? ComponentActivity) ?: return remember {
        object : androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>() {
            override fun launch(input: PickVisualMediaRequest?, options: androidx.activity.result.ActivityOptionsCompat?) {}
            override fun unregister() {}
            override fun getContract() = contract
        }
    }

    return remember(activity, contract) {
        activity.registerForActivityResult(contract, onResult)
    }
}

@Composable
fun PhotoEditProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
