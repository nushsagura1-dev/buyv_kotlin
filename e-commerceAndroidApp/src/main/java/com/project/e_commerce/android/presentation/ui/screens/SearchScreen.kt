package com.project.e_commerce.android.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil3.compose.rememberAsyncImagePainter
import com.project.e_commerce.android.R
import com.project.e_commerce.domain.model.Product
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.ui.screens.RequireLoginPrompt
import com.project.e_commerce.android.presentation.ui.composable.common.ErrorView
import com.project.e_commerce.android.presentation.ui.composable.common.LoadingView
import com.project.e_commerce.android.presentation.viewModel.ProductViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavHostController, viewModel: ProductViewModel = koinViewModel()) {
    var searchQuery by remember { mutableStateOf("") }
    var showLoginPrompt by remember { mutableStateOf(false) }
    val isLoggedIn = remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    // Filter states
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var minPrice by remember { mutableStateOf<Double?>(null) }
    var maxPrice by remember { mutableStateOf<Double?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var selectedSort by remember { mutableStateOf("relevance") }
    
    // Sort options
    val sortOptions = listOf(
        "relevance" to "Relevance",
        "recent" to "Newest",
        "popular" to "Popular",
        "price_asc" to "Price ↑",
        "price_desc" to "Price ↓"
    )

    // Get products safely with error handling
    val allProducts = remember(viewModel) { 
        viewModel.allProducts
    }
    
    // Categories list
    val categories = remember(allProducts) {
        allProducts.map { it.categoryName }.distinct().sorted()
    }

    // Enhanced filtering with category, price, and sort
    val filteredProducts = remember(searchQuery, allProducts, selectedCategory, minPrice, maxPrice, selectedSort) {
        if (searchQuery.isBlank() && selectedCategory == null && minPrice == null && maxPrice == null) {
            emptyList()
        } else {
            val filtered = allProducts.filter { product ->
                val matchesSearch = if (searchQuery.isBlank()) true 
                    else product.name.contains(searchQuery, ignoreCase = true)
                
                val matchesCategory = if (selectedCategory == null) true
                    else product.categoryName == selectedCategory
                
                val matchesPriceMin = if (minPrice == null) true
                    else (product.price.toDoubleOrNull() ?: 0.0) >= minPrice!!
                
                val matchesPriceMax = if (maxPrice == null) true
                    else (product.price.toDoubleOrNull() ?: 0.0) <= maxPrice!!
                
                matchesSearch && matchesCategory && matchesPriceMin && matchesPriceMax
            }
            // Apply sort
            when (selectedSort) {
                "recent" -> filtered.sortedByDescending { it.name } // newest first
                "popular" -> filtered.sortedByDescending { it.rating }
                "price_asc" -> filtered.sortedBy { it.price.toDoubleOrNull() ?: 0.0 }
                "price_desc" -> filtered.sortedByDescending { it.price.toDoubleOrNull() ?: 0.0 }
                else -> filtered // relevance = default order
            }
        }
    }

    // Show loading view if loading
    if (isLoading) {
        LoadingView(message = "Loading products...")
        return
    }

    // Show error view if there's an error
    if (hasError) {
        ErrorView(
            message = errorMessage,
            onRetry = {
                hasError = false
                errorMessage = ""
                isLoading = true
                // Try to reload products
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar - Original Design
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Back icon
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.offset(x = (-12).dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1B7ACE),
                    modifier = Modifier.size(26.dp)
                )
            }

            // Search Box
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    androidx.compose.material3.Text(
                        "Search products...",
                        color = Color.Gray
                    )
                },
                trailingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFFFF6F00)
                    )
                },
                singleLine = true,
                maxLines = 1,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .offset(x = (-6).dp)
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0F0F0)),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFFFF6F00),
                    unfocusedIndicatorColor = Color(0xFFE0E0E0),
                    cursorColor = Color(0xFFFF6F00)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        // Filter Button and Active Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showFilters = !showFilters },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (selectedCategory != null || minPrice != null || maxPrice != null || selectedSort != "relevance") 
                        Color(0xFFFF6F00) else Color(0xFFF0F0F0)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    Icons.Default.List,
                    contentDescription = "Filters",
                    tint = if (selectedCategory != null || minPrice != null || maxPrice != null || selectedSort != "relevance") 
                        Color.White else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Filters",
                    color = if (selectedCategory != null || minPrice != null || maxPrice != null) 
                        Color.White else Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            // Clear Filters Button
            if (selectedCategory != null || minPrice != null || maxPrice != null || selectedSort != "relevance") {
                TextButton(
                    onClick = {
                        selectedCategory = null
                        minPrice = null
                        maxPrice = null
                        selectedSort = "relevance"
                    }
                ) {
                    Text("Clear All", color = Color(0xFFFF6F00), fontSize = 13.sp)
                }
            }
        }
        
        // Filter Section (Expandable)
        if (showFilters) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Category Filter
                    Text("Category", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            FilterChip(
                                text = category,
                                isSelected = selectedCategory == category,
                                onClick = {
                                    selectedCategory = if (selectedCategory == category) null else category
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Sort By
                    Text("Sort By", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortOptions) { (value, label) ->
                            FilterChip(
                                text = label,
                                isSelected = selectedSort == value,
                                onClick = { selectedSort = value }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Price Range Filter
                    Text("Price Range", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = minPrice?.toString() ?: "",
                            onValueChange = { 
                                minPrice = it.toDoubleOrNull()
                            },
                            label = { Text("Min", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = maxPrice?.toString() ?: "",
                            onValueChange = { 
                                maxPrice = it.toDoubleOrNull()
                            },
                            label = { Text("Max", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show search results or empty state
        if (searchQuery.isBlank() && selectedCategory == null && minPrice == null && maxPrice == null) {
            // Show empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Text(
                        "Search for products or use filters",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            // Show results
            Text("${filteredProducts.size} Results found", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(12.dp))

            // Simple product list 
            LazyColumn {
                items(filteredProducts, key = { it.name }) { product ->
                    EnhancedProductCard(
                        product = product,
                        onClick = {
                            navController.navigate(
                                Screens.ProductScreen.DetailsScreen.route + "/${product.name}"
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // Show login prompt if needed
    if (showLoginPrompt) {
        RequireLoginPrompt(
            onLogin = { showLoginPrompt = false },
            onSignUp = { showLoginPrompt = false },
            onDismiss = { showLoginPrompt = false }
        )
    }
}

@Composable
fun EnhancedProductCard(
    product: Product,
    onClick: () -> Unit
) {
    var isFavorite by remember { mutableStateOf(false) }

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF176DBA)),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product image
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (product.image.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(product.image),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Product",
                        tint = Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Product info
            Column(modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text(
                    text = product.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    androidx.compose.material3.Text(
                        text = product.rating.toString(),
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                androidx.compose.material3.Text(
                    text = "${product.price}$",
                    color = Color(0xFF176DBA),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Action buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Heart button
                androidx.compose.material3.IconButton(
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier
                        .background(
                            Color(0xFFE3F2FD),
                            RoundedCornerShape(8.dp)
                        )
                        .size(36.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFF176DBA) else Color(0xFF176DBA),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Cart button
                androidx.compose.material3.IconButton(
                    onClick = { /* Add to cart */ },
                    modifier = Modifier
                        .background(
                            Color(0xFF176DBA),
                            RoundedCornerShape(8.dp)
                        )
                        .size(36.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Add to cart",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun SearchScreenPreview() {
    SearchScreen(
        navController = rememberNavController(),
        viewModel = koinViewModel()
    )
}