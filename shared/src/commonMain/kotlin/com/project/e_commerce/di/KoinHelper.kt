package com.project.e_commerce.di

import com.project.e_commerce.domain.usecase.auth.GetCurrentUserUseCase
import com.project.e_commerce.domain.usecase.auth.GoogleSignInUseCase
import com.project.e_commerce.domain.usecase.auth.LoginUseCase
import com.project.e_commerce.domain.usecase.auth.LogoutUseCase
import com.project.e_commerce.domain.usecase.auth.RegisterUseCase
import com.project.e_commerce.domain.usecase.auth.SendPasswordResetUseCase
import com.project.e_commerce.domain.usecase.auth.ConfirmPasswordResetUseCase
import com.project.e_commerce.domain.usecase.blockeduser.GetBlockedUsersUseCase
import com.project.e_commerce.domain.usecase.blockeduser.BlockUserUseCase
import com.project.e_commerce.domain.usecase.blockeduser.UnblockUserUseCase
import com.project.e_commerce.domain.usecase.blockeduser.CheckBlockStatusUseCase
import com.project.e_commerce.domain.usecase.cart.AddToCartUseCase
import com.project.e_commerce.domain.usecase.cart.ClearCartUseCase
import com.project.e_commerce.domain.usecase.cart.GetCartUseCase
import com.project.e_commerce.domain.usecase.cart.RemoveFromCartUseCase
import com.project.e_commerce.domain.usecase.cart.UpdateCartItemUseCase
import com.project.e_commerce.domain.usecase.comment.AddCommentUseCase
import com.project.e_commerce.domain.usecase.comment.DeleteCommentUseCase
import com.project.e_commerce.domain.usecase.comment.GetCommentsUseCase
import com.project.e_commerce.domain.usecase.comment.LikeCommentUseCase
import com.project.e_commerce.domain.usecase.sound.GetSoundsUseCase
import com.project.e_commerce.domain.usecase.sound.GetTrendingSoundsUseCase
import com.project.e_commerce.domain.usecase.sound.GetSoundDetailsUseCase
import com.project.e_commerce.domain.usecase.sound.GetSoundGenresUseCase
import com.project.e_commerce.domain.usecase.sound.IncrementSoundUsageUseCase
import com.project.e_commerce.domain.usecase.tracking.TrackViewUseCase
import com.project.e_commerce.domain.usecase.tracking.TrackClickUseCase
import com.project.e_commerce.domain.usecase.tracking.TrackConversionUseCase
import com.project.e_commerce.domain.usecase.order.CancelOrderUseCase
import com.project.e_commerce.domain.usecase.order.CreateOrderUseCase
import com.project.e_commerce.domain.usecase.order.GetOrderDetailsUseCase
import com.project.e_commerce.domain.usecase.order.GetOrdersByUserUseCase
import com.project.e_commerce.domain.usecase.order.GetRecentOrdersUseCase
import com.project.e_commerce.domain.usecase.post.BookmarkPostUseCase
import com.project.e_commerce.domain.usecase.post.CheckPostBookmarkStatusUseCase
import com.project.e_commerce.domain.usecase.post.CheckPostLikeStatusUseCase
import com.project.e_commerce.domain.usecase.post.GetBookmarkedPostsUseCase
import com.project.e_commerce.domain.usecase.post.GetLikedPostsUseCase
import com.project.e_commerce.domain.usecase.post.LikePostUseCase
import com.project.e_commerce.domain.usecase.post.UnbookmarkPostUseCase
import com.project.e_commerce.domain.usecase.post.UnlikePostUseCase
import com.project.e_commerce.domain.usecase.post.CreatePostUseCase
import com.project.e_commerce.domain.usecase.post.DeletePostUseCase
import com.project.e_commerce.domain.usecase.product.GetCategoriesUseCase
import com.project.e_commerce.domain.usecase.product.GetProductDetailsUseCase
import com.project.e_commerce.domain.usecase.product.GetProductsUseCase
import com.project.e_commerce.domain.usecase.user.FollowUserUseCase
import com.project.e_commerce.domain.usecase.user.GetFollowersUseCase
import com.project.e_commerce.domain.usecase.user.GetFollowingStatusUseCase
import com.project.e_commerce.domain.usecase.user.GetFollowingUseCase
import com.project.e_commerce.domain.usecase.user.GetUserPostsUseCase
import com.project.e_commerce.domain.usecase.user.SearchUsersUseCase
import com.project.e_commerce.domain.usecase.user.UnfollowUserUseCase
import com.project.e_commerce.domain.usecase.user.UpdateUserProfileUseCase
import com.project.e_commerce.domain.usecase.user.GetUserProfileUseCase
import com.project.e_commerce.domain.usecase.user.DeleteAccountUseCase
import com.project.e_commerce.domain.usecase.marketplace.GetProductsUseCase as MarketplaceGetProductsUseCase
import com.project.e_commerce.domain.usecase.marketplace.GetProductByIdUseCase
import com.project.e_commerce.domain.usecase.marketplace.GetMyWalletUseCase
import com.project.e_commerce.domain.usecase.marketplace.CreatePromotionUseCase
import com.project.e_commerce.data.local.TokenManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

/**
 * Helper Koin pour iOS.
 * 
 * Fournit un accès facile aux dépendances pour le code Swift.
 * iOS ne peut pas utiliser l'injection directe comme Android, donc ce helper
 * expose les use cases via des propriétés accessibles depuis Swift.
 */
