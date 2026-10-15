import SwiftUI
import PhotosUI

struct RecentCreationItem: Identifiable {
    let id = UUID()
    let imageUrl: URL
    let prompt: String
    let elapsed: Double
}

struct HomeScreenView: View {
    @StateObject private var api = ApiClient.shared

    @State private var selectedMode: String = "face_studio"
    @State private var selectedModelKey: String = "flux_klein"
    @State private var selectedAspectRatio: String = "portrait"
    @State private var selectedCategory: String = "All"
    @State private var selectedPresetId: String = "monaco_yacht"

    @State private var promptText: String = "wearing a glamorous backless gold chainmail dress, standing on the teak deck of a luxury superyacht in Monaco during golden hour sunset, cinematic lighting, 8k photo"
    @State private var negativePromptText: String = ""
    @State private var showSettingsSheet: Bool = false
    @State private var isHoldPeekOriginal: Bool = false

    @State private var selectedItem: PhotosPickerItem? = nil
    @State private var selectedImage: UIImage? = nil
    @State private var uploadedImageId: String? = nil
    @State private var beforeImageUrl: URL? = nil
    @State private var resultImageUrl: URL? = nil

    @State private var isUploading: Bool = false
    @State private var isGenerating: Bool = false
    @State private var elapsedSeconds: Int = 0
    @State private var isBackendOnline: Bool = false
    @State private var serverStatusMessage: String = "GPU Online · 0.0s"
    @State private var toastMessage: String? = nil

    @State private var recentCreations: [RecentCreationItem] = []

    let categoryTabs = ["All", "Red Carpet", "Vacation", "Street Style", "Luxury Glamour", "Casual"]
    let magicChips = [
        "🌅 Golden Hour Sunset",
        "📸 8K Studio Flash",
        "🧵 Haute Couture Silk",
        "✨ Glittering Sequins",
        "🎬 Dramatic Cinematic Lighting",
        "🏎️ Luxury Sports Car Background"
    ]

    var selectedModelName: String {
        switch selectedModelKey {
        case "flux_klein": return "Photovance Ultra-Portrait™"
        case "flux_klein_ultra": return "Editorial Couture Master™"
        case "realvis_xl": return "Classic 35mm Film™"
        case "serenity": return "Studio Glamour Soft™"
        default: return "Photovance Ultra-Portrait™"
        }
    }

    var body: some View {
        ZStack {
            Color.bgCanvas.ignoresSafeArea()

            VStack(spacing: 0) {
                // Top Header Bar
                HStack {
                    HStack(spacing: 4) {
                        Text("PHOTOVANCE")
                            .font(.system(size: 20, weight: .black))
                            .foregroundColor(.textMain)
                            .tracking(1)

                        Text("AI")
                            .font(.system(size: 20, weight: .black))
                            .foregroundColor(.accentPink)
                            .tracking(1)

                        Text("PRO")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(.black)
                            .padding(.horizontal, 7)
                            .padding(.vertical, 2)
                            .background(LinearGradient.goldGradient)
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                            .padding(.leading, 4)
                    }

                    Spacer()

                    HStack(spacing: 8) {
                        // Smart Hybrid Engine Pill
                        let isNpuCapable = DeviceCapabilityDetector.shared.currentSpec.supportsLocalNpuStudio
                        Button {
                            showSettingsSheet = true
                        } label: {
                            Text(isNpuCapable ? "⚡ Local NPU" : "☁️ Cloud 8K")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.neonCyan)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.accentIndigo.opacity(0.2))
                                .clipShape(Capsule())
                                .overlay(Capsule().stroke(Color.neonCyan.opacity(0.5), lineWidth: 1))
                        }

                        // Status Pill
                        Button {
                            showSettingsSheet = true
                        } label: {
                            HStack(spacing: 6) {
                                Circle()
                                    .fill(isBackendOnline ? Color.successGreen : Color.red)
                                    .frame(width: 8, height: 8)

                                Text(serverStatusMessage)
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundColor(.textMain)
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(Color.bgCard.opacity(0.8))
                            .clipShape(Capsule())
                            .overlay(Capsule().stroke(isBackendOnline ? Color.successGreen.opacity(0.5) : Color.white.opacity(0.2), lineWidth: 1))
                        }

                        // Settings Icon
                        Button {
                            showSettingsSheet = true
                        } label: {
                            Image(systemName: "gearshape.fill")
                                .font(.system(size: 15))
                                .foregroundColor(.white)
                                .frame(width: 36, height: 36)
                                .background(Color.bgCard.opacity(0.8))
                                .clipShape(Circle())
                                .overlay(Circle().stroke(Color.white.opacity(0.2), lineWidth: 1))
                        }
                    }
                }
                .padding(.horizontal, 18)
                .padding(.vertical, 10)

                // Scrollable Content Canvas
                ScrollView {
                    VStack(spacing: 16) {
                        // 1. Mode Switcher
                        HStack(spacing: 0) {
                            Button {
                                selectedMode = "face_studio"
                            } label: {
                                Text("🌟 Face Studio")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(selectedMode == "face_studio" ? .white : .textMuted)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 10)
                                    .background(selectedMode == "face_studio" ? AnyShapeStyle(LinearGradient.heroGradient) : AnyShapeStyle(Color.clear))
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }

                            Button {
                                selectedMode = "in_place"
                            } label: {
                                Text("👗 In-Place Swap")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(selectedMode == "in_place" ? .white : .textMuted)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 10)
                                    .background(selectedMode == "in_place" ? AnyShapeStyle(LinearGradient.heroGradient) : AnyShapeStyle(Color.clear))
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                        }
                        .padding(4)
                        .background(Color.bgCard)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.12), lineWidth: 1))

