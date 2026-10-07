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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExportNotes
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.roundToInt

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
)

@Composable
fun PhotoEditorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var brightness by remember { mutableFloatStateOf(1f) }
    var contrast by remember { mutableFloatStateOf(1.1f) }
    var saturation by remember { mutableFloatStateOf(1.2f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var customText by remember { mutableStateOf("PhotoPro") }
    var filterTint by remember { mutableStateOf(Color(0xFF7C3AED)) }
    var textOverlays by remember { mutableStateOf(listOf<TextOverlay>()) }
    var stickerOverlays by remember { mutableStateOf(listOf<StickerOverlay>()) }

    val filters = listOf(
        "Vivid" to Color(0xFF7C3AED),
        "Warm" to Color(0xFFF59E0B),
        "Rose" to Color(0xFFEC4899),
        "Mint" to Color(0xFF10B981),
        "Sky" to Color(0xFF3B82F6),
        "No Tint" to Color(0x00000000)
    )

    val stickerList = listOf("✨", "🔥", "❤️", "⭐", "🌈", "🎨")

    val pickPhoto = remember {
        androidx.activity.result.ActivityResultLauncher { }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImage = uri }
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PhotoEditPro",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "CapCut-inspired photo editor",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Open")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF111827))
                .border(1.dp, Color(0xFF374151), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (selectedImage != null) {
                AsyncImage(
                    model = selectedImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = rotation
                            scaleX = zoom
                            scaleY = zoom
                        },
                    colorFilter = ColorFilter.tint(
                        color = filterTint.copy(alpha = if (filterTint == Color(0x00000000)) 0f else 0.32f),
                        blendMode = BlendMode.SoftLight
                    )
                )

                textOverlays.forEach { overlay ->
                    Text(
                        text = overlay.text,
                        color = overlay.color,
                        fontSize = overlay.size.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .offset(
                                x = (overlay.x * 300).dp,
                                y = (overlay.y * 260).dp
                            )
                            .background(Color.Transparent)
                    )
                }

                stickerOverlays.forEach { sticker ->
                    Text(
                        text = sticker.emoji,
                        fontSize = 32.sp,
                        modifier = Modifier
                            .offset(
                                x = (sticker.x * 260).dp,
                                y = (sticker.y * 220).dp
                            )
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = Color(0xFF94A3B8)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Select a photo to begin editing",
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Adjustments", style = MaterialTheme.typography.titleMedium)
                }

                AdjustmentSlider("Brightness", brightness, 0.5f, 1.8f) { brightness = it }
                AdjustmentSlider("Contrast", contrast, 0.5f, 2f) { contrast = it }
                AdjustmentSlider("Saturation", saturation, 0.5f, 2f) { saturation = it }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { rotation -= 15f },
                        enabled = selectedImage != null
                    ) {
                        Icon(Icons.Default.RotateLeft, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Rotate")
                    }

                    Button(
                        onClick = { rotation += 15f },
                        enabled = selectedImage != null
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Flip")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { zoom = (zoom - 0.1f).coerceAtLeast(0.7f) },
                        enabled = selectedImage != null
                    ) {
                        Text("- Zoom")
                    }
                    Button(
                        onClick = { zoom = (zoom + 0.1f).coerceAtMost(2f) },
                        enabled = selectedImage != null
                    ) {
                        Text("+ Zoom")
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Filters", style = MaterialTheme.typography.titleMedium)
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters) { (label, tint) ->
                        FilterChip(
                            selected = filterTint == tint,
                            onClick = { filterTint = tint },
                            label = {
                                Text(label)
                            }
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Text & stickers", style = MaterialTheme.typography.titleMedium)
                }

                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Add caption") }
                )

                Button(
                    onClick = {
                        if (customText.isNotBlank()) {
                            textOverlays = textOverlays + TextOverlay(
                                id = System.currentTimeMillis(),
                                text = customText.trim(),
                                color = Color.White,
                                size = 32f,
                                x = 0.5f,
                                y = 0.5f
                            )
                            customText = ""
                        }
                    },
                    enabled = selectedImage != null
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add text")
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(stickerList) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0))
                                .clickable {
                                    stickerOverlays = stickerOverlays + StickerOverlay(
                                        id = System.currentTimeMillis(),
                                        emoji = emoji,
                                        x = 0.5f,
                                        y = 0.5f
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 26.sp)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    brightness = 1f
                    contrast = 1.1f
                    saturation = 1.2f
                    rotation = 0f
                    zoom = 1f
                    filterTint = Color(0xFF7C3AED)
                    textOverlays = emptyList()
                    stickerOverlays = emptyList()
                }
            ) {
                Text("Reset")
            }

            Button(
                onClick = {
                    if (selectedImage == null) {
                        Toast.makeText(context, "Choose a photo first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        val savedFile = withContext(Dispatchers.IO) {
                            exportEditedPhoto(
                                context = context,
                                sourceUri = selectedImage!!,
                                brightness = brightness,
                                contrast = contrast,
                                saturation = saturation,
                                tint = filterTint,
                                textOverlays = textOverlays,
                                stickers = stickerOverlays,
                            )
                        }

                        Toast.makeText(
                            context,
                            if (savedFile != null) "Saved to ${savedFile.absolutePath}" else "Export failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            ) {
                Icon(Icons.Default.ExportNotes, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export")
            }
        }
    }
}

@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text(value.toString().take(4))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
        )
    }
}

