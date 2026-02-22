package com.project.e_commerce.android.presentation.ui.screens.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.project.e_commerce.android.presentation.ui.composable.common.ImageWithPlaceholder
import com.project.e_commerce.android.presentation.ui.composable.common.ImageType
import com.project.e_commerce.android.presentation.ui.composable.common.HtmlText
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.project.e_commerce.android.presentation.viewModel.marketplace.ProductDetailUiState
import com.project.e_commerce.android.presentation.viewModel.marketplace.ProductDetailViewModel
import com.project.e_commerce.android.presentation.viewModel.marketplace.PromotionState
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.viewModel.CartItemUi
import com.project.e_commerce.android.presentation.viewModel.CartViewModel
import com.project.e_commerce.domain.model.marketplace.MarketplaceProduct
import com.project.e_commerce.domain.model.UserPost
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Écran de détails d'un produit
 * Affiche les informations complètes et permet de créer une promotion
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    navController: NavHostController,
    viewModel: ProductDetailViewModel = koinViewModel(),
    cartViewModel: CartViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val promotionState by viewModel.promotionState.collectAsState()
    val userPosts by viewModel.userPosts.collectAsState()
    val postsLoading by viewModel.postsLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var showPromoteDialog by remember { mutableStateOf(false) }
    
    // Load user posts when dialog opens
    LaunchedEffect(showPromoteDialog) {
        if (showPromoteDialog) {
            viewModel.loadUserPosts()
        }
    }

    // Initialize cart to ensure currentUserId is set
    LaunchedEffect(Unit) {
        cartViewModel.initializeCart()
    }

    // Load product
    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    // Handle promotion success
    LaunchedEffect(promotionState) {
        if (promotionState is PromotionState.Success) {
            showPromoteDialog = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            when (val state = uiState) {
                is ProductDetailUiState.Success -> {
                    BottomActionBar(
                        product = state.product,
                        onPromoteClick = {
                            // Navigate to AddNewContentScreen with product pre-selected
                            navController.navigate(
                                Screens.ProfileScreen.AddNewContentScreen.createRoute(state.product.id)
                            )
                        },
                        onBuyClick = {
                            val product = state.product
                            val cartItem = CartItemUi(
                                productId = product.id,
                                name = product.name,
                                price = product.sellingPrice,
                                imageUrl = product.mainImageUrl ?: product.images.firstOrNull() ?: "",
                                quantity = 1,
                                promoterUid = product.promoterUserId // Pour split de commission
                            )
                            cartViewModel.addToCart(cartItem)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Product added to cart",
                                    actionLabel = "View cart",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    navController.navigate(Screens.CartScreen.route)
                                }
                            }
                        },
                        isLoading = promotionState is PromotionState.Loading
                    )
                }
                else -> {}
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ProductDetailUiState.Loading -> {
                LoadingView(Modifier.padding(paddingValues))
            }
            
            is ProductDetailUiState.Success -> {
                ProductDetailContent(
                    product = state.product,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            is ProductDetailUiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    // Dialog pour promouvoir
    if (showPromoteDialog) {
        PromoteProductDialog(
            onDismiss = { showPromoteDialog = false },
            onConfirm = { postId ->
                viewModel.createPromotion(postId) {
                    // Succès - retour ou navigation
                    showPromoteDialog = false
                }
            },
            promotionState = promotionState,
            userPosts = userPosts,
            postsLoading = postsLoading
        )
    }
}

/**
 * Contenu principal avec détails du produit
 */
@OptIn(ExperimentalPagerApi::class)
@Composable
private fun ProductDetailContent(
    product: MarketplaceProduct,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Images carousel
        item {
            val pagerState = rememberPagerState()
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color.White)
            ) {
                HorizontalPager(
                    count = product.images.size,
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    ImageWithPlaceholder(
                        model = product.images[page],
                        contentDescription = product.name,
                        imageType = ImageType.PRODUCT,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Indicateur de page
                HorizontalPagerIndicator(
                    pagerState = pagerState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    activeColor = Color(0xFFFF9800),
                    inactiveColor = Color.White.copy(alpha = 0.5f)
                )
                
                // Badge commission
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    color = Color(0xFFFF9800),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎯 Earn",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${String.format("%.1f", product.commissionRate)}%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
        
        // Infos principales
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Nom
                    Text(
                        text = product.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Prix
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.getFormattedPrice(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                        
                        if (product.originalPrice > product.sellingPrice) {
                            Text(
                                text = String.format("$%.2f", product.originalPrice),
                                fontSize = 18.sp,
                                color = Color.Gray,
                                style = androidx.compose.ui.text.TextStyle(
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                )
                            )
                            
                            Surface(
                                color = Color(0xFFE91E63).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "-${product.getDiscountPercentage()}%",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE91E63)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            icon = Icons.Default.Star,
                            label = "Note",
                            value = String.format("%.1f", product.averageRating),
                            color = Color(0xFFFFD700)
                        )
                        StatItem(
                            icon = Icons.Default.ShoppingBag,
                            label = "Sales",
                            value = "${product.totalSales}",
                            color = Color(0xFF2196F3)
                        )
                        StatItem(
                            icon = Icons.Default.TrendingUp,
                            label = "Promos",
                            value = "${product.totalPromotions}",
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
        
        // Earning potential
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Earn per sale",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = String.format("$%.2f", product.getEstimatedCommission()),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
        
        // Description
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Description",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HtmlText(
                        html = product.description ?: "",
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Infos CJ (si disponibles)
        if (!product.cjProductId.isNullOrEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Commission Junction Info",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow("CJ Product ID", product.cjProductId ?: "N/A")
                        InfoRow("Affiliate Link", "CJ Link available")
                    }
                }
            }
        }
        
        // Espace pour le bottom bar
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Item de statistique
 */
@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

/**
 * Ligne d'information
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.Black,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Barre d'actions en bas
 */
@Composable
private fun BottomActionBar(
    product: MarketplaceProduct,
    onPromoteClick: () -> Unit,
    onBuyClick: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Rangée principale : Buy + Promote
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bouton Acheter
                Button(
                    onClick = onBuyClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    ),
                    enabled = product.isAvailable()
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buy", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                // Bouton Promouvoir
                Button(
                    onClick = onPromoteClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    ),
                    enabled = !isLoading && product.isAvailable()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Campaign, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Promote", fontSize = 16.sp)
                    }
                }
            }
            
            // Prix affiché
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${"%.2f".format(product.sellingPrice)}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53935)
                )
                if (product.originalPrice > product.sellingPrice) {
                    Text(
                        text = "$${"%.2f".format(product.originalPrice)}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        style = androidx.compose.ui.text.TextStyle(
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    )
                }
            }
        }
    }
}

