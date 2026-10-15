import SwiftUI

@main
struct PhotovanceApp: App {
    @ObservedObject var authManager = AuthManager.shared
    @State private var hasEnteredStudio: Bool = false

    init() {
        // Initialize API client from persistent UserDefaults
        ApiClient.shared.initialize()
    }

    var body: some Scene {
        WindowGroup {
            if authManager.currentUser != nil || hasEnteredStudio {
                HomeScreenView()
                    .preferredColorScheme(.dark)
            } else {
                AuthView {
                    hasEnteredStudio = true
                }
                .preferredColorScheme(.dark)
            }
        }
    }
}
