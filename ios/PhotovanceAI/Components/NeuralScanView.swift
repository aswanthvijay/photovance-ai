import SwiftUI

struct NeuralScanView: View {
    let elapsedSeconds: Int
    let modelName: String

    @State private var laserPosition: CGFloat = 0.05

    var stepInfo: (title: String, detail: String) {
        switch elapsedSeconds {
        case 0...3:
            return ("Step 1/5: Analyzing Facial Geometry", "Detecting 68 landmarks & eye vector alignment...")
        case 4...8:
            return ("Step 2/5: Identity Locking", "Preserving skin texture, micro-pores & facial likeness...")
        case 9...15:
            return ("Step 3/5: Latent DiT Synthesis", "Draping haute couture fabric in unrestricted latent space...")
        case 16...23:
            return ("Step 4/5: Studio Lighting & Shadow", "Rendering 8K photographic illumination & environment reflection...")
        default:
            return ("Step 5/5: Ultra-HD Mastering", "Finalizing high-frequency details & composite pass...")
        }
    }

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .bottom) {
                // Semi-translucent dark tint
                Color.black.opacity(0.45)

                // Laser Scan Line
                Rectangle()
                    .fill(
                        LinearGradient(
                            colors: [.clear, .neonCyan.opacity(0.4), .neonCyan, .neonViolet, .neonCyan, .clear],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(height: 3)
                    .position(x: geo.size.width / 2, y: geo.size.height * laserPosition)
                    .onAppear {
                        withAnimation(.easeInOut(duration: 2.2).repeatForever(autoreverses: true)) {
                            laserPosition = 0.95
                        }
                    }

                // Futuristic HUD Card
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        HStack(spacing: 8) {
                            ProgressView()
                                .tint(.neonCyan)
                                .scaleEffect(0.8)

                            Text("🧬 NEURAL SYNTHESIS")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.neonCyan)
                                .tracking(1)
                        }

                        Spacer()

                        Text("\(elapsedSeconds)s")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(.white)
                    }

                    Text(stepInfo.title)
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(.textMain)

                    Text(stepInfo.detail)
                        .font(.system(size: 11))
                        .foregroundColor(.textMuted)

                    Text("⚡ Engine: \(modelName) · Zero Censorship")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(.neonViolet)
                        .padding(.top, 2)
                }
                .padding(14)
                .background(Color.bgCard.opacity(0.92))
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.neonCyan.opacity(0.6), lineWidth: 1))
                .padding(14)
            }
        }
    }
}
