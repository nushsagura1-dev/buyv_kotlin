package com.project.e_commerce.android.presentation.ui.screens.reelsScreen.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.project.e_commerce.android.R
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Reels
import com.project.e_commerce.android.presentation.utils.UserDisplayName
import com.project.e_commerce.android.presentation.utils.UserDisplayType
import com.project.e_commerce.android.presentation.utils.UserInfoCache
import com.project.e_commerce.android.presentation.viewModel.CartViewModel
import com.project.e_commerce.android.presentation.viewModel.followingViewModel.FollowingViewModel
import com.project.e_commerce.android.presentation.viewModel.reelsScreenViewModel.ReelsScreenViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Contenu principal d'un reel (partie gauche)
 * 
 * Inclut: UserInfo, Description, MarketplaceProductBadge, Hashtags, OfferCard
 * 
 * Composant extrait de ReelsView.kt pour améliorer la maintenabilité.
 */
@Composable
fun ReelContent(
    modifier: Modifier,
    navController: NavController,
    reel: Reels,
    viewModel: ReelsScreenViewModel,
    cartViewModel: CartViewModel,
    onClickCommentButton: (Reels) -> Unit,
    onClickCartButton: (Reels) -> Unit,
    onClickMoreButton: (Reels) -> Unit,
    showLoginPrompt: MutableState<Boolean>,
    isLoggedIn: Boolean,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = 16.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 60.dp
            )
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            UserInfo(
                reel = reel,
                navController = navController,
                isLoggedIn = isLoggedIn,
                showLoginPrompt = showLoginPrompt
            )
            Spacer(modifier = Modifier.height(8.dp))
            ReelDescription(description = reel.contentDescription)
            Spacer(modifier = Modifier.height(4.dp))

            // Carte produit: image + nom + prix + bouton "View" → détail produit
            if (reel.marketplaceProductId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OfferCard(
                    productName = reel.marketplaceProductName ?: reel.productName,
                    productPrice = "$${String.format("%.2f", reel.marketplaceProductPrice ?: reel.productPrice.toDoubleOrNull() ?: 0.0)}",
                    productImageUrl = reel.productImage.ifBlank { null },
                    onViewClick = {
                        navController.navigate("marketplace_product/${reel.marketplaceProductId}")
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            ReelHashtags(hashtags = listOf("satisfying", "roadmarking"))
            Spacer(modifier = Modifier.height(12.dp))

            Spacer(modifier = Modifier.height(6.dp))
        }

        ReelsInteractionButtons(
            navController = navController,
            reel = reel,
            onClickLoveButton = {
                if (!isLoggedIn) {
                    showLoginPrompt.value = true
                } else {
                    viewModel.onClackLoveReelsButton(reel.id)
                }
            },
            onClickCommentButton = { onClickCommentButton(reel) },
            onClickCartButton = { onClickCartButton(reel) },
            onClickMoreButton = { onClickMoreButton(reel) },
            onClickBookmarkButton = {
                if (!isLoggedIn) {
                    showLoginPrompt.value = true
                } else {
                    viewModel.onToggleBookmark(reel.id)
                }
            },
            cartViewModel = cartViewModel,
            reelsViewModel = viewModel
        )
    }
}

/**
 * Informations utilisateur avec bouton Follow
 */
