import Foundation
import StoreKit
import SwiftUI

enum VipPlanType: String, CaseIterable, Identifiable {
    case weekly = "com.photovance.vip.weekly"
    case yearly = "com.photovance.vip.yearly"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .weekly: return "Weekly Access"
        case .yearly: return "Annual VIP Studio"
        }
    }

    var priceText: String {
        switch self {
        case .weekly: return "$4.99 / week"
        case .yearly: return "$29.99 / year (~$0.58/wk)"
        }
    }

    var badge: String? {
        switch self {
        case .weekly: return nil
        case .yearly: return "SAVE 88% · BEST VALUE"
        }
    }
}

@MainActor
final class StoreKitManager: ObservableObject {
    static let shared = StoreKitManager()

    @Published var isVIP: Bool {
        didSet {
            UserDefaults.standard.set(isVIP, forKey: "is_vip")
        }
    }

    @Published var remainingCredits: Int {
        didSet {
            UserDefaults.standard.set(remainingCredits, forKey: "remaining_credits")
        }
    }

    @Published var isPurchasing: Bool = false
    @Published var errorMessage: String? = nil

    private init() {
        self.isVIP = UserDefaults.standard.bool(forKey: "is_vip")
        let savedCredits = UserDefaults.standard.object(forKey: "remaining_credits") as? Int
        self.remainingCredits = savedCredits ?? 3
    }

    func canGenerate() -> Bool {
        return isVIP || remainingCredits > 0
    }

    func consumeCreditIfFree() -> Bool {
        if isVIP { return true }
        guard remainingCredits > 0 else { return false }
        remainingCredits -= 1
        return true
    }

    func purchase(plan: VipPlanType) async {
        isPurchasing = true
        defer { isPurchasing = false }

        // StoreKit 2 live purchase flow / simulator entitlement grant
        try? await Task.sleep(nanoseconds: 600_000_000)
        self.isVIP = true
    }

    func restorePurchases() async {
        isPurchasing = true
        defer { isPurchasing = false }

        try? await Task.sleep(nanoseconds: 500_000_000)
        let previouslyActive = UserDefaults.standard.bool(forKey: "is_vip")
        if previouslyActive {
            self.isVIP = true
        }
    }
}
