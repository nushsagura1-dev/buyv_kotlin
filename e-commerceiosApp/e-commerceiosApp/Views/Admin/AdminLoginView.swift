import SwiftUI

struct AdminLoginView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var email = ""
    @State private var password = ""
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var isLoggedIn = false
    
    var body: some View {
        NavigationView {
            ZStack {
                AppColors.background.ignoresSafeArea()
                
                VStack(spacing: 24) {
                    // Logo / Header
                    VStack(spacing: 8) {
                        Image(systemName: "shield.lefthalf.filled")
                            .font(.system(size: 60))
                            .foregroundColor(AppColors.primary)
                        
                        Text("Admin Panel")
                            .font(.title)
                            .fontWeight(.bold)
                        
                        Text("Sign in to manage BuyV")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.top, 40)
                    
                    // Form
                    VStack(spacing: 16) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Email")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            TextField("admin@buyv.app", text: $email)
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                                .autocapitalization(.none)
                                .keyboardType(.emailAddress)
                                .textContentType(.emailAddress)
                        }
                        
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Password")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            SecureField("Enter password", text: $password)
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                                .textContentType(.password)
                        }
                    }
                    .padding(.horizontal, 24)
                    
                    // Error message
                    if let errorMessage = errorMessage {
                        HStack {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.red)
                            Text(errorMessage)
                                .foregroundColor(.red)
                                .font(.caption)
                        }
                        .padding(.horizontal, 24)
                    }
                    
                    // Login Button
                    Button(action: login) {
                        HStack {
                            if isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            }
                            Text(isLoading ? "Signing in..." : "Sign In")
                                .fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(canSubmit ? AppColors.primary : Color.gray)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }
                    .disabled(!canSubmit || isLoading)
                    .padding(.horizontal, 24)
                    
                    Spacer()
                    
                    // Back to app
                    Button(action: { dismiss() }) {
                        Text("Back to App")
                            .foregroundColor(AppColors.primary)
                    }
                    .padding(.bottom, 24)
                }
            }
            .navigationBarHidden(true)
            .fullScreenCover(isPresented: $isLoggedIn) {
                AdminDashboardContainerView()
            }
        }
    }
    
    private var canSubmit: Bool {
        !email.isEmpty && !password.isEmpty
    }
    
    private func login() {
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                let response = try await AdminApiService.shared.login(email: email, password: password)
                await MainActor.run {
                    isLoading = false
                    isLoggedIn = true
                    AppLogger.success("Admin login successful: \(response.admin.username)")
                }
            } catch AdminApiError.unauthorized {
                await MainActor.run {
                    isLoading = false
                    errorMessage = "Invalid email or password"
                }
            } catch AdminApiError.serverError(let msg) {
                await MainActor.run {
                    isLoading = false
                    errorMessage = msg
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    errorMessage = error.localizedDescription
                }
            }
        }
    }
}

/// Container for the logged-in admin dashboard (with logout support)
struct AdminDashboardContainerView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var showLogoutAlert = false
    
    var body: some View {
        NavigationView {
            AdminDashboardView()
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button("Close") { dismiss() }
                    }
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button(action: { showLogoutAlert = true }) {
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                                .foregroundColor(.red)
                        }
                    }
                }
        }
        .alert("Admin Logout", isPresented: $showLogoutAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Logout", role: .destructive) {
                AdminApiService.shared.logout()
                dismiss()
            }
        } message: {
            Text("Are you sure you want to logout from admin panel?")
        }
    }
}
