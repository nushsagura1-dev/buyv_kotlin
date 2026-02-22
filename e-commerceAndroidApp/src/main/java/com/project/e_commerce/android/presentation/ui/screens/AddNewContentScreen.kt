package com.project.e_commerce.android.presentation.ui.screens

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.domain.usecase.post.CreatePostUseCase
import com.project.e_commerce.data.remote.api.MarketplaceApiService
import com.project.e_commerce.domain.model.marketplace.CreatePromotionRequest
import com.project.e_commerce.domain.model.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.project.e_commerce.android.R
import com.project.e_commerce.android.data.remote.CloudinaryConfig
import com.project.e_commerce.android.presentation.ui.screens.marketplace.components.ProductSelectionBottomSheet
import com.project.e_commerce.domain.model.marketplace.MarketplaceProduct
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.utils.ObjectUtils
import android.util.Log as AndroidLog

data class SizeEntry(
    var size: String = "",
    var colors: MutableList<ColorEntry> = mutableListOf()
)

data class ColorEntry(
    var color: String = "",
    var quantity: String = ""
)

data class CategoryBehavior(
    val units: List<String>,
    val enableSize: Boolean,
    val enableColor: Boolean
)

// Helper function to upload images to Cloudinary and save content to backend
private fun uploadImagesToCloudinary(
    videoUrl: String, 
    createPostUseCase: CreatePostUseCase,
    marketplaceApi: MarketplaceApiService,
    context: android.content.Context,
    productImageUris: List<Uri>,
    selectedCategory: String,
    description: String,
    productName: String,
    productPrice: String,
    productQuantity: String,
    reelTitle: String,
    productTags: String,
    sizes: List<String>,
    colorQuantities: Map<String, MutableMap<String, String>>,
    navController: NavHostController,
    marketplaceProduct: MarketplaceProduct? = null,
    currentUserProvider: CurrentUserProvider,
    onPostCreated: (() -> Unit)? = null,
    onStepUpdate: ((String) -> Unit)? = null,
    onComplete: (() -> Unit)? = null,
    onFailed: (() -> Unit)? = null
) {
    Log.d("UPLOAD_DEBUG", "🚀 Starting upload process:")
    Log.d("UPLOAD_DEBUG", "   Video URL: $videoUrl")
    Log.d("UPLOAD_DEBUG", "   Product Name: $productName")
    Log.d("UPLOAD_DEBUG", "   Reel Title: $reelTitle")
    Log.d("UPLOAD_DEBUG", "   Category: $selectedCategory")
    Log.d("UPLOAD_DEBUG", "   Images count: ${productImageUris.size}")
    
    // ✅ MIGRATED: Get currentUserId from CurrentUserProvider
    val currentUserId: String? = try {
        kotlinx.coroutines.runBlocking {
            currentUserProvider.getCurrentUserId()
        }
    } catch (e: Exception) {
        Log.e("UPLOAD_DEBUG", "Failed to get current user: ${e.message}")
        null
    }
    
    Log.d("UPLOAD_DEBUG", "   Current user: $currentUserId")
    
    if (currentUserId == null) {
        Toast.makeText(context, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show()
        onFailed?.invoke()
        return
    }
    
    if (productImageUris.isEmpty()) {
        // No images to upload, save content directly to backend
        onStepUpdate?.invoke("Saving reel...")
        saveContentToBackend(
            videoUrl, emptyList(), createPostUseCase, marketplaceApi, context,
            selectedCategory, description, productName, productPrice,
            productQuantity, reelTitle, productTags, sizes, colorQuantities, 
            onPostCreated = onPostCreated,
            marketplaceProduct = marketplaceProduct,
            navController = navController,
            currentUserProvider = currentUserProvider,
            onComplete = onComplete,
            onFailed = onFailed
        )
        return
    }
    
    var uploadedImages = 0
    val imageUrls = mutableListOf<String>()
    val totalImages = productImageUris.size
    
    onStepUpdate?.invoke("Uploading images (0/$totalImages)...")
    
    productImageUris.forEach { uri ->
        MediaManager.get().upload(uri)
            .unsigned(CloudinaryConfig.UPLOAD_PRESET)
            .option("public_id", "products/${System.currentTimeMillis()}_${uri.lastPathSegment}")
            .option("folder", CloudinaryConfig.Folders.PRODUCTS)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    // Upload started
                }
                
                override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                    val imageUrl = resultData["secure_url"] as String
                    imageUrls.add(imageUrl)
                    uploadedImages++
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onStepUpdate?.invoke("Uploading images ($uploadedImages/$totalImages)...")
                    }
                    
                    if (uploadedImages == totalImages) {
                        // All images uploaded, save content to backend
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            onStepUpdate?.invoke("Saving reel...")
                        }
                        saveContentToBackend(
                            videoUrl, imageUrls, createPostUseCase, marketplaceApi, context,
                            selectedCategory, description, productName, productPrice,
                            productQuantity, reelTitle, productTags, sizes, colorQuantities, 
                            onPostCreated = onPostCreated,
                            marketplaceProduct = marketplaceProduct,
                            navController = navController,
                            currentUserProvider = currentUserProvider,
                            onComplete = onComplete,
                            onFailed = onFailed
                        )
                    }
                }
                
                override fun onError(requestId: String, error: ErrorInfo) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(context, "Image upload failed: ${error.description}", Toast.LENGTH_SHORT).show()
                        onFailed?.invoke()
                    }
                }
                
                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    Toast.makeText(context, "Rescheduling image upload", Toast.LENGTH_SHORT).show()
                }
                
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    // Progress tracked via onSuccess count
                }
            }).dispatch()
    }
}

