package com.project.e_commerce.android.data.repository.marketplace

import android.util.Log
import com.project.e_commerce.android.data.api.AdminApi
import com.project.e_commerce.android.data.api.ProductUpdateRequest
import com.project.e_commerce.android.data.api.ProductUpdateResponse
import com.project.e_commerce.android.data.api.MessageResponse

/**
 * Repository for admin marketplace operations.
 * Handles product CRUD operations for administrators.
 */
class AdminMarketplaceRepository(
    private val adminApi: AdminApi
) {
    
    /**
     * Update a product (Admin only).
     * 
     * @param token Admin authentication token (Bearer format)
     * @param productId UUID of the product to update
     * @param request Product update data
     * @return Updated product information
     */
    suspend fun updateProduct(
        token: String,
        productId: String,
        request: ProductUpdateRequest
    ): ProductUpdateResponse {
        return try {
            val response = adminApi.updateProduct(
                token = if (token.startsWith("Bearer ")) token else "Bearer $token",
                productId = productId,
                request = request
            )
            Log.d("AdminMarketplaceRepo", "Product updated: ${response.id}")
            response
        } catch (e: Exception) {
            Log.e("AdminMarketplaceRepo", "Error updating product: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Delete a product (Admin only - Super Admin).
     * 
     * @param token Admin authentication token (Bearer format)
     * @param productId UUID of the product to delete
     * @return Success message
     */
    suspend fun deleteProduct(
        token: String,
        productId: String
    ): MessageResponse {
        return try {
            val response = adminApi.deleteProduct(
                token = if (token.startsWith("Bearer ")) token else "Bearer $token",
                productId = productId
            )
            Log.d("AdminMarketplaceRepo", "Product deleted: $productId")
            response
        } catch (e: Exception) {
            Log.e("AdminMarketplaceRepo", "Error deleting product: ${e.message}", e)
            throw e
        }
    }
}
