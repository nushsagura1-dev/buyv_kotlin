package com.project.e_commerce.android.presentation.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.project.e_commerce.android.R
import com.project.e_commerce.android.data.model.Order
import com.project.e_commerce.android.presentation.ui.composable.common.ErrorView
import com.project.e_commerce.android.presentation.ui.composable.common.LoadingView
import com.project.e_commerce.android.presentation.viewModel.admin.AdminOrderViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AdminOrderScreen(
    navController: NavController,
    viewModel: AdminOrderViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    
    // Load orders on first composition
    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }
    
    // Pull to refresh
    val pullRefreshState = rememberPullToRefreshState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            AdminOrderHeader(
                onBackClick = { navController.popBackStack() }
            )
            
            // Loading state
            if (uiState.isLoading) {
                LoadingView(message = "Loading orders...")
                return@Box
            }
            
            // Error state
            if (uiState.error != null) {
                ErrorView(
                    message = uiState.error ?: "Unknown error",
                    onRetry = { viewModel.loadOrders(selectedFilter) }
                )
                return@Box
            }
            
            // Order stats chips
            OrderStatsChips(
                stats = viewModel.getOrderStats(),
                selectedFilter = selectedFilter,
                onFilterSelected = { filter ->
                    selectedFilter = filter
                    viewModel.filterByStatus(filter)
                }
            )
            
            // Orders list
            if (uiState.orders.isEmpty()) {
                EmptyOrdersView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.orders, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            onOrderClick = { viewModel.selectOrder(order) },
                            onStatusChange = { newStatus ->
                                viewModel.updateOrderStatus(order.id.toIntOrNull() ?: 0, newStatus)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminOrderHeader(onBackClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        elevation = 4.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = Color(0xFF0066CC)
                )
            }
            
            Spacer(Modifier.width(8.dp))
            
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = Color(0xFF0066CC),
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(Modifier.width(12.dp))
            
            Text(
                "Order Management",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0066CC)
            )
        }
    }
}

@Composable
private fun OrderStatsChips(
    stats: Map<String, Int>,
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        // All orders
        item {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterSelected(null) },
                label = "All (${stats["all"] ?: 0})",
                color = Color(0xFF6200EE)
            )
        }
        
        // Pending
        item {
            FilterChip(
                selected = selectedFilter == "pending",
                onClick = { onFilterSelected("pending") },
                label = "Pending (${stats["pending"] ?: 0})",
                color = Color(0xFFFFA000)
            )
        }
        
        // Processing
        item {
            FilterChip(
                selected = selectedFilter == "processing",
                onClick = { onFilterSelected("processing") },
                label = "Processing (${stats["processing"] ?: 0})",
                color = Color(0xFF2196F3)
            )
        }
        
        // Shipped
        item {
            FilterChip(
                selected = selectedFilter == "shipped",
                onClick = { onFilterSelected("shipped") },
                label = "Shipped (${stats["shipped"] ?: 0})",
                color = Color(0xFF9C27B0)
            )
        }
        
        // Delivered
        item {
            FilterChip(
                selected = selectedFilter == "delivered",
                onClick = { onFilterSelected("delivered") },
                label = "Delivered (${stats["delivered"] ?: 0})",
                color = Color(0xFF4CAF50)
            )
        }
        
        // Cancelled
        item {
            FilterChip(
                selected = selectedFilter == "cancelled",
                onClick = { onFilterSelected("cancelled") },
                label = "Cancelled (${stats["cancelled"] ?: 0})",
                color = Color(0xFFF44336)
            )
        }
    }
}

@Composable
private fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    color: Color
) {
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) color else Color.White,
        elevation = if (selected) 4.dp else 2.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else Color.Gray
            )
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    onOrderClick: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOrderClick),
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp,
        backgroundColor = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Order number and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.orderNumber}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0066CC)
                )
                
                Text(
                    text = formatDate(order.createdAt),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Customer email
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = order.userEmail ?: "N/A",
                    fontSize = 14.sp,
                    color = Color(0xFF333333)
                )
            }
            
            Spacer(Modifier.height(4.dp))
            
            // Items count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${order.items.size} item(s)",
                    fontSize = 14.sp,
                    color = Color(0xFF333333)
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Total amount and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format("%.2f", order.totalAmount)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                
                Box {
                    // Status chip with dropdown
                    Surface(
                        modifier = Modifier
                            .clickable { showStatusMenu = true },
                        shape = RoundedCornerShape(16.dp),
                        color = getStatusColor(order.status).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = order.status.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = getStatusColor(order.status)
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = getStatusColor(order.status)
                            )
                        }
                    }
                    
                    // Status dropdown menu
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        listOf("pending", "processing", "shipped", "delivered", "cancelled").forEach { status ->
                            DropdownMenuItem(onClick = {
                                onStatusChange(status)
                                showStatusMenu = false
                            }) {
                                Text(
                                    text = status.uppercase(),
                                    color = getStatusColor(status)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyOrdersView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFFBDBDBD)
            )
            Text(
                text = "No orders found",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }
    }
}

private fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "pending" -> Color(0xFFFFA000)
        "processing" -> Color(0xFF2196F3)
        "shipped" -> Color(0xFF9C27B0)
        "delivered" -> Color(0xFF4CAF50)
        "cancelled" -> Color(0xFFF44336)
        else -> Color.Gray
    }
}

private fun formatDate(dateString: String?): String {
    if (dateString == null) return "N/A"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = parser.parse(dateString)
        date?.let { formatter.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}
