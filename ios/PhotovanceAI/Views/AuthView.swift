import SwiftUI
import AuthenticationServices

public struct AuthView: View {
    @ObservedObject var authManager = AuthManager.shared
    public var onAuthenticated: () -> Void

    @State private var selectedTab: Int = 0 // 0 = Email, 1 = Phone
    @State private var email: String = ""
    @State private var phone: String = ""
    @State private var otpCode: String = ""
    @State private var showOtpSheet: Bool = false
    @State private var verificationId: String = ""
    @State private var statusMessage: String? = nil
    @State private var errorMessage: String? = nil

    public init(onAuthenticated: @escaping () -> Void) {
        self.onAuthenticated = onAuthenticated
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Hero Banner
                    ZStack(alignment: .bottomLeading) {
                        Rectangle()
                            .fill(LinearGradient.heroGradient)
                            .frame(height: 180)
                            .clipShape(RoundedRectangle(cornerRadius: 20))

                        LinearGradient(
                            colors: [.clear, Color.bgCanvas.opacity(0.85)],
                            startPoint: .top,
                            endPoint: .bottom
                        )

                        VStack(alignment: .leading, spacing: 6) {
                            Text("A new look.\nStill you.")
                                .font(.system(size: 26, weight: .bold))
                                .foregroundColor(.white)
                            Text("PHOTOVANCE PRO STUDIO")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.neonCyan)
                                .tracking(1.4)
                        }
                        .padding(20)
                    }

                    // Social Sign-In Buttons
                    VStack(spacing: 12) {
                        SignInWithAppleButton(
                            onRequest: { request in
                                request.requestedScopes = [.fullName, .email]
                            },
                            onCompletion: { result in
                                switch result {
                                case .success(let authorization):
                                    if let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential {
                                        let name = [appleIDCredential.fullName?.givenName, appleIDCredential.fullName?.familyName]
                                            .compactMap { $0 }.joined(separator: " ")
                                        let email = appleIDCredential.email
                                        authManager.signInWithApple(idToken: "apple_token", email: email, displayName: name.isEmpty ? nil : name)
                                        onAuthenticated()
                                    }
                                case .failure(let error):
                                    errorMessage = error.localizedDescription
                                }
                            }
                        )
                        .signInWithAppleButtonStyle(.white)
                        .frame(height: 50)
                        .clipShape(RoundedRectangle(cornerRadius: 12))

                        Button {
                            authManager.signInWithGoogle(idToken: "google_token", email: "creator@gmail.com", displayName: "Google Creator")
                            onAuthenticated()
                        } label: {
                            HStack(spacing: 10) {
                                Image(systemName: "g.circle.fill")
                                    .font(.system(size: 20))
                                Text("Continue with Google")
                                    .font(.system(size: 15, weight: .semibold))
                            }
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(Color.white.opacity(0.08))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.18), lineWidth: 1))
                        }
                    }

                    // Divider
                    HStack {
                        Rectangle().fill(Color.white.opacity(0.12)).frame(height: 1)
                        Text("or sign in directly")
                            .font(.system(size: 12))
                            .foregroundColor(.textMuted)
                        Rectangle().fill(Color.white.opacity(0.12)).frame(height: 1)
                    }
                    .padding(.vertical, 4)

                    // Tab Picker
                    Picker("Method", selection: $selectedTab) {
                        Text("✉️ Email (Passwordless)").tag(0)
                        Text("📱 Phone (SMS OTP)").tag(1)
                    }
                    .pickerStyle(.segmented)

                    if selectedTab == 0 {
                        // Passwordless Email
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Passwordless sign-in: Enter your email to receive an instant sign-in link.")
                                .font(.system(size: 12))
                                .foregroundColor(.textMuted)

                            TextField("you@example.com", text: $email)
                                .autocorrectionDisabled()
                                .textInputAutocapitalization(.never)
                                .keyboardType(.emailAddress)
                                .padding(14)
                                .background(Color.bgCard)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.18), lineWidth: 1))

                            Button {
                                authManager.sendEmailMagicLink(email: email) { success, msg in
                                    if success {
                                        onAuthenticated()
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            } label: {
                                Text("Send Magic Sign-In Link")
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 50)
                                    .background(LinearGradient.heroGradient)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                        }
                    } else {
                        // Phone Number SMS
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Enter your mobile number with country code to receive a 6-digit SMS code.")
                                .font(.system(size: 12))
                                .foregroundColor(.textMuted)

                            TextField("+1 555-123-4567", text: $phone)
                                .keyboardType(.phonePad)
                                .padding(14)
                                .background(Color.bgCard)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.18), lineWidth: 1))

                            Button {
                                authManager.sendPhoneOtp(phoneNumber: phone) { success, verifId, msg in
                                    if success, let vId = verifId {
                                        verificationId = vId
                                        showOtpSheet = true
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            } label: {
                                Text("Send 6-Digit SMS Code")
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 50)
                                    .background(LinearGradient.heroGradient)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                        }
                    }

                    if let err = errorMessage {
                        Text(err)
                            .font(.system(size: 12))
                            .foregroundColor(.accentPink)
                    }

                    // Guest Mode
                    Button {
                        authManager.signInAsGuest()
                        onAuthenticated()
                    } label: {
                        HStack {
                            Text("Explore as a guest")
                                .font(.system(size: 14, weight: .medium))
                            Image(systemName: "arrow.right")
                        }
                        .foregroundColor(.textMuted)
                        .padding(.top, 8)
                    }
                }
                .padding(20)
            }
            .background(Color.bgCanvas)
            .sheet(isPresented: $showOtpSheet) {
                VStack(spacing: 18) {
                    Text("Enter 6-Digit SMS Code")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.white)

                    Text("We sent an SMS code to \(phone)")
                        .font(.system(size: 13))
                        .foregroundColor(.textMuted)

                    TextField("123456", text: $otpCode)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.center)
                        .font(.system(size: 26, weight: .bold))
                        .padding(14)
                        .background(Color.bgCard)
                        .clipShape(RoundedRectangle(cornerRadius: 12))

                    Button {
                        authManager.verifyPhoneOtp(verificationId: verificationId, code: otpCode, phoneNumber: phone) { success, msg in
                            if success {
                                showOtpSheet = false
                                onAuthenticated()
                            } else {
                                errorMessage = msg
                            }
                        }
                    } label: {
                        Text("Verify & Sign In")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(LinearGradient.heroGradient)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
                .padding(24)
                .background(Color.bgCanvas)
                .presentationDetents([.fraction(0.45)])
            }
        }
    }
}
