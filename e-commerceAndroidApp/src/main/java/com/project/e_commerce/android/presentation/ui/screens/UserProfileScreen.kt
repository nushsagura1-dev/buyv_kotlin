package com.project.e_commerce.android.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.project.e_commerce.android.R
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.ui.screens.profileScreen.ProfileStat
import com.google.firebase.auth.FirebaseAuth
import com.project.e_commerce.android.domain.model.UserProfile
import com.project.e_commerce.android.domain.usecase.GetUserProfileUseCase
import org.koin.androidx.compose.koinViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.util.Log
import coil3.compose.AsyncImage

@Composable
fun UserProfileScreen(navController: NavHostController) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(
        Pair(R.drawable.ic_reels, R.drawable.ic_reels),
        Pair(R.drawable.ic_products_filled, R.drawable.ic_products)
    )

    var isFollowing by remember { mutableStateOf(false) }
    
    // Get current user ID from navigation arguments or use current logged-in user
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val getUserProfileUseCase: GetUserProfileUseCase = koinViewModel()
    val scope = rememberCoroutineScope()
    
    // State for user profile data
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Load user profile data
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            isLoading = true
            scope.launch {
                try {
                    getUserProfileUseCase(currentUserId).onSuccess { profile ->
                        userProfile = profile
                        isLoading = false
                        Log.d("UserProfileScreen", "✅ Profile loaded: ${profile.displayName}")
                    }.onFailure { 
                        error = "Failed to load profile"
                        isLoading = false
                        Log.e("UserProfileScreen", "❌ Failed to load profile")
                    }
                } catch (e: Exception) {
                    error = "Error loading profile: ${e.message}"
                    isLoading = false
                    Log.e("UserProfileScreen", "❌ Exception loading profile: ${e.message}")
                }
            }
        } else {
            error = "User not found"
            isLoading = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Top Bar: Back (left), Menu (right)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 6.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Back",
                        tint = Color(0xFF0D3D67),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = { /* Menu action */ },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_menu),
                        contentDescription = "Menu",
                        tint = Color(0xFFFF6600),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            // Stats + Profile Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileStat(
                        number = userProfile?.followingCount?.toString() ?: "0",
                        label = "Following"
                    )
                    
                    // Profile Image - Use real data if available, fallback to default
                    if (userProfile?.profileImageUrl != null) {
                        AsyncImage(
                            model = userProfile?.profileImageUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.profile)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.profile),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                        )
                    }
                    
                    ProfileStat(
                        number = userProfile?.followersCount?.toString() ?: "0",
                        label = "Followers"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            // Name & Username
            if (isLoading) {
                // Show loading state
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Loading...",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D3D67)
                    )
                    Text(
                        text = "@loading",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            } else if (error != null) {
                // Show error state
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Error",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Text(
                        text = error!!,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            } else {
                // Show real user data
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userProfile?.displayName?.ifEmpty { "User" } ?: "User",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D3D67)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Image(
                        painter = painterResource(id = R.drawable.verified_badge),
                        contentDescription = "Verified",
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "@${userProfile?.username?.ifEmpty { "user" } ?: "user"}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            ProfileStat(
                number = userProfile?.likesCount?.toString() ?: "0",
                label = "Likes"
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            // Follow Button (main orange color)
            Button(
                onClick = {
                    isFollowing = !isFollowing
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isFollowing) Color.LightGray else Color(0xFFFF6600)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(150.dp)
                    .height(44.dp),
                elevation = ButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Text(
                    if (isFollowing) "Following" else "Follow",
                    fontSize = 16.sp,
                    color = if (isFollowing) Color.DarkGray else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        item {
            // Tabs Row (Reels, Products only)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                tabs.forEachIndexed { index, (filledIcon, outlineIcon) ->
                    IconButton(
                        onClick = { selectedTabIndex = index },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (selectedTabIndex == index) filledIcon else outlineIcon
                            ),
                            contentDescription = null,
                            tint = if (selectedTabIndex == index) Color(0xFFFF6600) else Color.Gray,
                            modifier = Modifier.size( if (index == 0) 30.dp else 20.dp )
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            when (selectedTabIndex) {
                0 -> SimpleReelGrid(navController)      // Reels
                1 -> SimpleProductGrid(navController)   // Products
            }
        }
    }
}

@Composable
fun SimpleReelGrid(navController: NavHostController) {
    val reels = listOf(
        R.drawable.perfume1, R.drawable.img2, R.drawable.img3, R.drawable.img4,
        R.drawable.perfume1, R.drawable.img2, R.drawable.img3, R.drawable.img4,
        R.drawable.perfume1, R.drawable.img2, R.drawable.img3
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(650.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(reels) { imageRes ->
            Box(
                modifier = Modifier
                    .background(Color(0xFFF8F8F8))
                    .height(130.dp)
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("105", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SimpleProductGrid(navController: NavHostController) {
    val products = listOf(
        Triple("White Laptop", "In Stock", "$100"),
        Triple("Elegant Perfume", "Out of Stock", "$50"),
        Triple("Smart Watch", "5 left", "$75"),
        Triple("Designer Bag", "10 left", "$150"),
        Triple("Smart Watch", "Out of Stock", "$75"),
        Triple("Smart Watch", "In Stock", "$100"),
        Triple("Smart Watch", "In Stock", "$75"),
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(650.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(products) { (title, stock, price) ->
            Column(
                modifier = Modifier
                    .background(Color(0xFFF8F8F8))
                    .padding(bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img2),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp, start = 8.dp, end = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stock,
                        color = if (stock == "Out of Stock") Color(0xFFEB1919)
                        else if (stock == "In Stock") Color(0xFF22C55E)
                        else Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        price,
                        color = Color(0xFFFF6F00),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

