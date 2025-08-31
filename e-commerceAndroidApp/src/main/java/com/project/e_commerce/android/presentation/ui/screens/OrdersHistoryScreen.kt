package com.project.e_commerce.android.presentation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedButton
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil3.compose.rememberAsyncImagePainter
import com.project.e_commerce.android.R
import com.project.e_commerce.android.domain.model.Order
import com.project.e_commerce.android.domain.model.OrderStatus
import com.project.e_commerce.android.presentation.viewModel.OrderHistoryViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OrdersHistoryScreen(
    navController: NavHostController,
    orderHistoryViewModel: OrderHistoryViewModel = koinViewModel()
) {
    val state by orderHistoryViewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 16.dp)
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

            androidx.compose.material.Text(
                text = "Orders History",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF0066CC),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Row
        OrdersTabRow(
            tabs = orderHistoryViewModel.getTabsWithCounts(),
            selectedTabIndex = state.selectedTabIndex,
            onTabSelected = { orderHistoryViewModel.onTabSelected(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Error Message
        state.error?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = error,
                    color = Color(0xFFD32F2F),
                    fontSize = 14.sp
                )
            }
        }

        // Content
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF0066CC))
            }
        } else if (state.filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cart),
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No orders found",
                        color = Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.filteredOrders) { order ->
                    OrderItemCard(
                        order = order,
                        onCancelOrder = { orderHistoryViewModel.cancelOrder(order.id) },
                        onUploadVideo = { /* Navigate to video upload */ }
                    )
                }
            }
        }
    }

    // Handle error dismissal
    LaunchedEffect(state.error) {
        if (state.error != null) {
            // Auto-dismiss error after 5 seconds
            kotlinx.coroutines.delay(5000)
            orderHistoryViewModel.clearError()
        }
    }
}

@Composable
fun OrderItemCard(
    order: Order,
    onCancelOrder: () -> Unit = {},
    onUploadVideo: () -> Unit = {}
) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val statusColor = when (order.status) {
        OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING -> Color(0xFFFF9800)
        OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY -> Color(0xFF2196F3)
        OrderStatus.DELIVERED -> Color(0xFF4CAF50)
        OrderStatus.CANCELED, OrderStatus.RETURNED, OrderStatus.REFUNDED -> Color(0xFFFF5722)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE9E9E9), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            // Order Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.orderNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0066CC)
                )
                Text(
                    text = order.status.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Order Date and Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = order.createdAt?.let { dateFormatter.format(it.toDate()) } ?: "",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = "$${String.format("%.2f", order.total)}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6F00),
                    fontSize = 16.sp
                )
            }

            if (order.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                // Show first item (or all items if needed)
                val firstItem = order.items.first()
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Product Image
                    if (firstItem.productImage.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(firstItem.productImage),
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Fallback image
                        Image(
                            painter = painterResource(id = R.drawable.perfume3),
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = firstItem.productName,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color(0xFF181D23)
                        )

                        if (order.items.size > 1) {
                            Text(
                                text = "+${order.items.size - 1} more items",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = "Qty: ${firstItem.quantity}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Action buttons for delivered orders
            if (order.status == OrderStatus.DELIVERED) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Share your experience",
                    color = Color(0xFF181D23),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { /* Navigate to review */ },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF6F00)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = Color(0xFFFFF3E7),
                            contentColor = Color(0xFFFF6F00)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star),
                            contentDescription = "Review Product",
                            tint = Color(0xFFFF6F00),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Review",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onUploadVideo,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF176DBA)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = Color(0xFFEFF6FA),
                            contentColor = Color(0xFF176DBA)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_video_camera),
                            contentDescription = "Upload Video",
                            tint = Color(0xFF176DBA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Video",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Cancel button for pending orders
            if (order.status in listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED)) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancelOrder,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF5722)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = Color.White,
                        contentColor = Color(0xFFFF5722)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Text(
                        "Cancel Order",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OrdersTabRow(
    tabs: List<Pair<String, Int>>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        backgroundColor = Color.White,
        edgePadding = 4.dp,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                height = 2.dp,
                color = Color(0xFF0066CC)
            )
        }
    ) {
        tabs.forEachIndexed { index, (title, count) ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = "$title ($count)",
                        color = if (selectedTabIndex == index) Color.Black else Color.Gray
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewOrdersHistoryScreen() {
    val navController = rememberNavController()
    OrdersHistoryScreen(navController = navController)
}