/**
 * Dialog pour promouvoir le produit avec picker de posts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromoteProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    promotionState: PromotionState,
    userPosts: List<UserPost>,
    postsLoading: Boolean
) {
    var selectedPostId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Promote this product",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                Text(
                    text = "Select the post/reel to promote this product on:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                when {
                    postsLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFF9800))
                        }
                    }
                    userPosts.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No posts found.\nCreate a reel to promote this product.",
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(userPosts) { post ->
                                PostPickerItem(
                                    post = post,
                                    isSelected = post.id == selectedPostId,
                                    onClick = { selectedPostId = post.id }
                                )
                            }
                        }
                    }
                }
                
                // Messages d'état
                when (promotionState) {
                    is PromotionState.Loading -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Creating...",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    is PromotionState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "❌ ${promotionState.message}",
                            fontSize = 12.sp,
                            color = Color(0xFFF44336)
                        )
                    }
                    is PromotionState.Success -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✅ Promotion created successfully!",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedPostId?.let { onConfirm(it) } },
                enabled = selectedPostId != null && promotionState !is PromotionState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                )
            ) {
                Text("Create promotion")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = promotionState !is PromotionState.Loading
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Item de post dans le picker
 */
@Composable
private fun PostPickerItem(
    post: UserPost,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFFFF9800) else Color.Transparent
    val borderWidth = if (isSelected) 3.dp else 0.dp
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = post.thumbnailUrl ?: post.mediaUrl,
            contentDescription = post.title ?: "Post",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Badge pour le type de post
        if (post.type == "REEL") {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Reel",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        
        // Indicateur de sélection
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .background(Color(0xFFFF9800), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Vue de chargement
 */
@Composable
private fun LoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFFFF9800))
    }
}

/**
 * Vue d'erreur
 */
@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFF44336)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}
