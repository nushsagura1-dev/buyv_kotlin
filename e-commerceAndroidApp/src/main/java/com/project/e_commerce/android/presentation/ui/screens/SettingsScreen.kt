package com.project.e_commerce.android.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.project.e_commerce.android.R
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.data.local.TokenManager
import com.project.e_commerce.data.local.CurrentUserProvider
import org.koin.compose.koinInject

// SettingsScreen.kt
@Composable
fun SettingsScreen(navController: NavHostController) {

    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Inject backend auth dependencies
    val tokenManager: TokenManager = koinInject()
    val currentUserProvider: CurrentUserProvider = koinInject()
    
    // Get current user role from storage asynchronously
    var userRole by remember { mutableStateOf("user") }
    
    LaunchedEffect(Unit) {
        val currentUser = currentUserProvider.getCurrentUser()
        userRole = currentUser?.role ?: "user"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            androidx.compose.material3.IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-20).dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = Color(0xFF0066CC),
                    modifier = Modifier.padding(10.dp)
                )
            }

            androidx.compose.material3.Text(
                text = "Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF0066CC),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
        SettingsItem("Orders Track", R.drawable.track) {
            navController.navigate(Screens.ProfileScreen.TrackOrderScreen.route)
        }
        Spacer(modifier = Modifier.height(16.dp))

        SettingsItem("Orders history", R.drawable.ic_orders) {
            navController.navigate(Screens.ProfileScreen.OrdersHistoryScreen.route)
        }
        Spacer(modifier = Modifier.height(16.dp))
        SettingsItem("Recently viewed", R.drawable.ic_eye) {
            navController.navigate(Screens.ProfileScreen.RecentlyScreen.route)
        }
        Spacer(modifier = Modifier.height(16.dp))
        SettingsItem("Payment Methods", R.drawable.ic_card){}
        Spacer(modifier = Modifier.height(16.dp))
        SettingsItem("Location", R.drawable.ic_location){}
        Spacer(modifier = Modifier.height(16.dp))
        SettingsItem("Language", R.drawable.ic_language){}
        Spacer(modifier = Modifier.height(16.dp))
        SettingsItem("Blocked Users", R.drawable.ic_profile) {
            navController.navigate(Screens.ProfileScreen.BlockedUsersScreen.route)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // Promoter Dashboard - Available for all users
        SettingsItem("Promoter Dashboard", R.drawable.ic_products) {
            navController.navigate(Screens.PromoterDashboard.route)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // Admin options - Only visible if user role is "admin"
        if (userRole == "admin") {
            SettingsItem("Admin Withdrawal", R.drawable.ic_settings) {
                navController.navigate(Screens.AdminWithdrawal.route)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsItem("Admin Panel", R.drawable.ic_settings) {
                navController.navigate(Screens.AdminLogin.route)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        SettingsItem("Ask Help", R.drawable.ic_help){}
        Spacer(modifier = Modifier.height(24.dp))
        
        SettingsItem("Logout", R.drawable.ic_logout) {
            showLogoutDialog = true
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = "Logout") },
            text = { Text(text = "Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Backend logout: clear tokens and cache
                        tokenManager.clearTokens()
                        currentUserProvider.clearCache()
                        navController.navigate(Screens.LoginScreen.route) {
                            popUpTo(0) { inclusive = true }
                        }
                        showLogoutDialog = false
                    }
                ) {
                    Text("Logout", color = Color(0xFFFF6600))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsItem(title: String, icon: Int,onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFF6600), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 12.dp)
            ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon), 
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(title,color = Color(0xFF0066CC), modifier = Modifier.weight(1f), fontSize = 15.sp)
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewSettingsScreen() {
    val navController = rememberNavController()
    SettingsScreen(navController = navController)
}
