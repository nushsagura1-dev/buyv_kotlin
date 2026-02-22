package com.project.e_commerce.android.presentation.ui.screens.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.project.e_commerce.android.presentation.viewModel.SocialViewModel
import com.project.e_commerce.domain.model.UserPost
import com.project.e_commerce.domain.model.UserProfile
import org.koin.androidx.compose.koinViewModel

@Composable
fun UserProfileScreen(
    userId: String,
    currentUserId: String, // Need to provide this from composition root or session
    navController: NavController,
    viewModel: SocialViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Initial data load
    LaunchedEffect(userId) {
        // triggers to load user profile if not selected, and posts, and follow status
        if (uiState.selectedUser?.uid != userId) {
             // In a real app we might need a fetchUserProfileUseCase if we navigated via deep link
             // For now assuming we navigated from search and selectedUser is set, 
             // but strictly we should fetch it if null.
             viewModel.loadUserProfile(userId)
        }
        viewModel.loadUserPosts(userId)
        if (userId != currentUserId) {
            viewModel.checkFollowingStatus(currentUserId, userId)
        }
    }

    val user = uiState.selectedUser

    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
           Text("User not found")
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            ProfileHeader(
                user = user,
                isCurrentUser = user.uid == currentUserId,
                isFollowing = uiState.isFollowing,
                onFollowClick = { 
                    if (uiState.isFollowing) {
                        viewModel.unfollowUser(currentUserId, user.uid)
                    } else {
                        viewModel.followUser(currentUserId, user.uid)
                    }
                },
                onEditProfileClick = { navController.navigate("edit_profile") },
                onFollowersClick = { navController.navigate("follow_list/${user.uid}/followers") },
                onFollowingClick = { navController.navigate("follow_list/${user.uid}/following") }
            )
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            if (uiState.isLoading && uiState.userPosts.isEmpty()) {
                 Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.userPosts) { post ->
                        PostThumbnail(post = post, onClick = {
                            // Navigate to post detail
                            navController.navigate("post_detail/${post.id}")
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    user: UserProfile,
    isCurrentUser: Boolean,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Surface(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            // AsyncImage(model = user.profileImageUrl, ...)
             Box(contentAlignment = Alignment.Center) {
                Text(
                    text = user.displayName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(text = user.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = "@${user.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (user.bio.isNotEmpty()) {
            Text(text = user.bio, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Followers", count = user.followersCount, onClick = onFollowersClick)
            StatItem(label = "Following", count = user.followingCount, onClick = onFollowingClick)
            StatItem(label = "Likes", count = user.likesCount, onClick = {})
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Actions
        if (isCurrentUser) {
            Button(onClick = onEditProfileClick, modifier = Modifier.fillMaxWidth()) {
                Text("Edit Profile")
            }
        } else {
            Button(
                onClick = onFollowClick,
                modifier = Modifier.fillMaxWidth(),
                colors = if (isFollowing) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) else ButtonDefaults.buttonColors()
            ) {
                Text(if (isFollowing) "Unfollow" else "Follow")
            }
        }
    }
}

@Composable
fun StatItem(label: String, count: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() } // Make stats clickable to view lists
            .padding(8.dp)
    ) {
        Text(text = count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun PostThumbnail(post: UserPost, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        // AsyncImage(model = post.thumbnailUrl ?: post.mediaUrl, ...)
        Box(contentAlignment = Alignment.Center) {
             Text(text = "Post", style = MaterialTheme.typography.bodySmall)
        }
    }
}
