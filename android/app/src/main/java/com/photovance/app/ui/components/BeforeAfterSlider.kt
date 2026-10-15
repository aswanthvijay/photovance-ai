package com.photovance.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun BeforeAfterSlider(beforeUrl: String, afterUrl: String, modifier: Modifier = Modifier,
    isHoldPeekOriginal: Boolean = false) {
    var rawSplit by remember(beforeUrl, afterUrl) { mutableFloatStateOf(0.5f) }
    val split by animateFloatAsState(if (isHoldPeekOriginal) 1f else rawSplit,
        animationSpec = tween(150), label = "comparison")
    BoxWithConstraints(modifier.clip(MaterialTheme.shapes.large).background(Color(0xFF242128))
        .semantics(mergeDescendants = true) {
            contentDescription = "Before and after photo comparison"
            stateDescription = "${(split * 100).toInt()} percent original"
            progressBarRangeInfo = ProgressBarRangeInfo(rawSplit, 0f..1f)
            setProgress { rawSplit = it.coerceIn(0f, 1f); true }
        }
        .pointerInput(beforeUrl, afterUrl, isHoldPeekOriginal) {
            if (!isHoldPeekOriginal) detectHorizontalDragGestures { change, distance ->
                change.consume()
                rawSplit = (rawSplit + distance / size.width).coerceIn(0f, 1f)
            }
        }) {
        AsyncImage(afterUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        // Clip the full-size original without remeasuring or recentering it as the divider moves.
        AsyncImage(beforeUrl, null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().drawWithContent {
                clipRect(right = size.width * split) { this@drawWithContent.drawContent() }
            })
        val divider = maxWidth * split
        if (!isHoldPeekOriginal) {
            Box(Modifier.offset(x = divider - 1.dp).width(2.dp).fillMaxHeight().background(Color.White))
            Box(Modifier.align(Alignment.CenterStart).offset(x = divider - 22.dp).size(44.dp)
                .background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.SwapHoriz, null, tint = Color(0xFF302637), modifier = Modifier.size(23.dp))
            }
        }
        Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            ComparisonLabel("Before")
            if (!isHoldPeekOriginal) ComparisonLabel("After")
        }
    }
}

@Composable
private fun ComparisonLabel(text: String) {
    Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.background(Color(0xBA111014), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp))
}
