import SwiftUI

struct LoginView: View {
    @StateObject private var viewModel = LoginViewModel()
    @State private var showPassword = false
    @State private var rememberMe = false
    @AppStorage("savedEmail") private var savedEmail: String = ""
    
    var body: some View {
        NavigationView {
            ZStack {
                AppColors.background.ignoresSafeArea()
                
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 24) {
                        Spacer(minLength: 32)
                        
                        // Logo + Welcome
                        VStack(spacing: 8) {
                            Text("BuyV")
                                .font(Typography.h1)
                                .foregroundColor(AppColors.primary)
                            Text("Welcome back!")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        
                        // Input Fields
                        VStack(spacing: 16) {
                            // Email
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Email")
                                    .font(.caption)
                                    .fontWeight(.medium)
                                    .foregroundColor(.secondary)
                                
                                HStack(spacing: 10) {
                                    Image(systemName: "envelope.fill")
                                        .foregroundColor(.gray)
                                        .frame(width: 20)
                                    TextField("Enter your email", text: $viewModel.email)
                                        .textInputAutocapitalization(.never)
                                        .keyboardType(.emailAddress)
                                        .autocorrectionDisabled()
                                    
                                    if !viewModel.email.isEmpty {
                                        if isValidEmail(viewModel.email) {
                                            Image(systemName: "checkmark.circle.fill")
                                                .foregroundColor(.green)
                                                .font(.caption)
                                        } else {
                                            Image(systemName: "exclamationmark.circle.fill")
                                                .foregroundColor(.red)
                                                .font(.caption)
                                        }
                                    }
                                }
                                .padding()
                                .background(Color.white)
                                .cornerRadius(12)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(emailBorderColor, lineWidth: 1)
                                )
                            }
                            
                            // Password
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Password")
                                    .font(.caption)
                                    .fontWeight(.medium)
                                    .foregroundColor(.secondary)
                                
                                HStack(spacing: 10) {
                                    Image(systemName: "lock.fill")
                                        .foregroundColor(.gray)
                                        .frame(width: 20)
                                    if showPassword {
                                        TextField("Enter your password", text: $viewModel.password)
                                    } else {
                                        SecureField("Enter your password", text: $viewModel.password)
                                    }
                                    Button(action: { showPassword.toggle() }) {
                                        Image(systemName: showPassword ? "eye.slash.fill" : "eye.fill")
                                            .foregroundColor(.gray)
                                    }
                                }
                                .padding()
                                .background(Color.white)
                                .cornerRadius(12)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                                )
                            }
                        }
                        
