package com.project.e_commerce.android.presentation.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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
import com.project.e_commerce.android.presentation.ui.screens.FollowListScreen
import com.project.e_commerce.android.presentation.ui.screens.otherUserProfile.OtherUserProfileScreen
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

        composable(Screens.ReelsScreen.route) { 
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
            arguments = listOf(navArgument("reelId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reelId = backStackEntry.arguments?.getString("reelId") ?: ""
            // Navigate to reels screen with specific reel ID
            ReelsView(
                navController = navController,
                viewModel = reelsScreenViewModel,
                cartViewModel = cartViewModel,
                isLoggedIn = true, // TODO: Get actual login state
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
            // Set the reelId in the savedStateHandle for ReelsView to use
            LaunchedEffect(Unit) {
                navController.currentBackStackEntry?.savedStateHandle?.set("reelId", reelId)
            }
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
        composable(Screens.BuyScreen.route) { BuyScreen(navController = navController) }


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
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
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

        composable(Screens.ProfileScreen.AddNewContentScreen.route) {
            AddNewContentScreen(navController = navController)
        }

        // Reel viewer route


        composable(Screens.ProfileScreen.NotificationScreen.route) { NotificationScreen(
            navController = navController
        )}

        composable(Screens.ProfileScreen.EditProfileScreen.route) {
            EditProfileScreen(navController)
        }

        // OtherUserProfileScreen route
        composable(
            route = Screens.OtherUserProfileScreen.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            com.project.e_commerce.android.presentation.ui.screens.otherUserProfile.OtherUserProfileScreen(
                navController = navController,
                userId = userId
            )
        }

        // FollowListScreen route
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
            
            FollowListScreen(
                navController = navController,
                username = username,
                isCurrentUser = true, // TODO: Determine if viewing own profile
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
            com.project.e_commerce.android.presentation.ui.screens.PaymentScreen(
                navController = navController
            )
        }



    }
}