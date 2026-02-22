import SwiftUI
import Shared

struct DeleteAccountView: View {
    @StateObject private var viewModel = DeleteAccountViewModel()
    @Environment(\.dismiss) private var dismiss
    @State private var confirmText = ""
    @State private var hasAcknowledged = false
    
    var body: some View {
        NavigationView {
            ZStack {
                AppColors.background.ignoresSafeArea()
                
                if viewModel.isLoading {
                    deletionProgressView
                } else {
                    ScrollView {
                        VStack(spacing: 20) {
                            // Warning header
                            warningHeader
                            
                            // Data deletion list
                            dataDeletionList
                            
                            // Acknowledgment checkbox
                            acknowledgmentToggle
                            
                            // Confirmation input
                            confirmationInput
                            
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
                            }
                            
                            // Delete button
                            Button(action: { viewModel.deleteAccount() }) {
                                HStack(spacing: 8) {
                                    Image(systemName: "trash.fill")
                                    Text("Delete My Account Permanently")
                                        .fontWeight(.semibold)
                                }
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(isFormValid ? Color.red : Color.gray)
                                .cornerRadius(12)
                            }
                            .disabled(!isFormValid)
                            
                            // Cancel button
                            Button(action: { dismiss() }) {
                                Text("Cancel, Keep My Account")
                                    .font(.subheadline.weight(.medium))
                                    .foregroundColor(AppColors.primary)
                                    .frame(maxWidth: .infinity)
                                    .padding()
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12)
                                            .stroke(AppColors.primary, lineWidth: 1)
                                    )
                            }
                            
                            Spacer(minLength: 20)
                        }
                        .padding()
                    }
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark")
                            .foregroundColor(.primary)
                    }
                }
            }
        }
        .onChange(of: viewModel.isDeleted) { deleted in
            if deleted {
                dismiss()
            }
        }
    }
    
    private var isFormValid: Bool {
        confirmText == "DELETE" && hasAcknowledged && !viewModel.isLoading
    }
    
    // MARK: - Warning Header
    private var warningHeader: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(Color.red.opacity(0.1))
                    .frame(width: 100, height: 100)
                
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 44))
                    .foregroundColor(.red)
            }
            
            Text("Delete Account")
                .font(.title2.weight(.bold))
                .foregroundColor(.red)
            
            Text("This action is **permanent** and **cannot be undone**. All your data will be permanently deleted from our servers.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
        }
        .padding(.top, 8)
    }
    
    // MARK: - Data Deletion List
    private var dataDeletionList: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("The following data will be deleted:")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.primary)
                .padding(.bottom, 4)
            
            ForEach(viewModel.deletionItems, id: \.title) { item in
                HStack(spacing: 12) {
                    Image(systemName: item.icon)
                        .font(.subheadline)
                        .foregroundColor(.red.opacity(0.7))
                        .frame(width: 28)
                    
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.title)
                            .font(.subheadline.weight(.medium))
                        Text(item.detail)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    Image(systemName: "xmark.circle.fill")
                        .font(.caption)
                        .foregroundColor(.red.opacity(0.5))
                }
                .padding(.vertical, 8)
                .padding(.horizontal, 12)
                .background(Color.red.opacity(0.03))
                .cornerRadius(8)
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
    
    // MARK: - Acknowledgment Toggle
    private var acknowledgmentToggle: some View {
        Button(action: { hasAcknowledged.toggle() }) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: hasAcknowledged ? "checkmark.square.fill" : "square")
                    .font(.title3)
                    .foregroundColor(hasAcknowledged ? .red : .gray)
                
                Text("I understand that deleting my account is permanent. I will lose all my data, posts, followers, and order history. This action cannot be reversed.")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.leading)
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
    
    // MARK: - Confirmation Input
    private var confirmationInput: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Type **DELETE** to confirm")
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            TextField("DELETE", text: $confirmText)
                .padding()
                .background(Color.white)
                .cornerRadius(10)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(
                            confirmText == "DELETE" ? Color.red : Color.gray.opacity(0.3),
                            lineWidth: confirmText == "DELETE" ? 2 : 1
                        )
                )
                .textInputAutocapitalization(.characters)
            
            if !confirmText.isEmpty && confirmText != "DELETE" {
                Text("Please type DELETE exactly")
                    .font(.caption2)
                    .foregroundColor(.orange)
            }
        }
    }
    
    // MARK: - Deletion Progress View
    private var deletionProgressView: some View {
        VStack(spacing: 24) {
            Spacer()
            
            ZStack {
                Circle()
                    .stroke(Color.gray.opacity(0.2), lineWidth: 6)
                    .frame(width: 100, height: 100)
                
                Circle()
                    .trim(from: 0, to: viewModel.deletionProgress)
                    .stroke(Color.red, style: StrokeStyle(lineWidth: 6, lineCap: .round))
                    .frame(width: 100, height: 100)
                    .rotationEffect(.degrees(-90))
                    .animation(.easeInOut(duration: 0.5), value: viewModel.deletionProgress)
                
                Image(systemName: "trash.fill")
                    .font(.system(size: 32))
                    .foregroundColor(.red)
            }
            
            VStack(spacing: 8) {
                Text("Deleting Account")
                    .font(.title3.weight(.bold))
                    .foregroundColor(.primary)
                
                Text(viewModel.currentPhase.rawValue)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .animation(.easeInOut, value: viewModel.currentPhase.rawValue)
                
                Text("\(Int(viewModel.deletionProgress * 100))%")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Text("Please do not close the app during this process.")
                .font(.caption)
                .foregroundColor(.orange)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            
            Spacer()
        }
    }
}
