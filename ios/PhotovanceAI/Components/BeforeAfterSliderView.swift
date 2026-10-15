import SwiftUI

struct BeforeAfterSliderView: View {
    let beforeUrl: URL?
    let afterUrl: URL?
    var isHoldPeekOriginal: Bool = false

    @State private var rawSplitFraction: CGFloat = 0.5

    var body: some View {
        GeometryReader { geo in
            let width = geo.size.width
            let height = geo.size.height
            let currentFraction = isHoldPeekOriginal ? 0.99 : rawSplitFraction

            ZStack(alignment: .leading) {
                // 1. After (Transformed) Image (Underneath, Full Width)
                AsyncImage(url: afterUrl) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                    case .failure:
                        Color.bgCard
                    case .empty:
                        ProgressView().tint(.neonMagenta)
                    @unknown default:
                        Color.bgCard
                    }
                }
                .frame(width: width, height: height)
                .clipped()

                // 2. Before (Original) Image (Clipped to fraction)
                AsyncImage(url: beforeUrl) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                    case .failure:
                        Color.bgCard
                    case .empty:
                        Color.clear
                    @unknown default:
                        Color.clear
                    }
                }
                .frame(width: width, height: height)
                .clipped()
                .frame(width: max(0, width * currentFraction), alignment: .leading)
                .clipped()

                // 3. Floating Badges
                VStack {
                    HStack {
                        Text("ORIGINAL")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 9)
                            .padding(.vertical, 4)
                            .background(Color.bgCanvas.opacity(0.85))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                            .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.white.opacity(0.2), lineWidth: 0.5))

                        Spacer()

                        Text("PHOTOVANCE 8K")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 9)
                            .padding(.vertical, 4)
                            .background(LinearGradient.heroGradient)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .padding(14)

                    Spacer()
                }

                // 4. Draggable Divider Line
                let dividerX = width * currentFraction
                Rectangle()
                    .fill(
                        LinearGradient(
                            colors: [.clear, .white, .accentPink, .white, .clear],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .frame(width: 3)
                    .position(x: dividerX, y: height / 2)

                // Glowing Handle Knob
                Circle()
                    .fill(LinearGradient.heroGradient)
                    .frame(width: 44, height: 44)
                    .overlay(Circle().stroke(Color.white, lineWidth: 2.5))
                    .shadow(color: .accentPink.opacity(0.6), radius: 10, x: 0, y: 0)
                    .overlay(
                        Image(systemName: "arrow.left.and.right")
                            .foregroundColor(.white)
                            .font(.system(size: 15, weight: .bold))
                    )
                    .position(x: dividerX, y: height / 2)
            }
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.18), lineWidth: 1))
            .gesture(
                DragGesture()
                    .onChanged { value in
                        let fraction = value.location.x / width
                        rawSplitFraction = min(max(fraction, 0.02), 0.98)
                    }
            )
            .animation(.easeInOut(duration: 0.2), value: isHoldPeekOriginal)
        }
    }
}
