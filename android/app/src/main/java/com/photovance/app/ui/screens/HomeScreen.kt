package com.photovance.app.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.photovance.app.network.*
import com.photovance.app.ui.components.*
import com.photovance.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

val MAGIC_MODIFIER_CHIPS = listOf(
    "🌅 Golden Hour Sunset",
    "📸 8K Studio Flash",
    "🧵 Haute Couture Silk",
    "✨ Glittering Sequins",
    "🎬 Dramatic Cinematic Lighting",
    "🏎️ Luxury Sports Car Background",
    "🌧️ Moody Rain Reflections"
)

val CATEGORY_TABS = listOf("All", "Red Carpet", "Vacation", "Street Style", "Luxury Glamour", "Casual")

data class RecentCreation(val imageUrl: String, val prompt: String, val elapsed: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedMode by remember { mutableStateOf("face_studio") }
    var selectedModelKey by remember { mutableStateOf("flux_klein") }
    var selectedAspectRatio by remember { mutableStateOf("portrait") }
    var selectedTarget by remember { mutableStateOf("dress") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedPresetId by remember { mutableStateOf("monaco_yacht") }

    var promptText by remember {
        mutableStateOf("wearing a glamorous backless gold chainmail dress, standing on the teak deck of a luxury superyacht in Monaco during golden hour sunset, cinematic lighting, 8k photo")
    }
    var negativePromptText by remember { mutableStateOf("") }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var isHoldPeekOriginal by remember { mutableStateOf(false) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedImageId by remember { mutableStateOf<String?>(null) }
    var beforeImageUrl by remember { mutableStateOf<String?>(null) }
    var resultImageUrl by remember { mutableStateOf<String?>(null) }

    var isUploading by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isBackendOnline by remember { mutableStateOf(false) }
    var serverStatusMessage by remember { mutableStateOf("GPU Online · 0.0s") }

    val recentCreations = remember { mutableStateListOf<RecentCreation>() }

    // Check backend status on launch
    LaunchedEffect(Unit) {
        try {
            val status = ApiClient.api.getStatus()
            isBackendOnline = status.ready
            serverStatusMessage = if (status.ready) "GPU Online · Ready" else "Backend initializing"
            if (negativePromptText.isEmpty() && status.defaultNegativePrompt != null) {
                negativePromptText = status.defaultNegativePrompt
            }
        } catch (e: Exception) {
            isBackendOnline = false
            serverStatusMessage = "Backend Offline"
        }
    }

    // Load sample portrait helper
    fun loadSamplePhoto() {
        coroutineScope.launch {
            isUploading = true
            try {
                val sample = ApiClient.api.getSample()
                uploadedImageId = sample.imageId
                beforeImageUrl = ApiClient.getFullUrl(sample.imageUrl)
                selectedImageUri = null
                resultImageUrl = null
                Toast.makeText(context, "Demo model portrait loaded!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load demo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isUploading = false
            }
        }
    }

    // Photo picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            resultImageUrl = null
            isUploading = true
            coroutineScope.launch {
                try {
                    uploadSelectedPhoto(context, uri) { id, url ->
                        uploadedImageId = id
                        beforeImageUrl = url
                    }
                } finally {
                    isUploading = false
                }
            }
        }
    }

    // Timer while generating
    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            elapsedSeconds = 0
            while (isGenerating) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    val selectedModelName = when (selectedModelKey) {
        "flux_klein" -> "Flux.2 Klein 4B Turbo (4-Step DiT)"
        "flux_klein_ultra" -> "Flux.2 Klein 4B Master (FP16)"
        "realvis_xl" -> "RealVisXL v5.0 (SDXL)"
        "serenity" -> "Serenity v2.1 (SD1.5)"
        else -> selectedModelKey
    }

    Scaffold(
        containerColor = BgCanvas,
        topBar = {
            // Glassmorphic Top Studio Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand Logo & Pro Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PHOTOVANCE",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextMain,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentPink,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(GoldGradient, RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                // Live Connection Pill & Settings Gear
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // GPU Online Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xCC0F1626))
                            .border(1.dp, if (isBackendOnline) Success.copy(alpha = 0.5f) else Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                            .clickable { showSettingsSheet = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isBackendOnline) Success else Color.Red, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = serverStatusMessage,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMain
                        )
                    }

                    // Settings Icon
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xCC0F1626), CircleShape)
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Studio Settings",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Mode Switcher (Face Studio vs In-Place Swap)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC0F1626), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedMode == "face_studio") HeroGradient else SolidColor(Color.Transparent))
                        .clickable { selectedMode = "face_studio" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🌟 Face Studio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (selectedMode == "face_studio") Color.White else TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedMode == "in_place") HeroGradient else SolidColor(Color.Transparent))
                        .clickable { selectedMode = "in_place" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👗 In-Place Swap",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (selectedMode == "in_place") Color.White else TextMuted
                    )
                }
            }

            // 2. Hero Studio Canvas (Portrait Frame)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.5.dp, if (isGenerating) NeonCyan else NeonViolet.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                    .background(BgCard)
            ) {
                val currentDisplayImage = beforeImageUrl ?: selectedImageUri

                if (currentDisplayImage != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(currentDisplayImage)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Portrait",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient Bottom Shadow on canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xE607090E))
                                )
                            )
                    )

                    // Top-Left Floating Badge: Face Locked
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopStart)
                            .background(Color(0xDD07090E), RoundedCornerShape(14.dp))
                            .border(1.dp, Success.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Success, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Face Identity Locked 100%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    // Top-Right Retake Button
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xDD07090E))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                            .clickable { galleryLauncher.launch("image/*") }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retake", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Change", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Bottom Canvas Chip: Engine
                    Text(
                        text = "⚡ Engine: $selectedModelName",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD1D5DB),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 14.dp, bottom = 12.dp)
                    )
                } else if (isUploading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = AccentPink, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Uploading & optimizing portrait...", fontSize = 13.sp, color = TextMain, fontWeight = FontWeight.Medium)
                    }
                } else {
                    // Empty State: Add Portrait Callout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(HeroGradient, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (selectedMode == "face_studio") "Add Your Reference Portrait" else "Add Photo to Swap Outfit",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                        Text(
                            text = "Selfie or full-body portrait with clear face",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Choose Photo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = { loadSamplePhoto() },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AccentPink)
                            ) {
                                Text("⚡ Use Demo Model", fontSize = 12.sp, color = AccentPink, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Generative Neural Scan Overlay while rendering
                if (isGenerating) {
                    NeuralScanOverlay(
                        elapsedSeconds = elapsedSeconds,
                        modelName = selectedModelName,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 3. Visual Style Inspiration Reel (The Star Consumer Feature!)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Style Inspiration Reel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    Text(
                        text = "Tap to Apply",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentPink
                    )
                }

                // Category Filter Tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CATEGORY_TABS) { cat ->
                        val isSelected = (selectedCategory == cat)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Accent else Color(0xCC0F1626))
                                .border(1.dp, if (isSelected) Accent else Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextMuted
                            )
                        }
                    }
                }

                // Horizontal Carousel of Visual Style Cards
                val filteredPresets = if (selectedCategory == "All") {
                    VISUAL_STYLE_PRESETS
                } else {
                    VISUAL_STYLE_PRESETS.filter { it.category == selectedCategory }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredPresets) { preset ->
                        StyleInspirationCard(
                            preset = preset,
                            isSelected = (selectedPresetId == preset.id),
                            onClick = {
                                selectedPresetId = preset.id
                                promptText = preset.prompt
                                selectedMode = preset.mode
                                Toast.makeText(context, "Applied: ${preset.title}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // 4. Prompt Studio Bar & Magic Modifier Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Prompt & Magic Modifiers",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xCC0F1626),
                        unfocusedContainerColor = Color(0xCC0F1626),
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Color(0x2EFFFFFF),
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain
                    ),
                    minLines = 2,
                    maxLines = 4
                )

                // Quick modifier chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MAGIC_MODIFIER_CHIPS) { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x1AFFFFFF))
                                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                .clickable {
                                    val addition = chip.replace(Regex("^[\\p{So}\\p{Sk}]+\\s*"), "")
                                    promptText = if (promptText.isBlank()) addition else "$promptText, $addition"
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = chip, fontSize = 11.sp, color = Color(0xFFE2E8F0), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // 5. Hero Generate Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp), spotColor = AccentPink)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isGenerating) SolidColor(Color(0xFF22173B)) else HeroGradient)
                    .border(1.5.dp, if (isGenerating) NeonCyan else Color(0x44FFFFFF), RoundedCornerShape(20.dp))
                    .clickable(enabled = !isGenerating) {
                        if (isUploading) {
                            Toast.makeText(context, "Please wait, photo is still uploading...", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }
                        if (uploadedImageId == null) {
                            Toast.makeText(context, "Please choose a portrait first (or tap 'Use Demo Model')!", Toast.LENGTH_LONG).show()
                            return@clickable
                        }
                        if (promptText.isBlank()) {
                            Toast.makeText(context, "Please enter an outfit prompt or tap a preset card above!", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }
                        isGenerating = true
                        coroutineScope.launch {
                            try {
                                val req = ChangeDressRequest(
                                    imageId = uploadedImageId!!,
                                    prompt = promptText,
                                    negativePrompt = negativePromptText,
                                    modelKey = selectedModelKey,
                                    mode = selectedMode,
                                    aspectRatio = selectedAspectRatio,
                                    target = selectedTarget,
                                    keepFace = true
                                )
                                val res = ApiClient.api.changeDress(req)
                                if (res.success && res.resultUrl != null) {
                                    val fullResult = ApiClient.getFullUrl(res.resultUrl)
                                    resultImageUrl = fullResult
                                    recentCreations.add(0, RecentCreation(fullResult, promptText, res.elapsed ?: 0.0))
                                    Toast.makeText(context, "✨ Render complete in ${res.elapsed}s!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, res.error ?: "Generation failed", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isGenerating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Rendering with Photovance AI (${elapsedSeconds}s)...", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✨ GENERATE WITH PHOTOVANCE AI", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 0.5.sp)
                        Text("⚡ 4-step DiT render · ~12s with 100% Face Lock", fontSize = 10.sp, color = Color(0xFFFCE7F3))
                    }
                }
            }

            // 6. Result & Comparison Suite
            if (resultImageUrl != null && beforeImageUrl != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCC0F1626))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Result: Interactive Before & After", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMain)
                        Text("Drag slider to compare", fontSize = 11.sp, color = TextMuted)
                    }

                    BeforeAfterSlider(
                        beforeUrl = beforeImageUrl!!,
                        afterUrl = resultImageUrl!!,
                        isHoldPeekOriginal = isHoldPeekOriginal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                    )

                    // Action toolbar (Save, Share, Hold to Compare)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    saveImageToGallery(context, resultImageUrl!!)
                                }
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF))
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save HD", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { resultImageUrl?.let { shareImage(context, it) } },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Hold to Compare Button
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isHoldPeekOriginal) Success else Accent)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isHoldPeekOriginal = true
                                            tryAwaitRelease()
                                            isHoldPeekOriginal = false
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isHoldPeekOriginal) "Original Photo" else "Hold to Compare",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 7. Recent AI Generations Reel (Shelf)
            if (recentCreations.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your Recent Creations", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recentCreations) { item ->
                            Box(
                                modifier = Modifier
                                    .size(100.dp, 130.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                                    .clickable { resultImageUrl = item.imageUrl }
                            ) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // 8. Studio Engine Settings Bottom Sheet
    if (showSettingsSheet) {
        var tempUrl by remember { mutableStateOf(ApiClient.baseUrl) }
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = Color(0xFF0F1626),
            contentColor = TextMain
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "⚙️ Studio Engine Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Model Selection
                Text("AI Diffusion Architecture:", fontSize = 13.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val models = listOf(
                        "flux_klein" to "🌟 Flux.2 Klein 4B Turbo (Fast · 4-step · Unrestricted)",
                        "flux_klein_ultra" to "👑 Flux.2 Klein 4B Master (FP16 · High Fidelity)",
                        "realvis_xl" to "📸 RealVisXL v5.0 (SDXL Photography)",
                        "serenity" to "👗 Serenity v2.1 (SD1.5 Photography)"
                    )
                    models.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedModelKey == key) Accent.copy(alpha = 0.2f) else Color(0x0AFFFFFF))
                                .border(1.dp, if (selectedModelKey == key) Accent else Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                                .clickable { selectedModelKey = key }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedModelKey == key),
                                onClick = { selectedModelKey = key },
                                colors = RadioButtonDefaults.colors(selectedColor = Accent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextMain)
                        }
                    }
                }

                // Aspect Ratio Selector
                Text("Aspect Ratio:", fontSize = 13.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("portrait" to "4:5 Portrait", "story" to "9:16 Story", "square" to "1:1 Square").forEach { (ratio, label) ->
                        val isSelected = (selectedAspectRatio == ratio)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Accent else Color(0x1AFFFFFF))
                                .clickable { selectedAspectRatio = ratio }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Server Connection URL
                Text("Backend Host (Wi-Fi IP / Cloudflare Tunnel):", fontSize = 13.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = tempUrl,
                    onValueChange = { tempUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xCC07090E),
                        unfocusedContainerColor = Color(0xCC07090E)
                    )
                )

                Button(
                    onClick = {
                        ApiClient.saveBaseUrl(context, tempUrl)
                        showSettingsSheet = false
                        coroutineScope.launch {
                            try {
                                val res = ApiClient.api.getStatus()
                                isBackendOnline = res.ready
                                serverStatusMessage = if (res.ready) "GPU Online · Ready" else "Backend connected"
                                Toast.makeText(context, "Connected successfully!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                isBackendOnline = false
                                serverStatusMessage = "Connection Failed"
                                Toast.makeText(context, "Failed to connect", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Text("Save & Connect", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// Utility: Upload photo as Base64 data URI (optimized downscaling to prevent OOM)
private suspend fun uploadSelectedPhoto(
    context: Context,
    uri: Uri,
    onSuccess: (id: String, url: String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val maxDim = 1536
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            var sampleSize = 1
            if (origW > maxDim || origH > maxDim) {
                val halfW = origW / 2
                val halfH = origH / 2
                while ((halfW / sampleSize) >= maxDim && (halfH / sampleSize) >= maxDim) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: throw IllegalStateException("Could not load photo from device")

            val currentMax = maxOf(bitmap.width, bitmap.height)
            val finalBitmap = if (currentMax > maxDim) {
                val scale = maxDim.toFloat() / currentMax
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else {
                bitmap
            }

            val baos = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 92, baos)
            val jpegBytes = baos.toByteArray()
            val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
            val dataUri = "data:image/jpeg;base64,$base64"

            val res = ApiClient.api.uploadImage(UploadRequest(data = dataUri))
            if (res.imageId != null && res.imageUrl != null) {
                withContext(Dispatchers.Main) {
                    onSuccess(res.imageId, ApiClient.getFullUrl(res.imageUrl))
                    Toast.makeText(context, "Portrait loaded successfully!", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, res.error ?: "Upload failed", Toast.LENGTH_LONG).show()
                }
            }
        } catch (t: Throwable) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Upload error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

// Utility: Save Image to MediaStore
private suspend fun saveImageToGallery(context: Context, imageUrl: String) {
    withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL(imageUrl)
            val bitmap = BitmapFactory.decodeStream(url.openStream())
            val filename = "Photovance_${System.currentTimeMillis()}.png"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotovanceAI")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = context.contentResolver.insert(collection, values)
            if (itemUri != null) {
                context.contentResolver.openOutputStream(itemUri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(itemUri, values, null, null)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Save failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// Utility: Share Image Intent
private fun shareImage(context: Context, imageUrl: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Created with Photovance AI")
        putExtra(Intent.EXTRA_TEXT, "Check out my new outfit created with Photovance AI!\n$imageUrl")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share with"))
}
