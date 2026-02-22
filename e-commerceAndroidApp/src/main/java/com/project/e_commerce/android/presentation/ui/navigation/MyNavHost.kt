package com.project.e_commerce.android.presentation.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.project.e_commerce.domain.deeplink.DeepLinkConfig
import com.project.e_commerce.android.presentation.ui.screens.AddNewContentScreen
import com.project.e_commerce.android.presentation.ui.screens.AllProductsScreen
import com.project.e_commerce.android.presentation.ui.screens.BuyScreen
import com.project.e_commerce.android.presentation.ui.screens.CartScreen
import com.project.e_commerce.android.presentation.ui.screens.DetailsScreen
import com.project.e_commerce.android.presentation.ui.screens.EditProfileScreen
import com.project.e_commerce.android.presentation.ui.screens.ExploreScreenWithHeader
import com.project.e_commerce.android.presentation.ui.screens.FavouriteScreen
import com.project.e_commerce.android.presentation.ui.screens.NotificationScreen
import com.project.e_commerce.android.presentation.ui.screens.OrdersHistoryScreen
import com.project.e_commerce.android.presentation.ui.screens.ProductScreen
import com.project.e_commerce.android.presentation.ui.screens.RecentlyViewedScreen
import com.project.e_commerce.android.presentation.ui.screens.SearchReelsAndUsersScreen
import com.project.e_commerce.android.presentation.ui.screens.SearchScreen
import com.project.e_commerce.android.presentation.ui.screens.SettingsScreen
import com.project.e_commerce.android.presentation.ui.screens.SoundPageScreen
import com.project.e_commerce.android.presentation.ui.screens.TrackOrderScreen
import com.project.e_commerce.android.presentation.ui.screens.loginScreen.LoginScreen
import com.project.e_commerce.android.presentation.ui.screens.profileScreen.ProfileScreen
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.ReelsView
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.SheetType

import com.project.e_commerce.android.presentation.ui.screens.changePasswordScreen.ChangePasswordScreen
import com.project.e_commerce.android.presentation.ui.screens.changePasswordScreen.PasswordChangedSuccessScreen

import com.project.e_commerce.android.presentation.ui.screens.forgetPassword.ResetPasswordScreen
import com.project.e_commerce.android.presentation.ui.screens.onboarding.OnboardingScreen1
import com.project.e_commerce.android.presentation.ui.screens.onboarding.OnboardingScreen2
import com.project.e_commerce.android.presentation.ui.screens.onboarding.OnboardingScreen3
import com.project.e_commerce.android.presentation.ui.screens.splashScreen.SplashScreen
import com.project.e_commerce.android.presentation.viewModel.CartViewModel
import com.project.e_commerce.android.presentation.viewModel.ProductViewModel
import com.project.e_commerce.android.presentation.viewModel.reelsScreenViewModel.ReelsScreenViewModel
import com.project.e_commerce.android.presentation.ui.screens.createAccountScreen.CreateAnAccountScreen
import com.project_e_commerce.android.presentation.ui.screens.forgetPassword.ForgetPasswordRequestScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.android.presentation.ui.screens.social.UserSearchScreen
import com.project.e_commerce.android.presentation.ui.screens.social.UserProfileScreen
import com.project.e_commerce.android.presentation.ui.screens.social.FollowListScreen
import com.project.e_commerce.android.presentation.ui.screens.social.EditProfileScreen
import com.project.e_commerce.android.presentation.ui.screens.marketplace.MarketplaceScreen
import com.project.e_commerce.android.presentation.ui.screens.marketplace.ProductDetailScreen
import com.project.e_commerce.android.presentation.ui.screens.promoter.PromoterDashboardScreen
import com.project.e_commerce.android.presentation.ui.screens.promoter.MyCommissionsScreen
import com.project.e_commerce.android.presentation.ui.screens.promoter.MyPromotionsScreen
import com.project.e_commerce.android.presentation.ui.screens.promoter.WalletScreen
import com.project.e_commerce.android.presentation.ui.screens.promoter.AffiliateSalesScreen
import com.project.e_commerce.android.presentation.ui.screens.withdrawal.WithdrawalRequestScreen
import com.project.e_commerce.android.presentation.ui.screens.BlockedUsersScreen
import com.project.e_commerce.android.presentation.ui.screens.admin.AdminWithdrawalScreen
// Phase 10: Admin screens
import com.project.e_commerce.android.presentation.ui.screens.admin.AdminLoginScreen
import com.project.e_commerce.android.presentation.ui.screens.admin.AdminDashboardScreen
import com.project.e_commerce.android.presentation.ui.screens.admin.AdminUserManagementScreen
import com.project.e_commerce.android.presentation.ui.screens.admin.AdminProductManagementScreen
import com.project.e_commerce.android.presentation.ui.screens.admin.AdminOrderScreen
import com.project.e_commerce.android.presentation.ui.screens.admin.AdminCommissionScreen
import com.project.e_commerce.android.presentation.ui.screens.admin.AdminCJImportScreen
import com.project.e_commerce.android.presentation.ui.screens.admin.AdminFollowsScreen
import com.project.e_commerce.android.presentation.ui.screens.admin.AdminPostsScreen
import com.project.e_commerce.android.presentation.ui.screens.admin.AdminCommentsScreen
import com.project.e_commerce.android.presentation.ui.screens.admin.AdminCategoriesScreen
import com.project.e_commerce.android.presentation.ui.screens.admin.AdminAffiliateSalesScreen
import com.project.e_commerce.android.presentation.ui.screens.admin.AdminPromoterWalletsScreen
import com.project.e_commerce.android.presentation.ui.screens.admin.AdminNotificationsScreen

