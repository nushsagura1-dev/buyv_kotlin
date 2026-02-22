package com.project.e_commerce.android.presentation.ui.screens

import android.R.attr.fontWeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.project.e_commerce.android.R
import com.project.e_commerce.android.presentation.ui.screens.RequireLoginPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import com.project.e_commerce.android.domain.model.FollowingStatus
import com.project.e_commerce.android.domain.model.UserProfile
import com.project.e_commerce.android.domain.model.UserFollowModel
import com.project.e_commerce.android.presentation.viewModel.followingViewModel.FollowingViewModel
import org.koin.androidx.compose.koinViewModel
import android.util.Log
import androidx.compose.foundation.background
import coil3.compose.AsyncImage
import com.project.e_commerce.android.presentation.utils.UserDisplayName
import com.project.e_commerce.android.presentation.utils.UserFollowStats
import com.project.e_commerce.android.presentation.utils.UserDisplayType
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    navController: NavHostController,
    username: String,
    isCurrentUser: Boolean,
    startTabIndex: Int = 0,
    showFriendsTab: Boolean = true
) {
    val followingViewModel: FollowingViewModel = koinViewModel()
    val currentUserProvider: com.project.e_commerce.data.local.CurrentUserProvider = org.koin.compose.koinInject()
    var currentUserId by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        currentUserId = currentUserProvider.getCurrentUserId()
    }

    val tabs = if (showFriendsTab) {
        listOf("Followers", "Following", "Friends", "Suggested")
    } else {
        listOf("Followers", "Following")
    }

    val pagerState = rememberPagerState(initialPage = startTabIndex)
    val coroutineScope = rememberCoroutineScope()

    // Load data when component is created
    LaunchedEffect(currentUserId, username) {
        currentUserId?.let { userId ->
            followingViewModel.loadUserData(userId, username)
        }
    }

    // Observe UI state
    val uiState by followingViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
    ) {

        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = username,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0066CC)
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0066CC)
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.White
            )
        )

        ScrollableTabRow(
            backgroundColor = Color.White,
            selectedTabIndex = pagerState.currentPage,
            contentColor = Color(0xFF0066CC),
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier
                        .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                        .height(3.dp),
                    color = Color(0xFF0066CC)
                )
            },
        ) {
            tabs.forEachIndexed { index, title ->
                val labelWithCount = when (title) {
                    "Followers" -> "Followers (${uiState.followersCount})"
                    "Following" -> "Following (${uiState.followingCount})"
                    "Friends" -> "Friends (${uiState.friends.size})"
                    "Suggested" -> "Suggested (${uiState.suggestedUsers.size})"
                    else -> title
                }

                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = labelWithCount,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (showFriendsTab) 13.sp else 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                )
            }
        }

        HorizontalPager(count = tabs.size, state = pagerState) { page ->
            when (tabs[page]) {
                "Followers" -> FollowersTab(
                    users = uiState.followers,
                    currentUserId = currentUserId,
                    navController = navController,
                    onFollowClick = { targetUserId ->
                        currentUserId?.let { userId ->
                            coroutineScope.launch {
                                followingViewModel.toggleFollow(
                                    userId,
                                    targetUserId
                                )
                            }
                        }
                    },
                    isLoading = uiState.isLoading
                )
                "Following" -> FollowingTab(
                    users = uiState.following,
                    currentUserId = currentUserId,
                    navController = navController,
                    onFollowClick = { targetUserId ->
                        currentUserId?.let { userId ->
                            coroutineScope.launch {
                                followingViewModel.toggleFollow(
                                    userId,
                                    targetUserId
                                )
                            }
                        }
                    },
                    isLoading = uiState.isLoading
                )
                "Friends" -> FriendsTab(
                    users = uiState.friends,
                    currentUserId = currentUserId,
                    navController = navController,
                    onFollowClick = { targetUserId ->
                        currentUserId?.let { userId ->
                            coroutineScope.launch {
                                followingViewModel.toggleFollow(
                                    userId,
                                    targetUserId
                                )
                            }
                        }
                    },
                    isLoading = uiState.isLoading
                )
                "Suggested" -> SuggestedTab(
                    users = uiState.suggestedUsers,
                    currentUserId = currentUserId,
                    navController = navController,
                    onFollowClick = { targetUserId ->
                        currentUserId?.let { userId ->
                            coroutineScope.launch {
                                followingViewModel.toggleFollow(
                                    userId,
                                    targetUserId
                                )
                            }
                        }
                    },
                    isLoading = uiState.isLoading
                )
            }
        }
    }
}

