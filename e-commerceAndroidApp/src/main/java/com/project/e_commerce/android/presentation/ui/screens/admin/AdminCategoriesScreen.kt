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
import com.project.e_commerce.android.data.api.AdminCategoryResponse
import com.project.e_commerce.android.presentation.viewModel.admin.AdminCategoriesViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Sprint 21: Admin Categories Management Screen
 * Full CRUD for product categories with search and toggle
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AdminCategoriesScreen(navController: NavHostController) {
    val viewModel: AdminCategoriesViewModel = koinViewModel()
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<AdminCategoryResponse?>(null) }
    var showDeleteDialog by remember { mutableStateOf<AdminCategoryResponse?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullRefreshState(isLoading, { viewModel.loadCategories() })

    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories
        else categories.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    (it.name_ar?.contains(searchQuery, ignoreCase = true) == true) ||
                    it.slug.contains(searchQuery, ignoreCase = true)
        }
    }

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
                title = { Text("Category Management") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "Add", tint = Color.White)
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
                    CategoryStatChip("Total", "${categories.size}", Color(0xFF1976D2))
                    CategoryStatChip("Active", "${categories.count { it.is_active }}", Color(0xFF388E3C))
                    CategoryStatChip("Inactive", "${categories.count { !it.is_active }}", Color(0xFFD32F2F))
                }

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Search for a category...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredCategories.isEmpty() && !isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No results"
                            else "No categories",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCategories, key = { it.id }) { category ->
                            CategoryCard(
                                category = category,
                                onToggleActive = { viewModel.toggleCategoryActive(category) },
                                onEdit = { showEditDialog = category },
                                onDelete = { showDeleteDialog = category }
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

    // Create Dialog
    if (showCreateDialog) {
        CategoryFormDialog(
            title = "New Category",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, nameAr, slug, iconUrl, displayOrder, isActive ->
                viewModel.createCategory(name, nameAr, slug, iconUrl, null, displayOrder, isActive)
                showCreateDialog = false
            }
        )
    }

    // Edit Dialog
    showEditDialog?.let { category ->
        CategoryFormDialog(
            title = "Edit Category",
            initialName = category.name,
            initialNameAr = category.name_ar ?: "",
            initialSlug = category.slug,
            initialIconUrl = category.icon_url ?: "",
            initialDisplayOrder = category.display_order,
            initialIsActive = category.is_active,
            isEdit = true,
            onDismiss = { showEditDialog = null },
            onConfirm = { name, nameAr, _, iconUrl, displayOrder, isActive ->
                viewModel.updateCategory(
                    category.id, name, nameAr, iconUrl, displayOrder, isActive
                )
                showEditDialog = null
            }
        )
    }

    // Delete Confirmation
    showDeleteDialog?.let { category ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Category") },
            text = { Text("Delete '${category.name}'?\nAssociated products must be reassigned first.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(category.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CategoryStatChip(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun CategoryCard(
    category: AdminCategoryResponse,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (category.is_active) Color(0xFFFF6F00).copy(alpha = 0.15f)
                else Color.Gray.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#${category.display_order}",
                        fontWeight = FontWeight.Bold,
                        color = if (category.is_active) Color(0xFFFF6F00) else Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!category.name_ar.isNullOrBlank()) {
                    Text(
                        text = category.name_ar,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "slug: ${category.slug}",
                    fontSize = 11.sp,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }

            Switch(
                checked = category.is_active,
                onCheckedChange = { onToggleActive() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFF6F00),
                    checkedTrackColor = Color(0xFFFF6F00).copy(alpha = 0.3f)
                )
            )

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF1976D2))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
            }
        }
    }
}

@Composable
private fun CategoryFormDialog(
    title: String,
    initialName: String = "",
    initialNameAr: String = "",
    initialSlug: String = "",
    initialIconUrl: String = "",
    initialDisplayOrder: Int = 0,
    initialIsActive: Boolean = true,
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (name: String, nameAr: String?, slug: String, iconUrl: String?, displayOrder: Int, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var nameAr by remember { mutableStateOf(initialNameAr) }
    var slug by remember { mutableStateOf(initialSlug) }
    var iconUrl by remember { mutableStateOf(initialIconUrl) }
    var displayOrder by remember { mutableStateOf(initialDisplayOrder.toString()) }
    var isActive by remember { mutableStateOf(initialIsActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (!isEdit) slug = it.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
                    },
                    label = { Text("Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text("Name (Arabic)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = slug,
                    onValueChange = { slug = it },
                    label = { Text("Slug *") },
                    singleLine = true,
                    enabled = !isEdit,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = iconUrl,
                    onValueChange = { iconUrl = it },
                    label = { Text("Icon URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = displayOrder,
                    onValueChange = { displayOrder = it.filter { c -> c.isDigit() } },
                    label = { Text("Display order") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Active", modifier = Modifier.weight(1f))
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name,
                        nameAr.ifBlank { null },
                        slug,
                        iconUrl.ifBlank { null },
                        displayOrder.toIntOrNull() ?: 0,
                        isActive
                    )
                },
                enabled = name.isNotBlank() && slug.isNotBlank()
            ) { Text(if (isEdit) "Edit" else "Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
