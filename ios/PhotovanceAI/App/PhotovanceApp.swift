import SwiftUI

@main
struct PhotovanceApp: App {
    init() {
        // Initialize API client from persistent UserDefaults
        ApiClient.shared.initialize()
    }

    var body: some Scene {
        WindowGroup {
            HomeScreenView()
                .preferredColorScheme(.dark)
        }
    }
}
