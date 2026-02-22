import SwiftUI
import Shared

struct CreateAccountView: View {
    @StateObject private var viewModel = CreateAccountViewModel()
    @Environment(\.dismiss) private var dismiss
    @State private var showPassword = false
    @State private var showConfirmPassword = false
    
    private var passwordStrengthProgress: CGFloat {
        viewModel.passwordStrength.progress
    }
    
    private var passwordStrengthColor: Color {
        switch viewModel.passwordStrength {
        case .weak: return .red
        case .fair: return .orange
        case .good: return .yellow
        case .strong: return .green
        }
    }
    
    var body: some View {
        ZStack {
            AppColors.background.ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 20) {
                    // Header
                    VStack(spacing: 8) {
                        Image(systemName: "person.crop.circle.badge.plus")
                            .font(.system(size: 50))
                            .foregroundColor(AppColors.primary)
                        
                        Text("Create Account")
                            .font(Typography.h1)
                            .foregroundColor(AppColors.primary)
                        
                        Text("Join BuyV and start shopping")
                            .font(Typography.body2)
                            .foregroundColor(.secondary)
                    }
                    .padding(.top, 16)
                    
                    // Progress bar
                    VStack(alignment: .leading, spacing: 4) {
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                RoundedRectangle(cornerRadius: 3)
                                    .fill(Color.gray.opacity(0.15))
                                    .frame(height: 6)
                                RoundedRectangle(cornerRadius: 3)
                                    .fill(AppColors.primary)
                                    .frame(width: geo.size.width * viewModel.formCompletionProgress, height: 6)
                                    .animation(.easeInOut(duration: 0.3), value: viewModel.formCompletionProgress)
                            }
                        }
                        .frame(height: 6)
                        
                        Text("\(Int(viewModel.formCompletionProgress * 100))% complete")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                    .padding(.horizontal, 4)
                    
                    // Form Fields
                    VStack(spacing: 14) {
                        // Username
                        VStack(alignment: .leading, spacing: 4) {
                            inputField(
                                icon: "person",
                                placeholder: "Username",
                                text: $viewModel.username,
                                keyboardType: .default,
                                autocap: .never
                            )
                            
                            if let error = viewModel.usernameError {
                                fieldErrorLabel(error)
                            } else if !viewModel.username.isEmpty && viewModel.isUsernameValid {
                                fieldSuccessLabel("Username available")
                            }
                        }
                        
                        // Email
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 12) {
                                Image(systemName: "envelope")
                                    .foregroundColor(.gray)
                                    .frame(width: 20)
                                TextField("Email", text: $viewModel.email)
                                    .keyboardType(.emailAddress)
                                    .textInputAutocapitalization(.never)
                                    .autocorrectionDisabled()
                                
                                if !viewModel.email.isEmpty {
                                    Image(systemName: viewModel.isEmailValid ? "checkmark.circle.fill" : "exclamationmark.circle.fill")
                                        .foregroundColor(viewModel.isEmailValid ? .green : .red)
                                        .font(.subheadline)
                                }
                            }
                            .padding()
                            .background(Color(.systemBackground))
                            .cornerRadius(10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(emailBorderColor, lineWidth: 1)
                            )
                            
                            if let error = viewModel.emailError {
                                fieldErrorLabel(error)
                            }
                        }
                        
