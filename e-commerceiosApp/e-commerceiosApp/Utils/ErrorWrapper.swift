import SwiftUI

/// Shared error wrapper used across views for alert bindings
struct ErrorWrapper: Identifiable {
    let id = UUID()
    let message: String
}
