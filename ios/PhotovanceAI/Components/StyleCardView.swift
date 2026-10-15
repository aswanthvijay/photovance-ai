import SwiftUI

struct StylePreset: Identifiable {
    let id: String
    let title: String
    let subtitle: String
    let prompt: String
    let imageName: String
    let badge: String
    let category: String
    let mode: String
}

let VISUAL_STYLE_PRESETS: [StylePreset] = [
    StylePreset(
        id: "monaco_yacht",
        title: "Monaco Yacht",
        subtitle: "Gold Chainmail",
        prompt: "wearing a glamorous backless gold chainmail dress, standing on the teak deck of a luxury superyacht in Monaco during golden hour sunset, cinematic lighting, 8k photo",
        imageName: "style_monaco_yacht",
        badge: "🔥 Trending",
        category: "Luxury Glamour",
        mode: "face_studio"
    ),
    StylePreset(
        id: "cannes_gala",
        title = "Cannes Gala",
        subtitle = "Emerald Velvet",
        prompt = "haute couture emerald green velvet backless evening gown, diamond necklace, at a prestigious Cannes red carpet film premiere, paparazzi flashlights, ultra detailed 8k",
        imageName: "style_cannes_gala",
        badge: "👑 VIP",
        category: "Red Carpet",
        mode: "face_studio"
    ),
    StylePreset(
        id: "maldives_beach",
        title: "Maldives Beach",
        subtitle: "Tropical Bikini",
        prompt: "sun-drenched vacation portrait wearing a stylish red triangle bikini, standing on pristine white sand turquoise tropical beach in the Maldives, wet hair, golden sunlight",
        imageName: "style_maldives_beach",
        badge: "✨ Popular",
        category: "Vacation",
        mode: "face_studio"
    ),
    StylePreset(
        id: "tokyo_street",
        title: "Tokyo Moto",
        subtitle: "Black Leather",
        prompt: "chic edgy street style wearing a black leather moto biker jacket and dark denim jeans, leaning against a luxury sports car on a neon Tokyo street at night",
        imageName: "style_tokyo_street",
        badge: "⚡ Edgy",
        category: "Street Style",
        mode: "face_studio"
    ),
    StylePreset(
        id: "opera_crimson",
        title: "Opera House",
        subtitle: "Crimson Silk",
        prompt: "breathtaking dramatic portrait wearing an opulent crimson red silk ballgown with flowing train, standing on the grand staircase of the Sydney Opera House at twilight",
        imageName: "style_opera_crimson",
        badge: "💎 Haute Couture",
        category: "Red Carpet",
        mode: "face_studio"
    ),
    StylePreset(
        id: "floral_sundress",
        title: "Floral Sundress",
        subtitle: "Summer Rose",
        prompt: "vibrant summer floral sundress with natural drape and delicate ruffles, standing in a sunlit blooming botanical garden, warm soft film glow",
        imageName: "style_floral_sundress",
        badge: "🌸 Fresh",
        category: "Casual",
        mode: "in_place"
    )
]

struct StyleCardView: View {
    let preset: StylePreset
    let isSelected: Bool
    let onSelect: () -> Unit? = nil
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack(alignment: .bottomLeading) {
                // Background Thumbnail Image
                Image(preset.imageName)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 135, height: 185)
                    .clipped()

                // Gradient Bottom Overlay for Text Contrast
                LinearGradient(
                    colors: [.clear, Color.bgCanvas.opacity(0.8), Color.bgCanvas.opacity(0.95)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(width: 135, height: 90)

                // Top-Left Badge
                VStack {
                    HStack {
                        Text(preset.badge)
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 7)
                            .padding(.vertical, 3)
                            .background(Color.bgCanvas.opacity(0.85))
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                            .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.white.opacity(0.2), lineWidth: 0.5))

                        Spacer()

                        if isSelected {
                            Image(systemName: "checkmark")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.white)
                                .frame(width: 22, height: 22)
                                .background(LinearGradient.heroGradient)
                                .clipShape(Circle())
                        }
                    }
                    .padding(8)

                    Spacer()

                    // Titles
                    VStack(alignment: .leading, spacing: 2) {
                        Text(preset.title)
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(.white)
                            .lineLimit(1)

                        Text(preset.subtitle)
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(.textMuted)
                            .lineLimit(1)
                    }
                    .padding(.horizontal, 10)
                    .padding(.bottom, 8)
                }
            }
            .frame(width: 135, height: 185)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(isSelected ? AnyShapeStyle(LinearGradient.neonGlowGradient) : AnyShapeStyle(Color.white.opacity(0.18)), lineWidth: isSelected ? 2.5 : 1)
            )
            .scaleEffect(isSelected ? 1.04 : 1.0)
            .animation(.easeInOut(duration: 0.2), value: isSelected)
        }
        .buttonStyle(.plain)
    }
}
