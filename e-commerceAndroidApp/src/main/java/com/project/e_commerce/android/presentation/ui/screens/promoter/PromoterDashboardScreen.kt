package com.project.e_commerce.android.presentation.ui.screens.promoter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.project.e_commerce.android.presentation.ui.screens.promoter.components.*
import com.project.e_commerce.android.presentation.viewModel.promoter.PromoterDashboardViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Phase 7: Promoter Dashboard Screen
 * Affiche les analytics, earnings et performances du promoteur
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromoterDashboardScreen(
    navController: NavController,
    viewModel: PromoterDashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Promoter Dashboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAnalytics() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF9800),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFFFF9800)
                    )
                }
                
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Error loading data",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "Unknown error",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.refreshAnalytics() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
                
                !uiState.hasData -> {
                    DashboardEmptyState()
                }
                
                else -> {
                    val analytics = uiState.analytics!!
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                    ) {
                        // Period Selector
                        PeriodSelector(
                            selectedPeriod = uiState.selectedPeriod,
                            onPeriodSelected = { viewModel.changePeriod(it) }
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Earnings Section
                        SectionHeader(
                            title = "Earnings Overview",
                            icon = Icons.Default.AttachMoney
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Main Earnings Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            EarningsCard(
                                title = "Total Earned",
                                amount = analytics.earnings.total_earned,
                                subtitle = "All time",
                                icon = Icons.Default.TrendingUp,
                                backgroundColor = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f)
                            )
                            
                            EarningsCard(
                                title = "Available",
                                amount = analytics.earnings.available_balance,
                                subtitle = "Ready to withdraw",
                                icon = Icons.Default.AccountBalanceWallet,
                                backgroundColor = Color(0xFF2196F3),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Secondary Earnings Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Pending",
                                value = "$${String.format("%.2f", analytics.earnings.pending_balance)}",
                                icon = Icons.Default.Schedule,
                                backgroundColor = Color(0xFFFFA726),
                                modifier = Modifier.weight(1f)
                            )
                            
                            StatCard(
                                title = "Withdrawn",
                                value = "$${String.format("%.2f", analytics.earnings.withdrawn_total)}",
                                icon = Icons.Default.CheckCircle,
                                backgroundColor = Color(0xFF66BB6A),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Performance Metrics
                        SectionHeader(
                            title = "Performance Metrics",
                            icon = Icons.Default.BarChart
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Views",
                                value = analytics.metrics.views.toString(),
                                icon = Icons.Default.Visibility,
                                backgroundColor = Color(0xFF9C27B0),
                                change = "Last ${uiState.selectedPeriod} days",
                                changePositive = true,
                                modifier = Modifier.weight(1f)
                            )
                            
                            StatCard(
                                title = "Clicks",
                                value = analytics.metrics.clicks.toString(),
                                icon = Icons.Default.TouchApp,
                                backgroundColor = Color(0xFFFF5722),
                                change = "Last ${uiState.selectedPeriod} days",
                                changePositive = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Conversions",
                                value = analytics.metrics.conversions.toString(),
                                icon = Icons.Default.ShoppingCart,
                                backgroundColor = Color(0xFF00BCD4),
                                change = "${analytics.stats.total_sales} total sales",
                                changePositive = true,
                                modifier = Modifier.weight(1f)
                            )
                            
                            StatCard(
                                title = "Avg. Commission",
                                value = "$${String.format("%.2f", analytics.stats.avg_commission_per_sale)}",
                                icon = Icons.Default.MonetizationOn,
                                backgroundColor = Color(0xFFFFEB3B),
                                iconTint = Color(0xFFFF9800),
                                change = "Per sale",
                                changePositive = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Conversion Metrics
                        SectionHeader(
                            title = "Conversion Analytics",
                            icon = Icons.Default.Assessment
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        MetricCard(
                            label = "Click-Through Rate (CTR)",
                            percentage = analytics.metrics.ctr,
                            description = "${analytics.metrics.clicks} clicks from ${analytics.metrics.views} views",
                            icon = Icons.Default.TouchApp,
                            color = Color(0xFF2196F3)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        MetricCard(
                            label = "Conversion Rate",
                            percentage = analytics.metrics.conversion_rate,
                            description = "${analytics.metrics.conversions} sales from ${analytics.metrics.clicks} clicks",
                            icon = Icons.Default.ShoppingCart,
                            color = Color(0xFF4CAF50)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Actions Section
                        SectionHeader(
                            title = "Actions",
                            icon = Icons.Default.TouchApp
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // View My Commissions Button
                        OutlinedButton(
                            onClick = {
                                navController.navigate(com.project.e_commerce.android.presentation.ui.navigation.Screens.MyCommissions.route)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFF9800)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "View my commissions",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // View My Promotions Button
                        OutlinedButton(
                            onClick = {
                                navController.navigate(com.project.e_commerce.android.presentation.ui.navigation.Screens.MyPromotions.route)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "View my promotions",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // View My Affiliate Sales Button
                        OutlinedButton(
                            onClick = {
                                navController.navigate(com.project.e_commerce.android.presentation.ui.navigation.Screens.AffiliateSales.route)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF9C27B0)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "My affiliate sales",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // View My Wallet Button
                        OutlinedButton(
                            onClick = {
                                navController.navigate(com.project.e_commerce.android.presentation.ui.navigation.Screens.Wallet.route)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF2196F3)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "My Wallet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Withdrawal Button
                        Button(
                            onClick = {
                                navController.navigate("withdrawal_request")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            ),
                            enabled = analytics.earnings.available_balance > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (analytics.earnings.available_balance > 0)
                                    "Request Withdrawal ($${String.format("%.2f", analytics.earnings.available_balance)})"
                                else
                                    "No Balance Available",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
