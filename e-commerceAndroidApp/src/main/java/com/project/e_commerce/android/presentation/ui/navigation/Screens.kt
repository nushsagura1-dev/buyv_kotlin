package com.project.e_commerce.android.presentation.ui.navigation

import com.project.e_commerce.android.R

sealed class Screens(val route: String, val title : String? = null, val icon : Int? = null) {


    object SplashScreen : Screens(route = "splash_screen" )
    object LoginScreen : Screens(route = "login_screen"){
        object ChangePasswordScreen : Screens(route = "reset_password_screen")
        object CreateAccountScreen : Screens(route = "create_account_screen")

        object EnterEmailScreen: Screens(route = "enter_email_screen")
        object ResetPasswordScreen : Screens(route = "verify_code_screen")
        object PasswordChangedSuccessScreen : Screens(route = "password_changed_success_screen")

    }
    object ReelsScreen: Screens(title = "Reels",route = "reels_screen", icon = R.drawable.ic_home){
        object SearchReelsAndUsersScreen : Screens(route = "search_reels_screen")
        object ExploreScreen : Screens(route = "explore")
        object SoundPageScreen : Screens(route = "sound") {
            fun createRoute(videoUrl: String = ""): String {
                val encodedUrl = java.net.URLEncoder.encode(videoUrl, "UTF-8")
                return "sound?videoUrl=$encodedUrl"
            }
        }
        object UserProfileScreen : Screens(route = "UserProfileScreen")
    }

    object Social : Screens(route = "social_graph") {
        object SearchScreen : Screens("user_search")
        object UserProfileScreen : Screens("user_profile/{userId}") {
            fun createRoute(userId: String) = "user_profile/$userId"
        }
        object FollowListScreen : Screens("follow_list/{userId}/{tab}") { // tab: 0=followers, 1=following
            fun createRoute(userId: String, tab: Int) = "follow_list/$userId/$tab"
        }
        object EditProfileScreen : Screens("edit_profile")
    }

    object ProductScreen : Screens(title = "Products",route = "product_screen",icon = R.drawable.ic_products_filled) {
        object DetailsScreen : Screens("details_screen")
        object SearchScreen : Screens("search_screen")
        object AllProductsScreen : Screens("all_products_screen")
    }

    object CartScreen : Screens(title =  "Cart", route = "cart_screen", icon = R.drawable.ic_cart){

        object PaymentScreen : Screens(route = "payment_screen")
    }


    object ProfileScreen : Screens(title = "Profile", route = "profile_screen",icon = R.drawable.ic_profile, ){

        object OrdersHistoryScreen : Screens(route = "orders_history_screen")

        object TrackOrderScreen : Screens(route = "orders_track_screen")

        object SettingsScreen : Screens(route = "settings_screen")

        object RequestHelpScreen : Screens(route = "request_help_screen")

        object FavouritesScreen  : Screens(route = "favourites_screen")

        object RecentlyScreen  : Screens(route = "recently_screen")

        object AddNewContentScreen: Screens(route = "add_new_content_screen?productId={productId}") {
            fun createRoute(productId: String? = null): String {
                return if (productId != null) "add_new_content_screen?productId=$productId"
                else "add_new_content_screen"
            }
        }

        object NotificationScreen : Screens(route = "notification_screen")

        object EditProfileScreen : Screens("edit_profile_screen")

        object BlockedUsersScreen : Screens(route = "blocked_users_screen")

    }
    
    object OtherUserProfileScreen : Screens(
        route = "other_user_profile/{userId}"
    ) {
        fun createRoute(userId: String) = "other_user_profile/$userId"
    }
    
    object FollowListScreen : Screens(
        route = "follow_list_screen/{username}?startTab={startTab}&showFriendsTab={showFriendsTab}"
    ) {
        fun createRoute(username: String, startTab: Int, showFriendsTab: Boolean) =
            "follow_list_screen/$username?startTab=$startTab&showFriendsTab=$showFriendsTab"
    }

    object BuyScreen : Screens(route = "buy_screen")

    object Marketplace : Screens(title = "Marketplace", route = "marketplace", icon = R.drawable.ic_products_filled) {
        object ProductDetail : Screens(route = "marketplace_product/{productId}") {
            fun createRoute(productId: String) = "marketplace_product/$productId"
        }
    }
    
    // Phase 7: Promoter Dashboard
    object PromoterDashboard : Screens(route = "promoter_dashboard")
    object MyCommissions : Screens(route = "my_commissions")
    object MyPromotions : Screens(route = "my_promotions")
    object Wallet : Screens(route = "wallet")
    object AffiliateSales : Screens(route = "affiliate_sales")
    
    object WithdrawalRequest : Screens(route = "withdrawal_request")
    
    // Phase 9-10: Admin Panel
    object AdminLogin : Screens(route = "admin_login")
    object AdminDashboard : Screens(route = "admin_dashboard")
    object AdminWithdrawal : Screens(route = "admin_withdrawal")
    object AdminUserManagement : Screens(route = "admin_user_management")
    object AdminProductManagement : Screens(route = "admin_product_management")
    object AdminOrderManagement : Screens(route = "admin_order_management")
    object AdminCommissionManagement : Screens(route = "admin_commission_management")
    object AdminCJImport : Screens(route = "admin_cj_import")
    object AdminFollows : Screens(route = "admin_follows")
    object AdminPosts : Screens(route = "admin_posts")
    object AdminComments : Screens(route = "admin_comments")
    object AdminCategories : Screens(route = "admin_categories")
    object AdminAffiliateSales : Screens(route = "admin_affiliate_sales")
    object AdminPromoterWallets : Screens(route = "admin_promoter_wallets")
    object AdminNotifications : Screens(route = "admin_notifications")

}