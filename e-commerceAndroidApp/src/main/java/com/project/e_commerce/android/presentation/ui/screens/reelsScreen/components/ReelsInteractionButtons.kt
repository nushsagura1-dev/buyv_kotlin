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
    reelsViewModel: ReelsScreenViewModel
) {
    val currentUserProvider: CurrentUserProvider = koinInject()
    var currentUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val user = currentUserProvider.getCurrentUser()
        currentUserId = user?.uid
    }
    val isOwner = currentUserId == reel.userId

    // Get real cart state
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

        // Like button
        InteractionButton(
            painter = painterResource(
                id = if (reel.love.isLoved) R.drawable.ic_love_checked else R.drawable.ic_love
            ),
            count = reel.love.number.toString(),
            tint = if (reel.love.isLoved) Color.Red else Color.White,
            onClick = onClickLoveButton
        )

        // Comment button
        InteractionButton(
            painter = painterResource(id = R.drawable.ic_comment),
            count = reel.numberOfComments.toString(),
            onClick = { onClickCommentButton(reel) }
        )

        // Cart button
        InteractionButton(
            painter = painterResource(
                id = if (isInCart) R.drawable.ic_cart_checked else R.drawable.ic_cart_checked
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
                }
            }
        )

        // Share button
        InteractionButton(
            painter = painterResource(id = R.drawable.ic_share),
            count = "Share",
            onClick = { onClickMoreButton(reel) }
        )

        // Bookmark button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.noRippleClickable { onClickBookmarkButton(reel) }
        ) {
            Icon(
                imageVector = if (reel.isBookmarked) Icons.Filled.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = if (reel.isBookmarked) Color(0xFFFFC107) else Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (reel.isBookmarked) "Saved" else "Save",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

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

        Spacer(modifier = Modifier.height(4.dp))
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
