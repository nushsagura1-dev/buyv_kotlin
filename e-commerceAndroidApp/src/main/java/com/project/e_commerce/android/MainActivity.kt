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
                // Determine current selected index based on navController current route
                val currentRoute = navController.currentBackStackEntry?.destination?.route ?: ""
                // Collect state for showing/hiding bottom bar and FAB
                val isBottomSheetVisible by mainUiStateViewModel.isBottomSheetVisible.collectAsState()
                // Only display bottom bar and FAB on these tabs
                val showBottomBar = currentRoute in listOf(
                    Screens.ReelsScreen.route,
                    Screens.ProductScreen.route,
                    Screens.CartScreen.route,
                    Screens.ProfileScreen.route,
                    Screens.ReelsScreen.ExploreScreen.route
                )
                val selectedTab =
                    tabScreens.indexOfFirst { it.route == currentRoute.substringBefore('/') }
                        .let { if (it < 0) 0 else it }

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
                                // Temporarily unconditionally render to debug
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
                            },
                            floatingActionButton = {
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
                                                center = Offset(center.x, center.y + shadowOffsetY)
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
