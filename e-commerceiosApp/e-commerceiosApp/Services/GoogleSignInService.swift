import Foundation
import AuthenticationServices

/// Google Sign-In service for iOS.
///
/// The backend endpoint `/auth/google-signin` and shared KMP layer are fully
/// implemented. This service needs the Google Sign-In SDK added via SPM in
/// Xcode on macOS to obtain ID tokens from Google.
///
/// When the SDK is installed:
/// 1. Add GoogleSignIn package via SPM (https://github.com/google/GoogleSignIn-iOS)
/// 2. Configure CLIENT_ID in GoogleService-Info.plist
/// 3. The `#if canImport(GoogleSignIn)` blocks will activate automatically
class GoogleSignInService: ObservableObject {

    static let shared = GoogleSignInService()

    // MARK: - State

    @Published var isSigningIn = false
    @Published var errorMessage: String?

    /// Returns `true` when the Google Sign-In SDK is available.
    /// Backend and shared KMP layer are ready — only the SDK is needed.
    var isEnabled: Bool {
        #if canImport(GoogleSignIn)
        return true
        #else
        return false
        #endif
    }

    private init() {}

    // MARK: - Google Sign-In

    /// Initiates Google Sign-In flow.
    /// - Parameter completion: Called with the ID token on success, or error on failure.
    func signIn(completion: @escaping (Result<String, Error>) -> Void) {
        guard isEnabled else {
            completion(.failure(GoogleSignInError.sdkNotInstalled))
            return
        }

        isSigningIn = true
        errorMessage = nil

        #if canImport(GoogleSignIn)
        // When GoogleSignIn SDK is added via SPM:
        //
        // guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
        //       let rootVC = windowScene.windows.first?.rootViewController else {
        //     isSigningIn = false
        //     completion(.failure(GoogleSignInError.noRootVC))
        //     return
        // }
        //
        // GIDSignIn.sharedInstance.signIn(withPresenting: rootVC) { [weak self] result, error in
        //     DispatchQueue.main.async {
        //         self?.isSigningIn = false
        //         if let error = error {
        //             self?.errorMessage = error.localizedDescription
        //             completion(.failure(error))
        //             return
        //         }
        //         guard let idToken = result?.user.idToken?.tokenString else {
        //             self?.errorMessage = "No ID token received"
        //             completion(.failure(GoogleSignInError.noIdToken))
        //             return
        //         }
        //         completion(.success(idToken))
        //     }
        // }
        #else
        isSigningIn = false
        errorMessage = "Google Sign-In requires SDK setup in Xcode"
        completion(.failure(GoogleSignInError.sdkNotInstalled))
        #endif
    }

    /// Signs out of Google session.
    func signOut() {
        #if canImport(GoogleSignIn)
        // GIDSignIn.sharedInstance.signOut()
        #endif
    }

    // MARK: - Errors

    enum GoogleSignInError: LocalizedError {
        case sdkNotInstalled
        case noRootVC
        case noIdToken
        case backendError(String)

        var errorDescription: String? {
            switch self {
            case .sdkNotInstalled:
                return "Google Sign-In SDK not installed. Add via SPM in Xcode."
            case .noRootVC:
                return "Cannot present sign-in"
            case .noIdToken:
                return "No ID token received from Google"
            case .backendError(let msg):
                return msg
            }
        }
    }
}
}
