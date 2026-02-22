import Foundation

// MARK: - Admin API Response Models

struct AdminLoginRequest: Codable {
    let email: String
    let password: String
}

struct AdminLoginResponse: Codable {
    let accessToken: String
    let tokenType: String
    let expiresIn: Int
    let admin: AdminInfo
    
    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case tokenType = "token_type"
        case expiresIn = "expires_in"
        case admin
    }
}

struct AdminInfo: Codable {
    let id: Int
    let username: String
    let email: String
    let role: String
}

struct DashboardStatsResponse: Codable {
    let totalUsers: Int
    let verifiedUsers: Int
    let newUsersToday: Int
    let newUsersThisWeek: Int
    let totalPosts: Int
    let totalReels: Int
    let totalProducts: Int
    let totalComments: Int
    let totalLikes: Int
    let totalFollows: Int
    let totalOrders: Int
    let pendingOrders: Int
    let totalCommissions: Int
    let pendingCommissions: Int
    let totalRevenue: Double
    let pendingWithdrawals: Int
    let pendingWithdrawalsAmount: Double
    
    enum CodingKeys: String, CodingKey {
        case totalUsers = "total_users"
        case verifiedUsers = "verified_users"
        case newUsersToday = "new_users_today"
        case newUsersThisWeek = "new_users_this_week"
        case totalPosts = "total_posts"
        case totalReels = "total_reels"
        case totalProducts = "total_products"
        case totalComments = "total_comments"
        case totalLikes = "total_likes"
        case totalFollows = "total_follows"
        case totalOrders = "total_orders"
        case pendingOrders = "pending_orders"
        case totalCommissions = "total_commissions"
        case pendingCommissions = "pending_commissions"
        case totalRevenue = "total_revenue"
        case pendingWithdrawals = "pending_withdrawals"
        case pendingWithdrawalsAmount = "pending_withdrawals_amount"
    }
}

struct RecentUserResponse: Codable, Identifiable {
    let id: String
    let username: String
    let email: String
    let displayName: String
    let isVerified: Bool
    let createdAt: String
    
    enum CodingKeys: String, CodingKey {
        case id, username, email
        case displayName = "display_name"
        case isVerified = "is_verified"
        case createdAt = "created_at"
    }
}

struct RecentOrderResponse: Codable, Identifiable {
    let id: Int
    let buyerEmail: String
    let totalAmount: Double
    let status: String
    let createdAt: String
    
    enum CodingKeys: String, CodingKey {
        case id
        case buyerEmail = "buyer_email"
        case totalAmount = "total_amount"
        case status
        case createdAt = "created_at"
    }
}

struct AdminOrderResponse: Codable, Identifiable {
    let id: Int
    let orderNumber: String?
    let userId: Int
    let status: String
    let total: Double
    let createdAt: String
    let items: [AdminOrderItemResponse]?
    
    enum CodingKeys: String, CodingKey {
        case id
        case orderNumber = "order_number"
        case userId = "user_id"
        case status, total
        case createdAt = "created_at"
        case items
    }
}

struct AdminOrderItemResponse: Codable {
    let productId: String?
    let productName: String?
    let price: Double
    let quantity: Int
    
    enum CodingKeys: String, CodingKey {
        case productId = "product_id"
        case productName = "product_name"
        case price, quantity
    }
}

struct UserManagementResponse: Codable, Identifiable {
    let id: String
    let username: String
    let email: String
    let displayName: String
    let isVerified: Bool
    let followersCount: Int
    let followingCount: Int
    let reelsCount: Int
    let createdAt: String
    
    enum CodingKeys: String, CodingKey {
        case id, username, email
        case displayName = "display_name"
        case isVerified = "is_verified"
        case followersCount = "followers_count"
        case followingCount = "following_count"
        case reelsCount = "reels_count"
        case createdAt = "created_at"
    }
}

struct MessageResponse: Codable {
    let message: String
    let count: Int?
}

