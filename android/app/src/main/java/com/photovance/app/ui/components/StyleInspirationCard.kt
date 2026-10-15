package com.photovance.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photovance.app.R
import com.photovance.app.ui.theme.*

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
        title = "Monaco Yacht",
        subtitle = "Gold Chainmail",
        prompt = "wearing a glamorous backless gold chainmail dress, standing on the teak deck of a luxury superyacht in Monaco during golden hour sunset, cinematic lighting, 8k photo",
        drawableRes = R.drawable.style_monaco_yacht,
        badge = "🔥 Trending",
        category = "Luxury Glamour",
        mode = "face_studio"
    ),
    StylePreset(
        id = "cannes_gala",
        title = "Cannes Gala",
        subtitle = "Emerald Velvet",
        prompt = "haute couture emerald green velvet backless evening gown, diamond necklace, at a prestigious Cannes red carpet film premiere, paparazzi flashlights, ultra detailed 8k",
        drawableRes = R.drawable.style_cannes_gala,
        badge = "👑 VIP",
        category = "Red Carpet",
        mode = "face_studio"
    ),
    StylePreset(
        id = "maldives_beach",
        title = "Maldives Beach",
        subtitle = "Tropical Bikini",
        prompt = "sun-drenched vacation portrait wearing a stylish red triangle bikini, standing on pristine white sand turquoise tropical beach in the Maldives, wet hair, golden sunlight",
        drawableRes = R.drawable.style_maldives_beach,
        badge = "✨ Popular",
        category = "Vacation",
        mode = "face_studio"
    ),
    StylePreset(
        id = "tokyo_street",
        title = "Tokyo Moto",
        subtitle = "Black Leather",
        prompt = "chic edgy street style wearing a black leather moto biker jacket and dark denim jeans, leaning against a luxury sports car on a neon Tokyo street at night",
        drawableRes = R.drawable.style_tokyo_street,
        badge = "⚡ Edgy",
        category = "Street Style",
        mode = "face_studio"
    ),
    StylePreset(
        id = "opera_crimson",
        title = "Opera House",
        subtitle = "Crimson Silk",
        prompt = "breathtaking dramatic portrait wearing an opulent crimson red silk ballgown with flowing train, standing on the grand staircase of the Sydney Opera House at twilight",
        drawableRes = R.drawable.style_opera_crimson,
        badge = "💎 Haute Couture",
        category = "Red Carpet",
        mode = "face_studio"
    ),
    StylePreset(
        id = "floral_sundress",
        title = "Floral Sundress",
        subtitle = "Summer Rose",
        prompt = "vibrant summer floral sundress with natural drape and delicate ruffles, standing in a sunlit blooming botanical garden, warm soft film glow",
        drawableRes = R.drawable.style_floral_sundress,
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
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "styleCardScale"
    )

    Box(
        modifier = modifier
            .width(138.dp)
            .height(188.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                brush = if (isSelected) NeonGlowGradient else SolidColor(Color(0x2EFFFFFF)),
                shape = RoundedCornerShape(18.dp)
            )
            .background(BgCard)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Thumbnail Image
        Image(
            painter = painterResource(id = preset.drawableRes),
            contentDescription = preset.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient Shadow Overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC07090E), Color(0xF507090E))
                    )
                )
        )

        // Top-Left Category Badge Pill
        Box(
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.TopStart)
                .background(Color(0xCC07090E), RoundedCornerShape(8.dp))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(
                text = preset.badge,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Top-Right Checkmark when Selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .background(HeroGradient, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        // Bottom Typography (Title & Subtitle)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = preset.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                maxLines = 1
            )
            Text(
                text = preset.subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}