@Composable
fun FollowersTab(
    users: List<UserFollowModel>,
    currentUserId: String?,
    navController: NavHostController,
    onFollowClick: (String) -> Unit,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFFF6F00))
        }
    } else if (users.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No followers yet", color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)) {
            items(users, key = { it.id }) { user ->
                UserFollowItem(
                    user = user,
                    currentUserId = currentUserId,
                    onFollowClick = { onFollowClick(user.id) },
                    onUserClick = { uid ->
                        navController.navigate(Screens.OtherUserProfileScreen.createRoute(uid))
                    }
                )
            }
        }
    }
}

@Composable
fun FollowingTab(
    users: List<UserFollowModel>,
    currentUserId: String?,
    navController: NavHostController,
    onFollowClick: (String) -> Unit,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFFF6F00))
        }
    } else if (users.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Not following anyone yet", color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)) {
            items(users, key = { it.id }) { user ->
                UserFollowItem(
                    user = user,
                    currentUserId = currentUserId,
                    onFollowClick = { onFollowClick(user.id) },
                    onUserClick = { uid ->
                        navController.navigate(Screens.OtherUserProfileScreen.createRoute(uid))
                    }
                )
            }
        }
    }
}

@Composable
fun FriendsTab(
    users: List<UserFollowModel>,
    currentUserId: String?,
    navController: NavHostController,
    onFollowClick: (String) -> Unit,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFFF6F00))
        }
    } else if (users.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No friends yet", color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)) {
            items(users, key = { it.id }) { user ->
                UserFollowItem(
                    user = user,
                    currentUserId = currentUserId,
                    onFollowClick = { onFollowClick(user.id) },
                    onUserClick = { uid ->
                        navController.navigate(Screens.OtherUserProfileScreen.createRoute(uid))
                    }
                )
            }
        }
    }
}

@Composable
fun SuggestedTab(
    users: List<UserFollowModel>,
    currentUserId: String?,
    navController: NavHostController,
    onFollowClick: (String) -> Unit,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFFF6F00))
        }
    } else if (users.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No suggested users yet", color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)) {
            items(users, key = { it.id }) { user ->
                UserFollowItem(
                    user = user,
                    currentUserId = currentUserId,
                    onFollowClick = { onFollowClick(user.id) },
                    onUserClick = { uid ->
                        navController.navigate(Screens.OtherUserProfileScreen.createRoute(uid))
                    }
                )
            }
        }
    }
}

@Composable
fun UserFollowItem(
    user: UserFollowModel,
    currentUserId: String?,
    onFollowClick: () -> Unit,
    onUserClick: (String) -> Unit
) {
    // State for confirmation dialog
    var showUnfollowDialog by remember { mutableStateOf(false) }

    // Determine button state based on follow relationship
    val buttonState = when {
        user.isFollowingMe && user.isIFollow -> "Following" // Mutual following
        user.isIFollow -> "Following" // Current user follows them
        user.isFollowingMe -> "Follow back" // They follow current user
        else -> "Follow" // No relationship
    }

    // Show confirmation dialog
    if (showUnfollowDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUnfollowDialog = false },
            title = {
                Text(
                    text = "Unfollow ${user.name}?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to unfollow ${user.name}?")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showUnfollowDialog = false
                        onFollowClick()
                    }
                ) {
                    Text(
                        "Unfollow",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showUnfollowDialog = false }
                ) {
                    Text(
                        "Cancel",
                        color = Color(0xFF0066CC)
                    )
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { onUserClick(user.id) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Image
        if (user.profileImageUrl != null && user.profileImageUrl!!.isNotBlank()) {
            AsyncImage(
                model = user.profileImageUrl,
                contentDescription = "Profile",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.profile),
                error = painterResource(id = R.drawable.profile)
            )
        } else {
            AsyncImage(
                model = R.drawable.profile,
                contentDescription = "Profile",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // User Info
        Column(modifier = Modifier.weight(1f)) {
            UserDisplayName(
                userId = user.id,
                displayType = UserDisplayType.DISPLAY_NAME_ONLY,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            )
            Text(
                text = "@${user.username}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        // Follow Button
        Button(
            modifier = Modifier.width(110.dp),
            onClick = {
                // Show confirmation dialog for unfollow actions
                if (buttonState == "Following") {
                    showUnfollowDialog = true
                } else {
                    onFollowClick()
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = when (buttonState) {
                    "Follow" -> Color(0xFFFF6600)
                    "Follow back" -> Color(0xFFFF6600)
                    "Following" -> Color(0xFFF2F2F2)
                    else -> Color(0xFFF2F2F2)
                },
                contentColor = if (buttonState == "Following") Color.Black else Color.White
            )
        ) {
            Text(
                buttonState,
                color = if (buttonState == "Following") Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun FollowListScreenPreview() {
    val fakeNavController = rememberNavController()
    FollowListScreen(
        navController = fakeNavController,
        username = "User_Test",
        isCurrentUser = true,
        startTabIndex = 0,
        showFriendsTab = true
    )
}