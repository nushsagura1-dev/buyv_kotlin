package com.project.e_commerce.android.presentation.ui.composable.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

/**
 * Type d'image pour sélectionner le placeholder approprié
 */
enum class ImageType {
    PRODUCT,
    PROFILE,
    POST,
    GENERIC
}

/**
 * AsyncImage avec placeholder et error handling automatiques.
 * 
 * Usage :
 * ```kotlin
 * ImageWithPlaceholder(
 *     model = product.imageUrl,
 *     contentDescription = "Product image",
 *     imageType = ImageType.PRODUCT,
 *     modifier = Modifier.size(100.dp)
 * )
 * ```
 * 
 * @param model URL de l'image (String, Uri, etc.)
 * @param contentDescription Description pour accessibilité
 * @param modifier Modifier optionnel
 * @param imageType Type d'image pour sélectionner l'icône appropriée
 * @param contentScale Comment l'image doit être mise à l'échelle
 */
@Composable
fun ImageWithPlaceholder(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    imageType: ImageType = ImageType.GENERIC,
    contentScale: ContentScale = ContentScale.Crop
) {
    // Guard against null/empty/blank image data that Coil cannot handle
    val effectiveModel = when {
        model == null -> null
        model is String && model.isBlank() -> null
        else -> model
    }
    if (effectiveModel == null) {
        PlaceholderContent(imageType = imageType, modifier = modifier)
        return
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = effectiveModel,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            onState = { state ->
                when (state) {
                    is AsyncImagePainter.State.Loading -> {
                        // Loading state handled by placeholder below
                    }
                    is AsyncImagePainter.State.Error -> {
                        // Error state handled by error below
                    }
                    is AsyncImagePainter.State.Success -> {
                        // Image loaded successfully
                    }
                    is AsyncImagePainter.State.Empty -> {
                        // Empty state
                    }
                }
            }
        )
        
        // Placeholder pendant le chargement
        PlaceholderContent(
            imageType = imageType,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Composable pour afficher le placeholder ou l'erreur
 */
@Composable
private fun PlaceholderContent(
    imageType: ImageType,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (imageType) {
        ImageType.PRODUCT -> Icons.Default.ShoppingCart
        ImageType.PROFILE -> Icons.Default.Person
        ImageType.POST -> Icons.Default.Image
        ImageType.GENERIC -> Icons.Default.Image
    }
    
    Box(
        modifier = modifier
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = Color(0xFFBDBDBD)
        )
    }
}

/**
 * AsyncImage avancée avec gestion complète des états (loading, error, success).
 * 
 * Usage pour un contrôle plus fin :
 * ```kotlin
 * AsyncImageWithStates(
 *     model = imageUrl,
 *     contentDescription = "Image",
 *     modifier = Modifier.size(200.dp),
 *     loading = {
 *         CircularProgressIndicator()
 *     },
 *     error = {
 *         Icon(Icons.Default.BrokenImage, null)
 *     }
 * )
 * ```
 */
@Composable
fun AsyncImageWithStates(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    loading: @Composable (BoxScope.() -> Unit)? = null,
    error: @Composable (BoxScope.() -> Unit)? = null
) {
    Box(modifier = modifier) {
        var currentState: AsyncImagePainter.State = AsyncImagePainter.State.Empty
        
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            onState = { state ->
                currentState = state
            }
        )
        
        // Loading state
        if (currentState is AsyncImagePainter.State.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                if (loading != null) {
                    loading()
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color(0xFF6200EE),
                        strokeWidth = 3.dp
                    )
                }
            }
        }
        
        // Error state
        if (currentState is AsyncImagePainter.State.Error) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                if (error != null) {
                    error()
                } else {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "Error loading image",
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFFE57373)
                    )
                }
            }
        }
    }
}

/**
 * Version compacte pour les petites images (avatars, thumbnails)
 */
@Composable
fun CompactImageWithPlaceholder(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    imageType: ImageType = ImageType.GENERIC,
    contentScale: ContentScale = ContentScale.Crop
) {
    ImageWithPlaceholder(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        imageType = imageType,
        contentScale = contentScale
    )
}