struct StatusUpdateRequest: Codable {
    let status: String
}

// MARK: - CJ Import Models

struct CJProduct: Codable, Identifiable {
    var id: String { productId }
    let productId: String
    let productName: String
    let productImage: String?
    let sellPrice: Double
    let originalPrice: Double?
    let productUrl: String?
    let categoryName: String?
    let description: String?
    let variants: [CJVariant]?
    
    enum CodingKeys: String, CodingKey {
        case productId = "product_id"
        case productName = "product_name"
        case productImage = "product_image"
        case sellPrice = "sell_price"
        case originalPrice = "original_price"
        case productUrl = "product_url"
        case categoryName = "category_name"
        case description
        case variants
    }
    
    var formattedPrice: String {
        String(format: "$%.2f", sellPrice)
    }
    
    var formattedOriginalPrice: String? {
        guard let original = originalPrice, original > sellPrice else { return nil }
        return String(format: "$%.2f", original)
    }
}

struct CJVariant: Codable, Identifiable {
    var id: String { variantId }
    let variantId: String
    let variantName: String
    let variantSku: String?
    let sellPrice: Double
    let stock: Int?
    
    enum CodingKeys: String, CodingKey {
        case variantId = "variant_id"
        case variantName = "variant_name"
        case variantSku = "variant_sku"
        case sellPrice = "sell_price"
        case stock
    }
}

struct CJSearchResponse: Codable {
    let products: [CJProduct]
    let total: Int
    let page: Int
    let pageSize: Int
    
    enum CodingKeys: String, CodingKey {
        case products, total, page
        case pageSize = "page_size"
    }
}

struct CJImportRequest: Codable {
    let cjProductId: String
    let cjVariantId: String?
    let commissionRate: Double
    let categoryId: Int?
    let customDescription: String?
    let sellingPrice: Double?
    
    enum CodingKeys: String, CodingKey {
        case cjProductId = "cj_product_id"
        case cjVariantId = "cj_variant_id"
        case commissionRate = "commission_rate"
        case categoryId = "category_id"
        case customDescription = "custom_description"
        case sellingPrice = "selling_price"
    }
}

struct CJImportResponse: Codable, Identifiable {
    let id: Int
    let name: String
    let mainImageUrl: String?
    let sellingPrice: Double
    let originalPrice: Double
    let commissionRate: Double
    let cjProductId: String?
    let status: String?
    let message: String?
    
    enum CodingKeys: String, CodingKey {
        case id, name
        case mainImageUrl = "main_image_url"
        case sellingPrice = "selling_price"
        case originalPrice = "original_price"
        case commissionRate = "commission_rate"
        case cjProductId = "cj_product_id"
        case status, message
    }
}

// MARK: - Commission Models

struct CommissionResponse: Codable, Identifiable {
    let id: Int
    let userId: String
    let orderId: Int
    let orderItemId: Int?
    let productId: String
    let productName: String
    let productPrice: Double
    let commissionRate: Double
    let commissionAmount: Double
    let status: String
    let createdAt: String
    let updatedAt: String?
    let paidAt: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case userId = "user_id"
        case orderId = "order_id"
        case orderItemId = "order_item_id"
        case productId = "product_id"
        case productName = "product_name"
        case productPrice = "product_price"
        case commissionRate = "commission_rate"
        case commissionAmount = "commission_amount"
        case status
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case paidAt = "paid_at"
    }
    
    var statusColor: Color {
        switch status {
        case "pending": return .orange
        case "approved": return .green
        case "rejected": return .red
        case "paid": return .blue
        default: return .gray
        }
    }
    
    var formattedAmount: String {
        String(format: "$%.2f", commissionAmount)
    }
    
    var formattedRate: String {
        String(format: "%.1f%%", commissionRate)
    }
}

// MARK: - Withdrawal Admin Models

