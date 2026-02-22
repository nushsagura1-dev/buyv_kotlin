package com.project.e_commerce.android.presentation.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.project.e_commerce.android.data.api.AdminAffiliateSaleResponse
import com.project.e_commerce.android.presentation.viewModel.admin.AdminAffiliateSalesViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Sprint 21: Admin Affiliate Sales Management Screen
 * View all affiliate sales, filter by status, approve/pay commissions
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AdminAffiliateSalesScreen(navController: NavHostController) {
    val viewModel: AdminAffiliateSalesViewModel = koinViewModel()
    val sales by viewModel.sales.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val selectedFilter by viewModel.selectedStatusFilter.collectAsState()

    var showPayDialog by remember { mutableStateOf<AdminAffiliateSaleResponse?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullRefreshState(isLoading, { viewModel.loadSales() })

    val statusFilters = listOf(null to "All", "pending" to "Pending", "approved" to "Approved", "paid" to "Paid")

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Affiliate Sales") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF6F00),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Stats bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SalesStatChip("Total", "${sales.size}", Color(0xFF1976D2))
                    SalesStatChip("Pending", "${viewModel.getCountByStatus("pending")}", Color(0xFFF57C00))
                    SalesStatChip("Commissions", String.format("$%.2f", viewModel.getTotalCommission()), Color(0xFF388E3C))
                }

                // Status filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    statusFilters.forEach { (status, label) ->
                        FilterChip(
                            selected = selectedFilter == status,
                            onClick = { viewModel.filterByStatus(status) },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (sales.isEmpty() && !isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No sales", color = Color.Gray, fontSize = 16.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sales, key = { it.id }) { sale ->
                            AffiliateSaleCard(
                                sale = sale,
                                onApprove = { viewModel.approveSale(sale.id) },
                                onMarkPaid = { showPayDialog = sale }
                            )
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    // Pay dialog
    showPayDialog?.let { sale ->
        var paymentRef by remember { mutableStateOf("") }
        var paymentNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPayDialog = null },
            title = { Text("Mark as paid") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Commission: $${String.format("%.2f", sale.commission_amount)}")
                    OutlinedTextField(
                        value = paymentRef,
                        onValueChange = { paymentRef = it },
                        label = { Text("Payment reference *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = paymentNotes,
                        onValueChange = { paymentNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.markAsPaid(sale.id, paymentRef, paymentNotes.ifBlank { null })
                        showPayDialog = null
                    },
                    enabled = paymentRef.isNotBlank()
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showPayDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SalesStatChip(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun AffiliateSaleCard(
    sale: AdminAffiliateSaleResponse,
    onApprove: () -> Unit,
    onMarkPaid: () -> Unit
) {
    val statusColor = when (sale.commission_status) {
        "pending" -> Color(0xFFF57C00)
        "approved" -> Color(0xFF1976D2)
        "paid" -> Color(0xFF388E3C)
        else -> Color.Gray
    }
    val statusLabel = when (sale.commission_status) {
        "pending" -> "Pending"
        "approved" -> "Approved"
        "paid" -> "Paid"
        else -> sale.commission_status
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: sale amount + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format("%.2f", sale.sale_amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    DetailRow("Commission", "$${String.format("%.2f", sale.commission_amount)} (${sale.commission_rate}%)")
                    DetailRow("Quantity", "${sale.quantity}")
                    DetailRow("Promoter", sale.promoter_user_id ?: "N/A")
                }
                Column(modifier = Modifier.weight(1f)) {
                    DetailRow("Buyer", sale.buyer_user_id)
                    DetailRow("Date", sale.created_at.take(10))
                    if (sale.payment_reference != null) {
                        DetailRow("Ref.", sale.payment_reference)
                    }
                }
            }

            // Actions
            if (sale.commission_status == "pending" || sale.commission_status == "approved") {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (sale.commission_status == "pending") {
                        TextButton(onClick = onApprove) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Approve")
                        }
                    }
                    if (sale.commission_status == "approved") {
                        TextButton(onClick = onMarkPaid) {
                            Icon(Icons.Default.Payment, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark paid")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text("$label: ", fontSize = 12.sp, color = Color.Gray)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
