package com.project.e_commerce.android.presentation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.project.e_commerce.android.R
import com.project.e_commerce.android.presentation.viewModel.CartViewModel
import com.project.e_commerce.android.domain.model.Order
import com.project.e_commerce.android.domain.model.OrderItem
import com.project.e_commerce.android.domain.model.OrderStatus
import com.project.e_commerce.android.domain.model.Address
import com.project.e_commerce.android.domain.usecase.CreateOrderUseCase
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlinx.coroutines.launch

@Composable
fun PaymentScreen(
    navController: NavController,
    cartViewModel: CartViewModel = koinViewModel(),
    createOrderUseCase: CreateOrderUseCase = koinInject(),
    auth: FirebaseAuth = koinInject()
) {
    val cartState by cartViewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var cardHolder by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("Mastercard") }
    var isProcessing by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf<String?>(null) }

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
            androidx.compose.material3.IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-20).dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = Color(0xFF0066CC),
                )
            }

            Text(
                text = "Payment",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF0066CC),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Error Message
        showError?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = error,
                    color = Color(0xFFD32F2F),
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Payment Methods
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PaymentMethodButton(
                name = "Mastercard",
                selected = selectedMethod == "Mastercard",
                onClick = { selectedMethod = "Mastercard" },
                modifier = Modifier.weight(1f)
            )
            PaymentMethodButton(
                name = "Paypal",
                selected = selectedMethod == "Paypal",
                onClick = { selectedMethod = "Paypal" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Form Fields
        PaymentFieldWithLabel(Icons.Default.Person, "Cardholder Name", "e.g. Mohamed", cardHolder) {
            cardHolder = it
        }

        Spacer(modifier = Modifier.height(6.dp))
        PaymentFieldWithLabel(Icons.Default.CreditCard, "Card Number", "XXXX XXXX XXXX XXXX", cardNumber) {
            cardNumber = it
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            PaymentFieldWithLabel(
                Icons.Default.Lock, "Cvv", "785", cvv,
                modifier = Modifier.weight(1f)
            ) { cvv = it }

            PaymentFieldWithLabel(
                Icons.Default.DateRange, "Expiry date", "MM/YY", expiryDate,
                modifier = Modifier.weight(1f)
            ) { expiryDate = it }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cost Summary
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Order Summary",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF174378)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Items count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Items (${cartState.items.size})", color = Color.Gray)
                Text("$${String.format("%.2f", cartState.subtotal)}")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Shipping
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Shipping", color = Color.Gray)
                Text("$${String.format("%.2f", cartState.shipping)}")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tax
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tax", color = Color.Gray)
                Text("$${String.format("%.2f", cartState.tax)}")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF174378)
                )
                Text(
                    text = "$${String.format("%.2f", cartState.total)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFFF6F00)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (cartState.items.isEmpty()) {
                    showError = "Cart is empty"
                    return@Button
                }

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    showError = "User not authenticated"
                    return@Button
                }

                // Basic validation
                if (cardHolder.isBlank() || cardNumber.isBlank() || cvv.isBlank() || expiryDate.isBlank()) {
                    showError = "Please fill all payment fields"
                    return@Button
                }

                isProcessing = true
                showError = null

                coroutineScope.launch {
                    try {
                        // Create order from cart
                        val orderItems = cartState.items.map { cartItem ->
                            OrderItem(
                                id = cartItem.lineId,
                                productId = cartItem.productId,
                                productName = cartItem.name,
                                productImage = cartItem.imageUrl,
                                price = cartItem.price,
                                quantity = cartItem.quantity,
                                size = cartItem.size,
                                color = cartItem.color,
                                attributes = cartItem.attributes
                            )
                        }

                        // Create sample address (in real app, this would come from user input)
                        val shippingAddress = Address(
                            id = "addr1",
                            name = cardHolder,
                            street = "123 Main St",
                            city = "Sample City",
                            state = "Sample State",
                            zipCode = "12345",
                            country = "Sample Country",
                            phone = "123-456-7890"
                        )

                        val order = Order(
                            userId = userId,
                            items = orderItems,
                            status = OrderStatus.PENDING,
                            subtotal = cartState.subtotal,
                            shipping = cartState.shipping,
                            tax = cartState.tax,
                            total = cartState.total,
                            shippingAddress = shippingAddress,
                            paymentMethod = selectedMethod,
                            createdAt = com.google.firebase.Timestamp.now()
                        )

                        // Create the order
                        createOrderUseCase(order)
                            .onSuccess { orderId ->
                                // Clear the cart after successful order creation
                                cartViewModel.clearCart()

                                // Navigate back or to order confirmation
                                navController.popBackStack()

                                // You could navigate to order tracking or confirmation screen here
                                // navController.navigate("order_confirmation/$orderId")
                            }
                            .onFailure { error ->
                                showError = "Failed to create order: ${error.message}"
                            }
                    } catch (e: Exception) {
                        showError = "Payment failed: ${e.message}"
                    } finally {
                        isProcessing = false
                    }
                }
            },
            enabled = !isProcessing && cartState.items.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF6600)),
            shape = RoundedCornerShape(10.dp),
            elevation = ButtonDefaults.elevation(6.dp)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Processing...",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text("Pay Now", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PaymentFieldWithLabel(
    icon: ImageVector,
    label: String,
    placeholder: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onValueChange: (String) -> Unit
) {
    Column(modifier = modifier.padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF0B74DA))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF174378))
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF1B7ACE),
                unfocusedBorderColor = Color(0xFFB3C1D1),
                cursorColor = Color(0xFF174378),
                backgroundColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@Composable
fun PaymentMethodButton(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) Color(0xFF0B74DA) else Color.White
    val textColor = if (selected) Color.White else Color.Black
    val borderColor = if (selected) Color.Transparent else Color(0xFF0B74DA)

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = bgColor),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.height(48.dp)
    ) {
        Image(
            painter = painterResource(
                id = if (name == "Mastercard") R.drawable.mastercard_logo else R.drawable.paypal_logo
            ),
            contentDescription = name,
            modifier = Modifier
                .width(24.dp)
                .height(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(name, color = textColor, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPaymentScreen() {
    PaymentScreen(navController = rememberNavController())
}
