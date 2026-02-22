package com.project.e_commerce.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.messaging.FirebaseMessaging
// REMOVED: import com.project.e_commerce.android.domain.repository.CartRepository (Firebase repository disabled)
// REMOVED: import com.project.e_commerce.android.domain.repository.NotificationRepository (Firebase repository disabled)
import com.project.e_commerce.android.presentation.ui.navigation.AppBottomBar
import com.project.e_commerce.android.presentation.ui.navigation.MyNavHost
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.BuyBottomSheet
import com.project.e_commerce.android.presentation.viewModel.CartViewModel
import com.project.e_commerce.android.presentation.viewModel.MainUiStateViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.java.KoinJavaComponent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput

class MainActivity : ComponentActivity() {
    private val mainUiStateViewModel: MainUiStateViewModel by viewModels()
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Permission launcher for notifications
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "✅ Notification permission granted")
            // Permission granted, FCM token will be registered automatically
        } else {
            Log.w("MainActivity", "❌ Notification permission denied")
            // Handle permission denial if needed
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CrashDebug", "MainActivity: onCreate started")
        try {
            // Request notification permission for Android 13+
            requestNotificationPermission()

            // Set up FCM token registration on auth state changes
            setupFCMTokenRegistration()

            Log.d("CrashDebug", "MainActivity: ViewModels initializing")

            // ✅ MIGRATION NOTE: Firebase repositories (Cart, Order, Following, Notification) disabled
            // These will be migrated to backend in future phases
            // Existing ViewModel initialization
            Log.d("CrashDebug", "MainActivity: ViewModels initialized")

            Log.d("CrashDebug", "MainActivity: Setting content")
            setContent {
                val navController = rememberNavController()
                val cartViewModel: CartViewModel = koinViewModel()
                
                // Déterminer la destination de départ selon l'état d'authentification
                val tokenManager = remember { 
                    KoinJavaComponent.getKoin().get<com.project.e_commerce.data.local.TokenManager>()
                }
                val isAuthenticated = remember {
                    val token = tokenManager.getAccessToken()
                    token != null && !tokenManager.isTokenExpired()
                }
                
                // Check if this is the first time the app is launched (show onboarding)
                val context = this@MainActivity
                val isFirstTime = remember {
                    val prefs = context.getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
                    prefs.getBoolean("isFirstTime", true)
                }
                
                val initialRoute = when {
                    isFirstTime -> {
                        Log.d("MainActivity", "First time user, starting at Splash/Onboarding")
                        Screens.SplashScreen.route
                    }
                    isAuthenticated -> {
                        Log.d("MainActivity", "User authenticated, starting at Reels")
                        Screens.ReelsScreen.route
                    }
                    else -> {
                        Log.d("MainActivity", "User not authenticated, starting at Login")
                        Screens.LoginScreen.route
                    }
                }
                
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
                val isAddToCartSheetVisible by mainUiStateViewModel.isAddToCartSheetVisible.collectAsState()
                val selectedReelForCart by mainUiStateViewModel.selectedReelForCart.collectAsState()
                val currentReel by mainUiStateViewModel.currentReel.collectAsState()

                // Auto-hide buy sheet when navigating away from reels screen
                LaunchedEffect(currentRoute) {
                    if (currentRoute != Screens.ReelsScreen.route) {
                        mainUiStateViewModel.hideAddToCartSheet()
                    }
                }

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

                // Show FAB only when on the main reels screen and there is a current reel
                val showFAB =
                    currentRoute == Screens.ReelsScreen.route && !hideBottomBar && currentReel != null

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
                                AnimatedVisibility(
                                    visible = showFAB,
                                    enter = scaleIn(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessHigh
                                        ),
                                        initialScale = 0.6f
                                    )
                                ) {
                                    FloatingActionButton(
                                        onClick = {
                                            currentReel?.let { reel ->
                                                Log.d(
                                                    "BuyFABDebug",
                                                    "🎬 Buy FAB clicked for reel: ${reel.id}"
                                                )
                                                mainUiStateViewModel.showAddToCartSheet(reel)
                                            }
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
                                } // <-- Properly closes AnimatedVisibility block
                            },
                            floatingActionButtonPosition = FabPosition.Center,
                            isFloatingActionButtonDocked = true
                        ) { paddingValues ->
                            MyNavHost(
                                modifier = Modifier
                                    .padding(paddingValues)
                                    .fillMaxSize(),
                                navController = navController,
                                startDestination = initialRoute, // Utiliser la destination déterminée dynamiquement
                                mainUiStateViewModel = mainUiStateViewModel
                            )
                            // BuyBottomSheet
                            if (isAddToCartSheetVisible && selectedReelForCart != null) {
                                // Ensure cart is initialized before showing BuyBottomSheet
                                LaunchedEffect(Unit) {
                                    cartViewModel.initializeCart()
                                }
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // Dismiss overlay (sibling, NOT parent of BuyBottomSheet)
                                    // This ensures scroll gestures on the sheet are not consumed
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            ) {
                                                mainUiStateViewModel.hideAddToCartSheet()
                                            }
                                    )
                                    // Sheet content (on top of overlay, handles its own scroll)
                                    BuyBottomSheet(
                                        onClose = { mainUiStateViewModel.hideAddToCartSheet() },
                                        productPrice = selectedReelForCart!!.productPrice.toDoubleOrNull()
                                            ?: 0.0,
                                        productImage = selectedReelForCart!!.productImage,
                                        reel = selectedReelForCart,
                                        cartViewModel = cartViewModel,
                                        navController = navController,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                    )
                                }
                            }
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "✅ Notification permission already granted")
                }

                PackageManager.PERMISSION_DENIED -> {
                    Log.d("MainActivity", "🔔 Requesting notification permission")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d("MainActivity", "📱 Android version < 13, notification permission not required")
        }
    }

    private fun setupFCMTokenRegistration() {
        // TODO: Migrate to backend authentication state monitoring
        // Temporarily disabled during Firebase migration
        // FCM token registration is now handled in EcommerceApp.kt via CurrentUserProvider
        Log.d("MainActivity", "⚠️ Firebase AuthStateListener disabled during migration")
        /*
        // Listen for authentication state changes
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                Log.d("MainActivity", "👤 User authenticated, registering FCM token")
                registerFCMToken(user.uid)
            } else {
                Log.d("MainActivity", "👤 User not authenticated")
            }
        }
        */
    }

    // ⏸️ DISABLED: registerFCMToken() depends on NotificationRepository (Firebase)
    // TODO: Migrate to backend API endpoint POST /users/me/fcm-token
    /*
    private fun registerFCMToken(userId: String) {
        activityScope.launch {
            try {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w("MainActivity", "❌ FCM token fetch failed", task.exception)
                        return@addOnCompleteListener
                    }

                    val token = task.result
                    Log.d("MainActivity", "🔔 FCM token retrieved for user $userId: $token")

                    // Update token in repository
                    activityScope.launch {
                        try {
                            val notificationRepository =
                                KoinJavaComponent.getKoin().get<NotificationRepository>()
                            val result = notificationRepository.updateFCMToken(userId, token)

                            if (result.isSuccess) {
                                Log.d("MainActivity", "✅ FCM token registered successfully")
                            } else {
                                Log.e(
                                    "MainActivity",
                                    "❌ Failed to register FCM token: ${result.exceptionOrNull()}"
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "❌ Error registering FCM token", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Error in FCM token registration", e)
            }
        }
    }
    */

    override fun onDestroy() {
        super.onDestroy()
        // Clean up coroutine scope
        activityScope.coroutineContext.cancel()
    }
}
