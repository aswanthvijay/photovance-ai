package com.photovance.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.photovance.app.ui.Creation
import com.photovance.app.ui.StudioViewModel
import com.photovance.app.ui.components.*

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(studio: StudioViewModel, onSettings: () -> Unit) {
    val scroll = rememberScrollState()
    LaunchedEffect(studio.step, studio.error, studio.isGenerating) { scroll.scrollTo(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) studio.selectPhoto(uri)
    }
    val choosePhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val context = LocalContext.current
    val savePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) studio.save() else studio.message = "Allow storage access to save photos, or use Share."
    }
    val savePhoto = {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            savePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else studio.save()
    }

    if (studio.showPaywall) {
        PaywallSheet(
            onDismiss = studio::dismissPaywall,
            onSubscribe = studio::subscribeVip,
            onRestore = studio::restorePurchases
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val engineMode by studio.engineMode.collectAsState()
            val isVip by studio.isVip.collectAsState()
            val credits by studio.remainingCredits.collectAsState()
            Column {
                StudioHeader(
                    onSettings = onSettings,
                    onBack = if (studio.step > 0 && !studio.isGenerating) ({ studio.goToStep(studio.step - 1) }) else null,
                    vipBadgeTitle = if (isVip) "👑 VIP" else "✨ $credits Free",
                    onTapVip = { studio.openPaywall() },
                    engineModeTitle = if (studio.hybridRouter.deviceSpec.supportsLocalNpuStudio) {
                        if (engineMode == com.photovance.app.engine.StudioEngineMode.LOCAL_OFFLINE) "⚡ Local NPU" else "☁️ Cloud 8K"
                    } else "☁️ Cloud 8K",
                    onToggleEngine = { studio.hybridRouter.toggleMode() }
                )
                StudioSteps(studio.step, studio.hasPhoto, studio.result != null, !studio.isGenerating, studio::goToStep)
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (studio.step) {
                        0 -> StudioButton(if (studio.hasPhoto) "Continue to styling" else "Choose a photo",
                            onClick = if (studio.hasPhoto) ({ studio.goToStep(1) }) else choosePhoto,
                            icon = if (studio.hasPhoto) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.AddPhotoAlternate)
                        1 -> StudioButton(if (studio.isGenerating) "Creating your look · ${studio.elapsedSeconds}s" else "Generate photo",
                            onClick = studio::generate, icon = Icons.Default.AutoAwesome,
                            enabled = studio.prompt.isNotBlank(), loading = studio.isGenerating)
                        2 -> StudioButton(if (studio.isExporting) "Preparing photo…" else "Save photo",
                            onClick = savePhoto, icon = Icons.Default.Download, loading = studio.isExporting)
                    }
                    if (studio.step == 1) Text("Your photo is sent to your connected studio when you generate.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(scroll)
            .padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)) {
            studio.error?.let { ErrorNotice(it, onSettings) }
            if (studio.isGenerating) GenerationProgress(studio.progressLabel, studio.elapsedSeconds)
            when (studio.step) {
                0 -> PhotoScreen(studio.photoModel, studio.hasPhoto, choosePhoto, studio::selectSample)
                1 -> StyleScreen(studio, choosePhoto, onSettings)
                2 -> studio.result?.let { ResultScreen(it, studio.isExporting, studio, studio::share, studio::shareStory,
                    { studio.goToStep(if (studio.hasPhoto) 1 else 0) }) }
            }
            if (studio.step == 0) {
                ConnectionStatus(studio.online, studio.checkingConnection, onSettings)
                if (studio.recent.isNotEmpty()) RecentCreations(studio.recent, studio::openCreation)
            }
        }
    }
}

