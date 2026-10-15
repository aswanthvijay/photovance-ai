package com.photovance.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.photovance.app.ui.theme.Accent
import com.photovance.app.ui.theme.AccentPink
import com.photovance.app.ui.theme.HeroGradient
import com.photovance.app.ui.theme.NeonGlowGradient
import kotlin.math.max
import kotlin.math.min

@Composable
fun BeforeAfterSlider(
    beforeUrl: String,
    afterUrl: String,
    modifier: Modifier = Modifier,
    isHoldPeekOriginal: Boolean = false
) {
    var rawSplitFraction by remember { mutableFloatStateOf(0.5f) }
    val splitFraction by animateFloatAsState(
        targetValue = if (isHoldPeekOriginal) 0.99f else rawSplitFraction,
        animationSpec = tween(durationMillis = 200),
        label = "splitFractionAnimation"
    )

    val context = LocalContext.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
            .background(Color.Black)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    val newFraction = rawSplitFraction + (dragAmount / size.width.toFloat())
                    rawSplitFraction = max(0.02f, min(0.98f, newFraction))
                }
            }
    ) {
        val totalWidth = maxWidth

        // 1. After (Transformed) Image (Full Width underneath)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(afterUrl)
                .crossfade(true)
                .build(),
            contentDescription = "After Outfit Transformation",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Before (Original) Image clipped to splitFraction
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(totalWidth * splitFraction)
                .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(beforeUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Original Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(totalWidth)
            )
        }

        // 3. Floating Badges: "ORIGINAL" vs "PHOTOVANCE AI"
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Original Badge
            Text(
                text = "ORIGINAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(Color(0xCC07090E), RoundedCornerShape(8.dp))
                    .border(0.5.dp, Color(0x44FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            )

            // AI Result Badge
            Text(
                text = "PHOTOVANCE 8K",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(HeroGradient, RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            )
        }

        // 4. Draggable Divider Line & Knob
        val dividerX = totalWidth * splitFraction
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .offset(x = dividerX - 1.5.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White,
                            AccentPink,
                            Color.White,
                            Color.Transparent
                        )
                    )
                )
        )

        // Glowing Handle Knob
        Box(
            modifier = Modifier
                .offset(x = dividerX - 22.dp)
                .align(Alignment.CenterStart)
                .size(44.dp)
                .shadow(elevation = 12.dp, shape = CircleShape, spotColor = AccentPink)
                .background(HeroGradient, CircleShape)
                .border(2.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Drag to compare before and after",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
