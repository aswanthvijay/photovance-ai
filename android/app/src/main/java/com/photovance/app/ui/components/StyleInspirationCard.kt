package com.photovance.app.ui.components

import androidx.annotation.DrawableRes
import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.photovance.app.R

data class StylePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val prompt: String,
    @DrawableRes val drawableRes: Int,
    val badge: String,
    val category: String,
    val mode: String = "face_studio"
)

val VISUAL_STYLE_PRESETS = listOf(
    StylePreset(
        id = "monaco_yacht",
        title = "Studio Knit",
        subtitle = "Soft ivory",
        prompt = "wearing a soft ivory knitted dress in a minimal charcoal studio, natural portrait lighting, refined editorial photography",
        drawableRes = R.drawable.cc0_studio,
        badge = "🔥 Trending",
        category = "Casual",
        mode = "face_studio"
    ),
    StylePreset(
        id = "cannes_gala",
        title = "Evening Edit",
        subtitle = "Burgundy satin",
        prompt = "wearing a burgundy satin evening dress with draped sleeves and delicate gold earrings, softly blurred outdoor background, elegant editorial portrait",
        drawableRes = R.drawable.cc0_evening,
        badge = "👑 VIP",
        category = "Red Carpet",
        mode = "face_studio"
    ),
    StylePreset(
        id = "maldives_beach",
        title = "Summer Colour",
        subtitle = "Soft coral print",
        prompt = "wearing a flowing coral and turquoise printed summer dress with sunglasses, warm daylight, relaxed summer street portrait",
        drawableRes = R.drawable.cc0_summer,
        badge = "✨ Popular",
        category = "Vacation",
        mode = "face_studio"
    ),
    StylePreset(
        id = "tokyo_street",
        title = "City Muse",
        subtitle = "Champagne layers",
        prompt = "wearing champagne layers with a light longline coat and a shimmering skirt, modern pale stone architecture, polished city street style",
        drawableRes = R.drawable.cc0_city,
        badge = "⚡ Edgy",
        category = "Street Style",
        mode = "face_studio"
    ),
    StylePreset(
        id = "opera_crimson",
        title = "Crimson Edit",
        subtitle = "Flowing red",
        prompt = "wearing a flowing crimson red gown beside a quiet garden pond, soft natural daylight, graceful full-length editorial photography",
        drawableRes = R.drawable.cc0_crimson,
        badge = "💎 Haute Couture",
        category = "Red Carpet",
        mode = "face_studio"
    ),
    StylePreset(
        id = "floral_sundress",
        title = "Forest Light",
        subtitle = "Ivory lace",
        prompt = "wearing an ivory lace dress in a shaded woodland garden, dappled warm sunlight, soft natural editorial photography",
        drawableRes = R.drawable.cc0_garden,
        badge = "🌸 Fresh",
        category = "Casual",
        mode = "in_place"
    )
)

@Composable
fun StyleInspirationCard(
    preset: StylePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.width(148.dp).height(205.dp).clip(MaterialTheme.shapes.medium)
        .border(if (isSelected) 2.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            MaterialTheme.shapes.medium)
        .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)) {
        AsyncImage(model = preset.drawableRes, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color.Transparent, Color.Transparent, Color(0xE8111014)))))
        if (isSelected) Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(26.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, "Selected", tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(17.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(preset.title, style = MaterialTheme.typography.labelLarge, color = Color.White,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(preset.subtitle, style = MaterialTheme.typography.labelSmall, color = Color(0xFFE2DCE9),
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}
