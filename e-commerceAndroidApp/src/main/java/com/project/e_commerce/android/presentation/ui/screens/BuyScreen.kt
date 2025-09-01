package com.project.e_commerce.android.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun BuyScreen(navController: NavController) {
    val sampleProducts = remember {
        listOf(
            Product("Premium T-Shirt", "$29.99", "Comfortable cotton blend"),
            Product("Designer Jeans", "$79.99", "Classic fit denim"),
            Product("Sneakers", "$149.99", "Athletic performance shoes"),
            Product("Hoodie", "$59.99", "Cozy fleece material"),
            Product("Watch", "$199.99", "Stylish timepiece"),
            Product("Backpack", "$89.99", "Durable travel companion")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Bar
        TopAppBar(
            title = { 
                Text(
                    "Buy Products", 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ) 
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack, 
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            backgroundColor = Color(0xFFFF6F00),
            elevation = 4.dp
        )

        // Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFFFF6F00).copy(alpha = 0.1f)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = "Shopping",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFFF6F00)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Welcome to Buy Screen!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6F00),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Browse our amazing products below",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Products List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sampleProducts) { product ->
                ProductCard(product = product)
            }
        }
    }
}

@Composable
fun ProductCard(product: Product) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image Placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Color(0xFFE3F2FD),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = "Product",
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFF1976D2)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Product Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.description,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = product.price,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6F00)
                )
            }

            // Buy Button
            Button(
                onClick = { /* Handle buy action */ },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFFF6F00)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    "Buy",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class Product(
    val name: String,
    val price: String,
    val description: String
)