                        // Password with visibility toggle
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 12) {
                                Image(systemName: "lock")
                                    .foregroundColor(.gray)
                                    .frame(width: 20)
                                
                                if showPassword {
                                    TextField("Password", text: $viewModel.password)
                                        .textInputAutocapitalization(.never)
                                } else {
                                    SecureField("Password", text: $viewModel.password)
                                }
                                
                                Button(action: { showPassword.toggle() }) {
                                    Image(systemName: showPassword ? "eye.slash" : "eye")
                                        .foregroundColor(.gray)
                                }
                            }
                            .padding()
                            .background(Color(.systemBackground))
                            .cornerRadius(10)
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.gray.opacity(0.3)))
                            
                            if let error = viewModel.passwordError {
                                fieldErrorLabel(error)
                            }
                        }
                        
                        // Password strength indicator
                        if !viewModel.password.isEmpty {
                            VStack(alignment: .leading, spacing: 6) {
                                GeometryReader { geo in
                                    ZStack(alignment: .leading) {
                                        RoundedRectangle(cornerRadius: 2)
                                            .fill(Color.gray.opacity(0.2))
                                            .frame(height: 4)
                                        
                                        RoundedRectangle(cornerRadius: 2)
                                            .fill(passwordStrengthColor)
                                            .frame(width: geo.size.width * passwordStrengthProgress, height: 4)
                                            .animation(.easeInOut, value: viewModel.password)
                                    }
                                }
                                .frame(height: 4)
                                
                                HStack(spacing: 4) {
                                    Circle().fill(passwordStrengthColor).frame(width: 6, height: 6)
                                    Text(viewModel.passwordStrength.rawValue)
                                        .font(.caption2)
                                        .foregroundColor(passwordStrengthColor)
                                }
                                
                                // Requirements checklist
                                VStack(alignment: .leading, spacing: 3) {
                                    requirementRow(met: viewModel.hasMinLength, text: "At least 6 characters")
                                    requirementRow(met: viewModel.hasUppercase, text: "One uppercase letter")
                                    requirementRow(met: viewModel.hasNumber, text: "One number")
                                    requirementRow(met: viewModel.hasSpecialChar, text: "One special character")
                                }
                            }
                            .padding(.horizontal, 4)
                        }
                        
                        // Confirm Password
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 12) {
                                Image(systemName: "lock.shield")
                                    .foregroundColor(.gray)
                                    .frame(width: 20)
                                
                                if showConfirmPassword {
                                    TextField("Confirm Password", text: $viewModel.confirmPassword)
                                        .textInputAutocapitalization(.never)
                                } else {
                                    SecureField("Confirm Password", text: $viewModel.confirmPassword)
                                }
                                
                                Button(action: { showConfirmPassword.toggle() }) {
                                    Image(systemName: showConfirmPassword ? "eye.slash" : "eye")
                                        .foregroundColor(.gray)
                                }
                                
                                // Match indicator
                                if !viewModel.confirmPassword.isEmpty {
                                    Image(systemName: viewModel.passwordsMatch ? "checkmark.circle.fill" : "xmark.circle.fill")
                                        .foregroundColor(viewModel.passwordsMatch ? .green : .red)
                                }
                            }
                            .padding()
                            .background(Color(.systemBackground))
                            .cornerRadius(10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(confirmPasswordBorderColor, lineWidth: 1)
                            )
                            
                            if let error = viewModel.confirmPasswordError {
                                fieldErrorLabel(error)
                            }
                        }
                    }
                    
                    // Terms acceptance
                    Button(action: { viewModel.acceptedTerms.toggle() }) {
                        HStack(alignment: .top, spacing: 10) {
                            Image(systemName: viewModel.acceptedTerms ? "checkmark.square.fill" : "square")
                                .foregroundColor(viewModel.acceptedTerms ? AppColors.primary : .gray)
                            
                            Text("I agree to the **Terms of Service** and **Privacy Policy**")
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.leading)
                        }
                    }
                    .buttonStyle(PlainButtonStyle())
                    
                    // Sign Up Button
                    Button(action: viewModel.register) {
                        if viewModel.isLoading {
                            ProgressView()
                                .tint(.white)
                        } else {
                            HStack(spacing: 8) {
                                Text("Create Account")
                                    .font(Typography.button)
                                Image(systemName: "arrow.right")
                                    .font(.subheadline)
                            }
                            .foregroundColor(.white)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(viewModel.canRegister ? AppColors.primary : Color.gray.opacity(0.4))
                    .cornerRadius(12)
                    .disabled(viewModel.isLoading || !viewModel.canRegister)
                    .shadow(color: AppColors.primary.opacity(viewModel.canRegister ? 0.3 : 0), radius: 6, y: 3)
                    
                    // Divider
                    HStack(spacing: 12) {
                        Rectangle().fill(Color.gray.opacity(0.3)).frame(height: 1)
                        Text("OR CONTINUE WITH")
                            .font(.caption2)
                            .fontWeight(.medium)
                            .foregroundColor(.secondary)
                        Rectangle().fill(Color.gray.opacity(0.3)).frame(height: 1)
                    }
                    
                    // Social Sign Up
                    HStack(spacing: 12) {
                        Button(action: {
                            viewModel.errorMessage = "Apple Sign-Up coming soon"
                        }) {
                            HStack(spacing: 8) {
                                Image(systemName: "apple.logo")
                                    .font(.title3)
                                Text("Apple")
                                    .fontWeight(.medium)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(12)
                            .foregroundColor(.white)
                            .background(Color.black)
                            .cornerRadius(12)
                        }
                        
                        Button(action: {
                            if GoogleSignInService.shared.isEnabled {
                                GoogleSignInService.shared.signIn { result in
                                    switch result {
                                    case .success(let idToken):
                                        AppLogger.debug("Google Sign-Up token received")
                                        viewModel.signUpWithGoogle(idToken: idToken)
                                    case .failure(let error):
                                        viewModel.errorMessage = error.localizedDescription
                                    }
                                }
                            } else {
                                viewModel.errorMessage = "Google Sign-Up requires SDK setup in Xcode"
                            }
                        }) {
                            HStack(spacing: 8) {
                                Image(systemName: "g.circle.fill")
                                    .font(.title3)
                                Text("Google")
                                    .fontWeight(.medium)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(12)
                            .foregroundColor(.primary)
                            .background(Color(.systemBackground))
                            .cornerRadius(12)
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.gray.opacity(0.3)))
                        }
                    }
                    
                    // Login Link
                    HStack(spacing: 4) {
                        Text("Already have an account?")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        Button("Log In") {
                            dismiss()
                        }
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.primary)
                    }
                    .padding(.bottom, 20)
                }
                .padding()
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .alert(item: Binding<ErrorWrapper?>(
            get: { viewModel.errorMessage.map { ErrorWrapper(message: $0) } },
            set: { viewModel.errorMessage = $0?.message }
        )) { errorWrapper in
            Alert(title: Text("Error"), message: Text(errorWrapper.message), dismissButton: .default(Text("OK")))
        }
        .alert("Account Created", isPresented: $viewModel.isRegistered) {
            Button("OK") { dismiss() }
        } message: {
            Text("Your account has been created successfully. Please log in.")
        }
    }
    
    // MARK: - Computed Properties
    private var emailBorderColor: Color {
        if viewModel.email.isEmpty { return Color.gray.opacity(0.3) }
        return viewModel.isEmailValid ? Color.green.opacity(0.4) : Color.red.opacity(0.4)
    }
    
    private var confirmPasswordBorderColor: Color {
        if viewModel.confirmPassword.isEmpty { return Color.gray.opacity(0.3) }
        return viewModel.passwordsMatch ? Color.green.opacity(0.4) : Color.red.opacity(0.4)
    }
    
    // MARK: - Helper Views
    private func requirementRow(met: Bool, text: String) -> some View {
        HStack(spacing: 4) {
            Image(systemName: met ? "checkmark.circle.fill" : "circle")
                .font(.caption2)
                .foregroundColor(met ? .green : .gray)
            Text(text)
                .font(.caption2)
                .foregroundColor(met ? .primary : .gray)
        }
    }
    
    private func fieldErrorLabel(_ text: String) -> some View {
        HStack(spacing: 4) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.caption2)
            Text(text)
                .font(.caption)
        }
        .foregroundColor(.red)
        .padding(.horizontal, 4)
    }
    
    private func fieldSuccessLabel(_ text: String) -> some View {
        HStack(spacing: 4) {
            Image(systemName: "checkmark.circle.fill")
                .font(.caption2)
            Text(text)
                .font(.caption)
        }
        .foregroundColor(.green)
        .padding(.horizontal, 4)
    }
    
    private func inputField(icon: String, placeholder: String, text: Binding<String>, keyboardType: UIKeyboardType = .default, autocap: TextInputAutocapitalization = .sentences) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(.gray)
                .frame(width: 20)
            TextField(placeholder, text: text)
                .keyboardType(keyboardType)
                .textInputAutocapitalization(autocap)
                .autocorrectionDisabled()
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(10)
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.gray.opacity(0.3)))
    }
}
    }
}
