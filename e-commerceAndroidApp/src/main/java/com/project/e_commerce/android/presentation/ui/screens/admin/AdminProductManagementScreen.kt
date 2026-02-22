package com.project.e_commerce.android.presentation.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.project.e_commerce.android.data.model.admin.AdminProduct
import com.project.e_commerce.android.presentation.ui.composable.admin.ProductCard
import com.project.e_commerce.android.presentation.ui.composable.admin.EditProductDialog
import com.project.e_commerce.android.presentation.ui.composable.admin.DeleteProductDialog
import com.project.e_commerce.android.presentation.ui.components.ImageWithPlaceholder
import com.project.e_commerce.android.presentation.ui.components.ImageType
import com.project.e_commerce.android.presentation.viewModel.admin.AdminProductViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AdminProductManagementScreen(
    navController: NavController,
    viewModel: AdminProductViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<AdminProduct?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFeaturedOnly by remember { mutableStateOf(false) }
    
    // Load products when screen opens or filters change
    LaunchedEffect(showFeaturedOnly) {
        viewModel.loadProducts(featuredOnly = showFeaturedOnly)
    }
    
    // Pull to refresh
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = { viewModel.loadProducts(featuredOnly = showFeaturedOnly) }
    )
    
    val filteredProducts = uiState.products.filter { product ->
        val matchesSearch = searchQuery.isEmpty() || 
            product.name.contains(searchQuery, ignoreCase = true)
        matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Product Management",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
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
                placeholder = { Text("Search products...") },
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
            
            // Filter chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showFeaturedOnly,
                    onClick = { showFeaturedOnly = !showFeaturedOnly },
                    label = { Text("Featured Only") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Star,
                            "Featured",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                
                Spacer(Modifier.weight(1f))
                
                Text(
                    text = "${filteredProducts.size} products",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Error loading products",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                error,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, "Dismiss")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            
            // Loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // Product list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredProducts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No products found",
                                    fontSize = 18.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                } else {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onEdit = {
                                selectedProduct = product
                                showEditDialog = true
                            },
                            onDelete = {
                                selectedProduct = product
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Edit dialog
    if (showEditDialog && selectedProduct != null) {
        EditProductDialog(
            product = selectedProduct!!,
            onDismiss = { 
                showEditDialog = false
                selectedProduct = null
            },
            onConfirm = { updatedProduct ->
                viewModel.updateProduct(
                    productId = updatedProduct.id,
                    price = updatedProduct.sellingPrice,
                    commissionRate = updatedProduct.commissionRate,
                    name = updatedProduct.name,
                    description = updatedProduct.description
                )
                showEditDialog = false
                selectedProduct = null
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && selectedProduct != null) {
        DeleteProductDialog(
            productName = selectedProduct!!.name,
            onDismiss = {
                showDeleteDialog = false
                selectedProduct = null
            },
            onConfirm = {
                viewModel.deleteProduct(productId = selectedProduct!!.id)
                showDeleteDialog = false
                selectedProduct = null
            }
        )
    }
}

@Composable
fun ProductCard(
    product: AdminProduct,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleFeatured: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product image
            ImageWithPlaceholder(
                imageUrl = product.mainImage ?: "https://via.placeholder.com/200",
                contentDescription = "Product",
                imageType = ImageType.PRODUCT,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(Modifier.width(12.dp))
            
            // Product info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (product.isFeatured) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Featured",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFFFFC107)
                        )
                    }
                }
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    text = product.categoryId ?: "No category",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                Spacer(Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Price
                    Column {
                        Text(
                            text = "Price",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "$${String.format("%.2f", product.sellingPrice)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    
                    // Commission
                    Column {
                        Text(
                            text = "Commission",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "${String.format("%.1f", product.commissionRate)}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                    }
                    
                    // Sales
                    Column {
                        Text(
                            text = "Sales",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "${product.totalSales}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(Modifier.width(8.dp))
            
            // Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF6200EE)
                    )
                }
                
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                
                IconButton(onClick = onToggleFeatured) {
                    Icon(
                        if (product.isFeatured) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Toggle Featured",
                        tint = if (product.isFeatured) Color(0xFFFFC107) else Color.Gray
                    )
                }
            }
        }
    }
}

// Local composables removed to use shared components from AdminComponents.kt
}
