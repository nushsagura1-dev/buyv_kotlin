package com.project.e_commerce.android.presentation.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.project.e_commerce.android.presentation.ui.composable.common.ErrorView
import com.project.e_commerce.android.presentation.ui.composable.common.LoadingView
import com.project.e_commerce.android.presentation.viewModel.admin.AdminCommissionViewModel
import com.project.e_commerce.domain.model.Commission
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Écran de gestion des commissions admin.
 * 
 * Permet de :
 * - Visualiser toutes les commissions avec filtrage par statut
 * - Approuver, rejeter ou marquer comme payées les commissions
 * - Voir les statistiques (total, par statut)
 * - Pull-to-refresh pour actualiser les données
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCommissionScreen(
    navController: NavController,
    viewModel: AdminCommissionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    var selectedFilter by remember { mutableStateOf("all") }

    Scaffold(
        topBar = {
            AdminCommissionHeader(onBackClick = { navController.popBackStack() })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Chips de filtrage par statut
                val stats = viewModel.getCommissionStats()
                CommissionStatsChips(
                    stats = stats,
                    selectedFilter = selectedFilter,
                    onFilterSelected = { filter ->
                        selectedFilter = filter
                        viewModel.loadCommissions(if (filter == "all") null else filter)
                    }
                )

                when {
                    uiState.isLoading -> {
                        LoadingView(message = "Loading commissions...")
                    }
                    uiState.error != null -> {
                        ErrorView(
                            message = uiState.error ?: "Unknown error",
                            onRetry = {
                                viewModel.loadCommissions(if (selectedFilter == "all") null else selectedFilter)
                            }
                        )
                    }
                    uiState.commissions.isEmpty() -> {
                        EmptyCommissionsView()
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.commissions,
                                key = { it.id }
                            ) { commission ->
                                CommissionCard(
                                    commission = commission,
                                    onCommissionClick = { viewModel.selectCommission(commission) },
                                    onStatusChange = { newStatus ->
                                        viewModel.updateCommissionStatus(commission.id, newStatus)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Header de l'écran avec bouton retour.
 */
@Composable
private fun AdminCommissionHeader(onBackClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.AttachMoney,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Commission Management",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Chips de filtrage avec statistiques.
 */
@Composable
private fun CommissionStatsChips(
    stats: Map<String, Int>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            label = "All (${stats["all"] ?: 0})",
            selected = selectedFilter == "all",
            onClick = { onFilterSelected("all") },
            color = Color(0xFF607D8B)
        )
        FilterChip(
            label = "Pending (${stats["pending"] ?: 0})",
            selected = selectedFilter == "pending",
            onClick = { onFilterSelected("pending") },
            color = Color(0xFFFF9800)
        )
        FilterChip(
            label = "Approved (${stats["approved"] ?: 0})",
            selected = selectedFilter == "approved",
            onClick = { onFilterSelected("approved") },
            color = Color(0xFF4CAF50)
        )
        FilterChip(
            label = "Rejected (${stats["rejected"] ?: 0})",
            selected = selectedFilter == "rejected",
            onClick = { onFilterSelected("rejected") },
            color = Color(0xFFF44336)
        )
        FilterChip(
            label = "Paid (${stats["paid"] ?: 0})",
            selected = selectedFilter == "paid",
            onClick = { onFilterSelected("paid") },
            color = Color(0xFF2196F3)
        )
    }
}

/**
 * Chip de filtre personnalisé.
 */
@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) color else Color(0xFFF5F5F5),
        shadowElevation = if (selected) 4.dp else 0.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color.Black
        )
    }
}

/**
 * Card affichant une commission avec menu dropdown pour changer le statut.
 */
@Composable
private fun CommissionCard(
    commission: Commission,
    onCommissionClick: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val statusColor = getStatusColor(commission.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCommissionClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Product Name et Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = commission.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Commission #${commission.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (commission.status.equals("pending", ignoreCase = true)) {
                            DropdownMenuItem(
                                text = { Text("Approve") },
                                onClick = {
                                    onStatusChange("approved")
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reject") },
                                onClick = {
                                    onStatusChange("rejected")
                                    showMenu = false
                                }
                            )
                        }
                        if (commission.status.equals("approved", ignoreCase = true)) {
                            DropdownMenuItem(
                                text = { Text("Mark as Paid") },
                                onClick = {
                                    onStatusChange("paid")
                                    showMenu = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("View Details") },
                            onClick = {
                                onCommissionClick()
                                showMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Informations de la commission
            InfoRow(label = "User ID", value = commission.userId.take(8) + "...")
            InfoRow(label = "Order ID", value = commission.orderId)
            InfoRow(label = "Product Price", value = "$${String.format("%.2f", commission.productPrice)}")
            InfoRow(label = "Commission Rate", value = "${(commission.commissionRate * 100).toInt()}%")
            InfoRow(
                label = "Commission Amount",
                value = "$${String.format("%.2f", commission.commissionAmount)}",
                valueColor = Color(0xFF4CAF50),
                valueWeight = FontWeight.Bold
            )
            InfoRow(label = "Created", value = formatDate(commission.createdAt))
            
            commission.paidAt?.let { paidAtValue ->
                InfoRow(label = "Paid At", value = formatDate(paidAtValue))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.1f),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = commission.status.uppercase(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

/**
 * Ligne d'information avec label et valeur.
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color.Black,
    valueWeight: FontWeight = FontWeight.Normal
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontWeight = valueWeight
        )
    }
}

/**
 * Vue vide quand aucune commission.
 */
@Composable
private fun EmptyCommissionsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AttachMoney,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No commissions found",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
        }
    }
}

/**
 * Retourne la couleur associée à un statut.
 */
private fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "pending" -> Color(0xFFFF9800)
        "approved" -> Color(0xFF4CAF50)
        "rejected" -> Color(0xFFF44336)
        "paid" -> Color(0xFF2196F3)
        else -> Color.Gray
    }
}

/**
 * Formate une date ISO en format lisible.
 */
private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}
