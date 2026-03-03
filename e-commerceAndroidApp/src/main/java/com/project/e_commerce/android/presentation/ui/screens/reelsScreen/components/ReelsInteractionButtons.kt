package com.project.e_commerce.android.presentation.ui.screens.reelsScreen.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.LottieConstants
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.domain.repository.UserRepository
import com.project.e_commerce.domain.model.Result as DomainResult
import com.project.e_commerce.android.R
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Reels
import com.project.e_commerce.android.presentation.ui.utail.noRippleClickable
import com.project.e_commerce.android.presentation.viewModel.CartItemUi
import com.project.e_commerce.android.presentation.viewModel.CartViewModel
import com.project.e_commerce.android.presentation.viewModel.reelsScreenViewModel.ReelsScreenViewModel
import org.koin.compose.koinInject

/**
 * Boutons d'interaction pour un reel (colonne droite)
 * 
 * Inclut: Avatar, Like, Comment, Cart, Share, Music
 * 
 * Composant extrait de ReelsView.kt pour améliorer la maintenabilité.
 */
@Composable
fun ReelsInteractionButtons(
    navController: NavController,
    reel: Reels,
    onClickLoveButton: () -> Unit,
    onClickCommentButton: (Reels) -> Unit,
    onClickCartButton: (Reels) -> Unit,
    onClickMoreButton: (Reels) -> Unit,
    onClickBookmarkButton: (Reels) -> Unit,
    cartViewModel: CartViewModel,
    reelsViewModel: ReelsScreenViewModel,
    onDeletePost: (String) -> Unit = {}            // VIDEO-003: optional delete callback
) {
    val currentUserProvider: CurrentUserProvider = koinInject()
    var currentUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val user = currentUserProvider.getCurrentUser()
        currentUserId = user?.uid
    }
    val isOwner = currentUserId == reel.userId
    // VIDEO-003: state for delete confirmation dialog
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val cartState by cartViewModel.state.collectAsState()

    LaunchedEffect(reel.id) {
        // Refresh cart state when reel changes
    }

    val isInCart = remember(currentUserId, reel.id, cartState.items) {
        runCatching {
            currentUserId?.let { userId ->
                if (reel.id.isNotBlank() && cartState.items.isNotEmpty()) {
                    cartState.items.any { it.productId == reel.id }
                } else {
                    false
                }
            } ?: false
        }.getOrElse { false }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Avatar
        UserAvatarButton(
            reel = reel,
            isOwner = isOwner,
            navController = navController
        )

        // VIDEO-002/003: Owner sees Edit/Delete; non-owners see social actions
        if (isOwner) {
            // Edit post
            InteractionButtonWithVector(
                imageVector = Icons.Filled.Edit,
                label = "Edit",
                onClick = {
                    navController.navigate(Screens.ProfileScreen.AddNewContentScreen.route)
                }
            )
            // Delete post — confirmed via dialog
            InteractionButtonWithVector(
                imageVector = Icons.Filled.Delete,
                label = "Delete",
                tint = Color(0xFFFF5252),
                onClick = { showDeleteConfirm = true }
            )
        } else {
            // Like button with Lottie heart animation (VIDEO-005)
            val lottieComposition by rememberLottieComposition(
                LottieCompositionSpec.Asset("lottie_heart_like.json")
            )
            val lottieProgress by animateLottieCompositionAsState(
                composition = lottieComposition,
                isPlaying = reel.love.isLoved,
                iterations = 1,
                restartOnPlay = false,
                speed = 1.5f
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.noRippleClickable { onClickLoveButton() }
            ) {
                if (reel.love.isLoved) {
                    LottieAnimation(
                        composition = lottieComposition,
                        progress = { lottieProgress },
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_love),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reel.love.number.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // Comment button
            InteractionButton(
                painter = painterResource(id = R.drawable.ic_comment),
                count = reel.numberOfComments.toString(),
                onClick = { onClickCommentButton(reel) }
            )

            // Cart button (VIDEO-002: fix — ic_cart for empty, ic_cart_checked for added)
            InteractionButton(
                painter = painterResource(
                    id = if (isInCart) R.drawable.ic_cart_checked else R.drawable.ic_cart
                ),
                count = if (isInCart && cartState.items.isNotEmpty()) {
                    cartState.items.find { it.productId == reel.id }?.quantity?.toString() ?: "1"
                } else {
                    "0"
                },
                tint = if (isInCart) Color(0xFFFFC107) else Color.White,
                onClick = {
                    Log.d("InteractionButtons", "Cart icon pressed for product ${reel.id}")
                    if (isInCart) {
                        cartViewModel.removeItem(reel.id)
                        cartViewModel.refreshCart()
                        reelsViewModel.checkCartStatus(reel.id)
                    } else {
                        val cartItem = CartItemUi(
                            productId = reel.id,
                            name = reel.productName.ifEmpty { "Product" },
                            price = reel.productPrice.toDoubleOrNull() ?: 0.0,
                            imageUrl = reel.productImage.ifEmpty { "" },
                            quantity = 1
                        )
                        cartViewModel.addToCart(cartItem)
                        cartViewModel.refreshCart()
                        reelsViewModel.checkCartStatus(reel.id)
                        // SET-004: Auto-bookmark when adding to cart (Save = Cart unified)
                        onClickBookmarkButton(reel)
                    }
                }
            )

            // Share button
            InteractionButton(
                painter = painterResource(id = R.drawable.ic_share),
                count = "Share",
                onClick = { onClickMoreButton(reel) }
            )

            // Music button
            InteractionButton(
                painter = painterResource(id = R.drawable.ic_music),
                count = "",
                iconSize = 32.dp,
                onClick = {
                    val videoUrl = reel.video?.toString() ?: ""
                    val route = if (videoUrl.isNotEmpty()) {
                        Screens.ReelsScreen.SoundPageScreen.createRoute(videoUrl)
                    } else {
                        Screens.ReelsScreen.SoundPageScreen.route
                    }
                    navController.navigate(route)
                }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }

    // VIDEO-003: Delete confirmation dialog
    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { androidx.compose.material3.Text("Delete Post") },
            text = { androidx.compose.material3.Text("Are you sure? This action cannot be undone.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeletePost(reel.id)
                }) {
                    androidx.compose.material3.Text("Delete", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteConfirm = false }) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }
}

/**
 * Bouton avatar utilisateur
 */
@Composable
private fun UserAvatarButton(
    reel: Reels,
    isOwner: Boolean,
    navController: NavController
) {
    val userRepository: UserRepository = koinInject()
    val userProfileImageUrl = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reel.userId) {
        if (reel.userId.isNotBlank()) {
            try {
                when (val result = userRepository.getUserProfile(reel.userId)) {
                    is DomainResult.Success -> {
                        userProfileImageUrl.value = result.data.profileImageUrl
                        Log.d("InteractionButtons", "✅ Fetched profile image for ${reel.userId}")
                    }
                    is DomainResult.Error -> {
                        Log.e("InteractionButtons", "❌ Failed to fetch profile image: ${result.error.message}")
                    }
                    is DomainResult.Loading -> {
                        // Loading state - no action needed
                    }
                }
            } catch (e: Exception) {
                Log.e("InteractionButtons", "❌ Failed to fetch profile image: ${e.message}")
            }
        }
    }

    Box(modifier = Modifier.size(46.dp)) {
        val imageModel = if (userProfileImageUrl.value.isNullOrBlank()) {
            R.drawable.profile
        } else {
            userProfileImageUrl.value
        }

        AsyncImage(
            model = imageModel,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.TopCenter)
                .clip(CircleShape)
                .clickable {
                    if (isOwner) {
                        navController.navigate(Screens.ProfileScreen.route)
                    } else if (reel.userId.isNotBlank()) {
                        navController.navigate(Screens.OtherUserProfileScreen.createRoute(reel.userId))
                    }
                },
            contentDescription = "User Avatar",
            placeholder = painterResource(id = R.drawable.profile),
            error = painterResource(id = R.drawable.profile)
        )
    }
}

/**
 * Bouton d'interaction générique
 */
@Composable
fun InteractionButton(
    painter: Painter,
    count: String,
    tint: Color = Color.White,
    iconSize: Dp = 28.dp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.noRippleClickable { onClick() }
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * VIDEO-003: Bouton d'interaction propriétaire acceptée un ImageVector (Edit, Delete, etc.)
 */
@Composable
private fun InteractionButtonWithVector(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Color.White,
    iconSize: Dp = 28.dp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.noRippleClickable { onClick() }
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
