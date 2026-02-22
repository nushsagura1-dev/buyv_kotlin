package com.project.e_commerce.di

import com.project.e_commerce.data.local.TokenManager
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.data.local.CurrentUserProviderImpl
import com.project.e_commerce.data.remote.KtorClientConfig
import com.project.e_commerce.data.remote.api.AuthApiService
import com.project.e_commerce.data.remote.api.CommentsApiService
import com.project.e_commerce.data.remote.api.OrderApiService
import com.project.e_commerce.data.remote.api.PostApiService
import com.project.e_commerce.data.remote.api.BlockedUserApiService
import com.project.e_commerce.data.remote.api.SoundApiService
import com.project.e_commerce.data.remote.api.TrackingApiService
import com.project.e_commerce.data.remote.api.UserApiService
import com.project.e_commerce.data.repository.AuthNetworkRepository
import com.project.e_commerce.data.repository.BlockedUserNetworkRepository
import com.project.e_commerce.data.repository.CartRepositoryImpl
import com.project.e_commerce.data.repository.CommentNetworkRepository
import com.project.e_commerce.data.repository.OrderNetworkRepository
import com.project.e_commerce.data.repository.PostNetworkRepository
import com.project.e_commerce.data.repository.ProductNetworkRepository
import com.project.e_commerce.data.repository.ProductRepositoryImpl
import com.project.e_commerce.data.repository.SoundNetworkRepository
import com.project.e_commerce.data.repository.TrackingNetworkRepository
import com.project.e_commerce.data.repository.UserNetworkRepository
import com.project.e_commerce.domain.repository.AuthRepository
import com.project.e_commerce.domain.repository.BlockedUserRepository
import com.project.e_commerce.domain.repository.CartRepository
import com.project.e_commerce.domain.repository.CommentRepository
import com.project.e_commerce.domain.repository.OrderRepository
import com.project.e_commerce.domain.repository.PostRepository
import com.project.e_commerce.domain.repository.ProductRepository
import com.project.e_commerce.domain.repository.SoundRepository
import com.project.e_commerce.domain.repository.TrackingRepository
import com.project.e_commerce.domain.repository.UserRepository
import com.project.e_commerce.domain.usecase.auth.GetCurrentUserUseCase
import com.project.e_commerce.domain.usecase.auth.GoogleSignInUseCase
import com.project.e_commerce.domain.usecase.auth.LoginUseCase
import com.project.e_commerce.domain.usecase.auth.LogoutUseCase
import com.project.e_commerce.domain.usecase.auth.RegisterUseCase
import com.project.e_commerce.domain.usecase.auth.SendPasswordResetUseCase
import com.project.e_commerce.domain.usecase.auth.ConfirmPasswordResetUseCase
import com.project.e_commerce.domain.usecase.blockeduser.GetBlockedUsersUseCase
import com.project.e_commerce.domain.usecase.blockeduser.BlockUserUseCase
import com.project.e_commerce.domain.usecase.blockeduser.UnblockUserUseCase
import com.project.e_commerce.domain.usecase.blockeduser.CheckBlockStatusUseCase
import com.project.e_commerce.domain.usecase.cart.AddToCartUseCase
import com.project.e_commerce.domain.usecase.cart.ClearCartUseCase
import com.project.e_commerce.domain.usecase.cart.GetCartUseCase
import com.project.e_commerce.domain.usecase.cart.RemoveFromCartUseCase
import com.project.e_commerce.domain.usecase.cart.UpdateCartItemUseCase
import com.project.e_commerce.domain.usecase.comment.AddCommentUseCase
import com.project.e_commerce.domain.usecase.comment.DeleteCommentUseCase
import com.project.e_commerce.domain.usecase.comment.GetCommentsUseCase
import com.project.e_commerce.domain.usecase.comment.LikeCommentUseCase
import com.project.e_commerce.domain.usecase.sound.GetSoundsUseCase
import com.project.e_commerce.domain.usecase.sound.GetTrendingSoundsUseCase
import com.project.e_commerce.domain.usecase.sound.GetSoundDetailsUseCase
import com.project.e_commerce.domain.usecase.sound.GetSoundGenresUseCase
import com.project.e_commerce.domain.usecase.sound.IncrementSoundUsageUseCase
import com.project.e_commerce.domain.usecase.tracking.TrackViewUseCase
import com.project.e_commerce.domain.usecase.tracking.TrackClickUseCase
import com.project.e_commerce.domain.usecase.tracking.TrackConversionUseCase
import com.project.e_commerce.domain.usecase.order.CancelOrderUseCase
import com.project.e_commerce.domain.usecase.order.CreateOrderUseCase
import com.project.e_commerce.domain.usecase.order.GetOrderDetailsUseCase
import com.project.e_commerce.domain.usecase.order.GetOrdersByUserUseCase
import com.project.e_commerce.domain.usecase.order.GetRecentOrdersUseCase
import com.project.e_commerce.domain.usecase.post.BookmarkPostUseCase
import com.project.e_commerce.domain.usecase.post.CheckPostBookmarkStatusUseCase
import com.project.e_commerce.domain.usecase.post.CheckPostLikeStatusUseCase
import com.project.e_commerce.domain.usecase.post.CreatePostUseCase
import com.project.e_commerce.domain.usecase.post.DeletePostUseCase
import com.project.e_commerce.domain.usecase.post.GetBookmarkedPostsUseCase
import com.project.e_commerce.domain.usecase.post.GetLikedPostsUseCase
import com.project.e_commerce.domain.usecase.post.LikePostUseCase
import com.project.e_commerce.domain.usecase.post.UnbookmarkPostUseCase
import com.project.e_commerce.domain.usecase.post.UnlikePostUseCase
import com.project.e_commerce.domain.usecase.product.GetProductDetailsUseCase
import com.project.e_commerce.domain.usecase.product.GetProductsUseCase
import com.project.e_commerce.domain.usecase.product.GetCategoriesUseCase
import com.project.e_commerce.domain.usecase.user.FollowUserUseCase
import com.project.e_commerce.domain.usecase.user.GetFollowersUseCase
import com.project.e_commerce.domain.usecase.user.GetFollowingStatusUseCase
import com.project.e_commerce.domain.usecase.user.GetFollowingUseCase
import com.project.e_commerce.domain.usecase.user.GetUserPostsUseCase
import com.project.e_commerce.domain.usecase.user.GetUserProfileUseCase
import com.project.e_commerce.domain.usecase.user.DeleteAccountUseCase
import com.project.e_commerce.domain.usecase.user.SearchUsersUseCase
import com.project.e_commerce.domain.usecase.user.UnfollowUserUseCase
import com.project.e_commerce.domain.usecase.user.UpdateUserProfileUseCase
import com.project.e_commerce.data.repository.MarketplaceRepository
import com.project.e_commerce.domain.usecase.marketplace.GetProductsUseCase as MarketplaceGetProductsUseCase
import com.project.e_commerce.domain.usecase.marketplace.GetProductByIdUseCase
import com.project.e_commerce.domain.usecase.marketplace.GetMyWalletUseCase
import com.project.e_commerce.domain.usecase.marketplace.CreatePromotionUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Module Koin pour l'injection de dépendances partagée (KMP).
 * 
 * Configure toutes les dépendances nécessaires pour les repositories et use cases.
 * Ce module est utilisé à la fois sur Android et iOS.
 */