@Composable
fun PhotoScreen(photo: Any?, hasPhoto: Boolean, onChoose: () -> Unit, onSample: () -> Unit) {
    PageHeading("01 / Your canvas", "Start with your photo.",
        "A clear, well-lit portrait is a beautiful place to begin.")
    Box(Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 420.dp).aspectRatio(0.88f)
        .clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
        .clickable(onClick = onChoose), contentAlignment = Alignment.Center) {
        if (hasPhoto) AsyncImage(model = photo, contentDescription = "Your selected photo. Tap to change.",
            contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        else Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(76.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text("Your next look starts here", style = MaterialTheme.typography.titleMedium)
            Text("Choose a selfie or full-body photo.\nKeep your face and outfit in view.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
    if (hasPhoto) StudioOutlineButton("Change photo", onClick = onChoose, icon = Icons.Default.PhotoLibrary)
    TextButton(onClick = onSample, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.AutoAwesome, null, Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (hasPhoto) "Use the sample portrait instead" else "Just exploring? Try a sample")
    }
}

@Composable
private fun ConnectionStatus(online: Boolean, checking: Boolean, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(if (online) MaterialTheme.colorScheme.primary else
            MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
        Spacer(Modifier.width(9.dp))
        Text(if (checking) "Connecting to your studio…" else if (online) "Your studio is ready" else "Connect your studio to generate",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        TextButton(onClick = onSettings) { Text("Settings") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StyleScreen(studio: StudioViewModel, onChoose: () -> Unit, onOptions: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PageHeading("02 / Make it yours", "Find your next look.", "Choose a direction. Add a little imagination.")
        Surface(onClick = onChoose, enabled = !studio.isGenerating, shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AsyncImage(studio.photoModel, "Your selected photo", contentScale = ContentScale.Crop,
                    modifier = Modifier.size(58.dp, 64.dp).clip(MaterialTheme.shapes.small))
                Column(Modifier.weight(1f)) {
                    Text("Your photo", style = MaterialTheme.typography.titleSmall)
                    Text("Ready for a new perspective", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, "Change photo")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("face_studio" to "Face Studio", "in_place" to "Outfit Swap", "wardrobe" to "Virtual Wardrobe 👗").forEach { (key, title) ->
                    FilterChip(selected = studio.mode == key, onClick = { studio.mode = key },
                        enabled = !studio.isGenerating, label = { Text(title) }, modifier = Modifier.weight(1f))
                }
            }
            Text(
                when (studio.mode) {
                    "face_studio" -> "Reimagine your portrait with a new outfit and scene."
                    "wardrobe" -> "Upload any garment to drape directly onto your portrait."
                    else -> "Change your outfit while keeping the original scene."
                },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (studio.mode == "wardrobe") {
            VirtualWardrobePicker(
                garmentUri = studio.garmentUri,
                onSelectGarment = studio::setGarment,
                onApplyTag = { tag ->
                    studio.editPrompt("${studio.prompt.trimEnd().trimEnd(',')}, wearing $tag".trimStart(',', ' '))
                }
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("The inspiration edit", style = MaterialTheme.typography.titleLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Red Carpet", "Vacation", "Street Style", "Luxury Glamour", "Casual")) { category ->
                    FilterChip(selected = studio.category == category, onClick = { studio.category = category },
                        enabled = !studio.isGenerating, label = { Text(category) })
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(VISUAL_STYLE_PRESETS.filter { studio.category == "All" || it.category == studio.category }, key = { it.id }) { preset ->
                    StyleInspirationCard(preset, studio.selectedPresetId == preset.id,
                        onClick = { if (!studio.isGenerating) studio.selectPreset(preset.id) })
                }
            }
        }
        OutlinedTextField(value = studio.prompt, onValueChange = studio::editPrompt,
            label = { Text("Describe your look") }, placeholder = { Text("A silk dress in warm evening light…") },
            enabled = !studio.isGenerating, minLines = 3, maxLines = 6,
            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Golden hour", "Studio lighting", "Soft film", "City at night")) { modifier ->
                SuggestionChip(onClick = { studio.editPrompt("${studio.prompt.trimEnd().trimEnd(',')}, $modifier".trimStart(',', ' ')) },
                    enabled = !studio.isGenerating, label = { Text("+ $modifier") })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Photo format", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("portrait" to "4:5 Portrait", "story" to "9:16 Story", "square" to "1:1 Square").forEach { (key, label) ->
                    FilterChip(selected = studio.aspectRatio == key, onClick = { studio.aspectRatio = key },
                        enabled = !studio.isGenerating, label = { Text(label) })
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Studio variations", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "1 Master Photo", 4 to "4 Variations ✨").forEach { (count, label) ->
                    FilterChip(
                        selected = studio.variationsCount == count,
                        onClick = { studio.variationsCount = count },
                        enabled = !studio.isGenerating,
                        label = { Text(label) }
                    )
                }
            }
        }
        if (studio.mode == "in_place") Column {
            Text("What would you like to change?", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("dress" to "Full outfit", "upper" to "Top", "lower" to "Bottom").forEach { (key, title) ->
                    FilterChip(selected = studio.target == key, onClick = { studio.target = key },
                        enabled = !studio.isGenerating, label = { Text(title) })
                }
            }
        }
        TextButton(onClick = onOptions, enabled = !studio.isGenerating) {
            Icon(Icons.Default.Tune, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("More options")
        }
    }
}

@Composable
private fun GenerationProgress(label: String, elapsed: Int) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                Text(label, style = MaterialTheme.typography.titleSmall)
            }
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("${elapsed}s elapsed · This can take a little while. Keep the app open.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun ResultScreen(
    creation: Creation,
    exporting: Boolean,
    studio: StudioViewModel,
    onShare: () -> Unit,
    onShareStory: () -> Unit,
    onAnother: () -> Unit
) {
    var showOriginal by rememberSaveable(creation.imageUrl) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PageHeading("03 / The reveal", "Your new look.", "Same you. A whole new perspective.")
        BeforeAfterSlider(creation.beforeUrl, creation.imageUrl,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.8f), isHoldPeekOriginal = showOriginal)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Drag to compare", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { showOriginal = !showOriginal }) {
                Text(if (showOriginal) "Show comparison" else "View original")
            }
        }

        if (creation.variationUrls.size > 1) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Studio Variations (${creation.variationUrls.size})", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(creation.variationUrls) { idx, url ->
                        val isSelected = url == creation.imageUrl
                        Box(
                            modifier = Modifier
                                .size(74.dp, 94.dp)
                                .clip(MaterialTheme.shapes.small)
                                .border(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) Color(0xFFFFB800) else MaterialTheme.colorScheme.outlineVariant,
                                    MaterialTheme.shapes.small
                                )
                                .clickable { studio.selectVariation(idx) }
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Variation ${idx + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .background(Color(0xFFFFB800))
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Active", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StudioOutlineButton("Share photo", onClick = onShare, icon = Icons.Default.Share, enabled = !exporting, modifier = Modifier.weight(1f))
            StudioOutlineButton("Story (9:16)", onClick = onShareStory, icon = Icons.Default.AutoStories, enabled = !exporting, modifier = Modifier.weight(1f))
        }

        TextButton(onClick = onAnother, enabled = !exporting, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Try another style")
        }
    }
}

@Composable
private fun RecentCreations(creations: List<Creation>, onOpen: (Creation) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your recent looks", style = MaterialTheme.typography.titleLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(creations, key = { it.imageUrl }) { creation ->
                AsyncImage(creation.imageUrl, "Open recent creation: ${creation.prompt}", contentScale = ContentScale.Crop,
                    modifier = Modifier.size(112.dp, 142.dp).clip(MaterialTheme.shapes.medium).clickable { onOpen(creation) })
            }
        }
        Text("Recent looks are kept for this session.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
