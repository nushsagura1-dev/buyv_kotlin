package com.project.e_commerce.android.presentation.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.project.e_commerce.android.data.api.WithdrawalRequestResponse
import com.project.e_commerce.android.presentation.ui.composable.admin.ApproveDialog
import com.project.e_commerce.android.presentation.ui.composable.admin.RejectDialog
import com.project.e_commerce.android.presentation.ui.composable.admin.CompleteDialog
import com.project.e_commerce.android.presentation.ui.composable.admin.DetailRow
import com.project.e_commerce.android.presentation.ui.composable.admin.InfoItem
import com.project.e_commerce.android.utils.formatDateTime
import com.project.e_commerce.android.presentation.viewModel.admin.AdminWithdrawalViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Phase 9: Admin Withdrawal Management Screen
 * Allows admins to view, approve, reject, and complete withdrawal requests
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWithdrawalScreen(
    navController: NavController,
    viewModel: AdminWithdrawalViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Show success snackbar
    LaunchedEffect(uiState.lastAction) {
        if (uiState.lastAction != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearLastAction()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Withdrawal Management", fontWeight = FontWeight.Bold)
                        Text(
                            "${uiState.total} requests",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EA),
                    titleContentColor = Color.White
                )
            )
        },
        snackbarHost = {
            if (uiState.lastAction != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Text(uiState.lastAction!!)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stats Summary
            StatsBar(
                pendingCount = viewModel.getCountByStatus("pending"),
                approvedCount = viewModel.getCountByStatus("approved"),
                completedCount = viewModel.getCountByStatus("completed"),
                rejectedCount = viewModel.getCountByStatus("rejected"),
                totalPendingAmount = viewModel.getTotalPendingAmount()
            )
            
            // Filter Chips
            FilterChipsRow(
                selectedFilter = uiState.selectedStatusFilter,
                onFilterSelected = { viewModel.filterByStatus(it) }
            )
            
            // Withdrawal List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF6200EA))
                }
            } else if (uiState.error != null) {
                ErrorView(
                    error = uiState.error!!,
                    onRetry = { viewModel.refresh() }
                )
            } else if (uiState.withdrawals.isEmpty()) {
                EmptyView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.withdrawals, key = { it.id }) { withdrawal ->
                        WithdrawalCard(
                            withdrawal = withdrawal,
                            onClick = { viewModel.selectWithdrawal(withdrawal) }
                        )
                    }
                }
            }
        }
    }
    
    // Detail Dialog
    if (uiState.selectedWithdrawal != null) {
        WithdrawalDetailDialog(
            withdrawal = uiState.selectedWithdrawal!!,
            isProcessing = uiState.isProcessing,
            processingError = uiState.processingError,
            onDismiss = { viewModel.clearSelection() },
            onApprove = { notes -> viewModel.approveWithdrawal(uiState.selectedWithdrawal!!.id, notes) },
            onReject = { notes -> viewModel.rejectWithdrawal(uiState.selectedWithdrawal!!.id, notes) },
            onComplete = { txId, notes -> viewModel.completeWithdrawal(uiState.selectedWithdrawal!!.id, txId, notes) }
        )
    }
}

@Composable
private fun StatsBar(
    pendingCount: Int,
    approvedCount: Int,
    completedCount: Int,
    rejectedCount: Int,
    totalPendingAmount: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF6200EA), Color(0xFF9C27B0))
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "Total Pending Amount",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
                Text(
                    "$${String.format("%.2f", totalPendingAmount)}",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem("Pending", pendingCount, Color(0xFFFFA726))
                    StatItem("Approved", approvedCount, Color(0xFF66BB6A))
                    StatItem("Completed", completedCount, Color(0xFF42A5F5))
                    StatItem("Rejected", rejectedCount, Color(0xFFEF5350))
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                count.toString(),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == null,
            onClick = { onFilterSelected(null) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF6200EA),
                selectedLabelColor = Color.White
            )
        )
        FilterChip(
            selected = selectedFilter == "pending",
            onClick = { onFilterSelected("pending") },
            label = { Text("Pending") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFFFA726),
                selectedLabelColor = Color.White
            )
        )
        FilterChip(
            selected = selectedFilter == "approved",
            onClick = { onFilterSelected("approved") },
            label = { Text("Approved") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF66BB6A),
                selectedLabelColor = Color.White
            )
        )
        FilterChip(
            selected = selectedFilter == "completed",
            onClick = { onFilterSelected("completed") },
            label = { Text("Completed") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF42A5F5),
                selectedLabelColor = Color.White
            )
        )
    }
}

@Composable
private fun WithdrawalCard(
    withdrawal: WithdrawalRequestResponse,
    onClick: () -> Unit
) {
    val statusColor = when (withdrawal.status) {
        "pending" -> Color(0xFFFFA726)
        "approved" -> Color(0xFF66BB6A)
        "rejected" -> Color(0xFFEF5350)
        "completed" -> Color(0xFF42A5F5)
        else -> Color.Gray
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        withdrawal.promoter_name ?: "Unknown Promoter",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "ID: #${withdrawal.id}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        withdrawal.status.uppercase(),
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(
                    icon = Icons.Default.AttachMoney,
                    label = "Amount",
                    value = "$${String.format("%.2f", withdrawal.amount)}",
                    valueColor = Color(0xFF6200EA)
                )
                
                InfoItem(
                    icon = Icons.Default.Payment,
                    label = "Method",
                    value = withdrawal.payment_method.replace("_", " ").uppercase()
                )
                
                InfoItem(
                    icon = Icons.Default.Schedule,
                    label = "Date",
                    value = formatDate(withdrawal.created_at)
                )
            }
        }
    }
}

