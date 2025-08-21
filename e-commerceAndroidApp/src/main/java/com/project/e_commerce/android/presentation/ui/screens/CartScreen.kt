package com.project.e_commerce.android.presentation.ui.screens

import androidx.compose.material.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Percent
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.viewModel.CartItem
import com.project.e_commerce.android.presentation.viewModel.CartState
import com.project.e_commerce.android.presentation.viewModel.CartViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CartScreen(
    navController: NavController,
    cartViewModel: CartViewModel = koinViewModel()
) {
    val state by cartViewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Cart",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF0066CC),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }

        if (state.items.isEmpty()) {
            Text(
                "Your cart is empty",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                color = Color.Gray
            )
        } else {
            state.items.forEach { item ->
                CartProductCard(
                    item = item,
                    onQuantityChange = { lineId, newQty ->
                        cartViewModel.updateQuantity(lineId, newQty)
                    },
                    onRemove = { lineId ->
                        cartViewModel.removeItem(lineId)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            CostSummary(state)

            Spacer(modifier = Modifier.height(12.dp))
            PromoCodeField()

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    navController.navigate(Screens.CartScreen.PaymentScreen.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF6600)),
                shape = RoundedCornerShape(10.dp),
                elevation = ButtonDefaults.elevation(6.dp)
            ) {
                Text("Checkout", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(86.dp))
        }
    }
}

@Composable
fun CartProductCard(
    item: CartItem,
    onQuantityChange: (String, Int) -> Unit,
    onRemove: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // صورة من الإنترنت
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2)
                if (item.size != null || item.color != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        listOfNotNull(
                            item.size?.let { "Size: $it" },
                            item.color?.let { "Color: $it" }
                        ).joinToString("  •  "),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("${item.price}$", color = Color(0xFFFF5722), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (item.quantity > 1) onQuantityChange(item.lineId, item.quantity - 1)
                    },
                    modifier = Modifier
                        .background(Color(0xFFE0E0E0), shape = CircleShape)
                        .size(28.dp)
                ) { Text("-", fontWeight = FontWeight.Bold) }

                Text(item.quantity.toString(), modifier = Modifier.padding(horizontal = 8.dp), fontSize = 16.sp)

                IconButton(
                    onClick = { onQuantityChange(item.lineId, item.quantity + 1) },
                    modifier = Modifier
                        .background(Color(0xFFE0E0E0), shape = CircleShape)
                        .size(28.dp)
                ) { Text("+", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun CostSummary(cartState: CartState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Subtotal", fontWeight = FontWeight.SemiBold)
            Text("${cartState.subtotal} $")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Shipping", fontWeight = FontWeight.SemiBold)
            Text("${cartState.shipping} $")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Tax", fontWeight = FontWeight.SemiBold)
            Text("${cartState.tax} $")
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Total", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("${cartState.total} $", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun PromoCodeField() {
    var code by remember { mutableStateOf("") }

    OutlinedTextField(
        value = code,
        onValueChange = { code = it },
        label = { Text("Enter your code") },
        trailingIcon = { Icon(Icons.Default.Percent, contentDescription = null, tint = Color(0xFF0B74DA)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewCartScreen() {
    CartScreen(navController = rememberNavController())
}