/**
 * Module Network - Fournit les instances de networking (Ktor Client, API Services)
 */
val networkModule = module {
    // HttpClient pour les appels authentifiés
    single(named("authenticated")) {
        val tokenManager: TokenManager = get()
        
        // Ne pas créer de dépendance circulaire avec AuthNetworkRepository
        // Le refresh token sera géré manuellement par l'app si nécessaire
        KtorClientConfig.create(
            tokenProvider = { tokenManager.getAccessToken() },
            refreshTokenProvider = { refreshToken ->
                // Pour éviter la récursion infinie, on ne fait pas de refresh automatique ici
                // L'app devra gérer le refresh manuellement si le token expire
                null
            }
        )
    }
    
    // HttpClient pour les appels non authentifiés (login, register)
    single(named("public")) {
        KtorClientConfig.createPublic()
    }
    
    // API Services
    single { AuthApiService(
        publicClient = get(named("public")),
        authenticatedClient = get(named("authenticated"))
    ) }
    single { PostApiService(get(named("authenticated"))) }
    single { CommentsApiService(get(named("authenticated"))) }
    single { OrderApiService(get(named("authenticated"))) }
    single { UserApiService(get(named("authenticated"))) }
    single { com.project.e_commerce.data.remote.api.MarketplaceApiService(get(named("authenticated"))) }
    single { BlockedUserApiService(get(named("authenticated"))) }
    single { SoundApiService(get(named("authenticated"))) }
    single { TrackingApiService(get(named("authenticated"))) }
}

// firebaseModule removed - Migration to backend completed
// All Firebase authentication and data operations now use backend APIs
// CurrentUserProvider replaces Firebase.auth.currentUser
// Backend REST APIs replace Firestore operations

/**
 * Module Repository - Fournit les implémentations des repositories.
 * Utilise maintenant les implémentations réseau (Network) au lieu de Firebase.
 */
