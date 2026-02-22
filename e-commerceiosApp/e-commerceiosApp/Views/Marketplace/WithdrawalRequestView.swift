import SwiftUI

struct WithdrawalRequestView: View {
    @StateObject private var viewModel = WithdrawalRequestViewModel()
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        Group {
            if viewModel.isLoading {
                VStack(spacing: 16) {
                    ProgressView()
                        .scaleEffect(1.2)
                    Text("Loading withdrawal info...")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                withdrawalForm
            }
        }
        .navigationTitle("Request Withdrawal")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { viewModel.loadStats() }
        .alert("Withdrawal Submitted", isPresented: $viewModel.isSuccess) {
            Button("OK") {
                dismiss()
            }
        } message: {
            Text("Your withdrawal request for $\(String(format: "%.2f", viewModel.submittedAmount)) has been submitted successfully. It will be processed within 3-5 business days.")
        }
    }
    
    // MARK: - Form Content
    private var withdrawalForm: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Gradient Header
                gradientHeader
                
                // Balance Overview
                balanceCard
                
                // Quick Amount Buttons
                quickAmountButtons
                
                // Amount Input
                amountSection
                
                // Payment Method
                paymentMethodSection
                
                // Payment Details
                paymentDetailsSection
                
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
                
                // Submit Button
                Button(action: { viewModel.submit() }) {
                    HStack(spacing: 8) {
                        if viewModel.isSubmitting {
                            ProgressView()
                                .tint(.white)
                        }
                        Image(systemName: viewModel.isSubmitting ? "arrow.triangle.2.circlepath" : "paperplane.fill")
                            .font(.subheadline)
                        Text(viewModel.isSubmitting ? "Submitting..." : "Submit Withdrawal Request")
                            .fontWeight(.semibold)
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(
                        viewModel.isFormValid ?
                        LinearGradient(colors: [AppColors.primary, AppColors.primary.opacity(0.8)], startPoint: .leading, endPoint: .trailing) :
                        LinearGradient(colors: [Color.gray, Color.gray], startPoint: .leading, endPoint: .trailing)
                    )
                    .cornerRadius(14)
                    .shadow(color: viewModel.isFormValid ? AppColors.primary.opacity(0.3) : .clear, radius: 8, y: 4)
                }
                .disabled(!viewModel.isFormValid || viewModel.isSubmitting)
                
                // Important Notes
                notesCard
                
                // Recent Withdrawal History
                withdrawalHistorySection
            }
            .padding()
        }
    }
    
    // MARK: - Gradient Header
    private var gradientHeader: some View {
        VStack(spacing: 8) {
            Image(systemName: "arrow.down.to.line.compact")
                .font(.system(size: 36))
                .foregroundColor(.white)
            
            Text("Withdraw Funds")
                .font(.title3.weight(.bold))
                .foregroundColor(.white)
            
            Text("Transfer your earnings to your preferred payment method")
                .font(.caption)
                .foregroundColor(.white.opacity(0.8))
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
        .background(
            LinearGradient(
                colors: [AppColors.primary, Color.orange.opacity(0.7), AppColors.primary.opacity(0.6)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .cornerRadius(20)
    }
    
    // MARK: - Balance Card
    private var balanceCard: some View {
        VStack(spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Available Balance")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.8))
                    Text(String(format: "$%.2f", viewModel.availableBalance))
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(.white)
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 8) {
                    HStack(spacing: 4) {
                        Image(systemName: "clock")
                            .font(.system(size: 8))
                        Text("Pending:")
                            .font(.caption2)
                            .foregroundColor(.white.opacity(0.7))
                        Text(String(format: "$%.2f", viewModel.pendingBalance))
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(.yellow)
                    }
                    
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark.circle")
                            .font(.system(size: 8))
                        Text("Withdrawn:")
                            .font(.caption2)
                            .foregroundColor(.white.opacity(0.7))
                        Text(String(format: "$%.2f", viewModel.totalWithdrawn))
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                    }
                }
            }
            
            // Progress bar showing withdrawn vs total
            if viewModel.totalWithdrawn > 0 || viewModel.availableBalance > 0 {
                let totalEarned = viewModel.totalWithdrawn + viewModel.availableBalance + viewModel.pendingBalance
                let withdrawnRatio = totalEarned > 0 ? viewModel.totalWithdrawn / totalEarned : 0
                
                VStack(alignment: .leading, spacing: 4) {
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            RoundedRectangle(cornerRadius: 3)
                                .fill(Color.white.opacity(0.2))
                                .frame(height: 6)
                            
                            RoundedRectangle(cornerRadius: 3)
                                .fill(Color.green)
                                .frame(width: geo.size.width * withdrawnRatio, height: 6)
                        }
                    }
                    .frame(height: 6)
                    
                    Text(String(format: "%.0f%% of total earnings withdrawn", withdrawnRatio * 100))
                        .font(.system(size: 9))
                        .foregroundColor(.white.opacity(0.6))
                }
            }
        }
        .padding()
        .background(
            LinearGradient(
                colors: [Color.orange, Color.orange.opacity(0.7)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .cornerRadius(16)
    }
    
    // MARK: - Quick Amount Buttons
    private var quickAmountButtons: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Quick Amount")
                .font(.subheadline.weight(.medium))
                .foregroundColor(.secondary)
            
            HStack(spacing: 8) {
                ForEach([50, 100, 250, 500], id: \.self) { amount in
                    let amt = Double(amount)
                    let isAvailable = amt <= viewModel.availableBalance
                    
                    Button(action: {
                        viewModel.amount = String(format: "%.2f", amt)
                    }) {
                        Text("$\(amount)")
                            .font(.caption.weight(.semibold))
                            .foregroundColor(
                                viewModel.parsedAmount == amt ? .white :
                                (isAvailable ? AppColors.primary : .gray)
                            )
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(
                                viewModel.parsedAmount == amt ? AppColors.primary :
                                (isAvailable ? AppColors.primary.opacity(0.1) : Color(.systemGray6))
                            )
                            .cornerRadius(10)
                    }
                    .disabled(!isAvailable)
                }
            }
        }
    }
    
    // MARK: - Amount Section
    private var amountSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Withdrawal Amount")
                .font(.headline)
            
            HStack {
                Text("$")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.orange)
                
                TextField("0.00", text: $viewModel.amount)
                    .font(.title2)
                    .keyboardType(.decimalPad)
                
                Button("Max") {
                    viewModel.setMaxAmount()
                }
                .font(.caption)
                .fontWeight(.bold)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.orange.opacity(0.2))
                .foregroundColor(.orange)
                .cornerRadius(8)
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(12)
            
            if let error = viewModel.amountError {
                HStack(spacing: 4) {
                    Image(systemName: "exclamationmark.circle.fill")
                        .font(.caption2)
                    Text(error)
                        .font(.caption)
                }
                .foregroundColor(.red)
            } else if viewModel.parsedAmount >= 50 && viewModel.parsedAmount <= viewModel.availableBalance {
                HStack(spacing: 4) {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.caption2)
                    Text("Valid amount")
                        .font(.caption)
                }
                .foregroundColor(.green)
            }
        }
    }
    
    // MARK: - Payment Method
    private var paymentMethodSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Payment Method")
                .font(.headline)
            
            HStack(spacing: 12) {
                ForEach(PaymentMethod.allCases, id: \.self) { method in
                    Button(action: { viewModel.paymentMethod = method }) {
                        VStack(spacing: 8) {
                            Image(systemName: method.icon)
                                .font(.title2)
                            Text(method.displayName)
                                .font(.caption)
                                .fontWeight(.medium)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(
                            viewModel.paymentMethod == method ?
                            Color.orange.opacity(0.2) : Color(.systemGray6)
                        )
                        .foregroundColor(
                            viewModel.paymentMethod == method ? .orange : .primary
                        )
                        .cornerRadius(12)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(viewModel.paymentMethod == method ? Color.orange : Color.clear, lineWidth: 2)
                        )
                    }
                }
            }
        }
    }
    
    // MARK: - Payment Details
    private var paymentDetailsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Payment Details")
                .font(.headline)
            
            switch viewModel.paymentMethod {
            case .paypal:
                ValidatedFormField(
                    label: "PayPal Email",
                    text: $viewModel.paypalEmail,
                    placeholder: "your@email.com",
                    keyboardType: .emailAddress,
                    validation: viewModel.paypalEmail.isEmpty ? nil :
                        (viewModel.paypalEmail.contains("@") ? .valid("Valid email") : .invalid("Enter a valid email"))
                )
                
            case .bankTransfer:
                ValidatedFormField(label: "Account Holder Name", text: $viewModel.accountHolderName, placeholder: "John Doe")
                ValidatedFormField(label: "Bank Name", text: $viewModel.bankName, placeholder: "Bank of America")
                ValidatedFormField(
                    label: "Account Number",
                    text: $viewModel.accountNumber,
                    placeholder: "1234567890",
                    keyboardType: .numberPad,
                    validation: viewModel.accountNumber.isEmpty ? nil :
                        (viewModel.accountNumber.count >= 8 ? .valid("Valid") : .invalid("Min 8 digits"))
                )
                ValidatedFormField(
                    label: "Routing Number",
                    text: $viewModel.routingNumber,
                    placeholder: "021000021",
                    keyboardType: .numberPad,
                    validation: viewModel.routingNumber.isEmpty ? nil :
                        (viewModel.routingNumber.count == 9 ? .valid("Valid") : .invalid("Must be 9 digits"))
                )
            }
        }
    }
    
    // MARK: - Notes Card
    private var notesCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "info.circle.fill")
                    .foregroundColor(.blue)
                Text("Important Notes")
                    .font(.caption)
                    .fontWeight(.bold)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                NoteRow(text: "Minimum withdrawal: $50.00")
                NoteRow(text: "Maximum withdrawal: $10,000.00")
                NoteRow(text: "Processing time: 3-5 business days")
                NoteRow(text: "Only one pending request allowed at a time")
                NoteRow(text: "Fees may apply for bank transfers")
            }
        }
        .padding()
        .background(Color.blue.opacity(0.05))
        .cornerRadius(12)
    }
    
    // MARK: - Withdrawal History Section
    private var withdrawalHistorySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "clock.arrow.circlepath")
                    .foregroundColor(AppColors.primary)
                Text("Recent Withdrawals")
                    .font(.subheadline.weight(.semibold))
                
                Spacer()
                
                NavigationLink(destination: Text("Full Withdrawal History")) {
                    Text("See All")
                        .font(.caption)
                        .foregroundColor(AppColors.primary)
                }
            }
            
            if viewModel.totalWithdrawn > 0 {
                // Show a summary card
                HStack(spacing: 16) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Total Withdrawn")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(String(format: "$%.2f", viewModel.totalWithdrawn))
                            .font(.headline.weight(.bold))
                            .foregroundColor(.green)
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing, spacing: 4) {
                        Text("Pending")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(String(format: "$%.2f", viewModel.pendingBalance))
                            .font(.headline.weight(.bold))
                            .foregroundColor(.orange)
                    }
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
            } else {
                HStack {
                    Image(systemName: "tray")
                        .foregroundColor(.gray)
                    Text("No withdrawal history yet")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
            }
        }
    }
}

