package com.project.e_commerce.android.presentation.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.project.e_commerce.android.data.api.TopFollowedUser
import com.project.e_commerce.android.presentation.viewModel.admin.AdminFollowsViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Écran Admin - Statistiques Follows
 * Affiche les stats globales et le top des utilisateurs suivis
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AdminFollowsScreen(
    navController: NavHostController,
    viewModel: AdminFollowsViewModel = koinViewModel()
) {
    val followStats by viewModel.followStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.loadFollowStats() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistiques Follows", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF6F00),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            if (isLoading && followStats == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Error
                    errorMessage?.let { error ->
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                            ) {
                                Text(error, color = Color(0xFFC62828), modifier = Modifier.padding(16.dp))
                            }
                        }
                    }

                    // Stats cards
                    item {
                        Text(
                            "Overview",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D3D67)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FollowStatCard(
                                modifier = Modifier.weight(1f),
                                title = "Total",
                                value = "${followStats?.total_follows ?: 0}",
                                icon = Icons.Default.People,
                                color = Color(0xFF1565C0)
                            )
                            FollowStatCard(
                                modifier = Modifier.weight(1f),
                                title = "Today",
                                value = "+${followStats?.new_follows_today ?: 0}",
                                icon = Icons.Default.TrendingUp,
                                color = Color(0xFF2E7D32)
                            )
                            FollowStatCard(
                                modifier = Modifier.weight(1f),
                                title = "This week",
                                value = "+${followStats?.new_follows_this_week ?: 0}",
                                icon = Icons.Default.DateRange,
                                color = Color(0xFFE65100)
                            )
                        }
                    }

                    // Top followed users
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Top 10 - Most followed users",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D3D67)
                        )
                    }

                    val topUsers = followStats?.top_followed_users ?: emptyList()
                    if (topUsers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No data available", color = Color.Gray)
                            }
                        }
                    } else {
                        items(topUsers.mapIndexed { index, user -> index to user }) { (index, user) ->
                            TopFollowedUserCard(rank = index + 1, user = user)
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun FollowStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = color
            )
            Text(
                title,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun TopFollowedUserCard(rank: Int, user: TopFollowedUser) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            Surface(
                shape = RoundedCornerShape(50),
                color = when (rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> Color(0xFFE0E0E0)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "#$rank",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (rank <= 3) Color.White else Color.DarkGray
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "@${user.username}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    user.display_name,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.People,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF1565C0)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${user.followers_count}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1565C0)
                )
            }
        }
    }
}