struct AdminWithdrawalResponse: Codable, Identifiable {
    let id: Int
    let promoterUid: String?
    let userId: String?
    let walletId: Int?
    let amount: Double
    let paymentMethod: String
    let paymentDetails: [String: String]?
    let status: String
    let adminNotes: String?
    let rejectionReason: String?
    let createdAt: String
    let approvedAt: String?
    let completedAt: String?
    let rejectedAt: String?
    let processedAt: String?
    let transactionId: String?
    let processedBy: String?
    let promoterName: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case promoterUid = "promoter_uid"
        case userId = "user_id"
        case walletId = "wallet_id"
        case amount
        case paymentMethod = "payment_method"
        case paymentDetails = "payment_details"
        case status
        case adminNotes = "admin_notes"
        case rejectionReason = "rejection_reason"
        case createdAt = "created_at"
        case approvedAt = "approved_at"
        case completedAt = "completed_at"
        case rejectedAt = "rejected_at"
        case processedAt = "processed_at"
        case transactionId = "transaction_id"
        case processedBy = "processed_by"
        case promoterName = "promoter_name"
    }
    
    var statusColor: Color {
        switch status {
        case "pending": return .orange
        case "approved": return .green
        case "completed": return .blue
        case "rejected": return .red
        default: return .gray
        }
    }
    
    var displayName: String {
        promoterName ?? promoterUid ?? userId ?? "Unknown"
    }
    
    var formattedAmount: String {
        String(format: "$%.2f", amount)
    }
    
    var paymentMethodDisplay: String {
        paymentMethod.replacingOccurrences(of: "_", with: " ").uppercased()
    }
}

import SwiftUI

// MARK: - Admin API Service

class AdminApiService {
    static let shared = AdminApiService()
    
    // Base URL — uses centralized ApiConfig
    private let baseURL = ApiConfig.baseURL
    
    private var adminToken: String? {
        get { UserDefaults.standard.string(forKey: "admin_token") }
        set { UserDefaults.standard.set(newValue, forKey: "admin_token") }
    }
    
    var isLoggedIn: Bool {
        return adminToken != nil
    }
    
    private let session: URLSession
    private let decoder: JSONDecoder
    
