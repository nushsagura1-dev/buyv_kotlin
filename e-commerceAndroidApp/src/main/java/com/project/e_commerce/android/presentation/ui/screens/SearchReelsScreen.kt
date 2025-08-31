package com.project.e_commerce.android.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.project.e_commerce.android.R
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.focusRequester
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import org.koin.androidx.compose.koinViewModel
import com.project.e_commerce.android.presentation.viewModel.ProductViewModel
import com.project.e_commerce.android.presentation.viewModel.searchViewModel.SearchViewModel
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Reels
import com.project.e_commerce.android.domain.model.UserProfile
import coil3.compose.AsyncImage
import com.project.e_commerce.android.presentation.ui.composable.composableScreen.public.VideoThumbnail
import com.project.e_commerce.android.presentation.utils.UserDisplayName
import com.project.e_commerce.android.presentation.utils.UserDisplayType

@Composable
fun SearchReelsAndUsersScreen(navController: NavHostController) {
    val searchViewModel: SearchViewModel = koinViewModel()

    val uiState by searchViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val textFieldFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        textFieldFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header Search Bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() },
                modifier = Modifier.offset(x = (-12).dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFF1B7ACE))
            }

            TextField(
                value = uiState.searchQuery,
                onValueChange = { searchViewModel.updateSearchQuery(it) },
                placeholder = { Text("Search reels or users...", color = Color.Gray) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFFFF6F00),
                        modifier = Modifier.clickable {
                            focusManager.clearFocus()
                        }
                    )
                },
                singleLine = true,
                maxLines = 1,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .offset(x = (-6).dp)
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0F0F0))
                    .focusRequester(textFieldFocusRequester),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFFFF6F00),
                    unfocusedIndicatorColor = Color(0xFFE0E0E0),
                    cursorColor = Color(0xFFFF6F00)
                ),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                )
            )
        }

        // Error message
        uiState.error?.let { errorMsg ->
            Text(
                text = errorMsg,
                color = Color.Red,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Tabs
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            backgroundColor = Color.White,
            contentColor = Color(0xFFFF6F00)
        ) {
            val tabs = listOf("Reels", "Users")
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = {
                        Text(
                            tab,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (pagerState.currentPage == index) Color(0xFFFF6F00) else Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                )
            }
        }

        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF6F00))
            }
        }

        // Pager Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> ReelsGridContent(
                    query = uiState.searchQuery,
                    reels = uiState.reels,
                    isLoading = uiState.isLoading,
                    navController = navController
                )

                1 -> UsersListContent(
                    query = uiState.searchQuery,
                    users = uiState.users,
                    isLoading = uiState.isLoading,
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun ReelsGridContent(
    query: String,
    reels: List<Reels>,
    isLoading: Boolean,
    navController: NavHostController
) {
    when {
        isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF6F00))
            }
        }
        query.isBlank() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Start typing to search for reels", color = Color.Gray)
                }
            }
        }

        reels.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No reels found for \"$query\"", color = Color.Gray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Try searching with different keywords",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }

        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reels) { reel ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF0F0F0))
                            .aspectRatio(10f / 15f)
                            .clickable {
                                // Navigate to main reels screen - this will show all reels
                                navController.navigate(Screens.ReelsScreen.route) {
                                    popUpTo(Screens.ReelsScreen.route) { inclusive = true }
                                }
                            }
                    ) {
                        // Display video thumbnail using VideoThumbnail composable
                        VideoThumbnail(
                            videoUri = reel.video,
                            fallbackImageRes = reel.fallbackImageRes,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Overlay with user info
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = reel.userImage),
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                reel.userName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .background(Color(0x80000000), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UsersListContent(
    query: String,
    users: List<com.project.e_commerce.android.presentation.viewModel.searchViewModel.SearchUserResult>,
    isLoading: Boolean,
    navController: NavHostController
) {
    val searchViewModel: SearchViewModel = koinViewModel()

    when {
        isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF6F00))
            }
        }
        query.isBlank() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Start typing to search for users", color = Color.Gray)
                }
            }
        }

        users.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No users found for \"$query\"", color = Color.Gray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Try searching with different keywords",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }

        else -> {
            LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                items(users) { userResult ->
                    val user = userResult.userProfile

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                // Navigate to user profile
                                if (userResult.isCurrentUser) {
                                    navController.navigate(Screens.ProfileScreen.route)
                                } else {
                                    navController.navigate(
                                        Screens.OtherUserProfileScreen.createRoute(user.uid)
                                    )
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Profile image
                        if (user.profileImageUrl?.isNotEmpty() == true) {
                            AsyncImage(
                                model = user.profileImageUrl,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFFFF6F00), CircleShape),
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
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFFFF6F00), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UserDisplayName(
                                    userId = user.uid,
                                    displayType = UserDisplayType.DISPLAY_NAME_ONLY,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                                // Add "You" indicator for current user
                                if (userResult.isCurrentUser) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "(You)",
                                        color = Color(0xFFFF6F00),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Text(
                                "@${user.username.ifEmpty { user.displayName }}",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                            if (user.bio.isNotEmpty()) {
                                Text(
                                    user.bio,
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // Only show follow button if not current user
                        if (!userResult.isCurrentUser) {
                            if (userResult.isFollowingLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFFFF6F00),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Button(
                                    onClick = { searchViewModel.toggleFollow(user.uid) },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = if (userResult.isFollowing)
                                            Color(0xFFE0E0E0) else Color(0xFF176DBA)
                                    ),
                                    modifier = Modifier.height(38.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        if (userResult.isFollowing) "Following" else "Follow",
                                        color = if (userResult.isFollowing) Color.Black else Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewSearchReelsAndUsersScreen() {
    val navController = rememberNavController()
    SearchReelsAndUsersScreen(navController)
}
