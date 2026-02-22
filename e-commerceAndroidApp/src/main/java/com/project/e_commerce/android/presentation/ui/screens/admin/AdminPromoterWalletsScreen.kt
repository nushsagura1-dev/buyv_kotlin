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
import com.project.e_commerce.android.data.api.WithdrawalRequestResponse
import com.project.e_commerce.android.presentation.viewModel.admin.AdminWithdrawalViewModel
import com.project.e_commerce.android.presentation.viewModel.admin.AdminWithdrawalUiState
import org.koin.androidx.compose.koinViewModel

/**
 * Sprint 21: Admin Promoter Wallets & Withdrawals Screen
 * Wires to existing AdminWithdrawalViewModel for withdrawal management
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AdminPromoterWalletsScreen(navController: NavHostController) {
    val viewModel: AdminWithdrawalViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var showApproveDialog by remember { mutableStateOf<WithdrawalRequestResponse?>(null) }
    var showRejectDialog by remember { mutableStateOf<WithdrawalRequestResponse?>(null) }
    var showCompleteDialog by remember { mutableStateOf<WithdrawalRequestResponse?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullRefreshState(uiState.isLoading, { viewModel.refresh() })

    val statusFilters = listOf(
        null to "All",
        "pending" to "Pending",
        "approved" to "Approved",
        "completed" to "Completed",
        "rejected" to "Rejected"
    )

    LaunchedEffect(uiState.lastAction) {
        uiState.lastAction?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearLastAction()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(uiState.processingError) {
        uiState.processingError?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallets & Withdrawals") },
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
                    WalletStatChip("Total", "${uiState.total}", Color(0xFF1976D2))
                    WalletStatChip(
                        "Pending",
                        "${viewModel.getCountByStatus("pending")}",
                        Color(0xFFF57C00)
                    )
                    WalletStatChip(
                        "Amount",
                        String.format("$%.0f", viewModel.getTotalPendingAmount()),
                        Color(0xFFD32F2F)
                    )
                }

                // Status filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    statusFilters.forEach { (status, label) ->
                        FilterChip(
                            selected = uiState.selectedStatusFilter == status,
                            onClick = { viewModel.filterByStatus(status) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.withdrawals.isEmpty() && !uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No withdrawals", color = Color.Gray, fontSize = 16.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.withdrawals, key = { it.id }) { withdrawal ->
                            WithdrawalCard(
                                withdrawal = withdrawal,
                                isProcessing = uiState.isProcessing,
                                onApprove = { showApproveDialog = withdrawal },
                                onReject = { showRejectDialog = withdrawal },
                                onComplete = { showCompleteDialog = withdrawal }
                            )
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = uiState.isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    // Approve dialog
    showApproveDialog?.let { withdrawal ->
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showApproveDialog = null },
            title = { Text("Approve withdrawal") },
            text = {
                Column {
                    Text("Amount: $${String.format("%.2f", withdrawal.amount)}")
                    Text("Method: ${withdrawal.payment_method}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Admin notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.approveWithdrawal(withdrawal.id, notes.ifBlank { null })
                    showApproveDialog = null
                }) { Text("Approve") }
            },
            dismissButton = {
                TextButton(onClick = { showApproveDialog = null }) { Text("Cancel") }
            }
        )
    }

    // Reject dialog
    showRejectDialog?.let { withdrawal ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRejectDialog = null },
            title = { Text("Reject withdrawal") },
            text = {
                Column {
                    Text("Amount: $${String.format("%.2f", withdrawal.amount)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Rejection reason *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.rejectWithdrawal(withdrawal.id, reason)
                        showRejectDialog = null
                    },
                    enabled = reason.isNotBlank(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Reject") }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = null }) { Text("Cancel") }
            }
        )
    }

    // Complete dialog
    showCompleteDialog?.let { withdrawal ->
        var txnId by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCompleteDialog = null },
            title = { Text("Mark as completed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Amount: $${String.format("%.2f", withdrawal.amount)}")
                    OutlinedTextField(
                        value = txnId,
                        onValueChange = { txnId = it },
                        label = { Text("ID Transaction *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.completeWithdrawal(withdrawal.id, txnId, notes.ifBlank { null })
                        showCompleteDialog = null
                    },
                    enabled = txnId.isNotBlank()
                ) { Text("Complete") }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun WalletStatChip(label: String, value: String, color: Color) {
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
private fun WithdrawalCard(
    withdrawal: WithdrawalRequestResponse,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onComplete: () -> Unit
) {
    val statusColor = when (withdrawal.status) {
        "pending" -> Color(0xFFF57C00)
        "approved" -> Color(0xFF1976D2)
        "completed" -> Color(0xFF388E3C)
        "rejected" -> Color(0xFFD32F2F)
        else -> Color.Gray
    }
    val statusLabel = when (withdrawal.status) {
        "pending" -> "Pending"
        "approved" -> "Approved"
        "completed" -> "Completed"
        "rejected" -> "Rejected"
        else -> withdrawal.status
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format("%.2f", withdrawal.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
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
            WalletDetailRow("User", withdrawal.promoter_uid)
            WalletDetailRow("Method", withdrawal.payment_method)
            WalletDetailRow("Date", withdrawal.created_at.take(16))
            withdrawal.approved_at?.let { WalletDetailRow("Processed on", it.take(16)) }
            withdrawal.admin_notes?.let { WalletDetailRow("Rejection reason", it) }

            // Actions
            if (withdrawal.status == "pending" || withdrawal.status == "approved") {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (withdrawal.status == "pending") {
                        TextButton(onClick = onApprove, enabled = !isProcessing) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Approve")
                        }
                        TextButton(
                            onClick = onReject,
                            enabled = !isProcessing,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject")
                        }
                    }
                    if (withdrawal.status == "approved") {
                        TextButton(onClick = onComplete, enabled = !isProcessing) {
                            Icon(Icons.Default.Payment, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Complete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletDetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
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