val repositoryModule = module {
    // Current User Provider - Remplace Firebase.auth.currentUser
    single<CurrentUserProvider> {
        CurrentUserProviderImpl(
            authRepository = get(),
            tokenManager = get()
        )
    }
    
    // Auth Repository - Network implementation
    single<AuthNetworkRepository> {
        AuthNetworkRepository(
            authApi = get(),
            tokenManager = get(),
            userApi = get()
        )
    }
    single<AuthRepository> { get<AuthNetworkRepository>() }
    
    // Product Repository - Network implementation (Marketplace API)
    single<ProductRepository> {
        ProductRepositoryImpl(
            marketplaceApi = get()
        )
    }
    
    // Order Repository - Network implementation
    single<OrderRepository> {
        OrderNetworkRepository(
            orderApi = get()
        )
    }
    
    // User Repository - Network implementation
    single<UserRepository> {
        UserNetworkRepository(
            userApi = get()
        )
    }
    
    // Post Repository - Network implementation
    single<PostRepository> {
        PostNetworkRepository(
            postApiService = get()
        )
    }
    
    // Comment Repository - Network implementation
    single<CommentRepository> {
        CommentNetworkRepository(
            commentsApiService = get()
        )
    }
    
    // Marketplace Repository - Uses MarketplaceApiService for promoter/wallet features
    single { MarketplaceRepository(get()) }

    // Blocked User Repository - Network implementation
    single<BlockedUserRepository> {
        BlockedUserNetworkRepository(
            blockedUserApiService = get()
        )
    }

    // Sound Repository - Network implementation
    single<SoundRepository> {
        SoundNetworkRepository(
            soundApiService = get()
        )
    }

    // Tracking Repository - Network implementation
    single<TrackingRepository> {
        TrackingNetworkRepository(
            trackingApiService = get()
        )
    }

    // Cart Repository - Uses local storage (offline-first approach)
    single<CartRepository> {
        CartRepositoryImpl(
            cartStorage = get()
        )
    }
    
    // Legacy Firebase auth repository removed - all auth now uses backend
}

/**
 * Module Use Cases - Fournit tous les use cases.
 */
val useCaseModule = module {
    // Auth Use Cases
    single { LoginUseCase(get()) }
    single { RegisterUseCase(get()) }
    single { LogoutUseCase(get()) }
    single { GetCurrentUserUseCase(get()) }
    single { SendPasswordResetUseCase(get()) }
    single { GoogleSignInUseCase(get()) }
    single { ConfirmPasswordResetUseCase(get()) }
    
    // Product Use Cases
    single { GetProductsUseCase(get()) }
    single { GetProductDetailsUseCase(get()) }
    single { GetCategoriesUseCase(get()) }
    
    // Cart Use Cases
    single { GetCartUseCase(get()) }
    single { AddToCartUseCase(get()) }
    single { UpdateCartItemUseCase(get()) }
    single { RemoveFromCartUseCase(get()) }
    single { ClearCartUseCase(get()) }
    
    // Order Use Cases
    single { CreateOrderUseCase(get()) }
    single { GetOrdersByUserUseCase(get()) }
    single { GetOrderDetailsUseCase(get()) }
    single { CancelOrderUseCase(get()) }
    single { GetRecentOrdersUseCase(get()) }
    
    // User Use Cases
    single { FollowUserUseCase(get()) }
    single { UnfollowUserUseCase(get()) }
    single { GetFollowersUseCase(get()) }
    single { GetFollowingUseCase(get()) }
    single { GetFollowingStatusUseCase(get()) }
    single { GetUserPostsUseCase(get()) }
    single { GetUserProfileUseCase(get()) }
    single { UpdateUserProfileUseCase(get()) }
    single { DeleteAccountUseCase(get()) }
    single { SearchUsersUseCase(get()) }
    
    // Post Use Cases
    single { CreatePostUseCase(get()) }
    single { DeletePostUseCase(get()) }
    single { LikePostUseCase(get()) }
    single { UnlikePostUseCase(get()) }
    single { BookmarkPostUseCase(get()) }
    single { UnbookmarkPostUseCase(get()) }
    single { GetLikedPostsUseCase(get()) }
    single { GetBookmarkedPostsUseCase(get()) }
    single { CheckPostLikeStatusUseCase(get()) }
    single { CheckPostBookmarkStatusUseCase(get()) }
    
    // Comment Use Cases
    single { GetCommentsUseCase(get()) }
    single { AddCommentUseCase(get()) }
    single { DeleteCommentUseCase(get()) }
    single { LikeCommentUseCase(get()) }

    // Blocked User Use Cases
    single { GetBlockedUsersUseCase(get()) }
    single { BlockUserUseCase(get()) }
    single { UnblockUserUseCase(get()) }
    single { CheckBlockStatusUseCase(get()) }

    // Sound Use Cases
    single { GetSoundsUseCase(get()) }
    single { GetTrendingSoundsUseCase(get()) }
    single { GetSoundDetailsUseCase(get()) }
    single { GetSoundGenresUseCase(get()) }
    single { IncrementSoundUsageUseCase(get()) }

    // Tracking Use Cases
    single { TrackViewUseCase(get()) }
    single { TrackClickUseCase(get()) }
    single { TrackConversionUseCase(get()) }

    // Marketplace Use Cases
    single { MarketplaceGetProductsUseCase(get()) }
    single { GetProductByIdUseCase(get()) }
    single { GetMyWalletUseCase(get()) }
    single { CreatePromotionUseCase(get()) }
}

/**
 * Liste de tous les modules partagés.
 * Utilisez cette liste pour initialiser Koin.
 */
val sharedModules = listOf(
    platformModule,
    networkModule,
    repositoryModule,
    useCaseModule
)

