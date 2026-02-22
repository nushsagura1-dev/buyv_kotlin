import SwiftUI

struct ChangePasswordView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = ChangePasswordViewModel()
    
    var body: some View {
        ZStack {
            AppColors.background.ignoresSafeArea()
            
            if viewModel.showSuccess {
                passwordChangedSuccessView
            } else {
                changePasswordForm
            }
        }
        .navigationTitle("Change Password")
        .navigationBarTitleDisplayMode(.inline)
    }
    
    // MARK: - Change Password Form
    private var changePasswordForm: some View {
        ScrollView {
            VStack(spacing: 24) {
                
                // Header Icon
                ZStack {
                    Circle()
                        .fill(AppColors.primary.opacity(0.1))
                        .frame(width: 100, height: 100)
                    
                    Image(systemName: "lock.rotation")
                        .font(.system(size: 40))
                        .foregroundColor(AppColors.primary)
                }
                .padding(.top, 20)
                
                VStack(spacing: 8) {
                    Text("Change Password")
                        .font(Typography.h2)
                        .foregroundColor(.primary)
                    
                    Text("Enter your current password and choose a new one.")
                        .font(Typography.body2)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }
                
                // Form Fields
                VStack(spacing: 16) {
                    // Current Password
                    passwordField(
                        label: "Current Password",
                        placeholder: "Enter current password",
                        text: $viewModel.currentPassword,
                        isVisible: viewModel.showCurrentPassword,
                        toggleVisibility: { viewModel.showCurrentPassword.toggle() }
                    )
                    
                    // New Password
                    VStack(alignment: .leading, spacing: 6) {
                        passwordField(
                            label: "New Password",
                            placeholder: "Enter new password",
                            text: $viewModel.newPassword,
                            isVisible: viewModel.showNewPassword,
                            toggleVisibility: { viewModel.showNewPassword.toggle() }
                        )
                        
                        // Password strength indicator
                        if !viewModel.newPassword.isEmpty {
                            HStack(spacing: 4) {
                                ForEach(0..<4, id: \.self) { i in
                                    Rectangle()
                                        .fill(strengthBarColor(index: i))
                                        .frame(height: 4)
                                        .cornerRadius(2)
                                }
                            }
                            
                            Text(viewModel.passwordStrengthText)
                                .font(.caption2.weight(.medium))
                                .foregroundColor(strengthBarColor(index: 0))
                        }
                    }
                    
                    // Requirements checklist
                    if !viewModel.newPassword.isEmpty {
                        requirementsChecklist
                    }
                    
                    // Confirm New Password
                    VStack(alignment: .leading, spacing: 6) {
                        passwordField(
                            label: "Confirm New Password",
                            placeholder: "Confirm new password",
                            text: $viewModel.confirmPassword,
                            isVisible: viewModel.showConfirmPassword,
                            toggleVisibility: { viewModel.showConfirmPassword.toggle() },
                            borderColor: viewModel.confirmPassword.isEmpty ? Color.gray.opacity(0.3) :
                                (viewModel.passwordsMatch ? Color.green.opacity(0.5) : Color.red.opacity(0.5))
                        )
                        
                        if !viewModel.confirmPassword.isEmpty && !viewModel.passwordsMatch {
                            HStack(spacing: 4) {
                                Image(systemName: "xmark.circle.fill")
                                    .font(.caption2)
                                Text("Passwords do not match")
                                    .font(.caption2)
                            }
                            .foregroundColor(.red)
                        } else if viewModel.passwordsMatch {
                            HStack(spacing: 4) {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.caption2)
                                Text("Passwords match")
                                    .font(.caption2)
                            }
                            .foregroundColor(.green)
                        }
                    }
                }
                .padding(.horizontal)
                
                // Error Message
                if let error = viewModel.errorMessage {
                    HStack(spacing: 8) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.red)
                        Text(error)
                            .font(.caption)
                            .foregroundColor(.red)
                    }
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color.red.opacity(0.1))
                    .cornerRadius(10)
                    .padding(.horizontal)
                }
                
                // Change Password Button
                Button(action: { viewModel.changePassword() }) {
                    HStack(spacing: 8) {
                        if viewModel.isLoading {
                            ProgressView()
                                .tint(.white)
                        }
                        Text(viewModel.isLoading ? "Changing..." : "Change Password")
                            .font(Typography.button)
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(viewModel.isFormValid ? AppColors.primary : Color.gray)
                    .cornerRadius(12)
                }
                .disabled(!viewModel.isFormValid || viewModel.isLoading)
                .padding(.horizontal)
                
                Spacer()
            }
        }
    }
    
    // MARK: - Requirements Checklist
    private var requirementsChecklist: some View {
        VStack(alignment: .leading, spacing: 6) {
            ForEach(viewModel.requirements, id: \.text) { req in
                HStack(spacing: 8) {
                    Image(systemName: req.met ? "checkmark.circle.fill" : "circle")
                        .font(.caption)
                        .foregroundColor(req.met ? .green : .gray)
                    
                    Text(req.text)
                        .font(.caption)
                        .foregroundColor(req.met ? .primary : .secondary)
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(10)
    }
    
    // MARK: - Password Field Component
    private func passwordField(
        label: String,
        placeholder: String,
        text: Binding<String>,
        isVisible: Bool,
        toggleVisibility: @escaping () -> Void,
        borderColor: Color = Color.gray.opacity(0.3)
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
            
            HStack {
                if isVisible {
                    TextField(placeholder, text: text)
                } else {
                    SecureField(placeholder, text: text)
                }
                Button(action: toggleVisibility) {
                    Image(systemName: isVisible ? "eye.slash" : "eye")
                        .foregroundColor(.secondary)
                }
            }
            .padding()
            .background(Color.white)
            .cornerRadius(10)
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(borderColor))
        }
    }
    
    // MARK: - Strength Bar Color
    private func strengthBarColor(index: Int) -> Color {
        guard index < viewModel.passwordStrength else { return Color.gray.opacity(0.3) }
        switch viewModel.passwordStrength {
        case 0...1: return .red
        case 2: return .orange
        case 3: return .yellow
        default: return .green
        }
    }
    
    // MARK: - Password Changed Success View
    private var passwordChangedSuccessView: some View {
        VStack(spacing: 24) {
            Spacer()
            
            ZStack {
                Circle()
                    .fill(Color.green.opacity(0.1))
                    .frame(width: 120, height: 120)
                
                Circle()
                    .fill(Color.green.opacity(0.2))
                    .frame(width: 90, height: 90)
                
                Image(systemName: "checkmark.shield.fill")
                    .font(.system(size: 44))
                    .foregroundColor(.green)
            }
            
            VStack(spacing: 12) {
                Text("Password Changed!")
                    .font(.title2.weight(.bold))
                    .foregroundColor(.primary)
                
                Text("Your password has been updated successfully. Your account is now secured with your new password.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
            
            VStack(spacing: 8) {
                HStack(spacing: 8) {
                    Image(systemName: "lock.shield.fill")
                        .foregroundColor(.green)
                    Text("Your account is secure")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                HStack(spacing: 8) {
                    Image(systemName: "clock.fill")
                        .foregroundColor(.blue)
                    Text("You may need to log in again on other devices")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(12)
            .padding(.horizontal)
            
            Spacer()
            
            Button(action: { dismiss() }) {
                Text("Done")
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(AppColors.primary)
                    .cornerRadius(12)
            }
            .padding(.horizontal)
            .padding(.bottom, 32)
        }
    }
}
