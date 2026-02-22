import SwiftUI
import Shared

/// Multi-step password reset: Email → OTP Verification → New Password → Success
/// Equivalent to Android ForgotPasswordScreen
struct ForgetPasswordView: View {
    @StateObject private var viewModel = ForgetPasswordViewModel()
    @Environment(\.dismiss) private var dismiss
    
    enum ResetStep: Int, CaseIterable {
        case email = 0
        case verification = 1
        case newPassword = 2
        case success = 3
    }
    
    @State private var currentStep: ResetStep = .email
    @State private var otpCode: String = ""
    @State private var newPassword: String = ""
    @State private var confirmPassword: String = ""
    @State private var showPassword: Bool = false
    @State private var showConfirmPassword: Bool = false
    
    var body: some View {
        ZStack {
            AppColors.background.ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Progress indicator
                if currentStep != .success {
                    stepIndicator
                        .padding(.top, 8)
                        .padding(.bottom, 16)
                }
                
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 24) {
                        Spacer(minLength: 20)
                        
                        switch currentStep {
                        case .email:
                            emailStep
                        case .verification:
                            verificationStep
                        case .newPassword:
                            newPasswordStep
                        case .success:
                            successStep
                        }
                        
                        Spacer(minLength: 40)
                    }
                    .padding(.horizontal)
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .navigationTitle("Reset Password")
        .alert(item: Binding<ErrorWrapper?>(
            get: { viewModel.errorMessage.map { ErrorWrapper(message: $0) } },
            set: { viewModel.errorMessage = $0?.message }
        )) { errorWrapper in
            Alert(title: Text("Error"), message: Text(errorWrapper.message), dismissButton: .default(Text("OK")))
        }
        .alert("Email Sent", isPresented: $viewModel.isEmailSent) {
            Button("Continue") {
                withAnimation(.easeInOut) {
                    currentStep = .verification
                }
                viewModel.isEmailSent = false
            }
        } message: {
            Text("A verification code has been sent to \(viewModel.email).")
        }
    }
    
    // MARK: - Step Indicator
    private var stepIndicator: some View {
        HStack(spacing: 4) {
            ForEach(0..<3, id: \.self) { index in
                RoundedRectangle(cornerRadius: 2)
                    .fill(index <= currentStep.rawValue ? AppColors.primary : Color.gray.opacity(0.3))
                    .frame(height: 4)
                    .animation(.easeInOut, value: currentStep)
            }
        }
        .padding(.horizontal)
    }
    
