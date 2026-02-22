import Foundation
import SwiftUI
import Shared

class LoginViewModel: ObservableObject {
    @Published var email = ""
    @Published var password = ""
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var isLoggedIn = false
    @Published var loginAttempts = 0
    
    enum LoginState {
        case idle, authenticating, success, error
    }
    @Published var loginState: LoginState = .idle
    
    // MARK: - Field-Level Validation
    var isEmailValid: Bool {
        guard !email.isEmpty else { return true } // Don't show error when empty
        let pattern = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        return email.range(of: pattern, options: .regularExpression) != nil
    }
    
    var emailError: String? {
        guard !email.isEmpty else { return nil }
        if !isEmailValid { return "Please enter a valid email address" }
        return nil
    }
    
    var isPasswordValid: Bool {
        password.isEmpty || password.count >= 6
    }
    
    var passwordError: String? {
        guard !password.isEmpty else { return nil }
        if password.count < 6 { return "Password must be at least 6 characters" }
        return nil
    }
    
    var canLogin: Bool {
        !email.isEmpty && !password.isEmpty && isEmailValid && password.count >= 6
    }
    
    var isAccountLocked: Bool {
        loginAttempts >= maxLoginAttempts
    }
    
    private let maxLoginAttempts = 5
    private let loginUseCase = DependencyWrapper.shared.loginUseCase
    private let googleSignInUseCase = DependencyWrapper.shared.googleSignInUseCase
    
    // MARK: - Login
    func login() {
        guard canLogin else {
            if email.isEmpty || password.isEmpty {
                errorMessage = "Please enter email and password"
            } else if !isEmailValid {
                errorMessage = "Please enter a valid email address"
            } else if password.count < 6 {
                errorMessage = "Password must be at least 6 characters"
            }
            return
        }
        
        guard !isAccountLocked else {
            errorMessage = "Too many login attempts. Please try again later or reset your password."
            return
        }
        
        isLoading = true
        errorMessage = nil
        loginState = .authenticating
        
        loginUseCase.invoke(email: email, password: password) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                
                if let error = error {
                    self.loginAttempts += 1
                    self.errorMessage = error.localizedDescription
                    self.loginState = .error
                    return
                }
                
                if let result = result as? ResultSuccess<UserProfile> {
                    AppLogger.success("Login success")
                    SessionManager.shared.saveUser(result.data)
                    self.loginState = .success
                    self.loginAttempts = 0
                    self.isLoggedIn = true
                } else if let errorResult = result as? ResultError {
                    self.loginAttempts += 1
                    self.errorMessage = errorResult.error.message
                    self.loginState = .error
                }
            }
        }
    }
    
    // MARK: - Social Login
    func loginWithGoogle(idToken: String) {
        isLoading = true
        errorMessage = nil
        loginState = .authenticating
        
        googleSignInUseCase.invoke(idToken: idToken) { result, error in
            DispatchQueue.main.async {
                self.isLoading = false
                
                if let error = error {
                    self.errorMessage = error.localizedDescription
                    self.loginState = .error
                    return
                }
                
                if let result = result as? ResultSuccess<UserProfile> {
                    AppLogger.success("Google Sign-In success")
                    SessionManager.shared.saveUser(result.data)
                    self.loginState = .success
                    self.isLoggedIn = true
                } else if let errorResult = result as? ResultError {
                    self.errorMessage = errorResult.error.message
                    self.loginState = .error
                }
            }
        }
    }
    
    func loginWithApple(identityToken: String) {
        isLoading = true
        errorMessage = nil
        loginState = .authenticating
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.isLoading = false
            self.errorMessage = "Apple Sign-In integration pending"
            self.loginState = .error
        }
    }
    
    // MARK: - Helpers
    func clearError() {
        errorMessage = nil
        loginState = .idle
    }
    
    func clearFields() {
        email = ""
        password = ""
        errorMessage = nil
        loginState = .idle
    }
    
    var remainingAttempts: Int {
        max(0, maxLoginAttempts - loginAttempts)
    }
    
    var attemptsWarning: String? {
        guard loginAttempts >= 3 && !isAccountLocked else { return nil }
        return "\(remainingAttempts) attempt(s) remaining before lockout"
    }
}
