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
import com.project.e_commerce.android.domain.model.Product
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.ui.screens.RequireLoginPrompt
import com.project.e_commerce.android.presentation.viewModel.ProductViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavHostController, viewModel: ProductViewModel = koinViewModel()) {
    var searchQuery by remember { mutableStateOf("") }
    var showLoginPrompt by remember { mutableStateOf(false) }
    val isLoggedIn = remember { mutableStateOf(false) }

    // Get products safely
    val allProducts = remember(viewModel) { viewModel.allProducts }

    // Safe filtering
    val filteredProducts = remember(searchQuery, allProducts) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allProducts.filter { product ->
                product.name.contains(searchQuery, ignoreCase = true)
            }
        }
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

        Spacer(modifier = Modifier.height(16.dp))

        // Show search results or empty state
        if (searchQuery.isBlank()) {
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
                        "Search for products",
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
                items(filteredProducts) { product ->
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