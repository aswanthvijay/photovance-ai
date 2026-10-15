import SwiftUI

struct PaywallView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var storeKit = StoreKitManager.shared
    @State private var selectedPlan: VipPlanType = .yearly

    var body: some View {
        ZStack {
            Color(red: 0.06, green: 0.05, blue: 0.08)
                .ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(spacing: 20) {
                    // Close button
                    HStack {
                        Spacer()
                        Button(action: { dismiss() }) {
                            Image(systemName: "xmark")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(.white.opacity(0.7))
                                .frame(width: 34, height: 34)
                                .background(Color.white.opacity(0.08))
                                .clipShape(Circle())
                        }
                    }
                    .padding(.top, 16)

                    // Crown Jewel
                    ZStack {
                        Circle()
                            .fill(
                                RadialGradient(
                                    colors: [Color(red: 1.0, green: 0.72, blue: 0.0), Color(red: 0.58, green: 0.2, blue: 0.92)],
                                    center: .center,
                                    startRadius: 4,
                                    endRadius: 36
                                )
                            )
                            .frame(width: 68, height: 68)
                            .overlay(
                                Circle()
                                    .stroke(Color(red: 1.0, green: 0.84, blue: 0.0).opacity(0.6), lineWidth: 2)
                            )

                        Image(systemName: "crown.fill")
                            .font(.system(size: 30))
                            .foregroundColor(.white)
                    }

                    VStack(spacing: 6) {
                        Text("PHOTOVANCE VIP")
                            .font(.system(size: 13, weight: .bold))
                            .tracking(3)
                            .foregroundColor(Color(red: 1.0, green: 0.72, blue: 0.0))

                        Text("Unlimited 8K Studio Power")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)

                        Text("Transform your portraits into editorial perfection with unlimited generations and zero watermarks.")
                            .font(.system(size: 14))
                            .foregroundColor(.white.opacity(0.7))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 16)
                    }

                    // VIP Perks Card
                    VStack(alignment: .leading, spacing: 14) {
                        perkRow("⚡ Unlimited 8K Photovance Ultra-Portraits")
                        perkRow("👗 Virtual Wardrobe: Try on ANY outfit or gown")
                        perkRow("👑 Unlock all Editorial & Runway couture tiers")
                        perkRow("🚫 Pristine 8K exports with ZERO watermark")
                        perkRow("⚡ Dedicated priority GPU cloud cluster")
                    }
                    .padding(18)
                    .background(Color.white.opacity(0.04))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(Color.white.opacity(0.08), lineWidth: 1)
                    )
                    .cornerRadius(16)

                    // Plan selection
                    VStack(spacing: 12) {
                        ForEach(VipPlanType.allCases) { plan in
                            planCard(plan: plan, isSelected: selectedPlan == plan) {
                                selectedPlan = plan
                            }
                        }
                    }

                    // Primary CTA
                    Button(action: {
                        Task {
                            await storeKit.purchase(plan: selectedPlan)
                            dismiss()
                        }
                    }) {
                        HStack(spacing: 8) {
                            if storeKit.isPurchasing {
                                ProgressView()
                                    .tint(.black)
                            } else {
                                Image(systemName: "sparkles")
                                Text(selectedPlan == .weekly ? "Start 3-Day Free Trial" : "Unlock Annual VIP Studio")
                                    .fontWeight(.bold)
                            }
                        }
                        .font(.system(size: 16))
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .background(
                            LinearGradient(
                                colors: [Color(red: 1.0, green: 0.72, blue: 0.0), Color(red: 1.0, green: 0.85, blue: 0.3)],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .cornerRadius(14)
                    }
                    .disabled(storeKit.isPurchasing)

                    // Secondary buttons
                    HStack(spacing: 12) {
                        Button(action: {
                            Task {
                                await storeKit.restorePurchases()
                                if storeKit.isVIP { dismiss() }
                            }
                        }) {
                            Text("Restore Purchases")
                                .font(.system(size: 12))
                                .foregroundColor(.white.opacity(0.6))
                        }

                        Text("•")
                            .font(.system(size: 12))
                            .foregroundColor(.white.opacity(0.3))

                        Text("Cancel anytime in App Store")
                            .font(.system(size: 12))
                            .foregroundColor(.white.opacity(0.4))
                    }
                    .padding(.bottom, 24)
                }
                .padding(.horizontal, 24)
            }
        }
    }

    private func perkRow(_ text: String) -> some View {
        HStack(spacing: 10) {
            ZStack {
                Circle()
                    .fill(Color(red: 0.58, green: 0.2, blue: 0.92).opacity(0.3))
                    .frame(width: 22, height: 22)

                Image(systemName: "checkmark")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(Color(red: 0.75, green: 0.45, blue: 1.0))
            }

            Text(text)
                .font(.system(size: 13.5))
                .foregroundColor(.white.opacity(0.9))
        }
    }

    private func planCard(plan: VipPlanType, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                // Radio indicator
                ZStack {
                    Circle()
                        .stroke(isSelected ? Color(red: 1.0, green: 0.72, blue: 0.0) : Color.white.opacity(0.3), lineWidth: 2)
                        .frame(width: 20, height: 20)

                    if isSelected {
                        Circle()
                            .fill(Color(red: 1.0, green: 0.72, blue: 0.0))
                            .frame(width: 10, height: 10)
                    }
                }

                VStack(alignment: .leading, spacing: 2) {
                    HStack {
                        Text(plan.title)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.white)

                        if let badge = plan.badge {
                            Text(badge)
                                .font(.system(size: 9, weight: .bold))
                                .foregroundColor(.black)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color(red: 1.0, green: 0.72, blue: 0.0))
                                .cornerRadius(4)
                        }
                    }

                    Text(plan.priceText)
                        .font(.system(size: 13))
                        .foregroundColor(.white.opacity(0.65))
                }

                Spacer()
            }
            .padding(16)
            .background(isSelected ? Color(red: 0.14, green: 0.11, blue: 0.06) : Color.white.opacity(0.04))
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(isSelected ? Color(red: 1.0, green: 0.72, blue: 0.0) : Color.white.opacity(0.12), lineWidth: isSelected ? 2 : 1)
            )
            .cornerRadius(14)
        }
    }
}
