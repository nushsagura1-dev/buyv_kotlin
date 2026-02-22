package com.project.e_commerce.android.presentation.ui.screens.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.project.e_commerce.android.presentation.ui.composable.common.ImageType
import com.project.e_commerce.android.presentation.ui.composable.common.ImageWithPlaceholder
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.viewModel.marketplace.MarketplaceViewModel
import com.project.e_commerce.android.presentation.viewModel.marketplace.ProductFilters
import com.project.e_commerce.domain.model.marketplace.MarketplaceProduct
import org.koin.androidx.compose.koinViewModel

/**
 * Écran principal du Marketplace avec Paging3.
 * Affiche la liste des produits avec filtres, recherche et pagination infinie.
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    navController: NavHostController,
    viewModel: MarketplaceViewModel = koinViewModel()
) {
    // Collecte du Flow Paging3
    val products: LazyPagingItems<MarketplaceProduct> = viewModel.productsFlow.collectAsLazyPagingItems()
    val categories by viewModel.categoriesState.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedSortOption by remember { mutableStateOf("commission") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    // Pull to refresh avec Paging3
    val isRefreshing = products.loadState.refresh is LoadState.Loading
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { products.refresh() }
    )

    Scaffold(
        topBar = {
            MarketplaceTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { 
                    searchQuery = it
                    if (it.length >= 3 || it.isEmpty()) {
                        viewModel.searchProducts(it)
                    }
                },
                onFilterClick = { showFilterSheet = true },
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            // Gestion des états Paging3
            when {
                // Chargement initial
                products.loadState.refresh is LoadState.Loading && products.itemCount == 0 -> {
                    LoadingView()
                }
                
                // Erreur au chargement initial
                products.loadState.refresh is LoadState.Error -> {
                    val error = products.loadState.refresh as LoadState.Error
                    ErrorView(
                        message = error.error.localizedMessage ?: "Loading error",
                        onRetry = { products.retry() }
                    )
                }
                
                // Liste vide après chargement
                products.loadState.refresh is LoadState.NotLoading && products.itemCount == 0 -> {
                    EmptyView(message = "No products found")
                }
                
                // Affichage normal des produits
                else -> {
                    Column {
                        // Filtres de catégories
                        if (categories.isNotEmpty()) {
                            CategoryFilterRow(
                                categories = categories,
                                selectedCategoryId = selectedCategoryId,
                                onCategorySelected = { categoryId ->
                                    selectedCategoryId = categoryId
                                    viewModel.filterByCategory(categoryId)
                                }
                            )
                        }
                        
                        // Filtres rapides
                        QuickFiltersRow(
                            selectedSort = selectedSortOption,
                            onSortChange = { 
                                selectedSortOption = it
                                viewModel.changeSortBy(it)
                            }
                        )
                        
                        // Grille de produits avec Paging3
                        ProductGridContentPaging(
                            products = products,
                            onProductClick = { productId ->
                                navController.navigate("product_detail/$productId")
                            }
                        )
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    // Bottom Sheet des filtres
    if (showFilterSheet) {
        FilterBottomSheet(
            currentFilters = viewModel.currentFilters,
            onDismiss = { showFilterSheet = false },
            onApplyFilters = { filters ->
                viewModel.applyFilters(filters)
                showFilterSheet = false
            }
        )
    }
}

/**
 * Top Bar avec recherche
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarketplaceTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Header avec bouton retour
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }
            
            Text(
                text = "Marketplace",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filters",
                    tint = Color.Black
                )
            }
        }
        
        // Barre de recherche
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search for a product...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedContainerColor = Color(0xFFF5F5F5),
                disabledContainerColor = Color(0xFFF5F5F5),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )
    }
}

/**
 * Filtres de catégories horizontaux
 */
@Composable
private fun CategoryFilterRow(
    categories: List<com.project.e_commerce.domain.model.Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Option "Toutes les catégories"
        item {
            FilterChip(
                label = "All",
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) }
            )
        }
        
        // Catégories dynamiques
        items(categories) { category ->
            FilterChip(
                label = category.name,
                selected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) }
            )
        }
    }
}

/**
 * Filtres rapides (tri)
 */