class KoinHelper : KoinComponent {
    
    // Auth Use Cases
    val loginUseCase: LoginUseCase by inject()
    val registerUseCase: RegisterUseCase by inject()
    val logoutUseCase: LogoutUseCase by inject()
    val getCurrentUserUseCase: GetCurrentUserUseCase by inject()
    val sendPasswordResetUseCase: SendPasswordResetUseCase by inject()
    val googleSignInUseCase: GoogleSignInUseCase by inject()
    val confirmPasswordResetUseCase: ConfirmPasswordResetUseCase by inject()
    
    // Product Use Cases
    val getProductsUseCase: GetProductsUseCase by inject()
    val getProductDetailsUseCase: GetProductDetailsUseCase by inject()
    val getCategoriesUseCase: GetCategoriesUseCase by inject()
    
    // Cart Use Cases
    val getCartUseCase: GetCartUseCase by inject()
    val addToCartUseCase: AddToCartUseCase by inject()
    val updateCartItemUseCase: UpdateCartItemUseCase by inject()
    val removeFromCartUseCase: RemoveFromCartUseCase by inject()
    val clearCartUseCase: ClearCartUseCase by inject()
    
    // Order Use Cases
    val createOrderUseCase: CreateOrderUseCase by inject()
    val getOrdersByUserUseCase: GetOrdersByUserUseCase by inject()
    val getOrderDetailsUseCase: GetOrderDetailsUseCase by inject()
    val cancelOrderUseCase: CancelOrderUseCase by inject()
    val getRecentOrdersUseCase: GetRecentOrdersUseCase by inject()
    
    // User Use Cases
    val followUserUseCase: FollowUserUseCase by inject()
    val unfollowUserUseCase: UnfollowUserUseCase by inject()
    val getFollowersUseCase: GetFollowersUseCase by inject()
    val getFollowingUseCase: GetFollowingUseCase by inject()
    val getFollowingStatusUseCase: GetFollowingStatusUseCase by inject()
    val getUserPostsUseCase: GetUserPostsUseCase by inject()
    val updateUserProfileUseCase: UpdateUserProfileUseCase by inject()
    val searchUsersUseCase: SearchUsersUseCase by inject()
    val getUserProfileUseCase: GetUserProfileUseCase by inject()
    val deleteAccountUseCase: DeleteAccountUseCase by inject()
    
    // Post Use Cases
    val likePostUseCase: LikePostUseCase by inject()
    val unlikePostUseCase: UnlikePostUseCase by inject()
    val bookmarkPostUseCase: BookmarkPostUseCase by inject()
    val unbookmarkPostUseCase: UnbookmarkPostUseCase by inject()
    val getLikedPostsUseCase: GetLikedPostsUseCase by inject()
    val getBookmarkedPostsUseCase: GetBookmarkedPostsUseCase by inject()
    val checkPostLikeStatusUseCase: CheckPostLikeStatusUseCase by inject()
    val checkPostBookmarkStatusUseCase: CheckPostBookmarkStatusUseCase by inject()
    val createPostUseCase: CreatePostUseCase by inject()
    val deletePostUseCase: DeletePostUseCase by inject()
    
    // Comment Use Cases
    val getCommentsUseCase: GetCommentsUseCase by inject()
    val addCommentUseCase: AddCommentUseCase by inject()
    val deleteCommentUseCase: DeleteCommentUseCase by inject()
    val likeCommentUseCase: LikeCommentUseCase by inject()
    
    // Blocked User Use Cases
    val getBlockedUsersUseCase: GetBlockedUsersUseCase by inject()
    val blockUserUseCase: BlockUserUseCase by inject()
    val unblockUserUseCase: UnblockUserUseCase by inject()
    val checkBlockStatusUseCase: CheckBlockStatusUseCase by inject()

    // Sound Use Cases
    val getSoundsUseCase: GetSoundsUseCase by inject()
    val getTrendingSoundsUseCase: GetTrendingSoundsUseCase by inject()
    val getSoundDetailsUseCase: GetSoundDetailsUseCase by inject()
    val getSoundGenresUseCase: GetSoundGenresUseCase by inject()
    val incrementSoundUsageUseCase: IncrementSoundUsageUseCase by inject()

    // Tracking Use Cases
    val trackViewUseCase: TrackViewUseCase by inject()
    val trackClickUseCase: TrackClickUseCase by inject()
    val trackConversionUseCase: TrackConversionUseCase by inject()

    // Marketplace Use Cases
    val marketplaceGetProductsUseCase: MarketplaceGetProductsUseCase by inject()
    val getProductByIdUseCase: GetProductByIdUseCase by inject()
    val getMyWalletUseCase: GetMyWalletUseCase by inject()
    val createPromotionUseCase: CreatePromotionUseCase by inject()

    // Token Manager - exposed for native API calls that bypass shared layer
    private val tokenManager: TokenManager by inject()
    
    /**
     * Get the current auth token for native API calls.
     * Used by iOS for direct backend calls (notifications, admin, etc.)
     */
    fun getAuthToken(): String? = tokenManager.getAccessToken()
    
    companion object {
        /**
         * Initialise Koin pour iOS.
         * Doit être appelé au démarrage de l'application iOS.
         */
        fun initKoin() {
            startKoin {
                modules(sharedModules)
            }
        }
    }
}
