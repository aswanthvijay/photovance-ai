import SwiftUI
import PhotosUI

struct VirtualWardrobePickerView: View {
    @Binding var selectedGarment: UIImage?
    var onApplyHint: (String) -> Void
    @State private var selectedItem: PhotosPickerItem? = nil

    private let hints = [
        "Emerald Silk Gown",
        "Tailored Black Tuxedo",
        "White Linen Sundress",
        "Tokyo Streetwear Leather",
        "Cashmere Trench Coat"
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Header
            HStack {
                HStack(spacing: 8) {
                    ZStack {
                        Circle()
                            .fill(Color(red: 0.58, green: 0.2, blue: 0.92).opacity(0.2))
                            .frame(width: 32, height: 32)

                        Image(systemName: "tshirt.fill")
                            .font(.system(size: 15))
                            .foregroundColor(Color(red: 0.75, green: 0.45, blue: 1.0))
                    }

                    VStack(alignment: .leading, spacing: 1) {
                        Text("Virtual Wardrobe™")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.white)

                        Text("Try on any dress, gown, suit, or outfit")
                            .font(.system(size: 11))
                            .foregroundColor(.white.opacity(0.6))
                    }
                }

                Spacer()

                if selectedGarment != nil {
                    Button(action: {
                        selectedGarment = nil
                        selectedItem = nil
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.white.opacity(0.5))
                            .font(.system(size: 18))
                    }
                }
            }

            if let garment = selectedGarment {
                // Active garment view
                HStack(spacing: 12) {
                    Image(uiImage: garment)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: 58, height: 70)
                        .cornerRadius(8)
                        .clipped()

                    VStack(alignment: .leading, spacing: 3) {
                        HStack(spacing: 4) {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.system(size: 12))
                                .foregroundColor(Color(red: 0.06, green: 0.73, blue: 0.51))

                            Text("Garment Reference Active")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(Color(red: 0.06, green: 0.73, blue: 0.51))
                        }

                        Text("The AI will drape this outfit with photorealistic fabric physics.")
                            .font(.system(size: 11))
                            .foregroundColor(.white.opacity(0.65))
                    }

                    Spacer()

                    PhotosPicker(selection: $selectedItem, matching: .images) {
                        Text("Change")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(Color(red: 0.75, green: 0.45, blue: 1.0))
                    }
                }
                .padding(10)
                .background(Color.white.opacity(0.04))
                .cornerRadius(12)
            } else {
                // Upload trigger
                PhotosPicker(selection: $selectedItem, matching: .images) {
                    HStack(spacing: 14) {
                        ZStack {
                            Circle()
                                .fill(Color(red: 0.58, green: 0.2, blue: 0.92).opacity(0.15))
                                .frame(width: 44, height: 44)

                            Image(systemName: "photo.badge.plus")
                                .font(.system(size: 18))
                                .foregroundColor(Color(red: 0.75, green: 0.45, blue: 1.0))
                        }

                        VStack(alignment: .leading, spacing: 2) {
                            Text("Upload Outfit / Garment Photo")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(.white)

                            Text("From Zara, Pinterest, camera, or your closet")
                                .font(.system(size: 11))
                                .foregroundColor(.white.opacity(0.6))
                        }

                        Spacer()
                    }
                    .padding(14)
                    .background(Color.white.opacity(0.03))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color(red: 0.58, green: 0.2, blue: 0.92).opacity(0.4), lineWidth: 1)
                    )
                    .cornerRadius(12)
                }
            }

            // Styling tags
            VStack(alignment: .leading, spacing: 8) {
                Text("Garment Styling Hints")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(.white.opacity(0.6))

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(hints, id: \.self) { hint in
                            Button(action: { onApplyHint(hint) }) {
                                Text("+ \(hint)")
                                    .font(.system(size: 11.5))
                                    .foregroundColor(.white.opacity(0.85))
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 5)
                                    .background(Color.white.opacity(0.06))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(Color.white.opacity(0.1), lineWidth: 1)
                                    )
                                    .cornerRadius(8)
                            }
                        }
                    }
                }
            }
        }
        .padding(16)
        .background(Color(red: 0.08, green: 0.07, blue: 0.1))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color(red: 0.58, green: 0.2, blue: 0.92).opacity(0.3), lineWidth: 1)
        )
        .cornerRadius(16)
        .onChange(of: selectedItem) { newItem in
            Task {
                if let data = try? await newItem?.loadTransferable(type: Data.self),
                   let image = UIImage(data: data) {
                    selectedGarment = image
                }
            }
        }
    }
}