// MARK: - Field Validation State
enum FieldValidation {
    case valid(String)
    case invalid(String)
}

// MARK: - Validated Form Field
struct ValidatedFormField: View {
    let label: String
    @Binding var text: String
    let placeholder: String
    var keyboardType: UIKeyboardType = .default
    var validation: FieldValidation? = nil
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundColor(.gray)
            
            TextField(placeholder, text: $text)
                .keyboardType(keyboardType)
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(borderColor, lineWidth: validation != nil ? 1 : 0)
                )
            
            if let validation = validation {
                HStack(spacing: 4) {
                    switch validation {
                    case .valid(let msg):
                        Image(systemName: "checkmark.circle.fill")
                            .font(.caption2)
                            .foregroundColor(.green)
                        Text(msg)
                            .font(.caption2)
                            .foregroundColor(.green)
                    case .invalid(let msg):
                        Image(systemName: "exclamationmark.circle.fill")
                            .font(.caption2)
                            .foregroundColor(.red)
                        Text(msg)
                            .font(.caption2)
                            .foregroundColor(.red)
                    }
                }
            }
        }
    }
    
    private var borderColor: Color {
        guard let validation = validation else { return .clear }
        switch validation {
        case .valid: return .green.opacity(0.5)
        case .invalid: return .red.opacity(0.5)
        }
    }
}

// MARK: - Note Row
struct NoteRow: View {
    let text: String
    
    var body: some View {
        HStack(alignment: .top, spacing: 6) {
            Text("•")
                .font(.caption)
                .foregroundColor(.blue)
            Text(text)
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}

#Preview {
    NavigationView {
        WithdrawalRequestView()
    }
}
