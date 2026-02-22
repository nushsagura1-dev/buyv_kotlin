import SwiftUI

struct ContentView: View {
    @StateObject private var sessionManager = SessionManager.shared
    @AppStorage("hasSeenOnboarding") private var hasSeenOnboarding = false
    @State private var showSplash = true
    
    var body: some View {
        if showSplash {
            SplashView {
                showSplash = false
            }
        } else if !hasSeenOnboarding {
            OnboardingView()
        } else if sessionManager.isLoggedIn {
            MainTabView()
        } else {
            LoginView()
        }
    }
}