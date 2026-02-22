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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.project.e_commerce.android.data.api.AdminPostResponse
import com.project.e_commerce.android.presentation.viewModel.admin.AdminPostsViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Écran Admin - Gestion des Posts
 * Liste, recherche, filtre et modération des posts/reels
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AdminPostsScreen(
    navController: NavHostController,
    viewModel: AdminPostsViewModel = koinViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var postToDelete by remember { mutableStateOf<AdminPostResponse?>(null) }

    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.loadPosts(searchQuery.ifEmpty { null }, selectedType) }
    )

    // Show success snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    // Filter posts locally
    val filteredPosts = posts.filter { post ->
        val matchesSearch = searchQuery.isEmpty() ||
            (post.caption?.contains(searchQuery, ignoreCase = true) == true) ||
            post.username.contains(searchQuery, ignoreCase = true)
        val matchesType = selectedType == null || post.type == selectedType
        matchesSearch && matchesType
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Posts Management", fontWeight = FontWeight.Bold) },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search by caption or user...") },
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
                        selected = selectedType == null,
                        onClick = { selectedType = null },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = selectedType == "reel",
                        onClick = { selectedType = if (selectedType == "reel") null else "reel" },
                        label = { Text("Reels") },
                        leadingIcon = {
                            Icon(Icons.Default.VideoLibrary, null, modifier = Modifier.size(16.dp))
                        }
                    )
                    FilterChip(
                        selected = selectedType == "product",
                        onClick = { selectedType = if (selectedType == "product") null else "product" },
                        label = { Text("Products") },
                        leadingIcon = {
                            Icon(Icons.Default.ShoppingBag, null, modifier = Modifier.size(16.dp))
                        }
                    )
                    FilterChip(
                        selected = selectedType == "photo",
                        onClick = { selectedType = if (selectedType == "photo") null else "photo" },
                        label = { Text("Photos") },
                        leadingIcon = {
                            Icon(Icons.Default.Photo, null, modifier = Modifier.size(16.dp))
                        }
                    )

                    Spacer(Modifier.weight(1f))

                    Text(
                        "${filteredPosts.size} posts",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Error message
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Text(
                            text = error,
                            color = Color(0xFFC62828),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                if (isLoading && posts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (filteredPosts.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Article,
                                            null,
                                            modifier = Modifier.size(64.dp),
                                            tint = Color.Gray
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text("No posts found", fontSize = 18.sp, color = Color.Gray)
                                    }
                                }
                            }
                        } else {
                            items(filteredPosts, key = { it.id }) { post ->
                                AdminPostCard(
                                    post = post,
                                    onDelete = {
                                        postToDelete = post
                                        showDeleteDialog = true
                                    }
                                )
                            }
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

    // Delete confirmation dialog
    if (showDeleteDialog && postToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this post?") },
            text = {
                Text("The post by @${postToDelete!!.username} will be permanently deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePost(postToDelete!!.uid)
                        showDeleteDialog = false
                        postToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AdminPostCard(
    post: AdminPostResponse,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = post.thumbnail_url ?: post.media_url,
                contentDescription = "Post thumbnail",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            // Post info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "@${post.username}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (post.type) {
                            "reel" -> Color(0xFFE3F2FD)
                            "product" -> Color(0xFFFFF3E0)
                            else -> Color(0xFFF3E5F5)
                        }
                    ) {
                        Text(
                            post.type.replaceFirstChar { it.uppercase() },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = when (post.type) {
                                "reel" -> Color(0xFF1565C0)
                                "product" -> Color(0xFFE65100)
                                else -> Color(0xFF6A1B9A)
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (post.is_promoted) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Campaign,
                            "Promoted",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFF6F00)
                        )
                    }
                }

                if (!post.caption.isNullOrBlank()) {
                    Text(
                        post.caption,
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Stats row
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, null, Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(Modifier.width(2.dp))
                        Text("${post.likes_count}", fontSize = 12.sp, color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ChatBubble, null, Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(Modifier.width(2.dp))
                        Text("${post.comments_count}", fontSize = 12.sp, color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, null, Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(Modifier.width(2.dp))
                        Text("${post.views_count}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    "Delete",
                    tint = Color(0xFFF44336)
                )
            }
        }
    }
}