@Composable
private fun QuickFiltersRow(
    selectedSort: String,
    onSortChange: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                label = "Commission ↑",
                selected = selectedSort == "commission",
                onClick = { onSortChange("commission") }
            )
        }
        item {
            FilterChip(
                label = "Price ↑",
                selected = selectedSort == "price_asc",
                onClick = { onSortChange("price_asc") }
            )
        }
        item {
            FilterChip(
                label = "Price ↓",
                selected = selectedSort == "price_desc",
                onClick = { onSortChange("price_desc") }
            )
        }
        item {
            FilterChip(
                label = "New Arrivals",
                selected = selectedSort == "newest",
                onClick = { onSortChange("newest") }
            )
        }
        item {
            FilterChip(
                label = "Popular",
                selected = selectedSort == "popular",
                onClick = { onSortChange("popular") }
            )
        }
    }
}

/**
 * Chip de filtre
 */
@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFFFF9800) else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(16.dp)
            ),
        color = if (selected) Color(0xFFFF9800).copy(alpha = 0.1f) else Color.White
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color(0xFFFF9800) else Color.Gray
        )
    }
}

/**
 * Grille de produits avec Paging3.
 * 
 * Utilise LazyPagingItems pour la pagination automatique et gère
 * les états de chargement au niveau des items.
 */
@Composable
private fun ProductGridContentPaging(
    products: LazyPagingItems<MarketplaceProduct>,
    onProductClick: (String) -> Unit
) {
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Items avec Paging3
        items(
            count = products.itemCount,
            key = { index -> products[index]?.id ?: index }
        ) { index ->
            products[index]?.let { product ->
                ProductCard(
                    product = product,
                    onClick = { onProductClick(product.id) }
                )
            }
        }
        
        // Indicateur de chargement en bas (load more)
        if (products.loadState.append is LoadState.Loading) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFF9800))
                }
            }
        }
        
        // Erreur lors du chargement de la page suivante
        if (products.loadState.append is LoadState.Error) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Loading error",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { products.retry() }) {
                        Text("Retry", color = Color(0xFFFF9800))
                    }
                }
            }
        }
    }
}

/**
 * Card d'un produit
 */
@Composable
private fun ProductCard(
    product: MarketplaceProduct,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Image du produit
            Box {
                ImageWithPlaceholder(
                    model = product.mainImageUrl,
                    contentDescription = product.name,
                    imageType = ImageType.PRODUCT,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Badge commission
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    color = Color(0xFFFF9800),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "🎯 ${String.format("%.1f", product.commissionRate)}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            // Infos du produit
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Nom
                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Prix
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.getFormattedPrice(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                    
                    if (product.originalPrice > product.sellingPrice) {
                        Text(
                            text = String.format("$%.2f", product.originalPrice),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            style = androidx.compose.ui.text.TextStyle(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Commission estimée
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Earn ${String.format("$%.2f", product.getEstimatedCommission())}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

/**
 * Vue de chargement
 */
@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFFFF9800))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading products...",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

/**
 * Vue vide
 */
@Composable
private fun EmptyView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 16.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Vue d'erreur
 */
@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
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
                fontWeight = FontWeight.Medium
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

/**
 * Bottom Sheet des filtres avancés
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    currentFilters: ProductFilters,
    onDismiss: () -> Unit,
    onApplyFilters: (ProductFilters) -> Unit
) {
    var minPrice by remember { mutableStateOf(currentFilters.minPrice?.toString() ?: "") }
    var maxPrice by remember { mutableStateOf(currentFilters.maxPrice?.toString() ?: "") }
    var minCommission by remember { mutableStateOf(currentFilters.minCommission?.toString() ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Advanced Filters",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Filtre prix
            Text("Price", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = minPrice,
                    onValueChange = { minPrice = it },
                    label = { Text("Min ($)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = maxPrice,
                    onValueChange = { maxPrice = it },
                    label = { Text("Max ($)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Filtre commission
            Text("Minimum commission (%)", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = minCommission,
                onValueChange = { minCommission = it },
                label = { Text("Min commission (%)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Boutons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        minPrice = ""
                        maxPrice = ""
                        minCommission = ""
                        onApplyFilters(ProductFilters())
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
                
                Button(
                    onClick = {
                        onApplyFilters(
                            currentFilters.copy(
                                minPrice = minPrice.toDoubleOrNull(),
                                maxPrice = maxPrice.toDoubleOrNull(),
                                minCommission = minCommission.toDoubleOrNull()
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Text("Apply")
                }
            }
        }
    }
}
