package com.project.e_commerce.android.presentation.ui.screens.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.project.e_commerce.android.presentation.viewModel.SocialViewModel
import com.project.e_commerce.domain.model.UserFollowModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun FollowListScreen(
    userId: String,
    currentUserId: String,
    initialTab: Int = 0, // 0 for followers, 1 for following
    navController: NavController,
    viewModel: SocialViewModel = koinViewModel()
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    val uiState by viewModel.uiState.collectAsState()
    val titles = listOf("Followers", "Following")

    LaunchedEffect(selectedTab, userId) {
        if (selectedTab == 0) {
            viewModel.loadFollowers(userId)
        } else {
            viewModel.loadFollowing(userId)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(text = title) }
                )
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.error ?: "", color = MaterialTheme.colorScheme.error)
            }
        } else {
            val list = if (selectedTab == 0) uiState.followers else uiState.following
            
            if (list.isEmpty()) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No users found")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(list, key = { it.id }) { user ->
                        FollowUserItem(
                             user = user,
                             onItemClick = { navController.navigate("user_profile/${user.id}") },
                             onFollowClick = { 
                                 if (user.isIFollow) {
                                     viewModel.unfollowUser(currentUserId, user.id)
                                 } else {
                                     viewModel.followUser(currentUserId, user.id)
                                 }
                                 // Optimistic update or wait for list reload?
                                 // Ideally ViewModel updates the list item status. 
                                 // For now simple trigger.
                             }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FollowUserItem(
    user: UserFollowModel,
    onItemClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
         Surface(
            modifier = Modifier.size(40.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = user.name.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.name, style = MaterialTheme.typography.bodyLarge)
            Text(text = "@${user.username}", style = MaterialTheme.typography.bodySmall)
        }
        
        Button(onClick = onFollowClick) { 
            Text(if (user.isIFollow) "Unfollow" else "Follow") 
        }
    }
}
