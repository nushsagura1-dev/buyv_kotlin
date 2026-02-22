import Foundation
import Shared

class DependencyWrapper {
    static let shared = DependencyWrapper()
    
    private let helper = KoinHelper()
    
    // Auth
    var loginUseCase: LoginUseCase { helper.loginUseCase }
    var registerUseCase: RegisterUseCase { helper.registerUseCase }
    var logoutUseCase: LogoutUseCase { helper.logoutUseCase }
    var getCurrentUserUseCase: GetCurrentUserUseCase { helper.getCurrentUserUseCase }
    var sendPasswordResetUseCase: SendPasswordResetUseCase { helper.sendPasswordResetUseCase }
    var googleSignInUseCase: GoogleSignInUseCase { helper.googleSignInUseCase }
    
    // Product
    var getProductsUseCase: GetProductsUseCase { helper.getProductsUseCase }
    var getProductDetailsUseCase: GetProductDetailsUseCase { helper.getProductDetailsUseCase }
    var getCategoriesUseCase: GetCategoriesUseCase { helper.getCategoriesUseCase }
    
    // Cart
    var getCartUseCase: GetCartUseCase { helper.getCartUseCase }
    var addToCartUseCase: AddToCartUseCase { helper.addToCartUseCase }
    var updateCartItemUseCase: UpdateCartItemUseCase { helper.updateCartItemUseCase }
    var removeFromCartUseCase: RemoveFromCartUseCase { helper.removeFromCartUseCase }
    var clearCartUseCase: ClearCartUseCase { helper.clearCartUseCase }
    
    // Order
    var createOrderUseCase: CreateOrderUseCase { helper.createOrderUseCase }
    var getOrdersByUserUseCase: GetOrdersByUserUseCase { helper.getOrdersByUserUseCase }
    var getOrderDetailsUseCase: GetOrderDetailsUseCase { helper.getOrderDetailsUseCase }
    var cancelOrderUseCase: CancelOrderUseCase { helper.cancelOrderUseCase }
    var getRecentOrdersUseCase: GetRecentOrdersUseCase { helper.getRecentOrdersUseCase }
    
    // User Social
    var followUserUseCase: FollowUserUseCase { helper.followUserUseCase }
    var unfollowUserUseCase: UnfollowUserUseCase { helper.unfollowUserUseCase }
    var getFollowersUseCase: GetFollowersUseCase { helper.getFollowersUseCase }
    var getFollowingUseCase: GetFollowingUseCase { helper.getFollowingUseCase }
    var getFollowingStatusUseCase: GetFollowingStatusUseCase { helper.getFollowingStatusUseCase }
    var getUserPostsUseCase: GetUserPostsUseCase { helper.getUserPostsUseCase }
    var updateUserProfileUseCase: UpdateUserProfileUseCase { helper.updateUserProfileUseCase }
    var searchUsersUseCase: SearchUsersUseCase { helper.searchUsersUseCase }
    var getUserProfileUseCase: GetUserProfileUseCase { helper.getUserProfileUseCase }
    var deleteAccountUseCase: DeleteAccountUseCase { helper.deleteAccountUseCase }
    
    // Post
    var likePostUseCase: LikePostUseCase { helper.likePostUseCase }
    var unlikePostUseCase: UnlikePostUseCase { helper.unlikePostUseCase }
    var bookmarkPostUseCase: BookmarkPostUseCase { helper.bookmarkPostUseCase }
    var unbookmarkPostUseCase: UnbookmarkPostUseCase { helper.unbookmarkPostUseCase }
    var getLikedPostsUseCase: GetLikedPostsUseCase { helper.getLikedPostsUseCase }
    var getBookmarkedPostsUseCase: GetBookmarkedPostsUseCase { helper.getBookmarkedPostsUseCase }
    var checkPostLikeStatusUseCase: CheckPostLikeStatusUseCase { helper.checkPostLikeStatusUseCase }
    var checkPostBookmarkStatusUseCase: CheckPostBookmarkStatusUseCase { helper.checkPostBookmarkStatusUseCase }
    var createPostUseCase: CreatePostUseCase { helper.createPostUseCase }
    var deletePostUseCase: DeletePostUseCase { helper.deletePostUseCase }

    // User Extended
    var getCommentsUseCase: GetCommentsUseCase { helper.getCommentsUseCase }
    var addCommentUseCase: AddCommentUseCase { helper.addCommentUseCase }
    var deleteCommentUseCase: DeleteCommentUseCase { helper.deleteCommentUseCase }
    var likeCommentUseCase: LikeCommentUseCase { helper.likeCommentUseCase }
    
    // Marketplace
    var marketplaceGetProductsUseCase: Shared.MarketplaceGetProductsUseCase { helper.marketplaceGetProductsUseCase }
    var getProductByIdUseCase: GetProductByIdUseCase { helper.getProductByIdUseCase }
    var getMyWalletUseCase: GetMyWalletUseCase { helper.getMyWalletUseCase }
    var createPromotionUseCase: CreatePromotionUseCase { helper.createPromotionUseCase }

    // Auth Token - for native API calls bypassing shared layer
    func getAuthToken() -> String? { helper.getAuthToken() }
}
