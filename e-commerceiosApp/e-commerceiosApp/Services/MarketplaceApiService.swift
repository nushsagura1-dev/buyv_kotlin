import Foundation

// MARK: - Marketplace API Response Models

struct MarketplaceProductResponse: Codable, Identifiable {
    let id: String
    let name: String
    let description: String
    let shortDescription: String?
    let originalPrice: Double
    let sellingPrice: Double
    let currency: String?
    let commissionRate: Double
    let commissionAmount: Double?
    let commissionType: String?
    let categoryId: String?
    let categoryName: String?
    let mainImageUrl: String?
    let images: [String]?
    let thumbnailUrl: String?
    let totalSales: Int?
    let totalViews: Int?
    let totalPromotions: Int?
    let averageRating: Double?
    let ratingCount: Int?
    let status: String?
    let isFeatured: Bool?
    let tags: [String]?
    let createdAt: String?
    let updatedAt: String?
    
    enum CodingKeys: String, CodingKey {
        case id, name, description, currency, images, tags, status
        case shortDescription = "short_description"
        case originalPrice = "original_price"
        case sellingPrice = "selling_price"
        case commissionRate = "commission_rate"
        case commissionAmount = "commission_amount"
        case commissionType = "commission_type"
        case categoryId = "category_id"
        case categoryName = "category_name"
        case mainImageUrl = "main_image_url"
        case thumbnailUrl = "thumbnail_url"
        case totalSales = "total_sales"
        case totalViews = "total_views"
        case totalPromotions = "total_promotions"
        case averageRating = "average_rating"
        case ratingCount = "rating_count"
        case isFeatured = "is_featured"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
    
    var estimatedCommission: Double {
        commissionAmount ?? (sellingPrice * commissionRate / 100.0)
    }
    
    var formattedPrice: String {
        String(format: "$%.2f", sellingPrice)
    }
    
    var formattedCommission: String {
        String(format: "%.0f%%", commissionRate)
    }
    
    var displayImage: String {
        mainImageUrl ?? thumbnailUrl ?? images?.first ?? ""
    }
}

struct ProductListApiResponse: Codable {
    let items: [MarketplaceProductResponse]
    let total: Int
    let page: Int
    let limit: Int
    let totalPages: Int
    
    enum CodingKeys: String, CodingKey {
        case items, total, page, limit
        case totalPages = "total_pages"
    }
}

struct CategoryResponse: Codable, Identifiable {
    let id: String
    let name: String
    let nameAr: String?
    let slug: String?
    let iconUrl: String?
    let parentId: String?
    let displayOrder: Int?
    let isActive: Bool?
    let createdAt: String?
    
    enum CodingKeys: String, CodingKey {
        case id, name, slug
        case nameAr = "name_ar"
        case iconUrl = "icon_url"
        case parentId = "parent_id"
        case displayOrder = "display_order"
        case isActive = "is_active"
        case createdAt = "created_at"
    }
}

struct WalletResponse: Codable {
    let id: String
    let userId: String
    let pendingAmount: Double
    let availableAmount: Double
    let withdrawnAmount: Double
    let totalEarned: Double
    let currency: String
    let totalSalesCount: Int
    let lastWithdrawalAt: String?
    let createdAt: String
    let updatedAt: String
    
    enum CodingKeys: String, CodingKey {
        case id, currency
        case userId = "user_id"
        case pendingAmount = "pending_amount"
        case availableAmount = "available_amount"
        case withdrawnAmount = "withdrawn_amount"
        case totalEarned = "total_earned"
        case totalSalesCount = "total_sales_count"
        case lastWithdrawalAt = "last_withdrawal_at"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
    
    var canWithdraw: Bool { availableAmount >= 50.0 }
}

struct WalletTransactionResponse: Codable, Identifiable {
    let id: String
    let walletId: String
    let type: String
    let amount: Double
    let balanceAfter: Double
    let currency: String
    let description: String?
    let referenceType: String?
    let referenceId: String?
    let createdAt: String
    
    enum CodingKeys: String, CodingKey {
        case id, type, amount, currency, description
        case walletId = "wallet_id"
        case balanceAfter = "balance_after"
        case referenceType = "reference_type"
        case referenceId = "reference_id"
        case createdAt = "created_at"
    }
    
    var isCredit: Bool {
        type == "commission" || type == "adjustment" && amount > 0
    }
}

struct WithdrawalResponse: Codable, Identifiable {
    let id: String
    let walletId: String
    let userId: String
    let amount: Double
    let currency: String
    let paymentMethod: String
    let status: String
    let processedBy: String?
    let processedAt: String?
    let rejectionReason: String?
    let createdAt: String
    let updatedAt: String
    
