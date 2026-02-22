package com.project.e_commerce.android.di

import com.project.e_commerce.android.data.repository.BackendFollowingRepository
import com.project.e_commerce.android.data.repository.BackendUserProfileRepository
import com.project.e_commerce.android.data.repository.CountriesRepository
import com.project.e_commerce.android.data.repository.RecentlyViewedRepository
import com.project.e_commerce.android.domain.repository.UserProfileRepository
import com.project.e_commerce.android.domain.repository.FollowingRepository
import com.project.e_commerce.android.domain.repository.CartRepository
import com.project.e_commerce.android.domain.repository.NotificationRepository
import com.project.e_commerce.android.domain.repository.OrderRepository
import com.project.e_commerce.android.domain.usecase.CheckEmailValidation
import com.project.e_commerce.android.domain.usecase.CheckLoginValidation
import com.project.e_commerce.android.domain.usecase.CheckPasswordValidation
import com.project.e_commerce.android.domain.usecase.CheckMatchedPasswordUseCase
import com.project.e_commerce.android.domain.usecase.LoginByEmailAndPasswordUseCase
import com.project.e_commerce.android.domain.usecase.GetUserPostsUseCase
import com.project.e_commerce.android.domain.usecase.GetUserProductsUseCase
import com.project.e_commerce.android.domain.usecase.GetUserProfileUseCase
import com.project.e_commerce.android.domain.usecase.GetUserLikedPostsUseCase
import com.project.e_commerce.android.domain.usecase.FollowUserUseCase
import com.project.e_commerce.android.domain.usecase.UnfollowUserUseCase
// KMP UseCases for Follow functionality (backend)
import com.project.e_commerce.domain.usecase.user.FollowUserUseCase as KmpFollowUserUseCase
import com.project.e_commerce.domain.usecase.user.UnfollowUserUseCase as KmpUnfollowUserUseCase
import com.project.e_commerce.domain.usecase.user.GetFollowersUseCase as KmpGetFollowersUseCase
import com.project.e_commerce.domain.usecase.user.GetFollowingUseCase as KmpGetFollowingUseCase
import com.project.e_commerce.domain.usecase.user.GetFollowingStatusUseCase as KmpGetFollowingStatusUseCase
// KMP UseCases for User Profile functionality (backend)
import com.project.e_commerce.domain.usecase.user.GetUserProfileUseCase as KmpGetUserProfileUseCase
import com.project.e_commerce.domain.usecase.user.GetUserPostsUseCase as KmpGetUserPostsUseCase
import com.project.e_commerce.domain.usecase.user.UpdateUserProfileUseCase as KmpUpdateUserProfileUseCase
import com.project.e_commerce.domain.usecase.post.DeletePostUseCase
import com.project.e_commerce.android.domain.usecase.GetFollowersUseCase
import com.project.e_commerce.android.domain.usecase.GetNotificationsUseCase
import com.project.e_commerce.android.domain.usecase.GetUnreadCountUseCase
import com.project.e_commerce.android.domain.usecase.MarkAsReadUseCase
import com.project.e_commerce.android.domain.usecase.CreateNotificationUseCase
import com.project.e_commerce.android.presentation.services.NotificationManagerService
import com.project.e_commerce.android.data.remote.api.CountriesApi
import com.project.e_commerce.android.data.remote.interceptor.AuthInterceptor
import com.project.e_commerce.android.domain.usecase.GetUserReelsUseCase
import com.project.e_commerce.android.domain.usecase.UpdateUserProfileUseCase
import com.project.e_commerce.android.domain.usecase.GetUserBookmarkedPostsUseCase
import com.project.e_commerce.android.domain.usecase.GetFollowingStatusUseCase
import com.project.e_commerce.android.domain.usecase.GetFollowingUsersUseCase
import com.project.e_commerce.android.domain.usecase.GetUserProfilesByIdsUseCase
import com.project.e_commerce.android.presentation.viewModel.CartViewModel
import com.project.e_commerce.android.presentation.viewModel.ProductViewModel
import com.project.e_commerce.android.presentation.viewModel.loginScreenViewModel.LoginScreenViewModel
import com.project.e_commerce.android.presentation.viewModel.profileViewModel.ProfileViewModel
import com.project.e_commerce.android.presentation.viewModel.editProfileViewModel.EditProfileViewModel
import com.project.e_commerce.android.presentation.ui.screens.profile.DeleteAccountViewModel
import com.project.e_commerce.android.presentation.viewModel.reelsScreenViewModel.ReelsScreenViewModel
import com.project.e_commerce.android.presentation.viewModel.restPasswordViewModel.RestPasswordViewModel
import com.project.e_commerce.android.presentation.viewModel.followingViewModel.FollowingViewModel
import com.project.e_commerce.android.presentation.viewModel.AuthViewModel
import com.project.e_commerce.android.presentation.viewModel.CommentsViewModel
import com.project.e_commerce.android.presentation.viewModel.otherUserProfile.OtherUserProfileViewModel
import com.project.e_commerce.android.presentation.viewModel.searchViewModel.SearchViewModel
import com.project.e_commerce.android.presentation.viewModel.NotificationViewModel
import com.project.e_commerce.android.presentation.viewModel.OrderHistoryViewModel
import com.project.e_commerce.android.presentation.viewModel.RecentlyViewedViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koin.core.qualifier.named
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.project.e_commerce.android.domain.usecase.FCMTokenUseCase
import com.project.e_commerce.android.domain.usecase.CreateOrderUseCase
import com.project.e_commerce.android.domain.usecase.GetUserOrdersUseCase
import com.project.e_commerce.android.domain.usecase.UpdateOrderStatusUseCase
import com.project.e_commerce.android.domain.usecase.CancelOrderUseCase
import com.project.e_commerce.android.domain.usecase.GoogleSignInUseCase
import com.project.e_commerce.android.data.helper.GoogleSignInHelper

