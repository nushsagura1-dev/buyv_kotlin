import Foundation
import SwiftUI

class ChangePasswordViewModel: ObservableObject {
    @Published var currentPassword = ""
    @Published var newPassword = ""
    @Published var confirmPassword = ""
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var showSuccess = false
    @Published var showCurrentPassword = false
    @Published var showNewPassword = false
    @Published var showConfirmPassword = false
    
    private let baseURL = ApiConfig.baseURL
    
    // MARK: - Validation
    
    var isFormValid: Bool {
        !currentPassword.isEmpty &&
        newPassword.count >= 6 &&
        confirmPassword == newPassword
    }
    
    var passwordsMatch: Bool {
        !confirmPassword.isEmpty && confirmPassword == newPassword
    }
    
    var passwordStrength: Int {
        var strength = 0
        if newPassword.count >= 6 { strength += 1 }
        if newPassword.count >= 8 { strength += 1 }
        if newPassword.rangeOfCharacter(from: .uppercaseLetters) != nil &&
           newPassword.rangeOfCharacter(from: .lowercaseLetters) != nil { strength += 1 }
        if newPassword.rangeOfCharacter(from: .decimalDigits) != nil ||
           newPassword.rangeOfCharacter(from: .punctuationCharacters) != nil { strength += 1 }
        return strength
    }
    
    var passwordStrengthText: String {
        switch passwordStrength {
        case 0...1: return "Weak"
        case 2: return "Fair"
        case 3: return "Good"
        default: return "Strong"
        }
    }
    
    // MARK: - Requirements checklist
    var requirements: [(text: String, met: Bool)] {
        [
            ("At least 6 characters", newPassword.count >= 6),
            ("At least 8 characters (recommended)", newPassword.count >= 8),
            ("Upper & lowercase letters", newPassword.rangeOfCharacter(from: .uppercaseLetters) != nil &&
                newPassword.rangeOfCharacter(from: .lowercaseLetters) != nil),
            ("Number or special character", newPassword.rangeOfCharacter(from: .decimalDigits) != nil ||
                newPassword.rangeOfCharacter(from: .punctuationCharacters) != nil)
        ]
    }
    
    func passwordStrengthColor(index: Int) -> String {
        guard index < passwordStrength else { return "gray" }
        switch passwordStrength {
        case 0...1: return "red"
        case 2: return "orange"
        case 3: return "yellow"
        default: return "green"
        }
    }
    
    // MARK: - API Call
    
    func changePassword() {
        guard let token = SessionManager.shared.authToken else {
            errorMessage = "Please log in again"
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                let url = URL(string: "\(baseURL)/auth/change-password")!
                var request = URLRequest(url: url)
                request.httpMethod = "POST"
                request.setValue("application/json", forHTTPHeaderField: "Content-Type")
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                
                let body: [String: String] = [
                    "current_password": currentPassword,
                    "new_password": newPassword
                ]
                request.httpBody = try JSONSerialization.data(withJSONObject: body)
                
                let (data, response) = try await URLSession.shared.data(for: request)
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    await MainActor.run {
                        errorMessage = "Network error"
                        isLoading = false
                    }
                    return
                }
                
                if (200...299).contains(httpResponse.statusCode) {
                    await MainActor.run {
                        isLoading = false
                        showSuccess = true
                    }
                } else {
                    let errorDetail: String
                    if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                       let detail = json["detail"] as? String {
                        errorDetail = detail
                    } else {
                        errorDetail = "Failed to change password (HTTP \(httpResponse.statusCode))"
                    }
                    
                    await MainActor.run {
                        errorMessage = errorDetail
                        isLoading = false
                    }
                }
            } catch {
                await MainActor.run {
                    errorMessage = error.localizedDescription
                    isLoading = false
                }
            }
        }
    }
    
    func clearError() {
        errorMessage = nil
    }
}