                        // 2. Hero Studio Canvas (Portrait Frame)
                        ZStack {
                            RoundedRectangle(cornerRadius: 22)
                                .fill(Color.bgCard)
                                .frame(height: 300)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 22)
                                        .stroke(isGenerating ? Color.neonCyan : Color.neonViolet.opacity(0.5), lineWidth: 1.5)
                                )

                            if let image = selectedImage {
                                Image(uiImage: image)
                                    .resizable()
                                    .scaledToFill()
                                    .frame(height: 300)
                                    .clipped()
                                    .clipShape(RoundedRectangle(cornerRadius: 22))

                                // Bottom Shadow Overlay
                                VStack {
                                    Spacer()
                                    LinearGradient(colors: [.clear, Color.bgCanvas.opacity(0.9)], startPoint: .top, endPoint: .bottom)
                                        .frame(height: 80)
                                }
                                .clipShape(RoundedRectangle(cornerRadius: 22))

                                // Top-Left Face Locked Badge
                                VStack {
                                    HStack {
                                        HStack(spacing: 6) {
                                            Circle().fill(Color.successGreen).frame(width: 7, height: 7)
                                            Text("Face Identity Locked 100%")
                                                .font(.system(size: 11, weight: .semibold))
                                                .foregroundColor(.white)
                                        }
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 5)
                                        .background(Color.bgCanvas.opacity(0.85))
                                        .clipShape(Capsule())
                                        .overlay(Capsule().stroke(Color.successGreen.opacity(0.7), lineWidth: 1))

                                        Spacer()

                                        // Change Photo Button
                                        PhotosPicker(selection: $selectedItem, matching: .images) {
                                            HStack(spacing: 4) {
                                                Image(systemName: "arrow.triangle.2.circlepath")
                                                    .font(.system(size: 12))
                                                Text("Change")
                                                    .font(.system(size: 11, weight: .semibold))
                                            }
                                            .foregroundColor(.white)
                                            .padding(.horizontal, 10)
                                            .padding(.vertical, 5)
                                            .background(Color.bgCanvas.opacity(0.85))
                                            .clipShape(Capsule())
                                            .overlay(Capsule().stroke(Color.white.opacity(0.2), lineWidth: 1))
                                        }
                                    }
                                    .padding(12)

                                    Spacer()

                                    // Bottom Chip: Engine
                                    HStack {
                                        Text("⚡ Engine: \(selectedModelName)")
                                            .font(.system(size: 11, weight: .medium))
                                            .foregroundColor(Color(red: 209/255, green: 213/255, blue: 219/255))
                                            .padding(.leading, 14)
                                            .padding(.bottom, 12)
                                        Spacer()
                                    }
                                }
                            } else if isUploading {
                                VStack(spacing: 12) {
                                    ProgressView().tint(.neonMagenta).scaleEffect(1.3)
                                    Text("Uploading & optimizing portrait...")
                                        .font(.system(size: 13, weight: .medium))
                                        .foregroundColor(.textMain)
                                }
                            } else {
                                // Empty State Callout
                                VStack(spacing: 12) {
                                    Circle()
                                        .fill(LinearGradient.heroGradient)
                                        .frame(width: 60, height: 60)
                                        .overlay(Image(systemName: "person.crop.rectangle.badge.plus").font(.system(size: 26)).foregroundColor(.white))

                                    Text(selectedMode == "face_studio" ? "Add Your Reference Portrait" : "Add Photo to Swap Outfit")
                                        .font(.system(size: 16, weight: .bold))
                                        .foregroundColor(.textMain)

                                    Text("Selfie or full-body portrait with clear face")
                                        .font(.system(size: 12))
                                        .foregroundColor(.textMuted)

                                    HStack(spacing: 10) {
                                        PhotosPicker(selection: $selectedItem, matching: .images) {
                                            Text("Choose Photo")
                                                .font(.system(size: 12, weight: .semibold))
                                                .foregroundColor(.white)
                                                .padding(.horizontal, 16)
                                                .padding(.vertical, 10)
                                                .background(Color.accentIndigo)
                                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                        }

                                        Button {
                                            loadDemoModel()
                                        } label: {
                                            Text("⚡ Use Demo Model")
                                                .font(.system(size: 12, weight: .semibold))
                                                .foregroundColor(.accentPink)
                                                .padding(.horizontal, 16)
                                                .padding(.vertical, 10)
                                                .background(Color.accentPink.opacity(0.12))
                                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.accentPink, lineWidth: 1))
                                        }
                                    }
                                    .padding(.top, 4)
                                }
                                .padding(20)
                            }

                            // Neural Scan Overlay while Generating
                            if isGenerating {
                                NeuralScanView(elapsedSeconds: elapsedSeconds, modelName: selectedModelName)
                                    .frame(height: 300)
                                    .clipShape(RoundedRectangle(cornerRadius: 22))
                            }
                        }

                        // 3. Visual Style Inspiration Reel
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                Text("Style Inspiration Reel")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.textMain)
                                Spacer()
                                Text("Tap to Apply")
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundColor(.accentPink)
                            }

                            // Categories
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(categoryTabs, id: \.self) { cat in
                                        let isSel = (selectedCategory == cat)
                                        Button {
                                            selectedCategory = cat
                                        } label: {
                                            Text(cat)
                                                .font(.system(size: 11, weight: isSel ? .bold : .medium))
                                                .foregroundColor(isSel ? .white : .textMuted)
                                                .padding(.horizontal, 12)
                                                .padding(.vertical, 6)
                                                .background(isSel ? Color.accentIndigo : Color.bgCard)
                                                .clipShape(RoundedRectangle(cornerRadius: 10))
                                                .overlay(RoundedRectangle(cornerRadius: 10).stroke(isSel ? Color.accentIndigo : Color.white.opacity(0.1), lineWidth: 1))
                                        }
                                    }
                                }
                            }

                            // Cards Carousel
                            let filtered = selectedCategory == "All" ? VISUAL_STYLE_PRESETS : VISUAL_STYLE_PRESETS.filter { $0.category == selectedCategory }
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 12) {
                                    ForEach(filtered) { preset in
                                        StyleCardView(
                                            preset: preset,
                                            isSelected: (selectedPresetId == preset.id),
                                            action: {
                                                selectedPresetId = preset.id
                                                promptText = preset.prompt
                                                selectedMode = preset.mode
                                                showToast("Applied: \(preset.title)")
                                            }
                                        )
                                    }
                                }
                                .padding(.vertical, 4)
                            }
                        }

                        // 4. Prompt Studio Bar & Magic Modifiers
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Prompt & Magic Modifiers")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(.textMain)

                            TextField("Describe desired outfit or scene...", text: $promptText, axis: .vertical)
                                .lineLimit(2...4)
                                .padding(12)
                                .background(Color.bgCard)
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.18), lineWidth: 1))
                                .foregroundColor(.textMain)

                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(magicChips, id: \.self) { chip in
                                        Button {
                                            let clean = chip.replacingOccurrences(of: "^[\\p{So}\\p{Sk}]+\\s*", with: "", options: .regularExpression)
                                            promptText = promptText.isEmpty ? clean : "\(promptText), \(clean)"
                                        } label: {
                                            Text(chip)
                                                .font(.system(size: 11, weight: .medium))
                                                .foregroundColor(Color(red: 226/255, green: 232/255, blue: 240/255))
                                                .padding(.horizontal, 10)
                                                .padding(.vertical, 6)
                                                .background(Color.white.opacity(0.08))
                                                .clipShape(RoundedRectangle(cornerRadius: 10))
                                                .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.white.opacity(0.12), lineWidth: 0.5))
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Hero Generate Action Button
                        Button {
                            generateImage()
                        } label: {
                            ZStack {
                                RoundedRectangle(cornerRadius: 20)
                                    .fill(isGenerating ? AnyShapeStyle(Color(red: 34/255, green: 23/255, blue: 59/255)) : AnyShapeStyle(LinearGradient.heroGradient))
                                    .frame(height: 60)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 20)
                                            .stroke(isGenerating ? Color.neonCyan : Color.white.opacity(0.2), lineWidth: 1.5)
                                    )
                                    .shadow(color: .accentPink.opacity(0.4), radius: 12, x: 0, y: 4)

                                if isGenerating {
                                    HStack(spacing: 10) {
                                        ProgressView().tint(.white)
                                        Text("Rendering with Photovance AI (\(elapsedSeconds)s)...")
                                            .font(.system(size: 14, weight: .bold))
                                            .foregroundColor(.white)
                                    }
                                } else {
                                    VStack(spacing: 2) {
                                        Text("✨ GENERATE WITH PHOTOVANCE AI")
                                            .font(.system(size: 15, weight: .black))
                                            .foregroundColor(.white)
                                            .tracking(0.5)

                                        Text("⚡ 4-step DiT render · ~12s with 100% Face Lock")
                                            .font(.system(size: 10))
                                            .foregroundColor(Color(red: 252/255, green: 231/255, blue: 243/255))
                                    }
                                }
                            }
                        }
                        .disabled(isGenerating)

                        // 6. Result & Comparison Suite
                        if let resUrl = resultImageUrl, let bUrl = beforeImageUrl {
                            VStack(spacing: 14) {
                                HStack {
                                    Text("Result: Interactive Before & After")
                                        .font(.system(size: 15, weight: .bold))
                                        .foregroundColor(.textMain)
                                    Spacer()
                                    Text("Drag slider to compare")
                                        .font(.system(size: 11))
                                        .foregroundColor(.textMuted)
                                }

                                BeforeAfterSliderView(beforeUrl: bUrl, afterUrl: resUrl, isHoldPeekOriginal: isHoldPeekOriginal)
                                    .frame(height: 380)

                                // Action Buttons
                                HStack(spacing: 10) {
                                    Button {
                                        saveResultToPhotos()
                                    } label: {
                                        HStack(spacing: 4) {
                                            Image(systemName: "square.and.arrow.down")
                                            Text("Save HD")
                                                .font(.system(size: 12, weight: .semibold))
                                        }
                                        .foregroundColor(.white)
                                        .frame(maxWidth: .infinity)
                                        .frame(height: 46)
                                        .background(Color.white.opacity(0.12))
                                        .clipShape(RoundedRectangle(cornerRadius: 12))
                                    }

                                    ShareLink(item: resUrl) {
                                        HStack(spacing: 4) {
                                            Image(systemName: "square.and.arrow.up")
                                            Text("Share")
                                                .font(.system(size: 12, weight: .semibold))
                                        }
                                        .foregroundColor(.white)
                                        .frame(maxWidth: .infinity)
                                        .frame(height: 46)
                                        .background(Color.white.opacity(0.12))
                                        .clipShape(RoundedRectangle(cornerRadius: 12))
                                    }

                                    // Hold to Compare Button
                                    Text(isHoldPeekOriginal ? "Original Photo" : "Hold to Compare")
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundColor(.white)
                                        .frame(maxWidth: .infinity)
                                        .frame(height: 46)
                                        .background(isHoldPeekOriginal ? Color.successGreen : Color.accentIndigo)
                                        .clipShape(RoundedRectangle(cornerRadius: 12))
                                        .simultaneousGesture(
                                            DragGesture(minimumDistance: 0)
                                                .onChanged { _ in isHoldPeekOriginal = true }
                                                .onEnded { _ in isHoldPeekOriginal = false }
                                        )
                                }
                            }
                            .padding(16)
                            .background(Color.bgCard)
                            .clipShape(RoundedRectangle(cornerRadius: 20))
                            .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.18), lineWidth: 1))
                        }

                        // 7. Recent Creations Reel
                        if !recentCreations.isEmpty {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("Your Recent Creations")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(.textMain)

                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 10) {
                                        ForEach(recentCreations) { item in
                                            AsyncImage(url: item.imageUrl) { img in
                                                img.resizable().scaledToFill()
                                            } placeholder: {
                                                Color.bgCard
                                            }
                                            .frame(width: 100, height: 130)
                                            .clipShape(RoundedRectangle(cornerRadius: 14))
                                            .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.white.opacity(0.18), lineWidth: 1))
                                            .onTapGesture {
                                                resultImageUrl = item.imageUrl
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer().frame(height: 20)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                }
            }
        }
        .sheet(isPresented: $showSettingsSheet) {
            StudioSettingsSheet(
                selectedModelKey: $selectedModelKey,
                selectedAspectRatio: $selectedAspectRatio,
                isBackendOnline: $isBackendOnline,
                serverStatusMessage: $serverStatusMessage
            )
        }
        .task {
            checkStatus()
        }
        .onChange(of: selectedItem) { newItem in
            Task {
                if let data = try? await newItem?.loadTransferable(type: Data.self),
                   let uiImage = UIImage(data: data) {
                    selectedImage = uiImage
                    resultImageUrl = nil
                    uploadPhoto(uiImage)
                }
            }
        }
    }

    // Network Helpers
    private func checkStatus() {
        Task {
            do {
                let status = try await api.getStatus()
                isBackendOnline = status.ready
                serverStatusMessage = status.ready ? "GPU Online · Ready" : "Initializing"
                if negativePromptText.isEmpty, let defNeg = status.defaultNegativePrompt {
                    negativePromptText = defNeg
                }
            } catch {
                isBackendOnline = false
                serverStatusMessage = "Backend Offline"
            }
        }
    }

    private func loadDemoModel() {
        isUploading = true
        Task {
            do {
                let sample = try await api.getSample()
                uploadedImageId = sample.imageId
                if let url = api.getFullUrl(sample.imageUrl) {
                    beforeImageUrl = url
                    // Load image into memory for canvas
                    let (data, _) = try await URLSession.shared.data(from: url)
                    if let img = UIImage(data: data) {
                        selectedImage = img
                    }
                }
                resultImageUrl = nil
                isUploading = false
                showToast("Demo portrait loaded!")
            } catch {
                isUploading = false
                showToast("Failed demo: \(error.localizedDescription)")
            }
        }
    }

    private func uploadPhoto(_ image: UIImage) {
        isUploading = true
        Task {
            do {
                let res = try await api.uploadImage(image: image)
                if let id = res.imageId, let urlStr = res.imageUrl, let fullUrl = api.getFullUrl(urlStr) {
                    uploadedImageId = id
                    beforeImageUrl = fullUrl
                    showToast("Portrait loaded successfully!")
                }
                isUploading = false
            } catch {
                isUploading = false
                showToast("Upload failed: \(error.localizedDescription)")
            }
        }
    }

    private func generateImage() {
        guard !isUploading else {
            showToast("Photo is uploading, please wait...")
            return
        }
        guard let imgId = uploadedImageId else {
            showToast("Please choose a portrait first (or tap 'Use Demo Model')!")
            return
        }
        guard !promptText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            showToast("Please enter an outfit prompt or choose a style card!")
            return
        }

        isGenerating = true
        elapsedSeconds = 0

        Task {
            while isGenerating {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                if isGenerating {
                    await MainActor.run {
                        elapsedSeconds += 1
                    }
                }
            }
        }

        Task {
            do {
                let req = ChangeDressRequest(
                    imageId: imgId,
                    prompt: promptText,
                    negativePrompt: negativePromptText,
                    modelKey: selectedModelKey,
                    mode: selectedMode,
                    aspectRatio: selectedAspectRatio,
                    target: "dress",
                    expand: 5,
                    keepFace: true
                )
                let res = try await api.changeDress(request: req)
                if res.success, let resUrlStr = res.resultUrl, let fullUrl = api.getFullUrl(resUrlStr) {
                    resultImageUrl = fullUrl
                    recentCreations.insert(RecentCreationItem(imageUrl: fullUrl, prompt: promptText, elapsed: res.elapsed ?? 0.0), at: 0)
                    showToast("✨ Generated in \(String(format: "%.1f", res.elapsed ?? 0))s!")
                } else {
                    showToast(res.error ?? "Generation failed")
                }
                isGenerating = false
            } catch {
                isGenerating = false
                showToast("Error: \(error.localizedDescription)")
            }
        }
    }

    private func saveResultToPhotos() {
        guard let url = resultImageUrl else { return }
        Task {
            do {
                let (data, _) = try await URLSession.shared.data(from: url)
                if let image = UIImage(data: data) {
                    UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil)
                    showToast("Saved to Photos!")
                }
            } catch {
                showToast("Failed to save image")
            }
        }
    }

    private func showToast(_ msg: String) {
        toastMessage = msg
    }
}
