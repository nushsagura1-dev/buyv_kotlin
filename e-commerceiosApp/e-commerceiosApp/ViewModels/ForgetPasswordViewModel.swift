import Foundation
import Shared

class ForgetPasswordViewModel: ObservableObject {
    @Published var email = ""
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var isEmailSent = false
    @Published var otpCode = ""
    @Published var newPassword = ""
    @Published var confirmPassword = ""
    @Published var isPasswordReset = false
    @Published var resendCooldown: Int = 0
    
    private let sendPasswordResetUseCase = DependencyWrapper.shared.sendPasswordResetUseCase
    private var resendTimer: Timer?
    
    // MARK: - Email Validation
    var isEmailValid: Bool {
        guard !email.isEmpty else { return false }
        let pattern = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        return email.range(of: pattern, options: .regularExpression) != nil
    }
    
    var emailError: String? {
        guard !email.isEmpty else { return nil }
        if !isEmailValid { return "Please enter a valid email address" }
        return nil
    }
    
    var canSendEmail: Bool {
        isEmailValid && resendCooldown == 0
    }
    
    // MARK: - OTP Validation
    var isOtpComplete: Bool {
        otpCode.count == 6 && otpCode.allSatisfy(\.isNumber)
    }
    
    // MARK: - Password Validation
    var hasMinLength: Bool { newPassword.count >= 8 }
    var hasUppercase: Bool { newPassword.range(of: "[A-Z]", options: .regularExpression) != nil }
    var hasNumber: Bool { newPassword.range(of: "[0-9]", options: .regularExpression) != nil }
    var hasSpecialChar: Bool { newPassword.range(of: "[^A-Za-z0-9]", options: .regularExpression) != nil }
    var passwordsMatch: Bool { !confirmPassword.isEmpty && newPassword == confirmPassword }
    
    var passwordStrength: PasswordStrength {
        var score = 0
        if newPassword.count >= 6 { score += 1 }
        if hasMinLength { score += 1 }
        if hasUppercase { score += 1 }
        if hasNumber { score += 1 }
        if hasSpecialChar { score += 1 }
        
        switch score {
        case 0...1: return .weak
        case 2...3: return .fair
        case 4: return .good
        default: return .strong
        }
    }
    
    enum PasswordStrength: String {
        case weak = "Weak"
        case fair = "Fair"
        case good = "Good"
        case strong = "Strong"
        
        var progress: CGFloat {
            switch self {
            case .weak: return 0.25
            case .fair: return 0.5
            case .good: return 0.75
            case .strong: return 1.0
            }
        }
        
        var color: String {
            switch self {
            case .weak: return "red"
            case .fair: return "orange"
            case .good: return "yellow"
            case .strong: return "green"
            }
        }
    }
    
    var canResetPassword: Bool {
        hasMinLength && passwordsMatch
    }
    
    // MARK: - Send Reset Link
    func sendResetLink() {
        guard canSendEmail else {
            if email.isEmpty {
                errorMessage = "Please enter your email address"
            } else if !isEmailValid {
                errorMessage = "Please enter a valid email address"
            }
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        sendPasswordResetUseCase.invoke(email: email) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                
                if let error = error {
                    self.errorMessage = error.localizedDescription
                    return
                }
                
                if result is ResultSuccess<AnyObject> {
                    self.isEmailSent = true
                    self.startResendCooldown()
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                }
            }
        }
    }
    
    // MARK: - Verify OTP (simulated — backend validates on password reset)
    func verifyOtp() -> Bool {
        guard isOtpComplete else {
            errorMessage = "Please enter the complete 6-digit code"
            return false
        }
        return true
    }
    
    // MARK: - Reset Password
    func resetPassword(completion: @escaping (Bool) -> Void) {
        guard canResetPassword else {
            if !hasMinLength {
                errorMessage = "Password must be at least 8 characters"
            } else if !passwordsMatch {
                errorMessage = "Passwords do not match"
            }
            completion(false)
            return
        }
        
        // In a real implementation, this would call a backend endpoint
        // with the OTP code and new password
        isLoading = true
        errorMessage = nil
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            self.isLoading = false
            self.isPasswordReset = true
            completion(true)
        }
    }
    
    // MARK: - Resend Cooldown
    private func startResendCooldown() {
        resendCooldown = 60
        resendTimer?.invalidate()
        resendTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] timer in
            guard let self = self else { timer.invalidate(); return }
            DispatchQueue.main.async {
                if self.resendCooldown > 0 {
                    self.resendCooldown -= 1
                } else {
                    timer.invalidate()
                }
            }
        }
    }
    
    var resendCooldownText: String? {
        guard resendCooldown > 0 else { return nil }
        return "Resend in \(resendCooldown)s"
    }
    
    var canResend: Bool {
        resendCooldown == 0 && !isLoading
    }
    
    // MARK: - Helpers
    func clearError() {
        errorMessage = nil
    }
    
    func clearAll() {
        email = ""
        otpCode = ""
        newPassword = ""
        confirmPassword = ""
        errorMessage = nil
        isEmailSent = false
        isPasswordReset = false
        resendTimer?.invalidate()
        resendCooldown = 0
    }
    
    deinit {
        resendTimer?.invalidate()
    }
}
