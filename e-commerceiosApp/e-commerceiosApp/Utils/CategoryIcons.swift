import SwiftUI

/// CAT-001/005: Maps category slugs (from backend) to SF Symbols icon names.
///
/// Usage:
/// ```swift
/// Image(systemName: CategoryIcons.forSlug(category.slug))
///     .foregroundColor(.primary)
/// ```
struct CategoryIcons {
    
    /// Returns the SF Symbols system name for the given slug.
    /// Falls back to `"tag"` when no mapping is found.
    static func forSlug(_ slug: String) -> String {
        return slugToSymbol[slug.lowercased().trimmingCharacters(in: .whitespaces)]
            ?? "tag"
    }
    
    // slug → SF Symbol mapping — extend as categories grow in the backend
    private static let slugToSymbol: [String: String] = [
        // Fashion & Clothing
        "fashion":          "tshirt",
        "clothing":         "tshirt",
        "women-fashion":    "tshirt",
        "men-fashion":      "tshirt.fill",
        // Electronics
        "electronics":      "cpu",
        "phones":           "iphone",
        "computers":        "laptopcomputer",
        // Beauty
        "beauty":           "sparkles",
        "skincare":         "drop",
        "makeup":           "paintpalette",
        // Home
        "home":             "house",
        "furniture":        "sofa",
        "kitchen":          "fork.knife",
        // Sports
        "sports":           "sportscourt",
        "fitness":          "figure.run",
        // Accessories
        "accessories":      "bag",
        "jewelry":          "crown",
        "watches":          "applewatch",
        // Food & Grocery
        "food":             "cart",
        "grocery":          "leaf",
        // Toys & Kids
        "toys":             "gamecontroller",
        "kids":             "figure.child",
        "baby":             "figure.child.circle"
    ]
}