import com.project.e_commerce.android.presentation.viewModel.SocialViewModel
import com.project.e_commerce.android.presentation.viewModel.FavouritesViewModel
import com.project.e_commerce.android.data.remote.security.CertificatePinningConfig

val viewModelModule = module {
    try {
        // ============================================================
        // MIGRATION FIREBASE → BACKEND - Phase 1: Authentication
        // ============================================================
        // Firebase Auth et Firestore supprimés - utilisation backend uniquement
        // L'app utilisera maintenant le backend FastAPI pour l'authentification
        // au lieu de Firebase Auth. Le repository est configuré dans:
        // shared/src/commonMain/kotlin/di/SharedModule.kt
        // ============================================================
        
        // ============================================================
        // FIREBASE BEANS - REMOVED (Migration completed)
        // ============================================================
        // All Firebase dependencies have been migrated to backend APIs
        // FirebaseAuth -> CurrentUserProvider (via shared module)
        // FirebaseFirestore -> Backend REST APIs
        // ============================================================
        
        // ============================================================
        // REPOSITORIES (Infrastructure - migrated to backend)
        // ============================================================
        
        // KMP UseCases for User Profile functionality (backend)
        single { KmpGetUserProfileUseCase(get()) }
        single { KmpGetUserPostsUseCase(get()) }
        single { KmpUpdateUserProfileUseCase(get()) }
        
        // Backend-based UserProfileRepository
        single<UserProfileRepository> {
            val repo = BackendUserProfileRepository(
                getUserProfileUseCase = get<KmpGetUserProfileUseCase>(),
                getUserPostsUseCase = get<KmpGetUserPostsUseCase>(),
                updateUserProfileUseCase = get<KmpUpdateUserProfileUseCase>(),
                followUserUseCase = get<KmpFollowUserUseCase>(),
                unfollowUserUseCase = get<KmpUnfollowUserUseCase>(),
                getFollowersUseCase = get<KmpGetFollowersUseCase>(),
                getFollowingUseCase = get<KmpGetFollowingUseCase>(),
                postRepository = get(),
                marketplaceApi = get()
            )
            android.util.Log.d("CrashDebug", "AppModule: BackendUserProfileRepository created")
            repo
        }
        
        // KMP UseCases for Follow functionality (backend)
        single { KmpFollowUserUseCase(get()) }
        single { KmpUnfollowUserUseCase(get()) }
        single { KmpGetFollowersUseCase(get()) }
        single { KmpGetFollowingUseCase(get()) }
        single { KmpGetFollowingStatusUseCase(get()) }
        
        // Backend-based FollowingRepository
        single<FollowingRepository> {
            val repo = BackendFollowingRepository(
                followUserUseCase = get(),
                unfollowUserUseCase = get(),
                getFollowersUseCase = get(),
                getFollowingUseCase = get(),
                getFollowingStatusUseCase = get()
            )
            android.util.Log.d("CrashDebug", "AppModule: BackendFollowingRepository created")
            repo
        }
        
        // NOTE: CartRepository, OrderRepository, NotificationRepository, NotificationSettingsRepository 
        // now use backend APIs (via shared module)
        
        // OrderRepository: bridge Android interface → shared KMP implementation
        single<OrderRepository> {
            val sharedOrderRepo: com.project.e_commerce.domain.repository.OrderRepository = get()
            com.project.e_commerce.android.data.repository.OrderRepositoryImpl(sharedOrderRepo)
        }

        // Notification Services
        single {
            val service = NotificationManagerService(androidContext())
            android.util.Log.d("CrashDebug", "AppModule: NotificationManagerService created")
            service
        }
        
        // ============================================================

        // Use cases
        single {
            val useCase = CheckMatchedPasswordUseCase()
            android.util.Log.d("CrashDebug", "AppModule: CheckMatchedPasswordUseCase created")
            useCase
        }
        single {
            val useCase = CheckLoginValidation()
            android.util.Log.d("CrashDebug", "AppModule: CheckLoginValidation created")
            useCase
        }
        single {
            val useCase = CheckEmailValidation()
            android.util.Log.d("CrashDebug", "AppModule: CheckEmailValidation created")
            useCase
        }
        single {
            val useCase = CheckPasswordValidation()
            android.util.Log.d("CrashDebug", "AppModule: CheckPasswordValidation created")
            useCase
        }
        // Legacy login use case removed - replaced by KMP LoginUseCase in AuthViewModel
        /* single {
            val useCase = LoginByEmailAndPasswordUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: LoginByEmailAndPasswordUseCase created")
            useCase
        } */

        // User Profile Use Cases
        single {
            val useCase = GetUserProfileUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetUserProfileUseCase created")
            useCase
        }
        single {
            val useCase = UpdateUserProfileUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: UpdateUserProfileUseCase created")
            useCase
        }
        single {
            val useCase = GetUserPostsUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetUserPostsUseCase created")
            useCase
        }
        single {
            val useCase = GetUserReelsUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetUserReelsUseCase created")
            useCase
        }
        single {
            val useCase = GetUserProductsUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetUserProductsUseCase created")
            useCase
        }
        single {
            val useCase = GetUserLikedPostsUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetUserLikedPostsUseCase created")
            useCase
        }
        single {
            val useCase = GetUserBookmarkedPostsUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetUserBookmarkedPostsUseCase created")
            useCase
        }


        // Removed Legacy Like & Comment UseCases (Migrated to Shared KMP)


        // Following Use Cases
        single {
            val useCase = FollowUserUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: FollowUserUseCase created")
            useCase
        }
        single {
            val useCase = UnfollowUserUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: UnfollowUserUseCase created")
            useCase
        }
        single {
            val useCase = GetFollowingStatusUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetFollowingStatusUseCase created")
            useCase
        }
        single {
            val useCase = GetFollowingUsersUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetFollowingUsersUseCase created")
            useCase
        }
        single {
            val useCase = GetUserProfilesByIdsUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetUserProfilesByIdsUseCase created")
            useCase
        }
        single {
            val useCase = GetFollowersUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetFollowersUseCase created")
            useCase
        }

        // Order Use Cases
        single {
            val useCase = CreateOrderUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: CreateOrderUseCase created")
            useCase
        }
        single {
            val useCase = GetUserOrdersUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetUserOrdersUseCase created")
            useCase
        }
        single {
            val useCase = UpdateOrderStatusUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: UpdateOrderStatusUseCase created")
            useCase
        }
        single {
            val useCase = CancelOrderUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: CancelOrderUseCase created")
            useCase
        }

        // Notification Use Cases
        single {
            val useCase = GetNotificationsUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetNotificationsUseCase created")
            useCase
        }
        single {
            val useCase = GetUnreadCountUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetUnreadCountUseCase created")
            useCase
        }
        single {
            val useCase = MarkAsReadUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: MarkAsReadUseCase created")
            useCase
        }
        single {
            val useCase = CreateNotificationUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: CreateNotificationUseCase created")
            useCase
        }
        single {
            val useCase = FCMTokenUseCase(get(), get())
            android.util.Log.d("CrashDebug", "AppModule: FCMTokenUseCase created")
            useCase
        }
        single {
            val useCase = GoogleSignInUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GoogleSignInUseCase created")
            useCase
        }

        single {
            val helper = GoogleSignInHelper(androidContext())
            android.util.Log.d("CrashDebug", "AppModule: GoogleSignInHelper created")
            helper
        }

        // ViewModels
        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: AuthViewModel created")
            AuthViewModel(
                repo = null, // Migrated to backend - no longer needed
                checkEmailValidation = get(),
                checkPasswordValidation = get(),
                checkMatchedPassword = get(),
                googleSignInUseCase = get(), // Backend Google Sign-In via shared KMP
                googleSignInHelper = get(),
                loginUseCase = get(),
                registerUseCase = get(),
                logoutUseCase = get(),
                sendPasswordResetUseCase = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: LoginScreenViewModel created")
            LoginScreenViewModel(
                checkLoginValidation = get(),
                checkEmailValidation = get(),
                checkPasswordValidation = get(),
                loginUseCase = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: ProfileViewModel created")
            ProfileViewModel(
                userProfileRepository = get(),
                getUserProfileUseCase = get(),
                getUserReelsUseCase = get(),
                getUserProductsUseCase = get(),
                getUserLikedPostsUseCase = get(),
                getUserBookmarkedPostsUseCase = get(),
                getFollowingUsersUseCase = get(),
                getFollowersUseCase = get(),
                currentUserProvider = get(),
                tokenManager = get(),
                deletePostUseCase = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: EditProfileViewModel created")
            EditProfileViewModel(
                getUserProfileUseCase = get(),
                updateUserProfileUseCase = get(),
                currentUserProvider = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: DeleteAccountViewModel created")
            DeleteAccountViewModel(
                deleteAccountUseCase = get(),
                tokenManager = get(),
                currentUserProvider = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: RestPasswordViewModel created")
            RestPasswordViewModel(get(), get(), get())
        }
        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: ProductViewModel created")
            ProductViewModel(
                getProductsUseCase = get(),
                getProductDetailsUseCase = get(),
                getCategoriesUseCase = get()
            )
        }
        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: ReelsScreenViewModel creation")
            ReelsScreenViewModel(
                productViewModel = get(),
                getUserProfileUseCase = get(),
                likePostUseCase = get(),
                unlikePostUseCase = get(),
                checkPostLikeStatusUseCase = get(),
                addCommentUseCase = get(),
                getCommentsUseCase = get(),
                likeCommentUseCase = get(),
                postRepository = get(),
                cartRepository = null, // ⚠️ MIGRATION: CartRepository disabled (not yet migrated)
                currentUserProvider = get()
            )
        }
        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: SoundPageViewModel created")
            com.project.e_commerce.android.presentation.viewModel.SoundPageViewModel(
                getSoundDetailsUseCase = get(),
                incrementSoundUsageUseCase = get()
            )
        }
        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: CartViewModel created")
            CartViewModel(
                getCartUseCase = get(),
                addToCartUseCase = get(),
                updateCartItemUseCase = get(),
                removeFromCartUseCase = get(),
                clearCartUseCase = get(),
                getCurrentUserUseCase = get()
            )
        }
        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: FollowingViewModel created")
            FollowingViewModel(
                followUserUseCase = get(),
                unfollowUserUseCase = get(),
                getFollowingStatusUseCase = get(),
                getFollowingUsersUseCase = get(),
                getUserProfilesByIdsUseCase = get(),
                getFollowersUseCase = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: CommentsViewModel created")
            CommentsViewModel(
                getCommentsUseCase = get(),
                addCommentUseCase = get(),
                deleteCommentUseCase = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: OtherUserProfileViewModel created")
            OtherUserProfileViewModel(
                getUserProfileUseCase = get(),
                getFollowingStatusUseCase = get(),
                followUserUseCase = get(),
                unfollowUserUseCase = get(),
                getUserReelsUseCase = get(),
                getUserProductsUseCase = get(),
                getFollowersUseCase = get(),
                getFollowingUsersUseCase = get(),
                currentUserProvider = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: SocialViewModel created")
            SocialViewModel(
                createPostUseCase = get(),
                followUserUseCase = get(),
                unfollowUserUseCase = get(),
                getFollowersUseCase = get(),
                getFollowingUseCase = get(),
                getFollowingStatusUseCase = get(),
                getUserProfileUseCase = get(),
                getUserPostsUseCase = get(),
                updateUserProfileUseCase = get(),
                searchUsersUseCase = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: SearchViewModel created")
            com.project.e_commerce.android.presentation.viewModel.searchViewModel.SearchViewModel(
                productViewModel = get(),
                followUserUseCase = get(),
                unfollowUserUseCase = get(),
                getFollowingStatusUseCase = get(),
                currentUserProvider = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: NotificationViewModel created")
            NotificationViewModel(
                getNotificationsUseCase = get(),
                getUnreadCountUseCase = get(),
                markAsReadUseCase = get(),
                createNotificationUseCase = get(),
                currentUserProvider = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: OrderHistoryViewModel created")
            OrderHistoryViewModel(
                getUserOrdersUseCase = get(),
                cancelOrderUseCase = get(),
                currentUserProvider = get()
            )
        }

        // RecentlyViewedRepository - Stub version (backend endpoint not yet implemented)
        single {
            val repo = RecentlyViewedRepository(get())
            android.util.Log.d("CrashDebug", "AppModule: RecentlyViewedRepository created (stub mode)")
            repo
        }
        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: RecentlyViewedViewModel created")
            RecentlyViewedViewModel(
                recentlyViewedRepository = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: FavouritesViewModel created")
            FavouritesViewModel(
                postRepository = get()
            )
        }

        viewModel {
            com.project.e_commerce.android.presentation.viewModel.BlockedUsersViewModel(
                getBlockedUsersUseCase = get(),
                unblockUserUseCase = get()
            )
        }

        // Retrofit - DEFAULT (For Backend)
        single {
            val okHttp = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(get()))
                .apply {
                    // 🔒 Certificate Pinning (production only)
                    CertificatePinningConfig.configurePinning(this)
                }
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)  // 15s pour établir connexion
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)     // 30s pour lire les données
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)    // 30s pour écrire les données
                .callTimeout(30, java.util.concurrent.TimeUnit.SECONDS)     // 30s timeout total par appel
                .build()
            
            android.util.Log.d("CrashDebug", "AppModule: OkHttpClient (Backend) created")
            
            Retrofit.Builder()
                .client(okHttp)
                .baseUrl("http://192.168.11.108:8000/") // Correct Backend URL
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        // Retrofit - COUNTRIES (Named)
        single(named("countriesRetrofit")) {
            Retrofit.Builder()
                .baseUrl("https://restcountries.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        single {
            val countriesApi = get<Retrofit>(named("countriesRetrofit")).create(CountriesApi::class.java)
            android.util.Log.d("CrashDebug", "AppModule: CountriesApi created")
            countriesApi
        }
        single {
            val countriesRepository = CountriesRepository(get())
            android.util.Log.d("CrashDebug", "AppModule: CountriesRepository created")
            countriesRepository
        }
        android.util.Log.d("CrashDebug", "AppModule: viewModelModule DI instantiations complete")
    } catch (e: Exception) {
        android.util.Log.e(
            "CrashDebug",
            "AppModule: Exception during viewModelModule instantiation: ${e.message}",
            e
        )
    }
}
