package com.project.e_commerce.android.presentation.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.project.e_commerce.android.presentation.ui.components.ImageWithPlaceholder
import com.project.e_commerce.android.presentation.ui.components.ImageType
import com.project.e_commerce.android.data.model.User
import com.project.e_commerce.android.presentation.ui.composable.admin.UserCard
import com.project.e_commerce.android.presentation.ui.composable.admin.StatBadge
import com.project.e_commerce.android.presentation.viewModel.admin.AdminUserManagementViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AdminUserManagementScreen(
    navController: NavController,
    viewModel: AdminUserManagementViewModel = koinViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var showVerifiedOnly by remember { mutableStateOf(false) }
    var showUnverifiedOnly by remember { mutableStateOf(false) }
    var selectedUsers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<() -> Unit>({}) }
    var confirmTitle by remember { mutableStateOf("") }
    var confirmMessage by remember { mutableStateOf("") }
    
    val users by viewModel.recentUsers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Pull to refresh
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.loadDashboardData() }
    )

    // Filter users based on search and filters
    val filteredUsers = users.filter { user ->
        val matchesSearch = searchQuery.isEmpty() || 
            user.username.contains(searchQuery, ignoreCase = true) ||
            user.email.contains(searchQuery, ignoreCase = true)
        
        val matchesVerified = when {
            showVerifiedOnly -> user.isVerified
            showUnverifiedOnly -> !user.isVerified
            else -> true
        }
        
        matchesSearch && matchesVerified
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "User Management",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Select all
                    if (filteredUsers.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                selectedUsers = if (selectedUsers.size == filteredUsers.size) {
                                    emptySet()
                                } else {
                                    filteredUsers.map { it.id }.toSet()
                                }
                            }
                        ) {
                            Icon(
                                if (selectedUsers.size == filteredUsers.size) 
                                    Icons.Default.CheckBox 
                                else 
                                    Icons.Default.CheckBoxOutlineBlank,
                                "Select All"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (selectedUsers.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF6200EE),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Verify button
                        Button(
                            onClick = {
                                confirmTitle = "Verify Users"
                                confirmMessage = "Verify ${selectedUsers.size} selected user(s)?"
                                confirmAction = {
                                    selectedUsers.forEach { userId ->
                                        viewModel.verifyUser(userId)
                                    }
                                    selectedUsers = emptySet()
                                }
                                showConfirmDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(Icons.Default.Verified, "Verify", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Verify")
                        }
                        
                        // Unverify button
                        Button(
                            onClick = {
                                confirmTitle = "Unverify Users"
                                confirmMessage = "Unverify ${selectedUsers.size} selected user(s)?"
                                confirmAction = {
                                    selectedUsers.forEach { userId ->
                                        viewModel.unverifyUser(userId)
                                    }
                                    selectedUsers = emptySet()
                                }
                                showConfirmDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            )
                        ) {
                            Icon(Icons.Default.Cancel, "Unverify", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Unverify")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by username or email...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showVerifiedOnly,
                    onClick = { 
                        showVerifiedOnly = !showVerifiedOnly
                        if (showVerifiedOnly) showUnverifiedOnly = false
                    },
                    label = { Text("Verified") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Verified,
                            "Verified",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                
                FilterChip(
                    selected = showUnverifiedOnly,
                    onClick = { 
                        showUnverifiedOnly = !showUnverifiedOnly
                        if (showUnverifiedOnly) showVerifiedOnly = false
                    },
                    label = { Text("Unverified") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Cancel,
                            "Unverified",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                
                Spacer(Modifier.weight(1f))
                
                Text(
                    text = "${filteredUsers.size} users",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Error message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFC62828),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            // Loading
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // User list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredUsers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.PersonOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Color.Gray
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "No users found",
                                        fontSize = 18.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredUsers, key = { it.id }) { user ->
                            UserCard(
                                userId = user.id,
                                userName = user.displayName ?: user.username ?: "Unknown",
                                userEmail = user.email,
                                isVerified = user.isVerified,
                                profileImageUrl = user.profileImageUrl,
                                onVerify = {
                                    viewModel.verifyUser(user.id)
                                },
                                onUnverify = {
                                    viewModel.unverifyUser(user.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(confirmTitle) },
            text = { Text(confirmMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmAction()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (confirmTitle.contains("Delete")) 
                            Color(0xFFF44336) 
                        else 
                            Color(0xFF6200EE)
                    )
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UserCard(
    user: User,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectionChanged(!isSelected) },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                Color(0xFFE3F2FD) 
            else 
                Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChanged
            )
            
            Spacer(Modifier.width(8.dp))
            
            // Avatar
            ImageWithPlaceholder(
                imageUrl = user.profileImageUrl ?: "https://via.placeholder.com/100",
                contentDescription = "Avatar",
                imageType = ImageType.PROFILE,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(Modifier.width(12.dp))
            
            // User info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.username,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (user.isVerified) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verified",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF2196F3)
                        )
                    }
                }
                
                Text(
                    text = user.email,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(Modifier.height(4.dp))
                
                // Stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatBadge(
                        icon = Icons.Default.VideoLibrary,
                        count = user.reelsCount,
                        label = "reels"
                    )
                    StatBadge(
                        icon = Icons.Default.People,
                        count = user.followersCount,
                        label = "followers"
                    )
                }
            }
        }
    }
}

@Composable
fun StatBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(14.dp),
            tint = Color.Gray
        )
        Text(
            text = "$count",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
    }
}
}