import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MyNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String,
    mainUiStateViewModel: com.project.e_commerce.android.presentation.viewModel.MainUiStateViewModel
) {
    val productViewModel: ProductViewModel = koinViewModel()
    val cartViewModel: CartViewModel = koinViewModel()
    val reelsScreenViewModel: ReelsScreenViewModel = koinViewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screens.SplashScreen.route){ SplashScreen(navController = navController)}
        composable(Screens.LoginScreen.route) { LoginScreen(navController) }
        composable(Screens.LoginScreen.EnterEmailScreen.route) { ForgetPasswordRequestScreen(navController) }
        composable(Screens.LoginScreen.CreateAccountScreen.route) {
            CreateAnAccountScreen(
                navController
            )
        }
        composable(Screens.LoginScreen.ResetPasswordScreen.route) { ResetPasswordScreen(navController) }
        composable(Screens.LoginScreen.ChangePasswordScreen.route) { ChangePasswordScreen(navController) }
        composable(Screens.LoginScreen.PasswordChangedSuccessScreen.route) { PasswordChangedSuccessScreen(navController) }

        composable(
            route = Screens.ReelsScreen.route,
            deepLinks = listOf(navDeepLink { uriPattern = "${DeepLinkConfig.SCHEME}://${DeepLinkConfig.HOST}/${DeepLinkConfig.Paths.REELS}" })
        ) { 
            Log.d("MyNavHost", "🎬 ReelsScreen route composable started")
            Log.d("MyNavHost", "🎬 Creating ReelsView with reelsScreenViewModel: $reelsScreenViewModel")
            
            ReelsView(
                navController = navController,
                viewModel = reelsScreenViewModel,
                cartViewModel = cartViewModel,
                isLoggedIn = true, // TODO: Get actual login state
                onShowSheet = { sheetType, reel ->
                    Log.d("MyNavHost", "🎬 onShowSheet called with type: $sheetType, reel: ${reel?.id}")
                    when (sheetType) {
                        SheetType.AddToCart -> {
                            if (reel != null) {
                                Log.d("MyNavHost", "🎬 Showing add to cart sheet for reel: ${reel.id}")
                                mainUiStateViewModel.showAddToCartSheet(reel)
                            }
                        }
                        SheetType.Comments -> {
                            Log.d("MyNavHost", "🎬 Comments sheet requested")
                            // Handle comments sheet if needed
                        }
                    }
                },
                mainUiStateViewModel = mainUiStateViewModel
            )
            Log.d("MyNavHost", "🎬 ReelsView rendered successfully")
        }



        composable(Screens.ReelsScreen.SearchReelsAndUsersScreen.route) {
            SearchReelsAndUsersScreen(navController)
        }
        composable(Screens.ReelsScreen.ExploreScreen.route) {
            ExploreScreenWithHeader(navController = navController)
        }
        
        // New route for opening individual reels from explore page
        composable(
            route = Screens.ReelsScreen.route + "/{reelId}",
            arguments = listOf(navArgument("reelId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "${DeepLinkConfig.SCHEME}://${DeepLinkConfig.HOST}/${DeepLinkConfig.Paths.POST}/{reelId}" })
        ) { backStackEntry ->
            val reelId = backStackEntry.arguments?.getString("reelId") ?: ""
            // Navigate to reels screen with specific reel ID
            ReelsView(
                navController = navController,
                viewModel = reelsScreenViewModel,
                cartViewModel = cartViewModel,
                isLoggedIn = true, // TODO: Get actual login state
                targetReelId = reelId,
                onShowSheet = { sheetType, reel ->
                    when (sheetType) {
                        SheetType.AddToCart -> {
                            if (reel != null) {
                                mainUiStateViewModel.showAddToCartSheet(reel)
                            }
                        }
                        SheetType.Comments -> {
                            // Handle comments sheet if needed
                        }
                    }
                },
                mainUiStateViewModel = mainUiStateViewModel
            )
        }
        composable(
            route = Screens.ReelsScreen.SoundPageScreen.route + "?videoUrl={videoUrl}",
            arguments = listOf(
                navArgument("videoUrl") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val encodedVideoUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
            val videoUrl = if (encodedVideoUrl.isNotEmpty()) {
                java.net.URLDecoder.decode(encodedVideoUrl, "UTF-8")
            } else {
                ""
            }
            SoundPageScreen(
                navController = navController,
                videoUrl = videoUrl
            )
        }

        composable(Screens.ProductScreen.route) { ProductScreen(navController,productViewModel) }
        // BuyScreen redirected to Marketplace (was using hardcoded fake data)
        composable(Screens.BuyScreen.route) { 
            LaunchedEffect(Unit) {
                navController.navigate("marketplace") {
                    popUpTo(Screens.BuyScreen.route) { inclusive = true }
                }
            }
        }


        composable(Screens.ProductScreen.SearchScreen.route) {
            SearchScreen(navController = navController, viewModel = productViewModel)
        }

        composable(
            route = Screens.ProductScreen.AllProductsScreen.route + "/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "Featured Products"

            val products = when (type) {
                "Featured Products" -> productViewModel.featuredProducts
                "bestseller" -> productViewModel.bestSellerProducts
                else -> productViewModel.categoryProducts
            }

            AllProductsScreen(navController, products = products, title = type)
        }
        composable(
            route = Screens.ProductScreen.DetailsScreen.route + "/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "${DeepLinkConfig.SCHEME}://${DeepLinkConfig.HOST}/${DeepLinkConfig.Paths.PRODUCT}/{productId}" })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            DetailsScreen(navController = navController, productId = productId,viewModel= productViewModel,cartViewModel=cartViewModel)
        }

        composable(Screens.ProfileScreen.route) { ProfileScreen(navController) }

        composable(Screens.ProfileScreen.EditProfileScreen.route) {
            EditProfileScreen(
                navController = navController
            )
        }
        composable(Screens.ProfileScreen.FavouritesScreen.route) {
            FavouriteScreen(
                navController = navController
            )
        }

        composable(Screens.ProfileScreen.RecentlyScreen.route) {
            RecentlyViewedScreen(navController = navController)
        }

        composable(Screens.ProfileScreen.TrackOrderScreen.route) {
            TrackOrderScreen(navController = navController)
        }

        composable(Screens.ProfileScreen.OrdersHistoryScreen.route) {
            OrdersHistoryScreen(navController = navController)
        }

        composable(Screens.ProfileScreen.SettingsScreen.route) {
            SettingsScreen(navController = navController)
        }

        composable(
            route = Screens.ProfileScreen.AddNewContentScreen.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val preSelectedProductId = backStackEntry.arguments?.getString("productId")
            AddNewContentScreen(
                navController = navController,
                preSelectedProductId = preSelectedProductId,
                onPostCreated = {
                    // Refresh product feed and reels after creating a new post
                    productViewModel.refreshProducts()
                    reelsScreenViewModel.refreshReels()
                }
            )
        }

        // Reel viewer route


        composable(Screens.ProfileScreen.NotificationScreen.route) { NotificationScreen(
            navController = navController
        )}

        composable(Screens.ProfileScreen.EditProfileScreen.route) {
            EditProfileScreen(navController)
        }

        composable(Screens.ProfileScreen.BlockedUsersScreen.route) {
            BlockedUsersScreen(navController = navController)
        }

        // Social Graph
        composable(Screens.Social.SearchScreen.route) {
            UserSearchScreen(navController = navController)
        }

        composable(
             route = Screens.Social.UserProfileScreen.route,
             arguments = listOf(navArgument("userId") { type = NavType.StringType }),
             deepLinks = listOf(navDeepLink { uriPattern = "${DeepLinkConfig.SCHEME}://${DeepLinkConfig.HOST}/${DeepLinkConfig.Paths.PROFILE}/{userId}" })
        ) { backStackEntry ->
             val userId = backStackEntry.arguments?.getString("userId") ?: ""
             val currentUserProvider: CurrentUserProvider = koinInject()
             var currentUserId by remember { mutableStateOf<String?>(null) }
             LaunchedEffect(Unit) {
                 currentUserId = currentUserProvider.getCurrentUserId()
             }
             UserProfileScreen(userId = userId, currentUserId = currentUserId ?: "", navController = navController)
        }

        composable(
            route = Screens.Social.FollowListScreen.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("tab") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val tab = backStackEntry.arguments?.getInt("tab") ?: 0
            val currentUserProvider: CurrentUserProvider = koinInject()
            var currentUserId by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                currentUserId = currentUserProvider.getCurrentUserId()
            }
            FollowListScreen(userId = userId, currentUserId = currentUserId ?: "", initialTab = tab, navController = navController)
        }

        composable(Screens.Social.EditProfileScreen.route) {
            EditProfileScreen(navController = navController)
        }

        // Route used by ProfileScreen Followers/Following stats click
        composable(
            route = Screens.FollowListScreen.route,
            arguments = listOf(
                navArgument("username") { type = NavType.StringType },
                navArgument("startTab") { type = NavType.IntType; defaultValue = 0 },
                navArgument("showFriendsTab") { type = NavType.BoolType; defaultValue = true }
            )
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            val startTab = backStackEntry.arguments?.getInt("startTab") ?: 0
            val showFriendsTab = backStackEntry.arguments?.getBoolean("showFriendsTab") ?: true
            com.project.e_commerce.android.presentation.ui.screens.FollowListScreen(
                navController = navController,
                username = username,
                isCurrentUser = true,
                startTabIndex = startTab,
                showFriendsTab = showFriendsTab
            )
        }

        composable("onboarding1") { OnboardingScreen1(navController) }
        composable("onboarding2") { OnboardingScreen2(navController) }
        composable("onboarding3") { OnboardingScreen3(navController) }

        composable(Screens.CartScreen.route) {
            CartScreen(
                navController = navController,
                cartViewModel = cartViewModel
            )
        }

        composable(Screens.CartScreen.PaymentScreen.route) {
            // Task 3.2: Using Stripe Payment Sheet instead of manual card entry
            com.project.e_commerce.android.presentation.ui.screens.StripePaymentScreen(
                navController = navController,
                cartViewModel = cartViewModel
            )
        }

        // Marketplace routes
        composable(Screens.Marketplace.route) {
            MarketplaceScreen(navController = navController)
        }

        composable(
            route = Screens.Marketplace.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                productId = productId,
                navController = navController
            )
        }
        
        // Phase 7: Promoter Dashboard
        composable(Screens.PromoterDashboard.route) {
            PromoterDashboardScreen(navController = navController)
        }
        
        composable(Screens.MyCommissions.route) {
            MyCommissionsScreen(navController = navController)
        }
        
        composable(Screens.MyPromotions.route) {
            MyPromotionsScreen(navController = navController)
        }
        
        composable(Screens.Wallet.route) {
            WalletScreen(navController = navController)
        }
        
        composable(Screens.AffiliateSales.route) {
            AffiliateSalesScreen(navController = navController)
        }
        
        composable(Screens.WithdrawalRequest.route) {
            WithdrawalRequestScreen(navController = navController)
        }
        
        // Phase 9: Admin Dashboard
        composable(Screens.AdminWithdrawal.route) {
            AdminWithdrawalScreen(navController = navController)
        }
        
        // Phase 10: Admin Panel Mobile
        composable(Screens.AdminLogin.route) {
            AdminLoginScreen(navController = navController)
        }
        
        composable(Screens.AdminDashboard.route) {
            AdminDashboardScreen(navController = navController)
        }
        
        composable(Screens.AdminUserManagement.route) {
            AdminUserManagementScreen(navController = navController)
        }
        
        composable(Screens.AdminProductManagement.route) {
            AdminProductManagementScreen(navController = navController)
        }
        
        composable(Screens.AdminOrderManagement.route) {
            AdminOrderScreen(navController = navController)
        }
        
        composable(Screens.AdminCommissionManagement.route) {
            AdminCommissionScreen(navController = navController)
        }
        
        composable(Screens.AdminCJImport.route) {
            AdminCJImportScreen(navController = navController)
        }

        // Admin Placeholders v1.0 (Features v1.1+)
        composable(Screens.AdminFollows.route) { AdminFollowsScreen(navController) }
        composable(Screens.AdminPosts.route) { AdminPostsScreen(navController) }
        composable(Screens.AdminComments.route) { AdminCommentsScreen(navController) }
        composable(Screens.AdminCategories.route) { AdminCategoriesScreen(navController) }
        composable(Screens.AdminAffiliateSales.route) { AdminAffiliateSalesScreen(navController) }
        composable(Screens.AdminPromoterWallets.route) { AdminPromoterWalletsScreen(navController) }
        composable(Screens.AdminNotifications.route) { AdminNotificationsScreen(navController) }

    }
}