    // MARK: - Step 1: Email
    private var emailStep: some View {
        VStack(spacing: 20) {
            // Icon
            ZStack {
                Circle()
                    .fill(AppColors.primary.opacity(0.1))
                    .frame(width: 100, height: 100)
                Image(systemName: "envelope.badge.shield.half.filled")
                    .font(.system(size: 44))
                    .foregroundColor(AppColors.primary)
            }
            
            Text("Forgot Password?")
                .font(Typography.h2)
                .foregroundColor(.primary)
            
            Text("Don't worry! Enter the email address associated with your account and we'll send you a verification code.")
                .font(Typography.body2)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            // Email field with validation
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 10) {
                    Image(systemName: "envelope.fill")
                        .foregroundColor(.gray)
                        .frame(width: 20)
                    TextField("Email address", text: $viewModel.email)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.emailAddress)
                        .autocorrectionDisabled()
                }
                .padding()
                .background(Color.white)
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(emailBorderColor, lineWidth: 1)
                )
                
                if !viewModel.email.isEmpty && !isValidEmail(viewModel.email) {
                    HStack(spacing: 4) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.caption2)
                        Text("Please enter a valid email address")
                            .font(.caption)
                    }
                    .foregroundColor(.red)
                }
            }
            .padding(.top, 8)
            
            // Send button
            Button(action: viewModel.sendResetLink) {
                if viewModel.isLoading {
                    ProgressView()
                        .tint(.white)
                } else {
                    HStack(spacing: 8) {
                        Image(systemName: "paperplane.fill")
                        Text("Send Verification Code")
                    }
                    .font(Typography.button)
                    .foregroundColor(.white)
                }
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(canSendEmail ? AppColors.primary : Color.gray)
            .cornerRadius(12)
            .disabled(!canSendEmail || viewModel.isLoading)
            
            Button(action: { dismiss() }) {
                HStack(spacing: 4) {
                    Image(systemName: "arrow.left")
                    Text("Back to Login")
                }
                .foregroundColor(AppColors.primary)
            }
        }
    }
    
    // MARK: - Step 2: OTP Verification
    private var verificationStep: some View {
        VStack(spacing: 20) {
            ZStack {
                Circle()
                    .fill(Color.blue.opacity(0.1))
                    .frame(width: 100, height: 100)
                Image(systemName: "lock.shield.fill")
                    .font(.system(size: 44))
                    .foregroundColor(.blue)
            }
            
            Text("Verify Code")
                .font(Typography.h2)
            
            Text("Enter the 6-digit code sent to")
                .font(Typography.body2)
                .foregroundColor(.secondary)
            Text(viewModel.email)
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(AppColors.primary)
            
            // OTP Input
            HStack(spacing: 8) {
                ForEach(0..<6, id: \.self) { index in
                    let char = index < otpCode.count ? String(otpCode[otpCode.index(otpCode.startIndex, offsetBy: index)]) : ""
                    Text(char)
                        .font(.title2)
                        .fontWeight(.bold)
                        .frame(width: 45, height: 55)
                        .background(Color.white)
                        .cornerRadius(10)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(index < otpCode.count ? AppColors.primary : Color.gray.opacity(0.3), lineWidth: index < otpCode.count ? 2 : 1)
                        )
                }
            }
            .overlay(
                TextField("", text: Binding(
                    get: { otpCode },
                    set: { newVal in
                        let filtered = newVal.filter { $0.isNumber }
                        otpCode = String(filtered.prefix(6))
                    }
                ))
                .keyboardType(.numberPad)
                .opacity(0.01)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            )
            
            // Verify button
            Button(action: {
                withAnimation(.easeInOut) {
                    currentStep = .newPassword
                }
            }) {
                HStack(spacing: 8) {
                    Image(systemName: "checkmark.shield.fill")
                    Text("Verify Code")
                }
                .font(Typography.button)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding()
                .background(otpCode.count == 6 ? AppColors.primary : Color.gray)
                .cornerRadius(12)
            }
            .disabled(otpCode.count != 6)
            
            // Resend
            HStack(spacing: 4) {
                Text("Didn't receive the code?")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                Button("Resend") {
                    viewModel.sendResetLink()
                }
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(AppColors.primary)
            }
            .padding(.top, 8)
            
            Button(action: {
                withAnimation { currentStep = .email }
            }) {
                HStack(spacing: 4) {
                    Image(systemName: "arrow.left")
                    Text("Change email")
                }
                .foregroundColor(.secondary)
            }
        }
    }
    
    // MARK: - Step 3: New Password
    private var newPasswordStep: some View {
        VStack(spacing: 20) {
            ZStack {
                Circle()
                    .fill(Color.green.opacity(0.1))
                    .frame(width: 100, height: 100)
                Image(systemName: "key.fill")
                    .font(.system(size: 44))
                    .foregroundColor(.green)
            }
            
            Text("Create New Password")
                .font(Typography.h2)
            
            Text("Your new password must be different from previously used passwords.")
                .font(Typography.body2)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            // New Password field
            VStack(alignment: .leading, spacing: 6) {
                Text("New Password")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                HStack {
                    if showPassword {
                        TextField("Enter new password", text: $newPassword)
                    } else {
                        SecureField("Enter new password", text: $newPassword)
                    }
                    Button(action: { showPassword.toggle() }) {
                        Image(systemName: showPassword ? "eye.slash.fill" : "eye.fill")
                            .foregroundColor(.gray)
                    }
                }
                .padding()
                .background(Color.white)
                .cornerRadius(12)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.gray.opacity(0.3)))
                
                // Password strength
                if !newPassword.isEmpty {
                    passwordStrengthView
                }
            }
            
            // Confirm Password field
            VStack(alignment: .leading, spacing: 6) {
                Text("Confirm Password")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                HStack {
                    if showConfirmPassword {
                        TextField("Confirm new password", text: $confirmPassword)
                    } else {
                        SecureField("Confirm new password", text: $confirmPassword)
                    }
                    Button(action: { showConfirmPassword.toggle() }) {
                        Image(systemName: showConfirmPassword ? "eye.slash.fill" : "eye.fill")
                            .foregroundColor(.gray)
                    }
                }
                .padding()
                .background(Color.white)
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(confirmPasswordBorderColor, lineWidth: 1)
                )
                
                if !confirmPassword.isEmpty && newPassword != confirmPassword {
                    HStack(spacing: 4) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.caption2)
                        Text("Passwords do not match")
                            .font(.caption)
                    }
                    .foregroundColor(.red)
                }
                
                if !confirmPassword.isEmpty && newPassword == confirmPassword {
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.caption2)
                        Text("Passwords match")
                            .font(.caption)
                    }
                    .foregroundColor(.green)
                }
            }
            
            // Reset button
            Button(action: {
                withAnimation(.easeInOut) {
                    currentStep = .success
                }
            }) {
                HStack(spacing: 8) {
                    Image(systemName: "lock.rotation")
                    Text("Reset Password")
                }
                .font(Typography.button)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding()
                .background(canResetPassword ? AppColors.primary : Color.gray)
                .cornerRadius(12)
            }
            .disabled(!canResetPassword)
        }
    }
    
    // MARK: - Step 4: Success
    private var successStep: some View {
        VStack(spacing: 24) {
            Spacer(minLength: 40)
            
            ZStack {
                Circle()
                    .fill(Color.green.opacity(0.1))
                    .frame(width: 120, height: 120)
                Circle()
                    .fill(Color.green.opacity(0.2))
                    .frame(width: 90, height: 90)
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 56))
                    .foregroundColor(.green)
            }
            
            Text("Password Reset!")
                .font(Typography.h2)
                .foregroundColor(.primary)
            
            Text("Your password has been successfully reset. You can now log in with your new password.")
                .font(Typography.body2)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Button(action: { dismiss() }) {
                HStack(spacing: 8) {
                    Image(systemName: "arrow.right.circle.fill")
                    Text("Back to Login")
                }
                .font(Typography.button)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding()
                .background(AppColors.primary)
                .cornerRadius(12)
            }
            .padding(.top, 16)
            
            Spacer(minLength: 40)
        }
    }
    
    // MARK: - Password Strength View
    private var passwordStrengthView: some View {
        VStack(alignment: .leading, spacing: 6) {
            // Strength bar
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 2)
                        .fill(Color.gray.opacity(0.2))
                        .frame(height: 4)
                    RoundedRectangle(cornerRadius: 2)
                        .fill(passwordStrengthColor)
                        .frame(width: geo.size.width * passwordStrengthProgress, height: 4)
                        .animation(.easeInOut, value: newPassword)
                }
            }
            .frame(height: 4)
            
            // Strength label
            HStack(spacing: 4) {
                Circle().fill(passwordStrengthColor).frame(width: 6, height: 6)
                Text(passwordStrengthLabel)
                    .font(.caption)
                    .foregroundColor(passwordStrengthColor)
            }
            
            // Requirements checklist
            VStack(alignment: .leading, spacing: 3) {
                requirementRow(met: newPassword.count >= 8, text: "At least 8 characters")
                requirementRow(met: newPassword.range(of: "[A-Z]", options: .regularExpression) != nil, text: "One uppercase letter")
                requirementRow(met: newPassword.range(of: "[0-9]", options: .regularExpression) != nil, text: "One number")
                requirementRow(met: newPassword.range(of: "[^A-Za-z0-9]", options: .regularExpression) != nil, text: "One special character")
            }
        }
    }
    
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
    
    // MARK: - Computed Properties
    private var canSendEmail: Bool {
        isValidEmail(viewModel.email) && !viewModel.email.isEmpty
    }
    
    private var canResetPassword: Bool {
        newPassword.count >= 8 && newPassword == confirmPassword
    }
    
    private var emailBorderColor: Color {
        if viewModel.email.isEmpty { return Color.gray.opacity(0.3) }
        return isValidEmail(viewModel.email) ? Color.green.opacity(0.5) : Color.red.opacity(0.5)
    }
    
    private var confirmPasswordBorderColor: Color {
        if confirmPassword.isEmpty { return Color.gray.opacity(0.3) }
        return newPassword == confirmPassword ? Color.green.opacity(0.5) : Color.red.opacity(0.5)
    }
    
    private var passwordStrengthProgress: CGFloat {
        var score: Double = 0
        if newPassword.count >= 6 { score += 0.25 }
        if newPassword.count >= 8 { score += 0.1 }
        if newPassword.range(of: "[A-Z]", options: .regularExpression) != nil { score += 0.2 }
        if newPassword.range(of: "[0-9]", options: .regularExpression) != nil { score += 0.2 }
        if newPassword.range(of: "[^A-Za-z0-9]", options: .regularExpression) != nil { score += 0.25 }
        return min(score, 1.0)
    }
    
    private var passwordStrengthColor: Color {
        let progress = passwordStrengthProgress
        if progress < 0.35 { return .red }
        if progress < 0.65 { return .orange }
        if progress < 0.85 { return .yellow }
        return .green
    }
    
    private var passwordStrengthLabel: String {
        let progress = passwordStrengthProgress
        if progress < 0.35 { return "Weak" }
        if progress < 0.65 { return "Fair" }
        if progress < 0.85 { return "Good" }
        return "Strong"
    }
    
    private func isValidEmail(_ email: String) -> Bool {
        let pattern = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        return email.range(of: pattern, options: .regularExpression) != nil
    }
}
