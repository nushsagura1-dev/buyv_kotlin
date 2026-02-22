package com.project.e_commerce.android.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage

enum class ImageType {
    PRODUCT,
    PROFILE,
    GENERIC
}

@Composable
fun ImageWithPlaceholder(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    imageType: ImageType = ImageType.GENERIC,
    contentScale: ContentScale = ContentScale.Crop,
    colorFilter: ColorFilter? = null
) {
    // Guard against null/empty/blank URLs that Coil cannot handle
    if (imageUrl.isNullOrBlank()) {
        Box(
            modifier = modifier
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (imageType) {
                    ImageType.PRODUCT -> Icons.Default.ShoppingCart
                    ImageType.PROFILE -> Icons.Default.Person
                    ImageType.GENERIC -> Icons.Default.BrokenImage
                },
                contentDescription = "Placeholder",
                tint = Color.Gray
            )
        }
        return
    }

    SubcomposeAsyncImage(
        model = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = colorFilter,
        loading = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (imageType) {
                        ImageType.PRODUCT -> Icons.Default.ShoppingCart
                        ImageType.PROFILE -> Icons.Default.Person
                        ImageType.GENERIC -> Icons.Default.BrokenImage
                    },
                    contentDescription = "Placeholder",
                    tint = Color.Gray
                )
            }
        }
    )
}