@Composable
fun UserInfo(
    reel: Reels,
    navController: NavController,
    isLoggedIn: Boolean,
    showLoginPrompt: MutableState<Boolean>
) {
    val currentUserProvider: com.project.e_commerce.data.local.CurrentUserProvider = org.koin.compose.koinInject()
    var currentUserId by remember { mutableStateOf<String?>(null) }
    var currentUsername by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        val user = currentUserProvider.getCurrentUser()
        currentUserId = user?.uid
        currentUsername = user?.username ?: user?.displayName ?: ""
    }
    val isOwner = currentUserId == reel.userId
    val isActuallyLoggedIn = currentUserId != null

    val followingViewModel: FollowingViewModel = koinViewModel()
    val uiState by followingViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Ensure following data is loaded
    LaunchedEffect(currentUserId) {
        val userId = currentUserId
        if (userId != null && !uiState.isLoading && uiState.following.isEmpty() && uiState.error == null) {
            followingViewModel.loadUserData(userId, currentUsername)
        }
    }

    // Local state for immediate UI updates
    val isFollowingFromState = uiState.following.any { it.id == reel.userId }
    var isFollowingLocal by remember(reel.userId) { mutableStateOf(isFollowingFromState) }

    LaunchedEffect(isFollowingFromState) {
        isFollowingLocal = isFollowingFromState
    }

    val isFollowing = isFollowingLocal

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        UserDisplayName(
            userId = reel.userId,
            displayType = UserDisplayType.DISPLAY_NAME_ONLY,
            color = Color.White,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.clickable {
                if (isOwner) {
                    navController.navigate(Screens.ProfileScreen.route)
                } else if (reel.userId.isNotBlank()) {
                    navController.navigate(Screens.OtherUserProfileScreen.createRoute(reel.userId))
                }
            }
        )

        // Only show follow button if NOT owner AND reel has a valid backend user ID
        if (!isOwner && reel.userId.isNotBlank()) {
            Box(
                modifier = Modifier
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(8.dp), clip = false)
                    .background(
                        brush = if (!isFollowing)
                            Brush.horizontalGradient(listOf(Color(0xFFf8a714), Color(0xFFed380a)))
                        else
                            Brush.horizontalGradient(listOf(Color(0xFF9E9E9E), Color(0xFF757575))),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        if (!isActuallyLoggedIn) {
                            showLoginPrompt.value = true
                        } else {
                            isFollowingLocal = !isFollowingLocal

                            currentUserId?.let { userId ->
                                coroutineScope.launch {
                                    try {
                                        followingViewModel.toggleFollow(userId, reel.userId)
                                        delay(500)
                                        followingViewModel.loadUserData(userId, currentUsername)
                                        UserInfoCache.clearUserCache(reel.userId)
                                    } catch (e: Exception) {
                                        Log.e("UserInfo", "❌ Follow/unfollow operation failed: ${e.message}")
                                        isFollowingLocal = !isFollowingLocal
                                    }
                                }
                            }
                        }
                    }
                    .height(26.dp)
                    .width(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (!isFollowing) "Follow +" else "Following",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Card d'offre produit
 */
@Composable
fun OfferCard(
    productName: String = "Hanger Shirt",
    productType: String = "Shirt",
    productPrice: String = "100.00 $",
    productImage: Int = R.drawable.profile,
    productImageUrl: String? = null,
    onViewClick: () -> Unit = {}
) {
    // Clean HTML from product name and price
    val cleanProductName = remember(productName) {
        try {
            android.text.Html.fromHtml(productName, android.text.Html.FROM_HTML_MODE_COMPACT)
                .toString().trim()
        } catch (e: Exception) {
            productName.replace(Regex("<[^>]*>"), "").trim()
        }
    }
    val cleanProductPrice = remember(productPrice) {
        try {
            android.text.Html.fromHtml(productPrice, android.text.Html.FROM_HTML_MODE_COMPACT)
                .toString().trim()
        } catch (e: Exception) {
            productPrice.replace(Regex("<[^>]*>"), "").trim()
        }
    }
    // Truncate long names
    val displayName = if (cleanProductName.length > 35) cleanProductName.take(35) + "..." else cleanProductName

    Row(
        modifier = Modifier
            .offset(x = (-12f).dp)
            .background(Color(0xCC222222), shape = RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product image: use URL if available, otherwise fallback to resource
        if (!productImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = productImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            )
        } else {
            Image(
                painter = painterResource(id = productImage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))

        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = displayName,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cleanProductPrice,
                    color = Color(0xFFFFEB3B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp), clip = false)
                        .background(
                            brush = Brush.horizontalGradient(colors = listOf(Color(0xFFf8a714), Color(0xFFed380a))),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onViewClick() }
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(text = "View", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Description du reel - renders HTML tags properly
 */
@Composable
fun ReelDescription(description: String) {
    // Render HTML tags properly (product descriptions from CJ come as HTML)
    val cleanText = remember(description) {
        try {
            val spanned = android.text.Html.fromHtml(description, android.text.Html.FROM_HTML_MODE_COMPACT)
            spanned.toString().trim()
        } catch (e: Exception) {
            description.replace(Regex("<[^>]*>"), "").trim()
        }
    }
    Text(
        text = cleanText,
        color = Color.White,
        fontSize = 14.sp,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 20.sp
    )
}

/**
 * Hashtags du reel
 */
@Composable
fun ReelHashtags(hashtags: List<String>) {
    Text(
        text = hashtags.joinToString(" ") { "#$it" },
        color = Color(0xFFFF6F00),
        fontSize = 14.sp
    )
}
