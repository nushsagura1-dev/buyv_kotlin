import Foundation
import os

/// Centralized debug-only logger for BuyV iOS.
/// All logs are stripped from Release builds via `#if DEBUG`.
enum AppLogger {
    
    private static let subsystem = Bundle.main.bundleIdentifier ?? "com.buyv.app"
    private static let osLog = Logger(subsystem: subsystem, category: "BuyV")
    
    /// General informational log
    static func info(_ message: String) {
        #if DEBUG
        osLog.info("ℹ️ \(message)")
        #endif
    }
    
    /// Success operation log
    static func success(_ message: String) {
        #if DEBUG
        osLog.info("✅ \(message)")
        #endif
    }
    
    /// Warning log
    static func warning(_ message: String) {
        #if DEBUG
        osLog.warning("⚠️ \(message)")
        #endif
    }
    
    /// Error log
    static func error(_ message: String) {
        #if DEBUG
        osLog.error("❌ \(message)")
        #endif
    }
    
    /// Debug log for development only
    static func debug(_ message: String) {
        #if DEBUG
        osLog.debug("🔍 \(message)")
        #endif
    }
    
    /// Network/notification log
    static func network(_ message: String) {
        #if DEBUG
        osLog.info("📡 \(message)")
        #endif
    }
}