    enum CodingKeys: String, CodingKey {
        case id, amount, currency, status
        case walletId = "wallet_id"
        case userId = "user_id"
        case paymentMethod = "payment_method"
        case processedBy = "processed_by"
        case processedAt = "processed_at"
        case rejectionReason = "rejection_reason"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
    
    var isPending: Bool { status == "pending" }
    var isCompleted: Bool { status == "completed" }
    var isRejected: Bool { status == "rejected" }
}

struct WithdrawalStatsResponse: Codable {
    let availableBalance: Double
    let pendingBalance: Double
    let totalWithdrawn: Double
    let totalRequests: Int
    let pendingRequests: Int
    let completedRequests: Int
    let rejectedRequests: Int
    
    enum CodingKeys: String, CodingKey {
        case availableBalance = "available_balance"
        case pendingBalance = "pending_balance"
        case totalWithdrawn = "total_withdrawn"
        case totalRequests = "total_requests"
        case pendingRequests = "pending_requests"
        case completedRequests = "completed_requests"
        case rejectedRequests = "rejected_requests"
    }
}

struct CommissionResponse: Codable, Identifiable {
    let id: Int
    let userId: Int
    let orderId: Int?
    let orderItemId: Int?
    let productId: Int?
    let productName: String?
    let productPrice: Double?
    let commissionRate: Double?
    let commissionAmount: Double
    let status: String
    let createdAt: String
    let updatedAt: String?
    let paidAt: String?
    
    enum CodingKeys: String, CodingKey {
        case id, status
        case userId = "userId"
        case orderId = "orderId"
        case orderItemId = "orderItemId"
        case productId = "productId"
        case productName = "productName"
        case productPrice = "productPrice"
        case commissionRate = "commissionRate"
        case commissionAmount = "commissionAmount"
        case createdAt = "createdAt"
        case updatedAt = "updatedAt"
        case paidAt = "paidAt"
    }
}

struct PromotionResponse: Codable, Identifiable {
    let id: String
    let postId: String
    let productId: String
    let promoterUserId: String
    let isOfficial: Bool?
    let viewsCount: Int?
    let clicksCount: Int?
    let salesCount: Int?
    let totalRevenue: Double?
    let totalCommissionEarned: Double?
    let createdAt: String?
    let updatedAt: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case postId = "post_id"
        case productId = "product_id"
        case promoterUserId = "promoter_user_id"
        case isOfficial = "is_official"
        case viewsCount = "views_count"
        case clicksCount = "clicks_count"
        case salesCount = "sales_count"
        case totalRevenue = "total_revenue"
        case totalCommissionEarned = "total_commission_earned"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
}

struct AffiliateSaleResponse: Codable, Identifiable {
    let id: String
    let orderId: String
    let productId: String
    let productName: String?
    let promotionId: String?
    let promoterUserId: String
    let buyerUserId: String
    let quantity: Int
    let unitPrice: Double
    let saleAmount: Double
    let commissionAmount: Double
    let currency: String?
    let commissionStatus: String
    let paymentMethod: String?
    let paymentReference: String?
    let paidAt: String?
    let createdAt: String
    
    enum CodingKeys: String, CodingKey {
        case id, quantity, currency
        case orderId = "order_id"
        case productId = "product_id"
        case productName = "product_name"
        case promotionId = "promotion_id"
        case promoterUserId = "promoter_user_id"
        case buyerUserId = "buyer_user_id"
        case unitPrice = "unit_price"
        case saleAmount = "sale_amount"
        case commissionAmount = "commission_amount"
        case commissionStatus = "commission_status"
        case paymentMethod = "payment_method"
        case paymentReference = "payment_reference"
        case paidAt = "paid_at"
        case createdAt = "created_at"
    }
}

// MARK: - Marketplace API Service

class MarketplaceApiService {
    static let shared = MarketplaceApiService()
    
    private let baseURL = ApiConfig.baseURL
    
    private let session: URLSession
    private let decoder: JSONDecoder
    
    private init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        self.session = URLSession(configuration: config)
        self.decoder = JSONDecoder()
    }
    
    private var authToken: String? {
        // Use the same auth token as the main app
        UserDefaults.standard.string(forKey: "auth_token")
    }
    
    // MARK: - Products
    
    func getProducts(
        category: String? = nil,
        minPrice: Double? = nil,
        maxPrice: Double? = nil,
        minCommission: Double? = nil,
        search: String? = nil,
        sortBy: String? = nil,
        page: Int = 1,
        limit: Int = 20
    ) async throws -> ProductListApiResponse {
        var queryItems: [String] = ["page=\(page)", "limit=\(limit)"]
        if let category = category { queryItems.append("category=\(category.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? category)") }
        if let minPrice = minPrice { queryItems.append("min_price=\(minPrice)") }
        if let maxPrice = maxPrice { queryItems.append("max_price=\(maxPrice)") }
        if let minCommission = minCommission { queryItems.append("min_commission=\(minCommission)") }
        if let search = search, !search.isEmpty { queryItems.append("search=\(search.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? search)") }
        if let sortBy = sortBy { queryItems.append("sort_by=\(sortBy)") }
        let query = queryItems.joined(separator: "&")
        return try await get(path: "api/v1/marketplace/products?\(query)", authenticated: false)
    }
    
