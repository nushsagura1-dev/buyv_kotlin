package com.project.e_commerce.android.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.project.e_commerce.android.domain.model.FirebaseNotification
import com.project.e_commerce.android.domain.model.NotificationCategory
import com.project.e_commerce.android.domain.model.NotificationType
import com.project.e_commerce.android.presentation.viewModel.NotificationViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationScreen(
    navController: NavHostController,
    notificationViewModel: NotificationViewModel = koinViewModel()
) {
    val state by notificationViewModel.state.collectAsState()

    // Load notifications when screen opens
    LaunchedEffect(Unit) {
        notificationViewModel.refresh()
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
            Text(
                text = "Notifications",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF0066CC),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }

        // Loading state
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF0066CC))
            }
        }
        // Error state
        else if (state.error != null) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error: ${state.error}",
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            notificationViewModel.clearError()
                            notificationViewModel.refresh()
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF0066CC))
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
            }
        }
        // Empty state
        else if (state.notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(Color(0xFF1B7ACE), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_no_notification),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "NO NOTIFICATIONS",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }
        }
        // Notifications content
        else {
            Spacer(modifier = Modifier.height(16.dp))

            // Category Filter Buttons
            CategoryFilterRow(
                selectedCategory = state.selectedCategory,
                onCategorySelected = { category ->
                    notificationViewModel.filterByCategory(category)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Today's notifications
            val todayNotifications = notificationViewModel.getTodayNotifications()
            if (todayNotifications.isNotEmpty()) {
                Text(
                    "Today",
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                todayNotifications.forEach { notification ->
                    FirebaseNotificationCard(
                        notification = notification,
                        onClick = {
                            if (!notification.isRead) {
                                notificationViewModel.markAsRead(notification.id)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Yesterday's notifications
            val yesterdayNotifications = notificationViewModel.getYesterdayNotifications()
            if (yesterdayNotifications.isNotEmpty()) {
                Text(
                    "Yesterday",
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                yesterdayNotifications.forEach { notification ->
                    FirebaseNotificationCard(
                        notification = notification,
                        onClick = {
                            if (!notification.isRead) {
                                notificationViewModel.markAsRead(notification.id)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Older notifications
            val olderNotifications = notificationViewModel.getOlderNotifications()
            if (olderNotifications.isNotEmpty()) {
                Text(
                    "Earlier",
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                olderNotifications.forEach { notification ->
                    FirebaseNotificationCard(
                        notification = notification,
                        onClick = {
                            if (!notification.isRead) {
                                notificationViewModel.markAsRead(notification.id)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(26.dp))
        }
    }
}

@Composable
fun CategoryFilterRow(
    selectedCategory: NotificationCategory?,
    onCategorySelected: (NotificationCategory?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All filter
        FilterChip(
            text = "All",
            isSelected = selectedCategory == null,
            onClick = { onCategorySelected(null) }
        )

        // Category filters
        NotificationCategory.values().forEach { category ->
            FilterChip(
                text = getCategoryDisplayName(category),
                isSelected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF0066CC) else Color(0xFFF5F5F5)
    val textColor = if (isSelected) Color.White else Color(0xFF666666)

    Text(
        text = text,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = textColor,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
fun FirebaseNotificationCard(
    notification: FirebaseNotification,
    onClick: () -> Unit
) {
    val backgroundColor = if (notification.isRead) Color(0xFFF5F5F5) else Color(0xFFDBECFF)
    val titleColor = if (notification.isRead) Color(0xFF1B7ACE) else Color(0xFF0066CC)
    val fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(
                width = if (notification.isRead) 0.dp else 2.dp,
                color = Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !notification.isRead) { onClick() }
            .padding(end = 10.dp, start = 8.dp, top = 10.dp, bottom = 10.dp)
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Notification icon/image
        if (!notification.isSystemNotification && notification.senderImageUrl != null) {
            // User notification with profile image (can be implemented with Coil)
            Icon(
                painter = painterResource(id = R.drawable.profile),
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .padding(end = 6.dp),
                tint = Color.Unspecified
            )
        } else {
            // System notification with app logo or category icon
            Icon(
                painter = painterResource(id = getNotificationIcon(notification.type)),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(42.dp)
                    .padding(end = 6.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    notification.title,
                    color = titleColor,
                    fontWeight = fontWeight,
                    fontSize = 14.sp
                )
                Text(
                    formatNotificationTime(notification.createdAt.seconds * 1000),
                    color = Color(0xFF0066CC),
                    fontSize = 10.sp
                )
            }
            Text(
                notification.body,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// Helper functions
private fun getCategoryDisplayName(category: NotificationCategory): String {
    return when (category) {
        NotificationCategory.ORDERS_SHIPPING -> "Orders"
        NotificationCategory.SOCIAL_ACTIVITY -> "Social"
        NotificationCategory.PROMOTIONS_DEALS -> "Deals"
        NotificationCategory.ACCOUNT_SECURITY -> "Account"
        NotificationCategory.APP_UPDATES -> "Updates"
        NotificationCategory.GENERAL -> "General"
    }
}

private fun getNotificationIcon(type: NotificationType): Int {
    return when (type) {
        // E-commerce & Orders
        NotificationType.ORDER_CONFIRMATION,
        NotificationType.ORDER_SHIPPED,
        NotificationType.ORDER_DELIVERED,
        NotificationType.ORDER_CANCELLED -> R.drawable.ic_cart

        NotificationType.PAYMENT_SUCCESS,
        NotificationType.PAYMENT_FAILED,
        NotificationType.REFUND_PROCESSED -> R.drawable.ic_cart

        // Social Interactions
        NotificationType.NEW_FOLLOWER -> R.drawable.ic_profile
        NotificationType.NEW_LIKE -> R.drawable.ic_love
        NotificationType.NEW_COMMENT,
        NotificationType.COMMENT_REPLY,
        NotificationType.MENTION -> R.drawable.ic_comment

        // Content & Reels
        NotificationType.REEL_APPROVED,
        NotificationType.CONTENT_MILESTONE,
        NotificationType.TRENDING_REEL,
        NotificationType.UPLOAD_COMPLETE -> R.drawable.ic_video_camera

        // Default
        else -> R.drawable.logo_300px
    }
}

private fun formatNotificationTime(timestampMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestampMillis

    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
        else -> {
            val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            formatter.format(Date(timestampMillis))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewNotificationScreen() {
    val navController = rememberNavController()
    NotificationScreen(navController = navController)
}
