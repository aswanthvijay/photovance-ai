import SwiftUI

extension Color {
    // Obsidian Glassmorphism Studio Palette
    static let bgCanvas = Color(red: 7/255, green: 9/255, blue: 14/255)
    static let bgCard = Color(red: 15/255, green: 22/255, blue: 38/255)
    static let bgCardHover = Color(red: 22/255, green: 32/255, blue: 53/255)
    static let glassSurface = Color(red: 17/255, green: 24/255, blue: 39/255).opacity(0.8)
    static let glassBorder = Color.white.opacity(0.18)

    // Neon Brand & Luxury Accents
    static let accentIndigo = Color(red: 99/255, green: 102/255, blue: 241/255)
    static let accentPink = Color(red: 236/255, green: 72/255, blue: 153/255)
    static let neonMagenta = Color(red: 236/255, green: 72/255, blue: 153/255)
    static let neonViolet = Color(red: 139/255, green: 92/255, blue: 246/255)
    static let neonCyan = Color(red: 6/255, green: 182/255, blue: 212/255)
    static let goldAmber = Color(red: 245/255, green: 158/255, blue: 11/255)
    static let successGreen = Color(red: 16/255, green: 185/255, blue: 129/255)

    // Typography
    static let textMain = Color(red: 248/255, green: 250/255, blue: 252/255)
    static let textMuted = Color(red: 148/255, green: 163/255, blue: 184/255)
    static let subtleBorder = Color.white.opacity(0.12)
}

extension LinearGradient {
    static let heroGradient = LinearGradient(
        colors: [.accentIndigo, .neonMagenta],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    static let neonGlowGradient = LinearGradient(
        colors: [.neonViolet, .neonMagenta, .neonCyan],
        startPoint: .leading,
        endPoint: .trailing
    )

    static let goldGradient = LinearGradient(
        colors: [Color(red: 251/255, green: 191/255, blue: 36/255), Color(red: 245/255, green: 158/255, blue: 11/255)],
        startPoint: .leading,
        endPoint: .trailing
    )
}
