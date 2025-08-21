package com.project.e_commerce.android.presentation.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.project.e_commerce.android.presentation.ui.screens.AddNewContentScreen
import com.project.e_commerce.android.presentation.ui.screens.AllProductsScreen
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
import com.project.e_commerce.android.presentation.ui.screens.changePasswordScreen.ChangePasswordScreen
import com.project.e_commerce.android.presentation.ui.screens.changePasswordScreen.PasswordChangedSuccessScreen
import com.project.e_commerce.android.presentation.ui.screens.exploreItems

import com.project.e_commerce.android.presentation.ui.screens.forgetPassword.ResetPasswordScreen
import com.project.e_commerce.android.presentation.ui.screens.onboarding.OnboardingScreen1
import com.project.e_commerce.android.presentation.ui.screens.onboarding.OnboardingScreen2
import com.project.e_commerce.android.presentation.ui.screens.onboarding.OnboardingScreen3
import com.project.e_commerce.android.presentation.ui.screens.splashScreen.SplashScreen
import com.project.e_commerce.android.presentation.viewModel.CartViewModel
import com.project.e_commerce.android.presentation.viewModel.ProductViewModel
import com.project_e_commerce.android.presentation.ui.screens.createAccountScreen.CreateAnAccountScreen
import com.project_e_commerce.android.presentation.ui.screens.forgetPassword.ForgetPasswordRequestScreen
import org.koin.androidx.compose.koinViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MyNavHost(
    navController: NavHostController,
    startDestination: String,
    mainUiStateViewModel: com.project.e_commerce.android.presentation.viewModel.MainUiStateViewModel
) {
    val productViewModel: ProductViewModel = koinViewModel()
    val cartViewModel: CartViewModel = koinViewModel()

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

        composable(Screens.ReelsScreen.route) { ReelsView(navController,productViewModel,cartViewModel) }



        composable(Screens.ReelsScreen.SearchReelsAndUsersScreen.route) {
            SearchReelsAndUsersScreen(navController)
        }
        composable(Screens.ReelsScreen.ExploreScreen.route) {
            ExploreScreenWithHeader(items = exploreItems, navController  )
        }
        composable(Screens.ReelsScreen.SoundPageScreen.route) {
            SoundPageScreen( navController)
        }

        composable(Screens.ProductScreen.route) { ProductScreen(navController,productViewModel) }


        composable(Screens.ProductScreen.SearchScreen.route) {
            SearchScreen(navController = navController)
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

        composable(Screens.ProfileScreen.EditPersonalProfile.route) {
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


        composable(Screens.ProfileScreen.NotificationScreen.route) { NotificationScreen(
            navController = navController
        )}

        composable(Screens.ProfileScreen.EditProfileScreen.route) {
            EditProfileScreen(navController)
        }

        /*composable(
            route = Screens.ProfileScreen.EditReelScreen.route,
            arguments = listOf(navArgument("reelId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reelId = backStackEntry.arguments?.getString("reelId") ?: "0"
            EditReelScreen(navController, reelId)
        }*/

        composable("onboarding1") { OnboardingScreen1(navController) }
        composable("onboarding2") { OnboardingScreen2(navController) }
        composable("onboarding3") { OnboardingScreen3(navController) }

        composable(Screens.CartScreen.route) {
            CartScreen(
                navController = navController,
                cartViewModel = cartViewModel
            )
        }

        composable(Screens.CartScreen.PaymentScreen.route){ com.project.e_commerce.android.presentation.ui.screens.PaymentScreen(navController = navController) }



    }
}