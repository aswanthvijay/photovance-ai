package com.photovance.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photovance.app.R

@Composable
fun FoldedLightMark(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.ic_folded_light), contentDescription = null, modifier = modifier)
}

@Composable
fun StudioHeader(
    onSettings: () -> Unit,
    onBack: (() -> Unit)? = null,
    onExplore: (() -> Unit)? = null,
    engineModeTitle: String? = null,
    onToggleEngine: (() -> Unit)? = null,
    vipBadgeTitle: String? = null,
    onTapVip: (() -> Unit)? = null
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.padding(end = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        } else {
            FoldedLightMark(Modifier.size(32.dp))
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Photovance", style = MaterialTheme.typography.titleLarge, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text("PRO PHOTO STUDIO", style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (vipBadgeTitle != null) {
            Surface(
                onClick = { onTapVip?.invoke() },
                shape = MaterialTheme.shapes.small,
                color = androidx.compose.ui.graphics.Color(0xFFFFB800).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFFFB800).copy(alpha = 0.6f))
            ) {
                Text(
                    text = vipBadgeTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color(0xFFFFB800),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        if (engineModeTitle != null) {
            Surface(
                onClick = { onToggleEngine?.invoke() },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Text(
                    text = engineModeTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        if (onExplore != null) TextButton(onClick = onExplore) { Text("Explore") }
        IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings and appearance") }
    }
}

@Composable
fun StudioSteps(current: Int, hasPhoto: Boolean, hasResult: Boolean, enabled: Boolean, onStep: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        listOf("Photo", "Style", "Result").forEachIndexed { index, title ->
            if (index > 0) {
                HorizontalDivider(Modifier.weight(1f).padding(horizontal = 10.dp),
                    color = if (current >= index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            }
            val available = enabled && (index == 0 || (index == 1 && hasPhoto) || (index == 2 && hasResult))
            Column(Modifier.widthIn(min = 48.dp).clip(MaterialTheme.shapes.small)
                .selectable(selected = current == index, enabled = available, role = Role.Tab,
                    onClick = { onStep(index) }).padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(22.dp).border(1.dp,
                    if (index <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    CircleShape).padding(4.dp).background(
                        if (index <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                        CircleShape), contentAlignment = Alignment.Center) {
                    if (index < current) Icon(Icons.Default.Check, null,
                        tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                }
                Text(title, style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (index == current) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (index == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PageHeading(eyebrow: String, title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall,
            letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(description, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StudioButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    icon: ImageVector? = null, enabled: Boolean = true, loading: Boolean = false) {
    Button(onClick = onClick, enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp), shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        else if (icon != null) Icon(icon, null, Modifier.size(20.dp))
        if (loading || icon != null) Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StudioOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    icon: ImageVector? = null, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp)) {
        if (icon != null) { Icon(icon, null, Modifier.size(19.dp)); Spacer(Modifier.width(9.dp)) }
        Text(text, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ErrorNotice(message: String, onSettings: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp))
                Text(message, color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
            TextButton(onClick = onSettings) { Text("Connection settings") }
        }
    }
}