                        // Remember Me + Forgot Password
                        HStack {
                            Button(action: { rememberMe.toggle() }) {
                                HStack(spacing: 6) {
                                    Image(systemName: rememberMe ? "checkmark.square.fill" : "square")
                                        .foregroundColor(rememberMe ? AppColors.primary : .gray)
                                        .font(.system(size: 16))
                                    Text("Remember me")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                            
                            Spacer()
                            
                            NavigationLink(destination: ForgetPasswordView()) {
                                Text("Forgot Password?")
                                    .font(.caption)
                                    .fontWeight(.medium)
                                    .foregroundColor(AppColors.primary)
                            }
                        }
                        
                        // Login Button
                        Button(action: {
                            if rememberMe { savedEmail = viewModel.email }
                            viewModel.login()
                        }) {
                            if viewModel.isLoading {
                                ProgressView()
                                    .tint(.white)
                            } else {
                                HStack(spacing: 8) {
                                    Text("Log In")
                                        .font(Typography.button)
                                    Image(systemName: "arrow.right")
                                        .font(.subheadline)
                                }
                                .foregroundColor(.white)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(canLogin ? AppColors.primary : Color.gray)
                        .cornerRadius(12)
                        .disabled(!canLogin || viewModel.isLoading)
                        .shadow(color: AppColors.primary.opacity(canLogin ? 0.3 : 0), radius: 6, y: 3)
                        
                        // Biometric Auth placeholder
                        Button(action: {
                            viewModel.errorMessage = "Biometric login coming soon"
                        }) {
                            HStack(spacing: 8) {
                                Image(systemName: "faceid")
                                    .font(.system(size: 20))
                                Text("Sign in with Face ID")
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                            }
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity)
                            .padding(12)
                            .background(Color(.systemGray6))
                            .cornerRadius(12)
                        }
                        
                        // Social Login Divider
                        HStack(spacing: 12) {
                            Rectangle().fill(Color.gray.opacity(0.3)).frame(height: 1)
                            Text("OR CONTINUE WITH")
                                .font(.caption2)
                                .fontWeight(.medium)
                                .foregroundColor(.secondary)
                            Rectangle().fill(Color.gray.opacity(0.3)).frame(height: 1)
                        }
                        .padding(.vertical, 4)
                        
                        // Social Buttons - side by side
                        HStack(spacing: 12) {
                            // Google Sign-In Button
                            Button(action: {
                                if GoogleSignInService.shared.isEnabled {
                                    GoogleSignInService.shared.signIn { result in
                                        switch result {
                                        case .success(let idToken):
                                            AppLogger.debug("Google Sign-In token received")
                                            viewModel.loginWithGoogle(idToken: idToken)
                                        case .failure(let error):
                                            viewModel.errorMessage = error.localizedDescription
                                        }
                                    }
                                } else {
                                    viewModel.errorMessage = "Google Sign-In requires SDK setup in Xcode"
                                }
                            }) {
                                HStack(spacing: 8) {
                                    Image(systemName: "g.circle.fill")
                                        .font(.title3)
                                    Text("Google")
                                        .font(.subheadline)
                                        .fontWeight(.medium)
                                }
                                .foregroundColor(.primary)
                                .frame(maxWidth: .infinity)
                                .padding(12)
                                .background(Color.white)
                                .cornerRadius(12)
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.gray.opacity(0.3)))
                            }
                            
                            // Apple Sign-In Button
                            Button(action: {
                                viewModel.errorMessage = "Apple Sign-In coming soon"
                            }) {
                                HStack(spacing: 8) {
                                    Image(systemName: "apple.logo")
                                        .font(.title3)
                                    Text("Apple")
                                        .font(.subheadline)
                                        .fontWeight(.medium)
                                }
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(12)
                                .background(Color.black)
                                .cornerRadius(12)
                            }
                        }
                        
                        Spacer(minLength: 16)
                        
                        // Sign Up link
                        HStack(spacing: 4) {
                            Text("Don't have an account?")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            NavigationLink(destination: CreateAccountView()) {
                                Text("Sign Up")
                                    .font(.subheadline)
                                    .fontWeight(.bold)
                                    .foregroundColor(AppColors.primary)
                            }
                        }
                        .padding(.bottom, 16)
                        
                        NavigationLink(destination: MainTabView(), isActive: $viewModel.isLoggedIn) {
                            EmptyView()
                        }
                    }
                    .padding()
                }
            }
            .alert(item: Binding<ErrorWrapper?>(
                get: { viewModel.errorMessage.map { ErrorWrapper(message: $0) } },
                set: { viewModel.errorMessage = $0?.message }
            )) { errorWrapper in
                 Alert(title: Text("Error"), message: Text(errorWrapper.message), dismissButton: .default(Text("OK")))
            }
            .onAppear {
                if !savedEmail.isEmpty && viewModel.email.isEmpty {
                    viewModel.email = savedEmail
                    rememberMe = true
                }
            }
        }
    }
    
    // MARK: - Computed Properties
    private var canLogin: Bool {
        isValidEmail(viewModel.email) && viewModel.password.count >= 6
    }
    
    private var emailBorderColor: Color {
        if viewModel.email.isEmpty { return Color.gray.opacity(0.3) }
        return isValidEmail(viewModel.email) ? Color.green.opacity(0.4) : Color.red.opacity(0.4)
    }
    
    private func isValidEmail(_ email: String) -> Bool {
        let pattern = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        return email.range(of: pattern, options: .regularExpression) != nil
    }
}
