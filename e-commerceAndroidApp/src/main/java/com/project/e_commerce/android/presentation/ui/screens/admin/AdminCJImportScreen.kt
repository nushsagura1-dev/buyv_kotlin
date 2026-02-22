package com.project.e_commerce.android.presentation.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.project.e_commerce.android.data.api.CJProduct
import com.project.e_commerce.android.presentation.ui.composable.common.ErrorView
import com.project.e_commerce.android.presentation.ui.composable.common.LoadingView
import com.project.e_commerce.android.presentation.viewModel.admin.AdminCJImportUiState
import com.project.e_commerce.android.presentation.viewModel.admin.AdminCJImportViewModel
import com.project.e_commerce.android.presentation.viewModel.admin.CJCategories
import org.koin.androidx.compose.koinViewModel

/**
 * Screen for importing products from CJ Dropshipping
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AdminCJImportScreen(
    navController: NavController,
    viewModel: AdminCJImportViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    
    // Detect when user scrolls to bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index == listState.layoutInfo.totalItemsCount - 1
        }
    }
    
    // Load more when reaching bottom
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !uiState.isLoadingMore && uiState.searchResults.isNotEmpty()) {
            viewModel.loadNextPage()
        }
    }
    
    // Pull to refresh state
    val pullRefreshState = rememberPullToRefreshState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            CJImportHeader(onBackClick = { navController.popBackStack() })
            
            // Search bar
            CJSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { /* Query is updated on search */ },
                onSearch = { query ->
                    viewModel.searchProducts(query, uiState.selectedCategory)
                    focusManager.clearFocus()
                },
                isLoading = uiState.isLoading
            )
            
            // Category filter chips
            CategoryFilterChips(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { category ->
                    if (uiState.searchQuery.isNotBlank()) {
                        viewModel.searchProducts(uiState.searchQuery, category)
                    }
                }
            )
            
            // Content
            when {
                uiState.isLoading -> {
                    LoadingView(message = "Searching CJ Dropshipping...")
                }
                
                uiState.error != null -> {
                    ErrorView(
                        message = uiState.error ?: "Unknown error",
                        onRetry = { 
                            viewModel.searchProducts(uiState.searchQuery, uiState.selectedCategory)
                        }
                    )
                }
                
                uiState.searchResults.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                    EmptyCJSearchView()
                }
                
                uiState.searchResults.isEmpty() -> {
                    CJWelcomeView()
                }
                
                else -> {
                    // Results list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Results count header
                        item {
                            Text(
                                text = "${uiState.totalResults} products found",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        // Product items
                        items(
                            items = uiState.searchResults,
                            key = { it.product_id }
                        ) { product ->
                            CJProductCard(
                                product = product,
                                isImporting = uiState.importingProductId == product.product_id,
                                isImported = uiState.importedProducts.any { 
                                    it.cj_product_id == product.product_id 
                                },
                                onClick = { viewModel.selectProduct(product) }
                            )
                        }
                        
                        // Loading more indicator
                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Import dialog
        if (uiState.selectedProduct != null) {
            CJImportDialog(
                product = uiState.selectedProduct!!,
                isImporting = uiState.isImporting,
                importError = uiState.importError,
                onDismiss = { viewModel.selectProduct(null) },
                onImport = { commissionRate, categoryId, description, sellingPrice ->
                    viewModel.importProduct(
                        cjProductId = uiState.selectedProduct!!.product_id,
                        commissionRate = commissionRate,
                        categoryId = categoryId,
                        customDescription = description,
                        sellingPrice = sellingPrice
                    )
                },
                onClearError = { viewModel.clearImportError() }
            )
        }
        
        // Success dialog
        if (uiState.showImportSuccess && uiState.lastImportedProduct != null) {
            ImportSuccessDialog(
                productName = uiState.lastImportedProduct!!.name,
                onDismiss = { viewModel.dismissImportSuccess() }
            )
        }
    }
}

@Composable
private fun CJImportHeader(onBackClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        color = MaterialTheme.colors.primary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CJ Import",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Import products from CJ Dropshipping",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            Icon(
                imageVector = Icons.Default.LocalShipping,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun CJSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    isLoading: Boolean
) {
    var searchText by remember { mutableStateOf(query) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search products on CJ...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { searchText = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear"
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(searchText) }),
                singleLine = true
            )
            
            Button(
                onClick = { onSearch(searchText) },
                modifier = Modifier
                    .height(56.dp)
                    .padding(end = 4.dp),
                enabled = searchText.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Search")
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterChips(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(CJCategories.categories) { (name, value) ->
            val isSelected = selectedCategory == value
            Surface(
                modifier = Modifier.clickable { onCategorySelected(value) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colors.primary else Color.White,
                elevation = if (isSelected) 4.dp else 1.dp
            ) {
                Text(
                    text = name,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = if (isSelected) Color.White else Color.DarkGray,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun CJProductCard(
    product: CJProduct,
    isImporting: Boolean,
    isImported: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isImporting && !isImported) { onClick() },
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Product image
            AsyncImage(
                model = product.product_image,
                contentDescription = product.product_name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Product name
                Text(
                    text = product.product_name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Category
                if (product.category_name != null) {
                    Text(
                        text = product.category_name,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Price row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${String.format("%.2f", product.sell_price)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    
                    if (product.original_price != null && product.original_price > product.sell_price) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$${String.format("%.2f", product.original_price)}",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Status / Action button
                when {
                    isImported -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Imported",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    isImporting -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Importing...",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    else -> {
                        Button(
                            onClick = onClick,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Import", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CJImportDialog(
    product: CJProduct,
    isImporting: Boolean,
    importError: String?,
    onDismiss: () -> Unit,
    onImport: (commissionRate: Double, categoryId: String?, description: String?, sellingPrice: Double) -> Unit,
    onClearError: () -> Unit
) {
    var commissionRate by remember { mutableStateOf("10") }
    var sellingPrice by remember { mutableStateOf(product.sell_price.toString()) }
    var customDescription by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    
    Dialog(onDismissRequest = { if (!isImporting) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Import Product",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isImporting
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                // Product preview
                Row(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = product.product_image,
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.product_name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "CJ Price: $${String.format("%.2f", product.sell_price)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selling Price Input
                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            sellingPrice = it
                        }
                    },
                    label = { Text("Selling Price ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    trailingIcon = {
                        Text("$", color = Color.Gray, modifier = Modifier.padding(end = 12.dp))
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Commission rate input
                OutlinedTextField(
                    value = commissionRate,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            commissionRate = it
                        }
                    },
                    label = { Text("Commission Rate (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        Text("%", color = Color.Gray, modifier = Modifier.padding(end = 12.dp))
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Custom description
                OutlinedTextField(
                    value = customDescription,
                    onValueChange = { customDescription = it },
                    label = { Text("Custom Description (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4
                )
                
                // Error message
                AnimatedVisibility(visible = importError != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        color = Color.Red.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = importError ?: "",
                                fontSize = 13.sp,
                                color = Color.Red,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onClearError,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isImporting
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val rate = commissionRate.toDoubleOrNull() ?: 10.0
                            val price = sellingPrice.toDoubleOrNull() ?: product.sell_price
                            val desc = customDescription.takeIf { it.isNotBlank() }
                            onImport(rate, selectedCategoryId, desc, price)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isImporting
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportSuccessDialog(
    productName: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Product Imported!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = productName,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue Importing")
                }
            }
        }
    }
}

@Composable
private fun CJWelcomeView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocalShipping,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.LightGray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "CJ Dropshipping Import",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Search for products on CJ Dropshipping and import them directly into your catalog with custom commission rates.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colors.primary.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Enter a search term above to get started",
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyCJSearchView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.LightGray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No products found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Try a different search term or category",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}
