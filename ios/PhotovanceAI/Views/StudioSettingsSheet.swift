import SwiftUI

struct StudioSettingsSheet: View {
    @Binding var selectedModelKey: String
    @Binding var selectedAspectRatio: String
    @Binding var isBackendOnline: Bool
    @Binding var serverStatusMessage: String
    @Environment(\.dismiss) private var dismiss

    @State private var tempUrl: String = ApiClient.shared.baseUrl
    @State private var isTesting: Bool = false
    @State private var alertMessage: String? = nil

    let models = [
        ("flux_klein", "Photovance Ultra-Portrait™ (8K Studio Lighting · Ultra-Sharp)"),
        ("flux_klein_ultra", "Editorial Couture Master™ (Runway Texture · Micro-Weave Fidelity)"),
        ("realvis_xl", "Classic 35mm Film™ (Warm Cinematic Grain · Analog Depth)"),
        ("serenity", "Studio Glamour Soft™ (Soft Portrait Glow · Natural Lighting)")
    ]

    let aspectRatios = [
        ("portrait", "4:5 Portrait"),
        ("story", "9:16 Story"),
        ("square", "1:1 Square")
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Header
                    HStack {
                        Text("⚙️ Studio Engine Settings")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(.white)
                        Spacer()
                    }

                    // 1. AI Model Architecture
                    VStack(alignment: .leading, spacing: 10) {
                        Text("AI Diffusion Model:")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(.textMuted)

                        VStack(spacing: 8) {
                            ForEach(models, id: \.0) { key, label in
                                let isSelected = (selectedModelKey == key)
                                Button {
                                    selectedModelKey = key
                                } label: {
                                    HStack(spacing: 12) {
                                        Image(systemName: isSelected ? "largecircle.fill.circle" : "circle")
                                            .foregroundColor(isSelected ? .accentIndigo : .textMuted)
                                            .font(.system(size: 18))

                                        Text(label)
                                            .font(.system(size: 12, weight: .medium))
                                            .foregroundColor(.textMain)
                                            .multilineTextAlignment(.leading)

                                        Spacer()
                                    }
                                    .padding(12)
                                    .background(isSelected ? Color.accentIndigo.opacity(0.18) : Color.white.opacity(0.04))
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12)
                                            .stroke(isSelected ? Color.accentIndigo : Color.white.opacity(0.1), lineWidth: 1)
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    // 2. Aspect Ratio
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Aspect Ratio:")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(.textMuted)

                        HStack(spacing: 10) {
                            ForEach(aspectRatios, id: \.0) { key, label in
                                let isSelected = (selectedAspectRatio == key)
                                Button {
                                    selectedAspectRatio = key
                                } label: {
                                    Text(label)
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundColor(isSelected ? .white : .textMuted)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 10)
                                        .background(isSelected ? Color.accentIndigo : Color.white.opacity(0.06))
                                        .clipShape(RoundedRectangle(cornerRadius: 10))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    // 3. Backend Host URL
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Backend Host (Wi-Fi IP / Cloudflare Tunnel):")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(.textMuted)

                        TextField("http://192.168.1.230:7860", text: $tempUrl)
                            .autocorrectionDisabled()
                            .textInputAutocapitalization(.never)
                            .padding(12)
                            .background(Color.bgCanvas)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.18), lineWidth: 1))
                            .foregroundColor(.white)
                    }

                    // Connect Button
                    Button {
                        isTesting = true
                        ApiClient.shared.saveBaseUrl(tempUrl)
                        Task {
                            do {
                                let status = try await ApiClient.shared.getStatus()
                                isBackendOnline = status.ready
                                serverStatusMessage = status.ready ? "GPU Online · Ready" : "Initializing"
                                isTesting = false
                                dismiss()
                            } catch {
                                isBackendOnline = false
                                serverStatusMessage = "Connection Failed"
                                isTesting = false
                                alertMessage = "Could not connect: \(error.localizedDescription)"
                            }
                        }
                    } label: {
                        HStack {
                            if isTesting {
                                ProgressView().tint(.white)
                                Spacer().frame(width: 8)
                            }
                            Text("Save & Connect")
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(.white)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(Color.accentIndigo)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                }
                .padding(20)
            }
            .background(Color.bgCard)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                    .foregroundColor(.accentPink)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
