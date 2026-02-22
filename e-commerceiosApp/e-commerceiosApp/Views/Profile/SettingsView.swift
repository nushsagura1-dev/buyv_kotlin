import SwiftUI

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var showDeleteAccountAlert = false
    @State private var showLogoutAlert = false
    @State private var showAdminLogin = false
    @State private var showClearCacheAlert = false
    @State private var cacheCleared = false
    @AppStorage("notifications_enabled") private var notificationsEnabled = true
    @AppStorage("dark_mode_enabled") private var darkModeEnabled = false
    @AppStorage("app_language") private var appLanguage = "English"
    
    private let languages = ["English", "Français", "Español", "العربية"]
    
    private var appVersion: String {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "v\(version) (\(build))"
    }
    
    var body: some View {
        ZStack {
            AppColors.background.ignoresSafeArea()
            
            List {
                // Account
                Section(header: Text("Account")) {
                    NavigationLink(destination: EditProfileView()) {
                        settingsRow(icon: "person.circle", title: "Edit Profile")
                    }
                    
                    NavigationLink(destination: ChangePasswordView()) {
                        settingsRow(icon: "lock.shield", title: "Change Password")
                    }
                }
                
                // Preferences
                Section(header: Text("Preferences")) {
                    Toggle(isOn: $notificationsEnabled) {
                        settingsRow(icon: "bell.fill", title: "Notifications")
                    }
                    .tint(AppColors.primary)
                    
                    Toggle(isOn: $darkModeEnabled) {
                        settingsRow(icon: "moon.fill", title: "Dark Mode")
                    }
                    .tint(AppColors.primary)
                    .onChange(of: darkModeEnabled) { newValue in
                        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene {
                            windowScene.windows.forEach { window in
                                window.overrideUserInterfaceStyle = newValue ? .dark : .unspecified
                            }
                        }
                    }
                    
                    Picker(selection: $appLanguage) {
                        ForEach(languages, id: \.self) { lang in
                            Text(lang).tag(lang)
                        }
                    } label: {
                        settingsRow(icon: "globe", title: "Language")
                    }
                }
                
                // Content
                Section(header: Text("Content")) {
                    NavigationLink(destination: FavouriteView()) {
                        settingsRow(icon: "heart.fill", title: "Favourites")
                    }
                    
                    NavigationLink(destination: OrderListView()) {
                        settingsRow(icon: "bag.fill", title: "My Orders")
                    }
                }
                
                // Marketplace
                Section(header: Text("Marketplace")) {
                    NavigationLink(destination: MarketplaceView()) {
                        settingsRow(icon: "storefront.fill", title: "Browse Marketplace")
                    }
                    
                    NavigationLink(destination: PromoterDashboardView()) {
                        settingsRow(icon: "chart.line.uptrend.xyaxis", title: "Promoter Dashboard")
                    }
                    
                    NavigationLink(destination: WalletView()) {
                        settingsRow(icon: "wallet.pass.fill", title: "My Wallet")
                    }
                    
                    NavigationLink(destination: WithdrawalRequestView()) {
                        settingsRow(icon: "arrow.up.circle.fill", title: "Request Withdrawal")
                    }
                }
                
                // Storage
                Section(header: Text("Storage")) {
                    Button(action: { showClearCacheAlert = true }) {
                        HStack {
                            settingsRow(icon: "trash.circle", title: "Clear Cache")
                            Spacer()
                            if cacheCleared {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.green)
                            }
                        }
                    }
                    .buttonStyle(PlainButtonStyle())
                }
                
                // Admin
                Section(header: Text("Admin")) {
                    Button(action: { showAdminLogin = true }) {
                        settingsRow(icon: "shield.lefthalf.filled", title: "Admin Panel")
                    }
                }
                
                // Support
                Section(header: Text("Support")) {
                    Link(destination: URL(string: "mailto:support@buyv.app")!) {
                        settingsRow(icon: "envelope", title: "Contact Support")
                    }
                    settingsRow(icon: "questionmark.circle", title: "Help Center")
                    settingsRow(icon: "doc.text", title: "Terms of Service")
                    settingsRow(icon: "hand.raised", title: "Privacy Policy")
                }
                
                // Danger Zone
                Section {
                    Button(action: { showLogoutAlert = true }) {
                        HStack {
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                                .foregroundColor(.red)
                            Text("Log Out")
                                .foregroundColor(.red)
                        }
                    }
                    
                    Button(action: { showDeleteAccountAlert = true }) {
                        HStack {
                            Image(systemName: "trash")
                                .foregroundColor(.red)
                            Text("Delete Account")
                                .foregroundColor(.red)
                        }
                    }
                }
                
                // About
                Section {
                    HStack {
                        Spacer()
                        VStack(spacing: 4) {
                            Text("BuyV")
                                .font(.headline)
                                .foregroundColor(.secondary)
                            Text(appVersion)
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Text("Made with ❤️")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Log Out", isPresented: $showLogoutAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Log Out", role: .destructive) {
                SessionManager.shared.clearUser()
            }
        } message: {
            Text("Are you sure you want to log out?")
        }
        .alert("Clear Cache", isPresented: $showClearCacheAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Clear", role: .destructive) {
                clearCache()
            }
        } message: {
            Text("This will clear cached images and data. You may need to reload content.")
        }
        .sheet(isPresented: $showDeleteAccountAlert) {
            DeleteAccountView()
        }
        .sheet(isPresented: $showAdminLogin) {
            AdminLoginView()
        }
    }
    
    private func settingsRow(icon: String, title: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .frame(width: 24)
                .foregroundColor(AppColors.primary)
            Text(title)
        }
    }
    
    private func clearCache() {
        URLCache.shared.removeAllCachedResponses()
        let tmp = FileManager.default.temporaryDirectory
        if let files = try? FileManager.default.contentsOfDirectory(at: tmp, includingPropertiesForKeys: nil) {
            for file in files { try? FileManager.default.removeItem(at: file) }
        }
        withAnimation { cacheCleared = true }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            withAnimation { cacheCleared = false }
        }
    }
}