private fun exportEditedPhoto(
    context: Context,
    sourceUri: Uri,
    brightness: Float,
    contrast: Float,
    saturation: Float,
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

    val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tint.copy(alpha = 0.25f).value.toLong().toInt() // placeholder to avoid alpha bug
    }
    tintPaint.color = tint.copy(alpha = 0.25f).value.toLong().toInt()
    canvas.drawRoundRect(
        RectF(0f, 0f, outputBitmap.width.toFloat(), outputBitmap.height.toFloat()),
        32f,
        32f,
        tintPaint
    )

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        typeface = Typeface.DEFAULT_BOLD
    }

    textOverlays.forEach { overlay ->
        textPaint.color = overlay.color.value.toLong().toInt()
        textPaint.textSize = overlay.size * 2f
        canvas.drawText(
            overlay.text,
            outputBitmap.width * overlay.x,
            outputBitmap.height * overlay.y,
            textPaint
        )
    }

    stickers.forEach { sticker ->
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textSize = 70f
        canvas.drawText(sticker.emoji, outputBitmap.width * sticker.x, outputBitmap.height * sticker.y, textPaint)
    }

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

    val brightnessMatrix = floatArrayOf(
        brightnessFactor, 0f, 0f, 0f, 0f,
        0f, brightnessFactor, 0f, 0f, 0f,
        0f, 0f, brightnessFactor, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    val contrastMatrix = floatArrayOf(
        contrastFactor, 0f, 0f, 0f, 0f,
        0f, contrastFactor, 0f, 0f, 0f,
        0f, 0f, contrastFactor, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    val saturationMatrix = floatArrayOf(
        0.213f * saturationFactor + 0.787f, 0.715f * saturationFactor - 0.715f, 0.072f * saturationFactor - 0.072f, 0f, 0f,
        0.213f * saturationFactor - 0.213f, 0.715f * saturationFactor + 0.285f, 0.072f * saturationFactor - 0.072f, 0f, 0f,
        0.213f * saturationFactor - 0.213f, 0.715f * saturationFactor - 0.715f, 0.072f * saturationFactor + 0.928f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    val combined = FloatArray(20)
    val b = FloatArray(20)
    val c = FloatArray(20)
    val s = FloatArray(20)
    System.arraycopy(brightnessMatrix, 0, b, 0, 20)
    System.arraycopy(contrastMatrix, 0, c, 0, 20)
    System.arraycopy(saturationMatrix, 0, s, 0, 20)

    val colorMatrix = ColorMatrix()
    colorMatrix.set(b)
    colorMatrix.postConcat(ColorMatrix(c))
    colorMatrix.postConcat(ColorMatrix(s))
    colorMatrix.get(combined)
    return combined
}

@SuppressLint("ComposableNaming")
@Composable
private fun PhotoEditProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