/**
 * Helper function to save content to backend (migrated from Firebase).
 * Creates a post via CreatePostUseCase and links to marketplace product if provided.
 */
private fun saveContentToBackend(
    videoUrl: String, 
    imageUrls: List<String>, 
    createPostUseCase: CreatePostUseCase,
    marketplaceApi: MarketplaceApiService,
    context: android.content.Context,
    selectedCategory: String,
    description: String,
    productName: String,
    productPrice: String,
    productQuantity: String,
    reelTitle: String,
    productTags: String,
    sizes: List<String>,
    colorQuantities: Map<String, MutableMap<String, String>>,
    onPostCreated: (() -> Unit)? = null,
    marketplaceProduct: MarketplaceProduct? = null,
    navController: NavHostController,
    currentUserProvider: CurrentUserProvider,
    onComplete: (() -> Unit)? = null,
    onFailed: (() -> Unit)? = null
) {
    Log.d("SAVE_DEBUG", "🚀 Saving content to backend...")
    Log.d("SAVE_DEBUG", "   Video URL: $videoUrl")
    Log.d("SAVE_DEBUG", "   Reel Title: $reelTitle")
    Log.d("SAVE_DEBUG", "   Marketplace Product: ${marketplaceProduct?.name ?: "None"}")
    
    // Build caption with all relevant info
    val caption = buildString {
        if (reelTitle.isNotBlank()) append(reelTitle)
        if (description.isNotBlank()) {
            if (isNotEmpty()) append(" - ")
            append(description)
        }
        if (productTags.isNotBlank()) {
            if (isNotEmpty()) append(" ")
            append(productTags.split(",").joinToString(" ") { "#${it.trim()}" })
        }
    }
    
    // Create post via backend
    CoroutineScope(Dispatchers.Main).launch {
        try {
            val result = createPostUseCase(
                type = "reel",
                mediaUrl = videoUrl,
                caption = caption.ifBlank { null }
            )
            
            when (result) {
                is Result.Success -> {
                    val createdPost = result.data
                    Log.d("SAVE_DEBUG", "✅ Post created successfully with ID: ${createdPost.id}")
                    
                    // If marketplace product is linked, create promotion
                    if (marketplaceProduct != null) {
                        try {
                            onComplete.let {
                                // update step before linking
                            }
                            val promotion = marketplaceApi.createPromotion(
                                CreatePromotionRequest(
                                    postId = createdPost.id,
                                    productId = marketplaceProduct.id
                                )
                            )
                            Log.d("SAVE_DEBUG", "✅ Promotion created: Post ${createdPost.id} → Product ${marketplaceProduct.id}")
                            Toast.makeText(context, "Reel added and linked to product successfully", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.e("SAVE_DEBUG", "⚠️ Post created but failed to link product: ${e.message}")
                            Toast.makeText(context, "Reel added but failed to link product", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Reel added successfully", Toast.LENGTH_SHORT).show()
                    }
                    
                    onComplete?.invoke()
                    onPostCreated?.invoke()
                    navController.popBackStack()
                }
                is Result.Error -> {
                    Log.e("SAVE_DEBUG", "❌ Failed to create post: ${result.error}")
                    Toast.makeText(context, "Failed to create content: ${result.error}", Toast.LENGTH_SHORT).show()
                    onFailed?.invoke()
                }
                is Result.Loading -> {
                    // Should not happen, but handle gracefully
                    Log.d("SAVE_DEBUG", "⏳ Creating post...")
                }
            }
        } catch (e: Exception) {
            Log.e("SAVE_DEBUG", "❌ Exception creating post: ${e.message}")
            Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            onFailed?.invoke()
        }
    }
}

@Composable
fun AddNewContentScreen(navController: NavHostController, preSelectedProductId: String? = null, onPostCreated: (() -> Unit)? = null) {
    // ✅ MIGRATED: Get dependencies from Koin in Composable context
    val currentUserProvider: CurrentUserProvider = koinInject()
    val createPostUseCase: CreatePostUseCase = koinInject()
    val marketplaceApi: MarketplaceApiService = koinInject()
    
    var reelTitle by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productQuantity by remember { mutableStateOf("") }
    var productTags by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }

    val reelVideoUri = remember { mutableStateOf<Uri?>(null) }
    val productImageUris = remember { mutableStateListOf<Uri>() }
    
    // NEW: Marketplace product selection (Phase 5)
    var selectedMarketplaceProduct by remember { mutableStateOf<MarketplaceProduct?>(null) }
    var showProductSelectionSheet by remember { mutableStateOf(false) }

    // Upload progress state
    var isUploading by remember { mutableStateOf(false) }
    var uploadStep by remember { mutableStateOf("") }

    // Pre-load product if navigating from Promote button
    LaunchedEffect(preSelectedProductId) {
        if (preSelectedProductId != null && selectedMarketplaceProduct == null) {
            try {
                val product = marketplaceApi.getProduct(preSelectedProductId)
                selectedMarketplaceProduct = product
                // Auto-fill form fields from the product
                productName = product.name
                description = product.description ?: product.shortDescription ?: ""
                productPrice = product.sellingPrice.toString()
                selectedCategory = product.categoryName ?: "Electronics"
                AndroidLog.d("AddNewContent", "Pre-selected product loaded: ${product.name}")
            } catch (e: Exception) {
                AndroidLog.e("AddNewContent", "Failed to load pre-selected product: ${e.message}")
            }
        }
    }

    // Auto-fill when user selects a product via ProductSelectionBottomSheet
    LaunchedEffect(selectedMarketplaceProduct) {
        selectedMarketplaceProduct?.let { product ->
            productName = product.name
            if (description.isBlank()) description = product.description ?: product.shortDescription ?: ""
            productPrice = product.sellingPrice.toString()
            if (selectedCategory.isBlank()) selectedCategory = product.categoryName ?: "Electronics"
        }
    }

    val reelLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        reelVideoUri.value = uri
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris != null) {
            val uniqueUris = uris.filterNot { it in productImageUris }
            productImageUris.addAll(uniqueUris)
        }
    }

    val categoryBehaviors = mapOf(
        "Perfumes" to CategoryBehavior(units = emptyList(), enableSize = true, enableColor = false),
        "Clothing" to CategoryBehavior(units = listOf("XS", "S", "M", "L", "XL"), enableSize = true, enableColor = true),
        "Furniture" to CategoryBehavior(units = emptyList(), enableSize = true, enableColor = false),
        "Electronics" to CategoryBehavior(units = emptyList(), enableSize = false, enableColor = false),
        "Accessories" to CategoryBehavior(units = listOf("one size"), enableSize = true, enableColor = true)
    )

    // ↓↓↓ Category/size definitions without repeating variable names ↓↓↓
    var selectedCategoryBehavior by remember { mutableStateOf(CategoryBehavior(emptyList(), false, false)) }
    var sizes by remember { mutableStateOf(mutableListOf<String>()) }
    val colorQuantities = remember { mutableStateMapOf<String, MutableMap<String, String>>() }
    val colorOptions = listOf("Red", "Blue", "Black", "White", "Yellow")

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .imePadding()
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
                    modifier = Modifier.padding(10.dp)
                )
            }

            androidx.compose.material.Text(
                text = if (selectedMarketplaceProduct != null) "Promote Product" else "Add New Product ",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF0066CC),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Upload Reel
        Text("Upload Product Reel", fontWeight = FontWeight.SemiBold, color = Color(0xFF0066CC))
        Spacer(Modifier.height(8.dp))
        if (reelVideoUri.value != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = reelTitle,
                        onValueChange = { reelTitle = it },
                        placeholder = { Text("Add description...", color = Color.LightGray) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            backgroundColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { reelTitle += "#" },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF0F0F0)),
                        shape = RoundedCornerShape(8.dp),
                        elevation = null,
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    ) {
                        Text("# Hashtags", color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(width = 140.dp, height = 200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE0E0E0))
                ) {
                    val bitmap = remember(reelVideoUri.value) {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, reelVideoUri.value)
                        val frame = retriever.getFrameAtTime(1_000_000)
                        retriever.release()
                        frame
                    }

                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        "Preview",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    )

                    IconButton(
                        onClick = { reelVideoUri.value = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 4.dp)
                    ) {
                        androidx.compose.material.Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color.Red
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        } else {
            UploadBox(
                height = 160.dp,
                text = "Upload video",
                buttonText = "Browse files",
                note = "Max 60 seconds, MP4/MOV, Max size 50MB",
                onClick = { reelLauncher.launch("video/*") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Upload Product Images
        Text("Upload Product Images", fontWeight = FontWeight.SemiBold, color = Color(0xFF0066CC))
        Spacer(Modifier.height(8.dp))
        if (productImageUris.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(productImageUris.size, key = { productImageUris[it].toString() }) { index ->
                    SelectedImage(uri = productImageUris[index], onRemove = { productImageUris.removeAt(index) })
                }

                item {
                    Button(
                        onClick = { imageLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1B7ACE)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(vertical = 25.dp)
                    ) {
                        Text("+ Add", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        } else {
            UploadBox(
                height = 160.dp,
                text = "Upload photos",
                buttonText = "Browse files",
                note = "Format: .jpeg, .png & Max file size: 25 MB",
                onClick = { imageLauncher.launch("image/*") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description / caption (always visible)
        CustomOutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = if (selectedMarketplaceProduct != null) "Your Caption" else "Description",
            placeholder = if (selectedMarketplaceProduct != null) "Add a caption for your reel..." else "Add description",
            minLines = 4
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Only show product details fields when NO marketplace product is selected
        // When a marketplace product is linked, all info comes from the product automatically
        if (selectedMarketplaceProduct == null) {
            // Product fields
            CustomOutlinedTextField(value = productName, onValueChange = { productName = it }, label = "Product Name", placeholder = "Enter product name")
            Spacer(modifier = Modifier.height(8.dp))

            // Category
            Text("Category", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0066CC))
            Spacer(modifier = Modifier.height(4.dp))

            CategoryDropdown(
                selectedCategory = selectedCategory,
                onCategorySelected = {
                    selectedCategory = it
                    selectedCategoryBehavior = categoryBehaviors[it] ?: CategoryBehavior(emptyList(), false, false)
                    sizes.clear()
                    colorQuantities.clear()
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (selectedCategoryBehavior.enableSize && selectedCategoryBehavior.enableColor) {
                // Clothing/Accessories: size + color
                var selectedSizeStr by remember { mutableStateOf("") }
                DropdownWithStyle(
                    label = "Select Size",
                    options = selectedCategoryBehavior.units,
                    selectedOption = selectedSizeStr,
                    onOptionSelected = { selectedSizeStr = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (selectedSizeStr.isNotBlank()) {
                    var selectedColor by remember { mutableStateOf("") }
                    var quantityInput by remember { mutableStateOf("") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DropdownWithStyle(
                            label = "Select Color",
                            options = colorOptions,
                            selectedOption = selectedColor,
                            onOptionSelected = { selectedColor = it },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = quantityInput,
                            onValueChange = { if (it.all(Char::isDigit)) quantityInput = it },
                            placeholder = { Text("Qty") },
                            modifier = Modifier.weight(0.6f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF1B7ACE),
                                unfocusedBorderColor = Color(0xFFB3C1D1),
                                disabledBorderColor = Color(0xFFB3C1D1),
                                backgroundColor = Color.White,
                                disabledTextColor = Color.Black,
                                disabledPlaceholderColor = Color(0xFFB3C1D1)
                            ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF6F00)),
                            onClick = {
                                if (selectedColor.isNotBlank() && quantityInput.isNotBlank()) {
                                    if (!sizes.contains(selectedSizeStr)) sizes.add(selectedSizeStr)
                                    if (colorQuantities[selectedSizeStr] == null) colorQuantities[selectedSizeStr] = mutableMapOf()
                                    colorQuantities[selectedSizeStr]?.put(selectedColor, quantityInput)
                                    selectedColor = ""
                                    quantityInput = ""
                                }
                            }
                        ) { Text("Add", color = Color.White) }
                    }
                }
            } else if (selectedCategoryBehavior.enableSize) {
                // Perfumes/Furniture ...: size only
                Spacer(modifier = Modifier.height(8.dp))
                var customSize by remember { mutableStateOf("") }
                var quantityInput by remember { mutableStateOf("") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customSize,
                        onValueChange = { if (it.all(Char::isDigit)) customSize = it },
                        placeholder = { Text("Size (ml)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1B7ACE),
                            unfocusedBorderColor = Color(0xFFB3C1D1),
                            disabledBorderColor = Color(0xFFB3C1D1),
                            backgroundColor = Color.White,
                            disabledTextColor = Color.Black,
                            disabledPlaceholderColor = Color(0xFFB3C1D1)
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { if (it.all(Char::isDigit)) quantityInput = it },
                        placeholder = { Text("Qty") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1B7ACE),
                            unfocusedBorderColor = Color(0xFFB3C1D1),
                            disabledBorderColor = Color(0xFFB3C1D1),
                            backgroundColor = Color.White,
                            disabledTextColor = Color.Black,
                            disabledPlaceholderColor = Color(0xFFB3C1D1)
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF6F00)),
                        onClick = {
                            if (customSize.isNotBlank() && quantityInput.isNotBlank()) {
                                sizes.add(customSize)
                                // Store quantity with empty key "" for display compatibility
                                colorQuantities[customSize] = mutableMapOf("" to quantityInput)
                                customSize = ""
                                quantityInput = ""
                            }
                        }
                    ) { Text("Add", color = Color.White) }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Display summary
            sizes.forEach { size ->
                if (!selectedCategoryBehavior.enableColor) {
                    val qty = colorQuantities[size]?.get("") ?: ""
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, Color(0xFFB3C1D1), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Size: ", fontWeight = FontWeight.SemiBold, color = Color(0xFF555555))
                        Text(size, modifier = Modifier.padding(horizontal = 4.dp), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        Text("Qty: ", color = Color.Gray)
                        Text(qty, color = Color(0xFF0066CC), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                sizes.remove(size)
                                colorQuantities.remove(size)
                            }
                        ) { Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red) }
                    }
                } else {
                    colorQuantities[size]?.forEach { (color, qty) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .border(1.dp, Color(0xFFB3C1D1), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Size: $size", fontWeight = FontWeight.Medium, color = Color(0xFF174378))
                            Spacer(Modifier.width(12.dp))
                            Text("Color: $color", fontWeight = FontWeight.Normal, color = Color(0xFF238576))
                            Spacer(Modifier.width(12.dp))
                            Text("Qty: $qty", fontWeight = FontWeight.Bold, color = Color(0xFF0066CC))
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    colorQuantities[size]?.remove(color)
                                    if (colorQuantities[size]?.isEmpty() == true) {
                                        sizes.remove(size)
                                        colorQuantities.remove(size)
                                    }
                                }
                            ) { Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red) }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!selectedCategoryBehavior.enableSize && !selectedCategoryBehavior.enableColor) {
                Spacer(modifier = Modifier.height(8.dp))
                CustomOutlinedTextField(
                    value = productQuantity,
                    onValueChange = { productQuantity = it },
                    label = "Quantity",
                    placeholder = "Enter quantity"
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Price
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomOutlinedTextField(
                    value = productPrice,
                    onValueChange = { productPrice = it },
                    label = "Price",
                    placeholder = "Enter price",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        } // end if (selectedMarketplaceProduct == null)

        // ========= NEW: Marketplace Product Selection (Phase 5 - REQUIRED) =========
        Text(
            text = "Marketplace Product to promote *",
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFF9800),
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Info message
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFFF3E0),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "You must select a product to promote from our CJ marketplace. You will earn a commission on every sale generated by this Reel!",
                    fontSize = 12.sp,
                    color = Color(0xFFE65100),
                    lineHeight = 16.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Selected product display or selection button
        if (selectedMarketplaceProduct != null) {
            // Show the selected product
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFFF9800), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFF9800).copy(alpha = 0.05f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = selectedMarketplaceProduct!!.mainImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedMarketplaceProduct!!.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            maxLines = 2
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = selectedMarketplaceProduct!!.getFormattedPrice(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                        
                        Text(
                            text = "Earn: ${String.format("%.1f", selectedMarketplaceProduct!!.commissionRate)}% (${selectedMarketplaceProduct!!.getFormattedCommission()})",
                            fontSize = 11.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        IconButton(onClick = { showProductSelectionSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Change",
                                tint = Color(0xFFFF9800)
                            )
                        }
                        Text(
                            text = "Change",
                            fontSize = 10.sp,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }
        } else {
            // Button to select a product
            OutlinedButton(
                onClick = { showProductSelectionSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF9800)
                ),
                border = BorderStroke(2.dp, Color(0xFFFF9800))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_products_filled),
                    contentDescription = null,
                    tint = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select a Marketplace Product",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(30.dp))

        // ========= Upload Progress Indicator =========
        if (isUploading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFF6F00),
                    backgroundColor = Color(0xFFFFE0B2)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = uploadStep.ifBlank { "Uploading..." },
                    color = Color(0xFF555555),
                    fontSize = 13.sp
                )
            }
        }

        // ========= Submit (Upload to Cloudinary + Save to Backend) =========
        Button(
            onClick = {
                // Validation: Marketplace product required
                if (selectedMarketplaceProduct == null) {
                    Toast.makeText(context, "⚠️ You must select a Marketplace product before publishing", Toast.LENGTH_LONG).show()
                    return@Button
                }
                
                val reelUri = reelVideoUri.value
                if (reelUri == null) {
                    Toast.makeText(context, "Please select a reel video", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                // Only validate product details when no marketplace product is linked
                if (selectedMarketplaceProduct == null && (productName.isBlank() || selectedCategory.isBlank() || productPrice.isBlank())) {
                    Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // Start upload — show progress indicator
                isUploading = true
                uploadStep = "Uploading video..."

                // 1) Upload the video to Cloudinary using unsigned upload with preset
                MediaManager.get().upload(reelUri)
                    .unsigned(CloudinaryConfig.UPLOAD_PRESET)
                    .option("public_id", "reels/${System.currentTimeMillis()}")
                    .option("folder", CloudinaryConfig.Folders.REELS)
                    .option("resource_type", "video")
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                uploadStep = "Uploading video..."
                            }
                        }
                        
                        override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                            val videoUrl = resultData["secure_url"] as String
                            
                            // Use Handler to post to main thread
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                uploadStep = if (productImageUris.isNotEmpty()) "Uploading images..." else "Saving reel..."
                                // 2) Upload images to Cloudinary and save to backend
                                uploadImagesToCloudinary(
                                    videoUrl, createPostUseCase, marketplaceApi, context, productImageUris,
                                    selectedCategory, description, productName, productPrice,
                                    productQuantity, reelTitle, productTags, sizes, colorQuantities, navController,
                                    selectedMarketplaceProduct,
                                    currentUserProvider,
                                    onPostCreated = onPostCreated,
                                    onStepUpdate = { step -> uploadStep = step },
                                    onComplete = { isUploading = false },
                                    onFailed = { isUploading = false }
                                )
                            }
                        }
                        
                        override fun onError(requestId: String, error: ErrorInfo) {
                            Log.e("UPLOAD_DEBUG", "❌ Video upload FAILED: code=${error.code}, desc=${error.description}")
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                isUploading = false
                                Toast.makeText(context, "Video upload failed: ${error.description}", Toast.LENGTH_LONG).show()
                            }
                        }
                        
                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                Toast.makeText(context, "Rescheduling video upload", Toast.LENGTH_SHORT).show()
                            }
                        }
                        
                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            val percent = if (totalBytes > 0) (bytes * 100 / totalBytes).toInt() else 0
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                uploadStep = "Uploading video ($percent%)..."
                            }
                        }
                    }).dispatch()
            },
            enabled = !isUploading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (isUploading) Color(0xFFBBBBBB) else Color(0xFFFF6F00),
                disabledBackgroundColor = Color(0xFFBBBBBB)
            ),
            elevation = ButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isUploading) "Uploading..." else "Submit", color = Color.White, fontWeight = Bold)
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
    
    // NEW: Product Selection Bottom Sheet (Phase 5)
    if (showProductSelectionSheet) {
        ProductSelectionBottomSheet(
            onDismiss = { showProductSelectionSheet = false },
            onProductSelected = { product ->
                selectedMarketplaceProduct = product
                showProductSelectionSheet = false
                Toast.makeText(context, "✅ Product selected: ${product.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun DropdownWithStyle(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.clickable { expanded = true }) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            placeholder = { Text(label, color = Color(0xFFB3C1D1)) },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.rotate(if (expanded) 180f else 0f))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF1B7ACE),
                unfocusedBorderColor = Color(0xFFB3C1D1),
                disabledBorderColor = Color(0xFFB3C1D1),
                backgroundColor = Color.White,
                disabledTextColor = Color.Black,
                disabledPlaceholderColor = Color(0xFFB3C1D1)
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach {
                DropdownMenuItem(onClick = {
                    onOptionSelected(it)
                    expanded = false
                }) {
                    Text(it)
                }
            }
        }
    }
}

@Composable
fun CategoryDropdown(
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("Perfumes", "Clothing", "Furniture", "Electronics", "Accessories")
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            OutlinedTextField(
                value = selectedCategory ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                placeholder = { Text("Select Category", color = Color(0xFFB3C1D1)) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(if (expanded) 180f else 0f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF1B7ACE),
                    unfocusedBorderColor = Color(0xFFB3C1D1),
                    disabledBorderColor = Color(0xFFB3C1D1),
                    backgroundColor = Color.White,
                    disabledTextColor = Color.Black,
                    disabledPlaceholderColor = Color(0xFFB3C1D1)
                ),
                singleLine = true
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                ) {
                    Text(category)
                }
            }
        }
    }
}

@Composable
fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0066CC))
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF114B7F)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (minLines * 24).dp),
            shape = RoundedCornerShape(8.dp),
            trailingIcon = trailingIcon,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF1B7ACE),
                unfocusedBorderColor = Color(0xFFB3C1D1),
                cursorColor = Color(0xFF174378),
                backgroundColor = Color.White
            )
        )
    }
}

@Composable
fun UploadBox(
    height: Dp,
    text: String,
    buttonText: String,
    note: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .border(1.dp, Color(0xFFB6B6B6), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.upload_icon),
                contentDescription = null,
                tint = Color(0xFF0066CC),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(text, color = Color.Gray, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1B7ACE)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(buttonText, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Text(note, color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
fun SelectedImage(uri: Uri, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .padding(4.dp)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
        )
        androidx.compose.material.IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
        ) {
            androidx.compose.material.Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.Red
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewAddNewContentScreen() {
    AddNewContentScreen(navController = rememberNavController())
}
