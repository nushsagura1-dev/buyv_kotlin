import SwiftUI

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (1, 1, 1, 0)
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue:  Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

struct AppColors {
    // Convenience aliases used by views
    static let primary = primaryColor
    static let background = Color(hex: "F5F5F5")
    static let surface = Color.white
    
    static let primaryColor = Color(hex: "F4A032")
    static let primaryColorText = Color(hex: "114B7F")
    static let primaryColor80Degree = Color(hex: "CCF4A032")
    static let primaryColor60Degree = Color(hex: "99F4A032")
    static let primaryColor37Degree = Color(hex: "5EF4A032")

    static let chipsColor = Color(hex: "34BE9D")

    static let secondaryColor = Color(hex: "0B649B")
    static let secondaryColor60Degree = Color(hex: "990B649B")
    static let secondaryColor37Degree = Color(hex: "5E0B649B")
    static let secondaryColor80Degree = Color(hex: "CC0B649B")

    static let titleTextColor = Color(hex: "00210E")
    static let titleTextColor80Degree = Color(hex: "CC00210E")
    static let titleTextColor60Degree = Color(hex: "9900210E")

    static let blackColor = Color(hex: "121212")
    static let blackColor80 = Color(hex: "CC121212")
    static let blackColor60 = Color(hex: "99121212")
    static let blackColor37 = Color(hex: "5E121212")

    static let grayDeep = Color(hex: "444444")
    static let grayColor = Color(hex: "D5D6DB")
    static let grayColor80 = Color(hex: "CCD5D6DB")
    static let grayColor60 = Color(hex: "99D5D6DB")
    static let grayColor37 = Color(hex: "E5D5D6DB")
    static let primaryGray = Color(hex: "7A7E91")

    static let errorPrimaryColor = Color(hex: "E46962")
    static let errorSecondaryColor = Color(hex: "EC928E")
    static let errorThirdColor = Color(hex: "FFDAD6")
}
