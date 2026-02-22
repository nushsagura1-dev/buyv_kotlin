import Foundation
import Shared

class SessionManager: ObservableObject {
    static let shared = SessionManager()
    
    @Published var currentUser: UserProfile?
    @Published var isLoggedIn = false
    
    // Dependencies
    private let getCurrentUserUseCase = DependencyWrapper.shared.getCurrentUserUseCase
    
    private init() {
        checkSession()
    }
    
    func checkSession() {
        getCurrentUserUseCase.invoke { result, error in
            DispatchQueue.main.async {
                if let result = result as? ResultSuccess<UserProfile> {
                     AppLogger.success("Session restored for: \(result.data.email)")
                     self.saveUser(result.data)
                } else {
                    // Token might be expired or invalid
                    self.clearUser()
                }
            }
        }
    }
    
    func saveUser(_ user: UserProfile) {
        self.currentUser = user
        self.isLoggedIn = true
    }
    
    func clearUser() {
        self.currentUser = nil
        self.isLoggedIn = false
    }
    
    var currentUserId: String {
        return currentUser?.id ?? "guest_user"
    }
    
    /// Get the current auth token from the shared KMP TokenManager
    var authToken: String? {
        return DependencyWrapper.shared.getAuthToken()
    }
    
    func logout() {
        clearUser()
    }
}