    func getProduct(productId: String) async throws -> MarketplaceProductResponse {
        return try await get(path: "api/v1/marketplace/products/\(productId)", authenticated: false)
    }
    
    func getFeaturedProducts(limit: Int = 10) async throws -> [MarketplaceProductResponse] {
        return try await get(path: "api/v1/marketplace/products/featured?limit=\(limit)", authenticated: false)
    }
    
    func getCategories(parentId: String? = nil) async throws -> [CategoryResponse] {
        var path = "api/v1/marketplace/categories"
        if let parentId = parentId { path += "?parent_id=\(parentId)" }
        return try await get(path: path, authenticated: false)
    }
    
    // MARK: - Wallet
    
    func getWallet() async throws -> WalletResponse {
        return try await get(path: "api/v1/wallet")
    }
    
    func getWalletTransactions(limit: Int = 50) async throws -> [WalletTransactionResponse] {
        return try await get(path: "api/v1/wallet/transactions?limit=\(limit)")
    }
    
    // MARK: - Withdrawals
    
    func requestWithdrawal(
        amount: Double,
        paymentMethod: String,
        paymentDetails: [String: String]
    ) async throws -> WithdrawalResponse {
        var body: [String: Any] = [
            "amount": amount,
            "payment_method": paymentMethod
        ]
        for (key, value) in paymentDetails {
            body[key] = value
        }
        return try await postJSON(path: "api/v1/wallet/withdraw", body: body)
    }
    
    func getMyWithdrawals() async throws -> [WithdrawalResponse] {
        return try await get(path: "api/v1/wallet/withdrawals")
    }
    
    func getWithdrawalStats() async throws -> WithdrawalStatsResponse {
        return try await get(path: "api/marketplace/withdrawal/stats")
    }
    
    // MARK: - Commissions
    
    func getMyCommissions() async throws -> [CommissionResponse] {
        return try await get(path: "commissions/me")
    }
    
    // MARK: - Promotions
    
    func getMyPromotions(userId: String) async throws -> [PromotionResponse] {
        return try await get(path: "api/v1/promotions/user/\(userId)")
    }
    
    // MARK: - Affiliate Sales
    
    func getAffiliateSales(status: String? = nil) async throws -> [AffiliateSaleResponse] {
        var path = "api/v1/affiliates/sales"
        if let status = status { path += "?status=\(status)" }
        return try await get(path: path)
    }
    
    // MARK: - Generic HTTP Methods
    
    private func get<T: Decodable>(path: String, authenticated: Bool = true) async throws -> T {
        let url = URL(string: "\(baseURL)/\(path)")!
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        if authenticated { addAuthHeader(&request) }
        
        let (data, response) = try await session.data(for: request)
        try validateResponse(response)
        return try decoder.decode(T.self, from: data)
    }
    
    private func postJSON<T: Decodable>(path: String, body: [String: Any]) async throws -> T {
        let url = URL(string: "\(baseURL)/\(path)")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        addAuthHeader(&request)
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, response) = try await session.data(for: request)
        try validateResponse(response)
        return try decoder.decode(T.self, from: data)
    }
    
    private func addAuthHeader(_ request: inout URLRequest) {
        if let token = authToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
    }
    
    private func validateResponse(_ response: URLResponse) throws {
        guard let httpResponse = response as? HTTPURLResponse else {
            throw MarketplaceApiError.invalidResponse
        }
        
        switch httpResponse.statusCode {
        case 200...299:
            return
        case 401:
            throw MarketplaceApiError.unauthorized
        case 403:
            throw MarketplaceApiError.forbidden
        case 404:
            throw MarketplaceApiError.notFound
        case 422:
            throw MarketplaceApiError.validationError
        default:
            throw MarketplaceApiError.serverError(statusCode: httpResponse.statusCode)
        }
    }
}

// MARK: - Error Types

enum MarketplaceApiError: LocalizedError {
    case invalidResponse
    case unauthorized
    case forbidden
    case notFound
    case validationError
    case serverError(statusCode: Int)
    
    var errorDescription: String? {
        switch self {
        case .invalidResponse: return "Invalid server response"
        case .unauthorized: return "Please log in to access this feature."
        case .forbidden: return "You don't have permission for this action."
        case .notFound: return "Resource not found."
        case .validationError: return "Invalid request data."
        case .serverError(let code): return "Server error (\(code)). Please try again."
        }
    }
}
