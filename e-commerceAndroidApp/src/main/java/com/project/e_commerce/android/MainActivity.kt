package com.project.e_commerce.android

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.FabPosition
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.project.e_commerce.android.presentation.ui.navigation.AppBottomBar
import com.project.e_commerce.android.presentation.ui.navigation.MyNavHost
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Reels
import com.project.e_commerce.android.presentation.viewModel.CartViewModel
import com.project.e_commerce.android.presentation.viewModel.MainUiStateViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    private val mainUiStateViewModel: MainUiStateViewModel by viewModels()
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CrashDebug", "MainActivity: onCreate started")
        try {
            Log.d("CrashDebug", "MainActivity: ViewModels initializing")

            val testRepo: com.project.e_commerce.android.domain.repository.CartRepository =
                org.koin.java.KoinJavaComponent.getKoin().get()
            val testAuth: com.google.firebase.auth.FirebaseAuth =
                org.koin.java.KoinJavaComponent.getKoin().get()
            Log.d("CrashDebug", "Koin test fetch: repo=$testRepo, auth=$testAuth")
            // Existing ViewModel initialization
            Log.d("CrashDebug", "MainActivity: ViewModels initialized")

            Log.d("CrashDebug", "MainActivity: Setting content")
            setContent {
                val navController = rememberNavController()
                val cartViewModel: CartViewModel = koinViewModel()
                val tabScreens = listOf(
                    Screens.ReelsScreen,
                    Screens.ProductScreen,
                    Screens.CartScreen,
                    Screens.ProfileScreen
                )
                val tabIcons = listOf(
                    R.drawable.ic_home,           // Reels
                    R.drawable.ic_products_filled, // Products
                    R.drawable.ic_cart,            // Cart
                    R.drawable.ic_profile          // Profile
                )
                // Use currentBackStackEntryAsState to observe navigation state and trigger recomposition
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: ""

                // Debug logging for route changes
                Log.d("NavigationDebug", "=== NAVIGATION STATE DEBUG ===")
                Log.d("NavigationDebug", "Current route: '$currentRoute'")
                Log.d("NavigationDebug", "BackStackEntry exists: ${navBackStackEntry != null}")
                Log.d("NavigationDebug", "Destination: ${navBackStackEntry?.destination}")

                // Log the screens for reference
                Log.d(
                    "NavigationDebug",
                    "Screen routes - Reels: '${Screens.ReelsScreen.route}', Products: '${Screens.ProductScreen.route}', Cart: '${Screens.CartScreen.route}', Profile: '${Screens.ProfileScreen.route}'"
                )

                // Collect state for showing/hiding bottom bar and FAB
                val isBottomSheetVisible by mainUiStateViewModel.isBottomSheetVisible.collectAsState()
                val hideBottomBar by mainUiStateViewModel.hideBottomBar.collectAsState()

                // Only display bottom bar and FAB on these main tab screens
                val showBottomBar = currentRoute in listOf(
                    Screens.ReelsScreen.route,
                    Screens.ProductScreen.route,
                    Screens.CartScreen.route,
                    Screens.ProfileScreen.route,
                    Screens.ReelsScreen.ExploreScreen.route
                ) && !currentRoute.startsWith("edit_profile_screen") &&
                        !currentRoute.startsWith("follow_list_screen") &&
                        !currentRoute.startsWith("login_screen") &&
                        !currentRoute.startsWith("create_account_screen") &&
                        !currentRoute.startsWith("enter_email_screen") &&
                        !currentRoute.startsWith("reset_password_screen") &&
                        !currentRoute.startsWith("verify_code_screen") &&
                        !currentRoute.startsWith("password_changed_success_screen") &&
                        !hideBottomBar

                // Show FAB only when on the main reels screen
                val showFAB = currentRoute == Screens.ReelsScreen.route && !hideBottomBar

                // Fixed selectedTab calculation to handle sub-screens correctly
                val selectedTab = when {
                    // Reels screen and its sub-screens
                    currentRoute == Screens.ReelsScreen.route ||
                            currentRoute == Screens.ReelsScreen.SearchReelsAndUsersScreen.route ||
                            currentRoute == Screens.ReelsScreen.ExploreScreen.route ||
                            currentRoute == Screens.ReelsScreen.SoundPageScreen.route ||
                            currentRoute == Screens.ReelsScreen.UserProfileScreen.route ||
                            currentRoute.startsWith("reels_screen") -> {
                        Log.d("NavigationDebug", "✅ MATCHED: Reels tab (index 0)")
                        0
                    }

                    // Product screen and its sub-screens
                    currentRoute == Screens.ProductScreen.route ||
                            currentRoute == Screens.ProductScreen.DetailsScreen.route ||
                            currentRoute == Screens.ProductScreen.SearchScreen.route ||
                            currentRoute == Screens.ProductScreen.AllProductsScreen.route ||
                            currentRoute.startsWith("product_screen") ||
                            currentRoute.startsWith("details_screen") ||
                            currentRoute.startsWith("search_screen") ||
                            currentRoute.startsWith("all_products_screen") -> {
                        Log.d("NavigationDebug", "✅ MATCHED: Products tab (index 1)")
                        1
                    }

                    // Cart screen and its sub-screens
                    currentRoute == Screens.CartScreen.route ||
                            currentRoute == Screens.CartScreen.PaymentScreen.route ||
                            currentRoute.startsWith("cart_screen") ||
                            currentRoute.startsWith("payment_screen") -> {
                        Log.d("NavigationDebug", "✅ MATCHED: Cart tab (index 2)")
                        2
                    }

                    // Profile screen and its sub-screens
                    currentRoute == Screens.ProfileScreen.route ||
                            currentRoute == Screens.ProfileScreen.OrdersHistoryScreen.route ||
                            currentRoute == Screens.ProfileScreen.TrackOrderScreen.route ||
                            currentRoute == Screens.ProfileScreen.SettingsScreen.route ||
                            currentRoute == Screens.ProfileScreen.RequestHelpScreen.route ||
                            currentRoute == Screens.ProfileScreen.FavouritesScreen.route ||
                            currentRoute == Screens.ProfileScreen.RecentlyScreen.route ||
                            currentRoute == Screens.ProfileScreen.AddNewContentScreen.route ||
                            currentRoute == Screens.ProfileScreen.NotificationScreen.route ||
                            currentRoute == Screens.ProfileScreen.EditProfileScreen.route ||
                            currentRoute.startsWith("profile_screen") ||
                            currentRoute.startsWith("edit_profile_screen") ||
                            currentRoute.startsWith("settings_screen") ||
                            currentRoute.startsWith("orders_history_screen") ||
                            currentRoute.startsWith("orders_track_screen") ||
                            currentRoute.startsWith("request_help_screen") ||
                            currentRoute.startsWith("favourites_screen") ||
                            currentRoute.startsWith("recently_screen") ||
                            currentRoute.startsWith("add_new_content_screen") ||
                            currentRoute.startsWith("notification_screen") -> {
                        Log.d("NavigationDebug", "✅ MATCHED: Profile tab (index 3)")
                        3
                    }

                    // Other user profile or follow list screens should highlight profile tab
                    currentRoute.startsWith("other_user_profile") ||
                            currentRoute.startsWith("follow_list_screen") -> {
                        Log.d(
                            "NavigationDebug",
                            "✅ MATCHED: Other user profile -> Profile tab (index 3)"
                        )
                        3
                    }

                    // Default to reels tab (index 0)
                    else -> {
                        Log.d("NavigationDebug", "⚠️ NO MATCH: Defaulting to Reels tab (index 0)")
                        0
                    }
                }
                // Debug logging for selectedTab
                Log.d(
                    "NavigationDebug",
                    "Selected tab: $selectedTab (0=Reels, 1=Products, 2=Cart, 3=Profile)"
                )
                MyApplicationTheme {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Log.d(
                            "BarFabDebug",
                            "route=$currentRoute showBottomBar=$showBottomBar isBottomSheetVisible=$isBottomSheetVisible"
                        )
                        Scaffold(
                            bottomBar = {
                                if (showBottomBar) {
                                    AppBottomBar(
                                        titles = tabScreens.map { it.title ?: "" },
                                        icons = tabIcons,
                                        selectedTab = selectedTab,
                                        onTabSelected = { index ->
                                            navController.navigate(tabScreens[index].route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            },
                            floatingActionButton = {
                                if (showFAB) {
                                    FloatingActionButton(
                                        onClick = {
                                            mainUiStateViewModel.showAddToCartSheet(
                                                Reels(
                                                    id = "buy_fab_reel",
                                                    productName = "Buy FAB Product",
                                                    productPrice = "0",
                                                    userName = "FAB",
                                                    userImage = R.drawable.profile,
                                                    video = null
                                                )
                                            )
                                        },
                                        shape = CircleShape,
                                        containerColor = Color(0xFFFF6F00),
                                        contentColor = Color.White,
                                        elevation = FloatingActionButtonDefaults.elevation(
                                            defaultElevation = 0.dp,
                                            pressedElevation = 0.dp
                                        ),
                                        modifier = Modifier
                                            .zIndex(1f)
                                            .padding(bottom = 12.dp)
                                            .size(64.dp)
                                            .border(5.dp, Color.White, CircleShape)
                                            .drawBehind {
                                                val shadowRadius = size.width * 0.58f
                                                val shadowOffsetY = size.height * 0.0f
                                                drawCircle(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(
                                                            Color.Black,
                                                            Color.Black.copy(alpha = 0.2f)
                                                        ),
                                                        center = Offset(
                                                            center.x,
                                                            center.y + shadowOffsetY
                                                        ),
                                                        radius = shadowRadius
                                                    ),
                                                    radius = shadowRadius,
                                                    center = Offset(
                                                        center.x,
                                                        center.y + shadowOffsetY
                                                    )
                                                )
                                            }
                                    ) {
                                        Text(
                                            "Buy",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            },
                            floatingActionButtonPosition = FabPosition.Center,
                            isFloatingActionButtonDocked = true
                        ) { paddingValues ->
                            MyNavHost(
                                modifier = Modifier
                                    .padding(paddingValues)
                                    .fillMaxSize(),
                                navController = navController,
                                startDestination = Screens.ReelsScreen.route,
                                mainUiStateViewModel = mainUiStateViewModel
                            )
                        }
                    }
                }
            }
            Log.d("CrashDebug", "MainActivity: setContent complete")
        } catch (e: Exception) {
            Log.e("CrashDebug", "MainActivity: Exception in onCreate: ${e.message}", e)
        }
        Log.d("CrashDebug", "MainActivity: onCreate exit")
    }
}
