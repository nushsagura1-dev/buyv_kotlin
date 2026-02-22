package com.project.e_commerce.android.presentation.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.viewModel.admin.AdminAuthViewModel
import com.project.e_commerce.android.presentation.viewModel.admin.AdminDashboardViewModel
import com.project.e_commerce.android.presentation.ui.composable.admin.QuickActionCard
import com.project.e_commerce.android.presentation.ui.composable.admin.StatCard
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AdminDashboardScreen(
    navController: NavHostController,
    viewModel: AdminDashboardViewModel = koinViewModel(),
    authViewModel: AdminAuthViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()

    // Auto-redirect if logged out
    LaunchedEffect(authState.isAdminLoggedIn) {
        if (!authState.isAdminLoggedIn) {
            navController.navigate(Screens.AdminLogin.route) {
                popUpTo(Screens.AdminDashboard.route) { inclusive = true }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = { viewModel.loadDashboardData() }
    )

    // Admin Sections Definition
    var sections by remember { mutableStateOf(
        listOf(
            AdminSection(
                "User Management", 
                Icons.Default.Group,
                listOf(
                    AdminMenuItem("Users", Icons.Default.Person, Screens.AdminUserManagement.route),
                    AdminMenuItem("Follows", Icons.Default.Person, Screens.AdminFollows.route)
                )
            ),
            AdminSection(
                "Content", 
                Icons.Default.List,
                listOf(
                    AdminMenuItem("Posts", Icons.Default.List, Screens.AdminPosts.route),
                    AdminMenuItem("Comments", Icons.Default.Email, Screens.AdminComments.route),
                    AdminMenuItem("Likes", Icons.Default.Favorite, Screens.AdminPosts.route)
                )
            ),
            AdminSection(
                "Commerce", 
                Icons.Default.ShoppingCart,
                listOf(
                    AdminMenuItem("Orders", Icons.Default.ShoppingCart, Screens.AdminOrderManagement.route),
                    AdminMenuItem("Commissions", Icons.Default.AccountBox, Screens.AdminCommissionManagement.route)
                )
            ),
            AdminSection(
                "Marketplace", 
                Icons.Default.Home,
                listOf(
                    AdminMenuItem("Products", Icons.Default.ShoppingCart, Screens.AdminProductManagement.route),
                    AdminMenuItem("Categories", Icons.Default.List, Screens.AdminCategories.route),
                    AdminMenuItem("Import from CJ", Icons.Default.Add, Screens.AdminCJImport.route),
                    AdminMenuItem("Affiliate Sales", Icons.Default.Share, Screens.AdminAffiliateSales.route),
                    AdminMenuItem("Promoter Wallets", Icons.Default.AccountBox, Screens.AdminPromoterWallets.route),
                    AdminMenuItem("Withdrawals", Icons.Default.ArrowForward, Screens.AdminWithdrawal.route)
                )
            ),
            AdminSection(
                "System", 
                Icons.Default.Settings,
                listOf(
                    AdminMenuItem("Notifications", Icons.Default.Warning, Screens.AdminNotifications.route)
                )
            )
        )
    ) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Admin Dashboard",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        // Use role from authViewModel if viewModel.adminRole is null
                        val displayRole = uiState.adminRole ?: authState.adminRole
                        if (displayRole != null) {
                            Text(
                                text = displayRole,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f) // Better visibility
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboardData() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    IconButton(onClick = {
                        authViewModel.adminLogout()
                        navController.navigate(Screens.AdminLogin.route) {
                            popUpTo(Screens.AdminDashboard.route) { inclusive = true }
                        }
                    }) {
                        // Use logout icon if available, otherwise use ExitToApp equivalent
                        Icon(
                             painter = androidx.compose.ui.res.painterResource(id = com.project.e_commerce.android.R.drawable.ic_logout),
                             contentDescription = "Logout",
                             tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF9C27B0),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            if (uiState.isLoading && uiState.stats == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF9C27B0))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Admin Sections Accordion
                item {
                    Text(
                        text = "Administration",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(sections) { section ->
                    AdminSectionItem(
                        section = section,
                        onToggle = { 
                            sections = sections.map { 
                                if (it.title == section.title) it.copy(expanded = !it.expanded) else it 
                            }
                        },
                        onItemClick = { route -> navController.navigate(route) }
                    )
                }
                
                item {
                     Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
                
                // Statistics Cards
                uiState.stats?.let { stats ->
                    // Users Section
                    item {
                        Text(
                            text = "Users",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Total",
                                value = stats.total_users.toString(),
                                icon = Icons.Default.Person,
                                backgroundColor = Color(0xFF2196F3),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Verified",
                                value = stats.verified_users.toString(),
                                icon = Icons.Default.CheckCircle,
                                backgroundColor = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // Content Section
                    item {
                        Text(
                            text = "Content",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Posts",
                                value = stats.total_posts.toString(),
                                icon = Icons.Default.Article,
                                backgroundColor = Color(0xFF9C27B0),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Reels",
                                value = stats.total_reels.toString(),
                                icon = Icons.Default.VideoLibrary,
                                backgroundColor = Color(0xFFE91E63),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // Commerce Section (only for finance/super_admin)
                    if (uiState.adminRole in listOf("super_admin", "finance")) {
                        item {
                            Text(
                                text = "Commerce",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StatCard(
                                        title = "Orders",
                                        value = stats.total_orders.toString(),
                                        icon = Icons.Default.ShoppingCart,
                                        backgroundColor = Color(0xFFFF9800),
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        title = "Revenue",
                                        value = "$${String.format("%.0f", stats.total_revenue)}",
                                        icon = Icons.Default.AttachMoney,
                                        backgroundColor = Color(0xFF4CAF50),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFFF9800),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Pending Withdrawals",
                                                fontSize = 14.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "${stats.pending_withdrawals} requests ($${String.format("%.2f", stats.pending_withdrawals_amount)})",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFF9800)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Recent Activity
                    item {
                        Text(
                            text = "Recent Users",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (uiState.recentUsers.isEmpty()) {
                            Text(
                                text = "No recent users",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.recentUsers.take(5).forEach { user ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF2196F3).copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = user.username.first().uppercase(),
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2196F3)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = user.username,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    if (user.is_verified) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            Icons.Default.CheckCircle,
                                                            contentDescription = "Verified",
                                                            tint = Color(0xFF4CAF50),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = user.email,
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Error message
                if (uiState.error != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = uiState.error ?: "",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            // Pull refresh indicator
            PullRefreshIndicator(
                refreshing = uiState.isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}


@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}
}
