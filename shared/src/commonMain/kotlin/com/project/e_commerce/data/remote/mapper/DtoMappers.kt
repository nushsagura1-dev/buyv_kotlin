package com.project.e_commerce.data.remote.mapper

import com.project.e_commerce.data.remote.dto.*
import com.project.e_commerce.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Extension functions to map backend DTOs to domain models
 */

/**
 * Convert UserDto to UserProfile domain model
 */
fun UserDto.toDomain(): UserProfile {
    return UserProfile(
        uid = id, // Backend returns 'id' which is the UID
        email = email,
        displayName = displayName,
        username = username,
        profileImageUrl = profileImageUrl,
        bio = bio ?: "",
        phone = "", // Not provided by backend
        role = role, // user, promoter, admin
        followersCount = followersCount,
        followingCount = followingCount,
        likesCount = 0, // Not in UserDto, would come from stats
        createdAt = parseTimestamp(createdAt),
        lastUpdated = parseTimestamp(updatedAt)
    )
}

/**
 * Convert UserFollowInfoDto to UserFollowModel domain model
 */
fun UserFollowInfoDto.toDomain(): UserFollowModel {
    return UserFollowModel(
        id = id,
        name = displayName,
        username = username,
        profileImageUrl = profileImageUrl,
        isFollowingMe = false, // Will be updated by caller if needed
        isIFollow = false // Will be updated by caller if needed
    )
}

/**
 * Convert FollowingStatusDto to FollowingStatus domain model
 */
fun FollowingStatusDto.toDomain(): FollowingStatus {
    return FollowingStatus(
        isFollowing = isFollowing,
        isFollowedBy = isFollowedBy,
        isMutual = isFollowing && isFollowedBy
    )
}

/**
 * Convert UserPostDto to UserPost domain model
 */
fun UserPostDto.toDomain(): UserPost {
    return UserPost(
        id = id,
        userId = userId,
        type = type.uppercase(),
        title = caption ?: "",
        description = caption ?: "",
        mediaUrl = videoUrl,
        thumbnailUrl = null, // Not in backend DTO
        images = emptyList(), // Not in backend DTO
        likesCount = likesCount,
        commentsCount = commentsCount,
        viewsCount = 0, // Not in backend DTO
        isPublished = true,
        createdAt = parseTimestamp(createdAt),
        updatedAt = parseTimestamp(updatedAt)
    )
}

/**
 * Convert UserProfile to UserUpdateDto for profile update
 */
fun UserProfile.toUpdateDto(): UserUpdateDto {
    return UserUpdateDto(
        displayName = displayName,
        profileImageUrl = profileImageUrl,
        bio = bio,
        interests = emptyList(), // Not in domain model
        settings = null // Not in domain model
    )
}

/**
 * Convert PostDto to Product domain model (for product listings)
 */
fun PostDto.toProduct(): Product {
    return Product(
        id = id,
        userId = userId,
        name = caption ?: "Unnamed Product",
        description = caption ?: "",
        price = "0", // Price should be extracted from post metadata if available
        image = videoUrl ?: thumbnailUrl ?: "",
        reelVideoUrl = videoUrl ?: "",
        rating = likesCount.toDouble(),
        categoryId = type,
        categoryName = "",
        quantity = "0",
        productImages = listOfNotNull(thumbnailUrl, videoUrl),
        reelTitle = caption ?: "",
        createdAt = parseTimestamp(createdAt),
        // Carry over user info and post metadata
        username = username,
        displayName = displayName ?: "",
        userProfileImage = userProfileImage ?: "",
        postType = type,
        isLiked = isLiked,
        isBookmarked = isBookmarked,
        likesCount = likesCount,
        commentsCount = commentsCount
    )
}

/**
 * Convert OrderDto to Order domain model
 */
fun OrderDto.toDomain(): Order {
    return Order(
        id = id.toString(),
        userId = userId.toString(),
        orderNumber = orderNumber,
        items = items.map { it.toDomain() },
        status = OrderStatus.valueOf(status.uppercase()),
        subtotal = subtotal,
        shipping = shipping,
        tax = tax,
        total = total,
        shippingAddress = shippingAddress?.toDomain(),
        paymentMethod = paymentMethod ?: "CARD",
        createdAt = parseTimestamp(createdAt),
        updatedAt = parseTimestamp(updatedAt)
    )
}

/**
 * Convert OrderItemDto to OrderItem
 */
fun OrderItemDto.toDomain(): OrderItem {
    return OrderItem(
        productId = productId,
        productName = productName,
        productImage = productImage,
        quantity = quantity,
        price = price,
        size = size,
        color = color
    )
}

/**
 * Convert AddressDto to Address domain model
 */
fun AddressDto.toDomain(): Address {
    return Address(
        id = id ?: "",
        name = fullName,
        street = address,
        city = city,
        state = state,
        zipCode = zipCode,
        country = country,
        phone = phone
    )
}

/**
 * Convert UserStatsDto to user statistics
 * Returns a map with key-value stats
 */
fun UserStatsDto.toStatsMap(): Map<String, Int> {
    return mapOf(
        "followers" to followersCount,
        "following" to followingCount,
        "reels" to reelsCount,
        "products" to productsCount,
        "totalLikes" to totalLikes,
        "savedPosts" to savedPostsCount
    )
}

/**
 * Parse ISO 8601 timestamp string to milliseconds
 * Backend returns: "2024-01-15T10:30:00Z"
 * Domain models use: Long (milliseconds since epoch)
 */
private fun parseTimestamp(timestamp: String?): Long {
    if (timestamp == null) return Clock.System.now().toEpochMilliseconds()
    return try {
        Instant.parse(timestamp).toEpochMilliseconds()
    } catch (e: Exception) {
        Clock.System.now().toEpochMilliseconds()
    }
}

/**
 * Convert domain Address to DTO for backend requests
 */
fun Address.toDto(): AddressDto {
    return AddressDto(
        id = id.ifBlank { null },
        fullName = name,
        address = street,
        city = city,
        state = state,
        zipCode = zipCode,
        country = country,
        phone = phone
    )
}

