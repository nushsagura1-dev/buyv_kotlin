import Foundation

/// Centralized API configuration for the iOS app.
/// All services and view models should use `ApiConfig.baseURL` instead of hardcoding URLs.
///
/// To switch to production, set `ApiConfig.environment = .production` in AppDelegate.
enum ApiConfig {

    // MARK: - Environments

    enum Environment: String {
        case development
        case staging
        case production
    }

    // MARK: - Current Environment

    /// Set this in AppDelegate before any API calls.
    /// Defaults to `.development`.
    static var environment: Environment = .development

    // MARK: - URLs

    /// Production API URL (HTTPS).
    /// Update this when you deploy your backend.
    private static let productionURL = "https://buyv-api.up.railway.app"

    /// Staging API URL.
    private static let stagingURL = "https://buyv-api-staging.up.railway.app"

    /// Dev URL — auto-detects simulator vs physical device.
    private static var developmentURL: String {
        #if targetEnvironment(simulator)
        return "http://127.0.0.1:8000"
        #else
        return "http://192.168.11.109:8000"
        #endif
    }

    // MARK: - Public API

    /// The base URL for all API calls.
    /// Uses the current `environment` setting.
    static var baseURL: String {
        switch environment {
        case .development:
            return developmentURL
        case .staging:
            return stagingURL
        case .production:
            return productionURL
        }
    }

    /// Whether we're running in production mode.
    static var isProduction: Bool {
        environment == .production
    }

    /// Whether debug logging should be enabled.
    static var isDebugLoggingEnabled: Bool {
        environment != .production
    }
}
