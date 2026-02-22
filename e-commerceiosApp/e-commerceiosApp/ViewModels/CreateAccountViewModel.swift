import Foundation
import Shared

class CreateAccountViewModel: ObservableObject {
    @Published var username = ""
    @Published var email = ""
    @Published var password = ""
    @Published var confirmPassword = ""
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var isRegistered = false
    @Published var acceptedTerms = false
    
    enum RegistrationState {
        case idle, registering, success, error
    }
    @Published var registrationState: RegistrationState = .idle
    
    private let registerUseCase = DependencyWrapper.shared.registerUseCase
    private let googleSignInUseCase = DependencyWrapper.shared.googleSignInUseCase
    
    // MARK: - Username Validation
    var isUsernameValid: Bool {
        username.isEmpty || (username.count >= 3 && username.count <= 20 && username.range(of: "^[a-zA-Z0-9_]+$", options: .regularExpression) != nil)
    }
    
    var usernameError: String? {
        guard !username.isEmpty else { return nil }
        if username.count < 3 { return "Username must be at least 3 characters" }
        if username.count > 20 { return "Username must be at most 20 characters" }
        if username.range(of: "^[a-zA-Z0-9_]+$", options: .regularExpression) == nil {
            return "Only letters, numbers, and underscores allowed"
        }
        return nil
    }
    
    // MARK: - Email Validation
    var isEmailValid: Bool {
        guard !email.isEmpty else { return true }
        let pattern = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        return email.range(of: pattern, options: .regularExpression) != nil
    }
    
    var emailError: String? {
        guard !email.isEmpty else { return nil }
        if !isEmailValid { return "Please enter a valid email address" }
        return nil
    }
    
    // MARK: - Password Validation
    var hasMinLength: Bool { password.count >= 6 }
    var hasUppercase: Bool { password.range(of: "[A-Z]", options: .regularExpression) != nil }
    var hasNumber: Bool { password.range(of: "[0-9]", options: .regularExpression) != nil }
    var hasSpecialChar: Bool { password.range(of: "[^A-Za-z0-9]", options: .regularExpression) != nil }
    
    var passwordStrength: PasswordStrength {
        var score = 0
        if password.count >= 6 { score += 1 }
        if password.count >= 8 { score += 1 }
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
        
        var colorName: String {
            switch self {
            case .weak: return "red"
            case .fair: return "orange"
            case .good: return "yellow"
            case .strong: return "green"
            }
        }
    }
    
    var passwordError: String? {
        guard !password.isEmpty else { return nil }
        if password.count < 6 { return "Password must be at least 6 characters" }
        return nil
    }
    
    var passwordsMatch: Bool {
        confirmPassword.isEmpty || password == confirmPassword
    }
    
    var confirmPasswordError: String? {
        guard !confirmPassword.isEmpty else { return nil }
        if password != confirmPassword { return "Passwords don't match" }
        return nil
    }
    
    // MARK: - Form Validity
    var canRegister: Bool {
        !username.isEmpty && username.count >= 3 &&
        !email.isEmpty && isEmailValid &&
        !password.isEmpty && hasMinLength &&
        password == confirmPassword &&
        acceptedTerms
    }
    
    var formCompletionProgress: CGFloat {
        var completed: CGFloat = 0
        let total: CGFloat = 5
        if !username.isEmpty && isUsernameValid { completed += 1 }
        if !email.isEmpty && isEmailValid { completed += 1 }
        if !password.isEmpty && hasMinLength { completed += 1 }
        if !confirmPassword.isEmpty && passwordsMatch { completed += 1 }
        if acceptedTerms { completed += 1 }
        return completed / total
    }
    
    // MARK: - Register
    func register() {
        guard canRegister else {
            if username.isEmpty {
                errorMessage = "Please enter a username"
            } else if !isUsernameValid {
                errorMessage = usernameError
            } else if email.isEmpty {
                errorMessage = "Please enter an email"
            } else if !isEmailValid {
                errorMessage = "Please enter a valid email"
            } else if password.isEmpty {
                errorMessage = "Please enter a password"
            } else if !hasMinLength {
                errorMessage = "Password must be at least 6 characters"
            } else if password != confirmPassword {
                errorMessage = "Passwords don't match"
            } else if !acceptedTerms {
                errorMessage = "Please accept the Terms of Service"
            }
            return
        }
        
        isLoading = true
        errorMessage = nil
        registrationState = .registering
        
        registerUseCase.invoke(username: username, email: email, password: password) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                
                if let error = error {
                    self.errorMessage = error.localizedDescription
                    self.registrationState = .error
                    return
                }
                
                if result is ResultSuccess<AnyObject> {
                    self.registrationState = .success
                    self.isRegistered = true
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                    self.registrationState = .error
                }
            }
        }
    }
    
    // MARK: - Google Sign-Up
    func signUpWithGoogle(idToken: String) {
        isLoading = true
        errorMessage = nil
        registrationState = .registering
        
        googleSignInUseCase.invoke(idToken: idToken) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                
                if let error = error {
                    self.errorMessage = error.localizedDescription
                    self.registrationState = .error
                    return
                }
                
                if let result = result as? ResultSuccess<UserProfile> {
                    AppLogger.success("Google Sign-Up success")
                    SessionManager.shared.saveUser(result.data)
                    self.registrationState = .success
                    self.isRegistered = true
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                    self.registrationState = .error
                }
            }
        }
    }
    
    // MARK: - Helpers
    func clearError() {
        errorMessage = nil
        registrationState = .idle
    }
    
    func clearAll() {
        username = ""
        email = ""
        password = ""
        confirmPassword = ""
        errorMessage = nil
        acceptedTerms = false
        registrationState = .idle
    }
}
