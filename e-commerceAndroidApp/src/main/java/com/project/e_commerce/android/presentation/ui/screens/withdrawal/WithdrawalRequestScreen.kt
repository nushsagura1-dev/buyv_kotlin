package com.project.e_commerce.android.presentation.ui.screens.withdrawal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.project.e_commerce.android.presentation.viewModel.withdrawal.WithdrawalRequestViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Phase 8: Withdrawal Request Screen
 * Form for promoters to request withdrawals
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalRequestScreen(
    navController: NavController,
    viewModel: WithdrawalRequestViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Success dialog
    if (uiState.isSuccess && uiState.submittedRequest != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Request Submitted!") },
            text = {
                Column {
                    Text("Your withdrawal request has been submitted successfully.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Amount: \$${String.format("%.2f", uiState.submittedRequest!!.amount)}",
                        fontWeight = FontWeight.Bold
                    )
                    Text("Status: Pending approval")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "You'll be notified once an admin reviews your request.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetForm()
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("OK")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Withdrawal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF9800),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Available Balance Card
            if (uiState.isLoadingStats) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = Color(0xFFFF9800)
                )
            } else {
                AvailableBalanceCard(
                    availableBalance = uiState.availableBalance,
                    stats = uiState.stats
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Amount Input
                AmountInputField(
                    amount = uiState.amount,
                    onAmountChange = { viewModel.updateAmount(it) },
                    error = uiState.amountError,
                    availableBalance = uiState.availableBalance
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Payment Method Selector
                PaymentMethodSelector(
                    selectedMethod = uiState.paymentMethod,
                    onMethodSelected = { viewModel.updatePaymentMethod(it) },
                    error = uiState.paymentMethodError
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Payment Details Form
                if (uiState.paymentMethod.isNotEmpty()) {
                    PaymentDetailsForm(
                        paymentMethod = uiState.paymentMethod,
                        paymentDetails = uiState.paymentDetails,
                        onDetailChange = { key, value -> viewModel.updatePaymentDetails(key, value) },
                        error = uiState.paymentDetailsError
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Error message
                if (uiState.error != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                uiState.error!!,
                                color = Color(0xFFD32F2F),
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Submit Button
                Button(
                    onClick = { viewModel.submitWithdrawalRequest() },
                    enabled = !uiState.isSubmitting && 
                              uiState.amount.isNotEmpty() && 
                              uiState.paymentMethod.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit Request", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Important Notes
                Spacer(modifier = Modifier.height(16.dp))
                ImportantNotes()
            }
        }
    }
}

@Composable
private fun AvailableBalanceCard(
    availableBalance: Double,
    stats: com.project.e_commerce.android.data.api.WithdrawalStatsResponse?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF2196F3), Color(0xFF1976D2))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Available Balance",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "$${String.format("%.2f", availableBalance)}",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (stats != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.White.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BalanceInfo("Pending", stats.pending_balance)
                        BalanceInfo("Withdrawn", stats.total_withdrawn)
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceInfo(label: String, amount: Double) {
    Column {
        Text(
            label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
        Text(
            "$${String.format("%.2f", amount)}",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountInputField(
    amount: String,
    onAmountChange: (String) -> Unit,
    error: String?,
    availableBalance: Double
) {
    Column {
        Text(
            "Withdrawal Amount",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter amount") },
            leadingIcon = {
                Text(
                    "$",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
            },
            trailingIcon = {
                TextButton(
                    onClick = { onAmountChange(availableBalance.toString()) }
                ) {
                    Text("Max", color = Color(0xFFFF9800))
                }
            },
            isError = error != null,
            supportingText = if (error != null) {
                { Text(error, color = Color(0xFFD32F2F)) }
            } else {
                { Text("Minimum: \$50 • Maximum: \$10,000", fontSize = 12.sp) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF9800),
                focusedLabelColor = Color(0xFFFF9800)
            )
        )
    }
}

@Composable
private fun PaymentMethodSelector(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit,
    error: String?
) {
    Column {
        Text(
            "Payment Method",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PaymentMethodCard(
                title = "PayPal",
                icon = Icons.Default.Payment,
                isSelected = selectedMethod == "paypal",
                onClick = { onMethodSelected("paypal") },
                modifier = Modifier.weight(1f)
            )
            
            PaymentMethodCard(
                title = "Bank Transfer",
                icon = Icons.Default.AccountBalance,
                isSelected = selectedMethod == "bank_transfer",
                onClick = { onMethodSelected("bank_transfer") },
                modifier = Modifier.weight(1f)
            )
        }
        
        if (error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(error, color = Color(0xFFD32F2F), fontSize = 12.sp)
        }
    }
}

@Composable
private fun PaymentMethodCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF9800)) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFFF3E0) else Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFFFF9800) else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color(0xFFFF9800) else Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentDetailsForm(
    paymentMethod: String,
    paymentDetails: Map<String, String>,
    onDetailChange: (String, String) -> Unit,
    error: String?
) {
    Column {
        Text(
            "Payment Details",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        when (paymentMethod) {
            "paypal" -> {
                OutlinedTextField(
                    value = paymentDetails["email"] ?: "",
                    onValueChange = { onDetailChange("email", it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("PayPal Email") },
                    placeholder = { Text("your@email.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF9800),
                        focusedLabelColor = Color(0xFFFF9800)
                    )
                )
            }
            "bank_transfer" -> {
                OutlinedTextField(
                    value = paymentDetails["account_holder_name"] ?: "",
                    onValueChange = { onDetailChange("account_holder_name", it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Account Holder Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF9800),
                        focusedLabelColor = Color(0xFFFF9800)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = paymentDetails["bank_name"] ?: "",
                    onValueChange = { onDetailChange("bank_name", it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bank Name") },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF9800),
                        focusedLabelColor = Color(0xFFFF9800)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = paymentDetails["account_number"] ?: "",
                    onValueChange = { onDetailChange("account_number", it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Account Number") },
                    leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF9800),
                        focusedLabelColor = Color(0xFFFF9800)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = paymentDetails["routing_number"] ?: "",
                    onValueChange = { onDetailChange("routing_number", it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Routing Number") },
                    leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF9800),
                        focusedLabelColor = Color(0xFFFF9800)
                    )
                )
            }
        }
        
        if (error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(error, color = Color(0xFFD32F2F), fontSize = 12.sp)
        }
    }
}

@Composable
private fun ImportantNotes() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFF57C00),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Important Notes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFF57C00)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "• Minimum withdrawal amount is \$50\n" +
                "• Processing time: 3-5 business days\n" +
                "• You can only have one pending request at a time\n" +
                "• Ensure payment details are accurate to avoid delays",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFF5D4037)
            )
        }
    }
}
