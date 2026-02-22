package com.project.e_commerce.android.presentation.ui.screens

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.project.e_commerce.android.R
import com.project.e_commerce.android.presentation.viewModel.CartViewModel
import com.project.e_commerce.android.presentation.viewModel.payment.PaymentViewModel
import com.project.e_commerce.android.presentation.viewModel.payment.PaymentState
import com.project.e_commerce.domain.model.Order
import com.project.e_commerce.domain.model.OrderItem
import com.project.e_commerce.domain.model.OrderStatus
import com.project.e_commerce.domain.model.Address
import com.project.e_commerce.android.domain.usecase.CreateOrderUseCase
import com.project.e_commerce.android.data.repository.TrackingRepository
import com.project.e_commerce.data.local.CurrentUserProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import android.util.Log

/**
 * Payment Screen with Stripe Payment Sheet Integration
 * Task 3.2: Real payment processing with Stripe test keys
 */
@Composable
fun StripePaymentScreen(
    navController: NavController,
    cartViewModel: CartViewModel = koinViewModel(),
    paymentViewModel: PaymentViewModel = koinViewModel(),
    createOrderUseCase: CreateOrderUseCase = koinInject(),
    currentUserProvider: CurrentUserProvider = koinInject(),
    trackingRepository: TrackingRepository = koinInject()
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val cartState by cartViewModel.state.collectAsState()
    val paymentState by paymentViewModel.paymentState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Captured payment intent ID (to attach to order for verification)
    var capturedPaymentIntentId by remember { mutableStateOf<String?>(null) }

    // Shipping address state
    var shippingName by remember { mutableStateOf("") }
    var shippingStreet by remember { mutableStateOf("") }
    var shippingCity by remember { mutableStateOf("") }
    var shippingState by remember { mutableStateOf("") }
    var shippingZip by remember { mutableStateOf("") }
    var shippingCountry by remember { mutableStateOf("US") }
    var shippingPhone by remember { mutableStateOf("") }

    // Stripe Payment Sheet launcher
    val paymentSheet = rememberPaymentSheet { paymentResult ->
        when (paymentResult) {
            is PaymentSheetResult.Completed -> {
                paymentViewModel.onPaymentSuccess()
            }
            is PaymentSheetResult.Canceled -> {
                paymentViewModel.onPaymentCancelled()
                Toast.makeText(context, "Payment cancelled", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                paymentViewModel.onPaymentFailed(paymentResult.error.message ?: "Payment failed")
                Toast.makeText(context, "Payment failed: ${paymentResult.error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Handle payment state changes
    LaunchedEffect(paymentState) {
        when (val state = paymentState) {
            is PaymentState.Success -> {
                // Capture payment intent ID for order creation / verification
                capturedPaymentIntentId = state.paymentIntent.paymentIntentId

                // Configure Stripe with publishable key from BuildConfig
                PaymentConfiguration.init(
                    context,
                    com.project.e_commerce.android.BuildConfig.STRIPE_PUBLISHABLE_KEY
                )

                // Launch Payment Sheet
                paymentSheet.presentWithPaymentIntent(
                    paymentIntentClientSecret = state.paymentIntent.clientSecret,
                    configuration = PaymentSheet.Configuration(
                        merchantDisplayName = "BuyV E-Commerce",
                        customer = PaymentSheet.CustomerConfiguration(
                            id = state.paymentIntent.customer,
                            ephemeralKeySecret = state.paymentIntent.ephemeralKey
                        ),
                        allowsDelayedPaymentMethods = true
                    )
                )
            }
            is PaymentState.PaymentCompleted -> {
                // Create order after successful payment
                coroutineScope.launch {
                    try {
                        val userId = currentUserProvider.getCurrentUserId() ?: return@launch
                        
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
                                attributes = cartItem.attributes,
                                promoterUid = cartItem.promoterUid,
                                isPromotedProduct = !cartItem.promoterUid.isNullOrBlank()
                            )
                        }

                        // Find promoter UID from first promoted cart item
                        val promoterUidForOrder = cartState.items
                            .firstOrNull { !it.promoterUid.isNullOrBlank() }?.promoterUid

                        val shippingAddress = Address(
                            id = "addr1",
                            name = shippingName.ifBlank { "Customer" },
                            street = shippingStreet.ifBlank { "N/A" },
                            city = shippingCity.ifBlank { "N/A" },
                            state = shippingState.ifBlank { "N/A" },
                            zipCode = shippingZip.ifBlank { "00000" },
                            country = shippingCountry.ifBlank { "US" },
                            phone = shippingPhone.ifBlank { "N/A" }
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
                            paymentMethod = "stripe",
                            createdAt = System.currentTimeMillis(),
                            promoterUid = promoterUidForOrder,
                            paymentIntentId = capturedPaymentIntentId
                        )

                        createOrderUseCase(order)
                            .onSuccess { orderId ->
                                // Track conversion
                                coroutineScope.launch {
                                    try {
                                        val orderIdInt = orderId.toIntOrNull() ?: 0
                                        if (orderIdInt > 0) {
                                            trackingRepository.trackConversion(orderIdInt)
                                            Log.d("StripePayment", "✅ Conversion tracked for order $orderId")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("StripePayment", "❌ Failed to track conversion: ${e.message}")
                                    }
                                }

                                // Clear cart
                                cartViewModel.clearCart()

                                // Show success message
                                Toast.makeText(context, "Order placed successfully!", Toast.LENGTH_LONG).show()

                                // Navigate back
                                navController.popBackStack()

                                // Reset payment state
                                paymentViewModel.resetPaymentState()
                            }
                            .onFailure { error ->
                                Toast.makeText(context, "Failed to create order: ${error.message}", Toast.LENGTH_LONG).show()
                                paymentViewModel.resetPaymentState()
                            }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        paymentViewModel.resetPaymentState()
                    }
                }
            }
            is PaymentState.Error -> {
                Toast.makeText(context, "Payment error: ${state.message}", Toast.LENGTH_LONG).show()
                // Reset to Idle so the button re-enables cleanly and error card shows
                // (error is shown inline in the UI, see errorMessage state below)
            }
            else -> {}
        }
    }

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
            IconButton(
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

        // Stripe Logo & Security
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            backgroundColor = Color(0xFFF5F7FA),
            elevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secure",
                    tint = Color(0xFF00A86B)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Secure Payment with Stripe",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF174378)
                    )
                    Text(
                        text = "Your payment information is encrypted and secure",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Order Summary
        Text(
            text = "Order Summary",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF174378)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Items
                cartState.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.name} x${item.quantity}",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$${String.format("%.2f", item.price * item.quantity)}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.Gray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                // Subtotal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", color = Color.Gray)
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
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Shipping Address Section
        Text(
            text = "Shipping Address",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF174378)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = shippingName,
                    onValueChange = { shippingName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = shippingStreet,
                    onValueChange = { shippingStreet = it },
                    label = { Text("Street Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shippingCity,
                        onValueChange = { shippingCity = it },
                        label = { Text("City") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = shippingState,
                        onValueChange = { shippingState = it },
                        label = { Text("State") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shippingZip,
                        onValueChange = { shippingZip = it },
                        label = { Text("ZIP Code") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = shippingCountry,
                        onValueChange = { shippingCountry = it },
                        label = { Text("Country") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = shippingPhone,
                    onValueChange = { shippingPhone = it },
                    label = { Text("Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Inline error card — shown when payment intent creation fails
        if (paymentState is PaymentState.Error) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(8.dp),
                backgroundColor = Color(0xFFFFEBEE),
                elevation = 0.dp
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Text(
                        text = "⚠️ ${(paymentState as PaymentState.Error).message}",
                        color = Color(0xFFC62828),
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.TextButton(
                        onClick = { paymentViewModel.resetPaymentState() }
                    ) {
                        androidx.compose.material3.Text("Dismiss", color = Color(0xFFC62828), fontSize = 12.sp)
                    }
                }
            }
        }

        // Pay with Stripe Button
        androidx.compose.material3.Button(
            onClick = {
                if (cartState.items.isEmpty()) {
                    Toast.makeText(context, "Cart is empty. Add items before paying.", Toast.LENGTH_LONG).show()
                    return@Button
                }

                // Convert total to cents (Stripe requires minimum 50 cents)
                val amountInCents = (cartState.total * 100).toInt()
                if (amountInCents < 50) {
                    Toast.makeText(context, "Order total too small (minimum \$0.50)", Toast.LENGTH_LONG).show()
                    return@Button
                }

                // Reset any prior error so the button shows loading cleanly
                paymentViewModel.resetPaymentState()

                coroutineScope.launch {
                    val userId = currentUserProvider.getCurrentUserId()
                    if (userId == null) {
                        Toast.makeText(context, "Please sign in to continue", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // ── MOCK PAYMENT (dev/testing) ──────────────────────────
                    // Bypasses Stripe entirely to test order & commission flow.
                    // Replace with createPaymentIntent(amountInCents) for production.
                    capturedPaymentIntentId = "mock_pi_${System.currentTimeMillis()}"
                    paymentViewModel.mockPaymentSuccess()
                    // ───────────────────────────────────────────────────────
                }
            },
            enabled = paymentState !is PaymentState.Loading && cartState.items.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF635BFF)), // Stripe purple
            shape = RoundedCornerShape(10.dp)
        ) {
            if (paymentState is PaymentState.Loading) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Text(
                    "Preparing Payment...",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                androidx.compose.material3.Text(
                    "✓ Checkout (Mock Payment)",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Test Mode Notice
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            backgroundColor = Color(0xFFFFF3CD),
            elevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "🧪 Test Mode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF856404)
                )
                Text(
                    text = "Use test card: 4242 4242 4242 4242",
                    fontSize = 12.sp,
                    color = Color(0xFF856404)
                )
                Text(
                    text = "Any future date, any 3-digit CVC",
                    fontSize = 12.sp,
                    color = Color(0xFF856404)
                )
            }
        }
    }
}