    private init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        self.session = URLSession(configuration: config)
        self.decoder = JSONDecoder()
    }
    
    // MARK: - Authentication
    
    func login(email: String, password: String) async throws -> AdminLoginResponse {
        let body = AdminLoginRequest(email: email, password: password)
        let response: AdminLoginResponse = try await post(path: "auth/admin/login", body: body, authenticated: false)
        self.adminToken = response.accessToken
        return response
    }
    
    func logout() {
        adminToken = nil
        UserDefaults.standard.removeObject(forKey: "admin_token")
    }
    
    // MARK: - Dashboard
    
    func getDashboardStats() async throws -> DashboardStatsResponse {
        return try await get(path: "api/admin/dashboard/stats")
    }
    
    func getRecentUsers(limit: Int = 10) async throws -> [RecentUserResponse] {
        return try await get(path: "api/admin/dashboard/recent-users?limit=\(limit)")
    }
    
    func getRecentOrders(limit: Int = 10) async throws -> [RecentOrderResponse] {
        return try await get(path: "api/admin/dashboard/recent-orders?limit=\(limit)")
    }
    
    // MARK: - User Management
    
    func getUsers(search: String? = nil, isVerified: Bool? = nil, limit: Int = 50, offset: Int = 0) async throws -> [UserManagementResponse] {
        var queryItems: [String] = ["limit=\(limit)", "offset=\(offset)"]
        if let search = search { queryItems.append("search=\(search)") }
        if let isVerified = isVerified { queryItems.append("is_verified=\(isVerified)") }
        let query = queryItems.joined(separator: "&")
        return try await get(path: "api/admin/users?\(query)")
    }
    
    func verifyUsers(userIds: [String]) async throws -> MessageResponse {
        let body = ["user_ids": userIds]
        return try await post(path: "api/admin/users/verify", body: body)
    }
    
    func unverifyUsers(userIds: [String]) async throws -> MessageResponse {
        let body = ["user_ids": userIds]
        return try await post(path: "api/admin/users/unverify", body: body)
    }
    
    func deleteUser(userUid: String) async throws -> MessageResponse {
        return try await delete(path: "api/admin/users/\(userUid)")
    }
    
    // MARK: - Order Management
    
    func getAllOrders() async throws -> [AdminOrderResponse] {
        return try await get(path: "api/orders/admin/all")
    }
    
    func getOrdersByStatus(status: String) async throws -> [AdminOrderResponse] {
        return try await get(path: "api/orders/admin/status?status=\(status)")
    }
    
    func updateOrderStatus(orderId: Int, status: String) async throws -> MessageResponse {
        let body = StatusUpdateRequest(status: status)
        return try await patch(path: "api/orders/\(orderId)/status", body: body)
    }
    
    // MARK: - Product Management (admin-specific operations)
    
    func createProduct(body: [String: Any]) async throws -> Data {
        return try await rawPost(path: "api/v1/admin/marketplace/products", jsonDict: body)
    }
    
    func updateProduct(productId: String, updates: [String: Any]) async throws -> Data {
        return try await rawPut(path: "api/v1/admin/marketplace/products/\(productId)", jsonDict: updates)
    }
    
    func deleteProduct(productId: String) async throws -> MessageResponse {
        return try await delete(path: "api/v1/admin/marketplace/products/\(productId)")
    }
    
    // MARK: - CJ Import
    
    func searchCJProducts(query: String, category: String? = nil, page: Int = 1) async throws -> CJSearchResponse {
        var queryItems = ["query=\(query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? query)", "page=\(page)"]
        if let category = category, category != "All" {
            queryItems.append("category=\(category.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? category)")
        }
        let queryString = queryItems.joined(separator: "&")
        return try await get(path: "api/v1/admin/cj/search?\(queryString)")
    }
    
    func importCJProduct(request: CJImportRequest) async throws -> CJImportResponse {
        return try await post(path: "api/v1/admin/cj/import", body: request)
    }
    
    func syncCJProduct(productId: Int) async throws -> MessageResponse {
        let emptyBody: [String: String] = [:]
        return try await post(path: "api/v1/admin/cj/sync/\(productId)", body: emptyBody)
    }
    
    // MARK: - Commission Management
    
    func getAllCommissions() async throws -> [CommissionResponse] {
        return try await get(path: "api/commissions/admin/all")
    }
    
    func getCommissionsByStatus(status: String) async throws -> [CommissionResponse] {
        return try await get(path: "api/commissions/admin/status?status=\(status)")
    }
    
    func updateCommissionStatus(commissionId: Int, status: String) async throws -> MessageResponse {
        let body = StatusUpdateRequest(status: status)
        return try await patch(path: "api/commissions/\(commissionId)/status", body: body)
    }
    
    // MARK: - Withdrawal Management (Admin)
    
    func getAdminWithdrawals(statusFilter: String? = nil) async throws -> [AdminWithdrawalResponse] {
        var path = "api/marketplace/withdrawal/admin/list"
        if let status = statusFilter {
            path += "?status_filter=\(status)"
        }
        return try await get(path: path)
    }
    
    func approveWithdrawal(id: Int, adminNotes: String? = nil) async throws -> MessageResponse {
        var body: [String: String] = [:]
        if let notes = adminNotes { body["admin_notes"] = notes }
        return try await post(path: "api/marketplace/withdrawal/admin/\(id)/approve", body: body)
    }
    
    func rejectWithdrawal(id: Int, adminNotes: String) async throws -> MessageResponse {
        let body = ["admin_notes": adminNotes]
        return try await post(path: "api/marketplace/withdrawal/admin/\(id)/reject", body: body)
    }
    
    func completeWithdrawal(id: Int, transactionId: String, adminNotes: String? = nil) async throws -> MessageResponse {
        var body: [String: String] = ["transaction_id": transactionId]
        if let notes = adminNotes { body["admin_notes"] = notes }
        return try await post(path: "api/marketplace/withdrawal/admin/\(id)/complete", body: body)
    }
    
    // MARK: - Generic HTTP Methods
    
    private func get<T: Decodable>(path: String) async throws -> T {
        let url = URL(string: "\(baseURL)/\(path)")!
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        addAuthHeader(&request)
        
        let (data, response) = try await session.data(for: request)
        try validateResponse(response)
        return try decoder.decode(T.self, from: data)
    }
    
    private func post<T: Decodable, B: Encodable>(path: String, body: B, authenticated: Bool = true) async throws -> T {
        let url = URL(string: "\(baseURL)/\(path)")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if authenticated { addAuthHeader(&request) }
        request.httpBody = try JSONEncoder().encode(body)
        
        let (data, response) = try await session.data(for: request)
        try validateResponse(response)
        return try decoder.decode(T.self, from: data)
    }
    
    private func patch<T: Decodable, B: Encodable>(path: String, body: B) async throws -> T {
        let url = URL(string: "\(baseURL)/\(path)")!
        var request = URLRequest(url: url)
        request.httpMethod = "PATCH"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        addAuthHeader(&request)
        request.httpBody = try JSONEncoder().encode(body)
        
        let (data, response) = try await session.data(for: request)
        try validateResponse(response)
        return try decoder.decode(T.self, from: data)
    }
    
    private func delete<T: Decodable>(path: String) async throws -> T {
        let url = URL(string: "\(baseURL)/\(path)")!
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        addAuthHeader(&request)
        
        let (data, response) = try await session.data(for: request)
        try validateResponse(response)
        return try decoder.decode(T.self, from: data)
    }
    
    private func rawPost(path: String, jsonDict: [String: Any]) async throws -> Data {
        let url = URL(string: "\(baseURL)/\(path)")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        addAuthHeader(&request)
        request.httpBody = try JSONSerialization.data(withJSONObject: jsonDict)
        
        let (data, response) = try await session.data(for: request)
        try validateResponse(response)
        return data
    }
    
    private func rawPut(path: String, jsonDict: [String: Any]) async throws -> Data {
        let url = URL(string: "\(baseURL)/\(path)")!
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        addAuthHeader(&request)
        request.httpBody = try JSONSerialization.data(withJSONObject: jsonDict)
        
        let (data, response) = try await session.data(for: request)
        try validateResponse(response)
        return data
    }
    
    // MARK: - Helpers
    
    private func addAuthHeader(_ request: inout URLRequest) {
        if let token = adminToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
    }
    
    private func validateResponse(_ response: URLResponse) throws {
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AdminApiError.invalidResponse
        }
        
        switch httpResponse.statusCode {
        case 200...299:
            return
        case 401:
            adminToken = nil
            throw AdminApiError.unauthorized
        case 403:
            throw AdminApiError.forbidden
        case 404:
            throw AdminApiError.notFound
        case 422:
            throw AdminApiError.validationError
        default:
            throw AdminApiError.serverError(statusCode: httpResponse.statusCode)
        }
    }
}

// MARK: - Error Types

enum AdminApiError: LocalizedError {
    case invalidResponse
    case unauthorized
    case forbidden
    case notFound
    case validationError
    case serverError(statusCode: Int)
    
    var errorDescription: String? {
        switch self {
        case .invalidResponse: return "Invalid server response"
        case .unauthorized: return "Session expired. Please login again."
        case .forbidden: return "You don't have permission for this action."
        case .notFound: return "Resource not found."
        case .validationError: return "Invalid request data."
        case .serverError(let code): return "Server error (\(code)). Please try again."
        }
    }
}