@Composable
private fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color.Black
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 10.sp,
            color = Color.Gray
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun ErrorView(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFEF5350)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Error",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            error,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EA))
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF66BB6A)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "All Clear!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "No withdrawal requests found",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithdrawalDetailDialog(
    withdrawal: WithdrawalRequestResponse,
    isProcessing: Boolean,
    processingError: String?,
    onDismiss: () -> Unit,
    onApprove: (String?) -> Unit,
    onReject: (String) -> Unit,
    onComplete: (String, String?) -> Unit
) {
    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Withdrawal Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status Badge
                val statusColor = when (withdrawal.status) {
                    "pending" -> Color(0xFFFFA726)
                    "approved" -> Color(0xFF66BB6A)
                    "rejected" -> Color(0xFFEF5350)
                    "completed" -> Color(0xFF42A5F5)
                    else -> Color.Gray
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        withdrawal.status.uppercase(),
                        color = statusColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Details
                DetailRow("Request ID", "#${withdrawal.id}")
                DetailRow("Promoter", withdrawal.promoter_name ?: "Unknown")
                DetailRow("Amount", "$${String.format("%.2f", withdrawal.amount)}")
                DetailRow("Payment Method", withdrawal.payment_method.replace("_", " ").uppercase())
                
                Spacer(modifier = Modifier.height(12.dp))
                Text("Payment Details", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        withdrawal.payment_details.forEach { (key, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    key.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    value,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow("Created", formatDateTime(withdrawal.created_at))
                
                if (withdrawal.approved_at != null) {
                    DetailRow("Approved", formatDateTime(withdrawal.approved_at!!))
                }
                if (withdrawal.completed_at != null) {
                    DetailRow("Completed", formatDateTime(withdrawal.completed_at!!))
                }
                if (withdrawal.rejected_at != null) {
                    DetailRow("Rejected", formatDateTime(withdrawal.rejected_at!!))
                }
                
                if (withdrawal.transaction_id != null) {
                    DetailRow("Transaction ID", withdrawal.transaction_id!!)
                }
                
                if (withdrawal.admin_notes != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Admin Notes", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))
                    ) {
                        Text(
                            withdrawal.admin_notes!!,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp
                        )
                    }
                }
                
                // Error message
                if (processingError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Text(
                            processingError,
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                when (withdrawal.status) {
                    "pending" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showApproveDialog = true },
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB6A)),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Approve")
                                }
                            }
                            Button(
                                onClick = { showRejectDialog = true },
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reject")
                            }
                        }
                    }
                    "approved" -> {
                        Button(
                            onClick = { showCompleteDialog = true },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Done, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mark as Completed")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Approve Dialog
    if (showApproveDialog) {
        ApproveDialog(
            onDismiss = { showApproveDialog = false },
            onConfirm = { notes ->
                showApproveDialog = false
                onApprove(notes)
            }
        )
    }
    
    // Reject Dialog
    if (showRejectDialog) {
        RejectDialog(
            onDismiss = { showRejectDialog = false },
            onConfirm = { notes ->
                showRejectDialog = false
                onReject(notes)
            }
        )
    }
    
    // Complete Dialog
    if (showCompleteDialog) {
        CompleteDialog(
            onDismiss = { showCompleteDialog = false },
            onConfirm = { txId, notes ->
                showCompleteDialog = false
                onComplete(txId, notes)
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = Color.Gray
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApproveDialog(
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var notes by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Approve Withdrawal") },
        text = {
            Column {
                Text("Are you sure you want to approve this withdrawal request?")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Admin Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(notes.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB6A))
            ) {
                Text("Approve")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RejectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject Withdrawal") },
        text = {
            Column {
                Text("Please provide a reason for rejection:")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = it
                        error = false
                    },
                    label = { Text("Reason (Required)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = error,
                    supportingText = if (error) {
                        { Text("Reason is required", color = Color(0xFFD32F2F)) }
                    } else null,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (notes.isBlank()) {
                        error = true
                    } else {
                        onConfirm(notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
            ) {
                Text("Reject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompleteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var transactionId by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Withdrawal") },
        text = {
            Column {
                Text("Mark this withdrawal as completed (payment sent):")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = transactionId,
                    onValueChange = {
                        transactionId = it
                        error = false
                    },
                    label = { Text("Transaction ID (Required)") },
                    placeholder = { Text("PP-123456789") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = error,
                    supportingText = if (error) {
                        { Text("Transaction ID is required", color = Color(0xFFD32F2F)) }
                    } else null,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Admin Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (transactionId.isBlank()) {
                        error = true
                    } else {
                        onConfirm(transactionId, notes.ifBlank { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5))
            ) {
                Text("Complete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDateTime.parse(dateString.substring(0, 19))
        date.format(DateTimeFormatter.ofPattern("MMM dd"))
    } catch (e: Exception) {
        dateString.substring(0, 10)
    }
}

private fun formatDateTime(dateString: String): String {
    return try {
        val date = LocalDateTime.parse(dateString.substring(0, 19))
        date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
    } catch (e: Exception) {
        dateString.substring(0, 16).replace("T", " ")
    }
}
