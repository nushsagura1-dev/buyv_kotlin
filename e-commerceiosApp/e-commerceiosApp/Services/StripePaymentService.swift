import Foundation
#if canImport(StripePaymentSheet)
import StripePaymentSheet
private let STRIPE_SDK_AVAILABLE = true
#else
private let STRIPE_SDK_AVAILABLE = false
#endif

// MARK: - Stripe Payment Service
// To enable real Stripe payments, add Stripe iOS SDK via Swift Package Manager:
//   URL: https://github.com/stripe/stripe-ios
//   Select "StripePaymentSheet" product
// The code auto-detects whether the SDK is available via canImport.

/// Response from /payments/create-payment-intent
struct PaymentIntentResponse: Codable {
    let clientSecret: String
    let ephemeralKey: String
    let customer: String
    let publishableKey: String?
}

/// Payment result returned to the caller
enum PaymentResult {
    case completed
    case cancelled
    case failed(String)
}

/// Service responsible for Stripe payment integration.
/// Calls the BuyV backend to create a PaymentIntent, then
/// presents Stripe's PaymentSheet for the user to complete payment.
@MainActor
class StripePaymentService: ObservableObject {
    
    static let shared = StripePaymentService()
    
    @Published var isProcessing = false
    @Published var paymentError: String?
    
    #if canImport(StripePaymentSheet)
    private var paymentSheet: PaymentSheet?
    #endif
    
    private let baseURL = ApiConfig.baseURL
    
    private init() {}
    
    // MARK: - Create Payment Intent (Backend Call)
    
    /// Calls the backend to create a Stripe PaymentIntent.
    /// Returns the data needed to present the PaymentSheet.
    func createPaymentIntent(amountInCents: Int, currency: String = "usd") async throws -> PaymentIntentResponse {
        guard let token = SessionManager.shared.authToken else {
            throw PaymentError.notAuthenticated
        }
        
        let url = URL(string: "\(baseURL)/payments/create-payment-intent")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        
        let body: [String: Any] = [
            "amount": amountInCents,
            "currency": currency
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw PaymentError.networkError
        }
        
        guard (200...299).contains(httpResponse.statusCode) else {
            if httpResponse.statusCode == 401 {
                throw PaymentError.notAuthenticated
            }
            let message = String(data: data, encoding: .utf8) ?? "Unknown error"
            throw PaymentError.serverError(message)
        }
        
        let decoder = JSONDecoder()
        return try decoder.decode(PaymentIntentResponse.self, from: data)
    }
    
    // MARK: - Prepare Payment Sheet
    
    /// Prepares the Stripe PaymentSheet with the given PaymentIntent data.
    /// Call this before presenting the sheet.
    func preparePaymentSheet(with intentResponse: PaymentIntentResponse) {
        #if canImport(StripePaymentSheet)
        // Configure Stripe publishable key
        if let publishableKey = intentResponse.publishableKey, !publishableKey.isEmpty {
            STPAPIClient.shared.publishableKey = publishableKey
        }
        
        var configuration = PaymentSheet.Configuration()
        configuration.merchantDisplayName = "BuyV"
        configuration.customer = .init(
            id: intentResponse.customer,
            ephemeralKeySecret: intentResponse.ephemeralKey
        )
        configuration.allowsDelayedPaymentMethods = true
        
        self.paymentSheet = PaymentSheet(
            paymentIntentClientSecret: intentResponse.clientSecret,
            configuration: configuration
        )
        AppLogger.success("PaymentSheet prepared with Stripe SDK")
        #else
        AppLogger.warning("PaymentSheet prepared (Stripe SDK not installed — add via SPM)")
        #endif
    }
    
    // MARK: - Present Payment Sheet
    
    /// Presents the Stripe PaymentSheet and returns the result.
    func presentPaymentSheet() async -> PaymentResult {
        #if canImport(StripePaymentSheet)
        guard let paymentSheet = paymentSheet else {
            return .failed("Payment not prepared")
        }
        
        return await withCheckedContinuation { continuation in
            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let rootVC = windowScene.windows.first?.rootViewController else {
                continuation.resume(returning: .failed("Cannot present payment sheet"))
                return
            }
            
            var topVC = rootVC
            while let presented = topVC.presentedViewController {
                topVC = presented
            }
            
            paymentSheet.present(from: topVC) { result in
                switch result {
                case .completed:
                    continuation.resume(returning: .completed)
                case .canceled:
                    continuation.resume(returning: .cancelled)
                case .failed(let error):
                    continuation.resume(returning: .failed(error.localizedDescription))
                }
            }
        }
        #else
        // MOCK: Simulates a successful payment until Stripe SDK is added via SPM
        AppLogger.warning("Using mock payment — add Stripe SDK via SPM to enable real payments")
        try? await Task.sleep(nanoseconds: 1_500_000_000)
        return .completed
        #endif
    }
    
    // MARK: - Full Payment Flow
    
    /// Complete payment flow: create intent → prepare sheet → present sheet
    func processPayment(amountInCents: Int, currency: String = "usd") async -> PaymentResult {
        isProcessing = true
        paymentError = nil
        
        defer { isProcessing = false }
        
        do {
            // 1. Create PaymentIntent on backend
            let intentResponse = try await createPaymentIntent(
                amountInCents: amountInCents,
                currency: currency
            )
            
            // 2. Prepare the PaymentSheet
            preparePaymentSheet(with: intentResponse)
            
            // 3. Present PaymentSheet to user
            let result = await presentPaymentSheet()
            
            switch result {
            case .completed:
                AppLogger.success("Payment completed successfully")
            case .cancelled:
                AppLogger.warning("Payment cancelled by user")
            case .failed(let error):
                paymentError = error
                AppLogger.error("Payment failed: \(error)")
            }
            
            return result
            
        } catch let error as PaymentError {
            paymentError = error.localizedDescription
            return .failed(error.localizedDescription)
        } catch {
            paymentError = error.localizedDescription
            return .failed(error.localizedDescription)
        }
    }
}

// MARK: - Payment Errors

enum PaymentError: LocalizedError {
    case notAuthenticated
    case networkError
    case serverError(String)
    case paymentFailed(String)
    
    var errorDescription: String? {
        switch self {
        case .notAuthenticated:
            return "Please log in to make a payment"
        case .networkError:
            return "Network error. Please check your connection."
        case .serverError(let message):
            return "Server error: \(message)"
        case .paymentFailed(let message):
            return "Payment failed: \(message)"
        }
    }
}
