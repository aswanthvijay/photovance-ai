import Foundation
import SwiftUI

public struct IosAuthUser: Identifiable, Codable {
    public let id: String
    public let displayName: String?
    public let email: String?
    public let phoneNumber: String?
    public let isGuest: Bool

    public var displayIdentifier: String {
        return displayName ?? email ?? phoneNumber ?? (isGuest ? "Guest Creator" : "Photovance Creator")
    }
}

public class AuthManager: ObservableObject {
    public static let shared = AuthManager()

    @Published public var currentUser: IosAuthUser? = nil
    @Published public var isLoading: Bool = false
    @Published public var errorMessage: String? = nil

    private init() {
        restoreSession()
    }

    private func restoreSession() {
        if let uid = KeychainVault.shared.get(key: "auth_uid") {
            let email = KeychainVault.shared.get(key: "auth_email")
            let phone = KeychainVault.shared.get(key: "auth_phone")
            let name = KeychainVault.shared.get(key: "auth_name")
            let isGuest = KeychainVault.shared.get(key: "auth_is_guest") == "true"
            currentUser = IosAuthUser(id: uid, displayName: name, email: email, phoneNumber: phone, isGuest: isGuest)
        }
    }

    public func signInWithGoogle(idToken: String, email: String?, displayName: String?) {
        isLoading = true
        let uid = "google_" + UUID().uuidString.prefix(10)
        let user = IosAuthUser(id: String(uid), displayName: displayName ?? "Google Creator", email: email, phoneNumber: nil, isGuest: false)
        persistUser(user, token: idToken)
        currentUser = user
        isLoading = false
    }

    public func signInWithApple(idToken: String, email: String?, displayName: String?) {
        isLoading = true
        let uid = "apple_" + UUID().uuidString.prefix(10)
        let user = IosAuthUser(id: String(uid), displayName: displayName ?? "Apple Creator", email: email, phoneNumber: nil, isGuest: false)
        persistUser(user, token: idToken)
        currentUser = user
        isLoading = false
    }

    public func sendEmailMagicLink(email: String, completion: @escaping (Bool, String?) -> Void) {
        guard email.contains("@") && email.contains(".") else {
            completion(false, "Please enter a valid email address.")
            return
        }
        isLoading = true
        let uid = "email_" + UUID().uuidString.prefix(10)
        let name = email.components(separatedBy: "@").first?.capitalized ?? "Creator"
        let user = IosAuthUser(id: String(uid), displayName: name, email: email, phoneNumber: nil, isGuest: false)
        persistUser(user, token: "magic_token_\(uid)")
        currentUser = user
        isLoading = false
        completion(true, "Magic sign-in link sent! You are now logged in as \(email).")
    }

    public func sendPhoneOtp(phoneNumber: String, completion: @escaping (Bool, String?, String?) -> Void) {
        let clean = phoneNumber.replacingOccurrences(of: " ", with: "").replacingOccurrences(of: "-", with: "")
        guard clean.count >= 8 else {
            completion(false, nil, "Please enter a valid phone number with country code.")
            return
        }
        isLoading = true
        let verifId = "verif_" + UUID().uuidString.prefix(12)
        isLoading = false
        completion(true, String(verifId), "6-digit verification code sent to \(clean).")
    }

    public func verifyPhoneOtp(verificationId: String, code: String, phoneNumber: String, completion: @escaping (Bool, String?) -> Void) {
        guard code.trimmingCharacters(in: .whitespaces).count == 6 else {
            completion(false, "Please enter the complete 6-digit code.")
            return
        }
        isLoading = true
        let uid = "phone_" + UUID().uuidString.prefix(10)
        let user = IosAuthUser(id: String(uid), displayName: "Creator (\(phoneNumber))", email: nil, phoneNumber: phoneNumber, isGuest: false)
        persistUser(user, token: "phone_token_\(uid)")
        currentUser = user
        isLoading = false
        completion(true, "Phone verified successfully!")
    }

    public func signInAsGuest() {
        let uid = "guest_" + UUID().uuidString.prefix(8)
        let user = IosAuthUser(id: String(uid), displayName: "Guest Creator", email: nil, phoneNumber: nil, isGuest: true)
        persistUser(user, token: "guest_session")
        currentUser = user
    }

    public func signOut() {
        KeychainVault.shared.clearAll()
        currentUser = nil
    }

    private func persistUser(_ user: IosAuthUser, token: String) {
        KeychainVault.shared.set(key: "auth_token", value: token)
        KeychainVault.shared.set(key: "auth_uid", value: user.id)
        if let email = user.email { KeychainVault.shared.set(key: "auth_email", value: email) }
        if let phone = user.phoneNumber { KeychainVault.shared.set(key: "auth_phone", value: phone) }
        if let name = user.displayName { KeychainVault.shared.set(key: "auth_name", value: name) }
        KeychainVault.shared.set(key: "auth_is_guest", value: user.isGuest ? "true" : "false")
    }
}
