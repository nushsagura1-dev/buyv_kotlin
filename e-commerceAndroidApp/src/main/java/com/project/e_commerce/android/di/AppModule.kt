package com.project.e_commerce.android.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.e_commerce.android.data.repository.AuthRepository
import com.project.e_commerce.android.data.repository.FirebaseAuthRepository
import com.project.e_commerce.android.data.repository.FirebaseUserProfileRepository
import com.project.e_commerce.android.data.repository.FirebaseFollowingRepository
import com.project.e_commerce.android.data.repository.FirebaseCartRepository
import com.project.e_commerce.android.data.repository.FirebaseNotificationRepository
import com.project.e_commerce.android.data.repository.FirebaseNotificationSettingsRepository
import com.project.e_commerce.android.data.repository.CountriesRepository
import com.project.e_commerce.android.data.repository.RecentlyViewedRepository
import com.project.e_commerce.android.domain.repository.UserProfileRepository
import com.project.e_commerce.android.domain.repository.FollowingRepository
import com.project.e_commerce.android.domain.repository.CartRepository
import com.project.e_commerce.android.domain.repository.NotificationRepository
import com.project.e_commerce.android.domain.repository.NotificationSettingsRepository
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
import com.project.e_commerce.android.domain.usecase.LikeReelUseCase
import com.project.e_commerce.android.domain.usecase.GetReelLikeStatusUseCase
import com.project.e_commerce.android.domain.usecase.GetReelLikeCountUseCase
import com.project.e_commerce.android.domain.usecase.UpdateUserLikeCountUseCase
import com.project.e_commerce.android.domain.usecase.AddCommentUseCase
import com.project.e_commerce.android.domain.usecase.GetReelCommentsUseCase
import com.project.e_commerce.android.domain.usecase.LikeCommentUseCase
import com.project.e_commerce.android.domain.usecase.GetCommentLikeStatusUseCase
import com.project.e_commerce.android.domain.usecase.GetCommentLikeCountUseCase
import com.project.e_commerce.android.domain.usecase.FollowUserUseCase
import com.project.e_commerce.android.domain.usecase.UnfollowUserUseCase
import com.project.e_commerce.android.domain.usecase.GetFollowersUseCase
import com.project.e_commerce.android.domain.usecase.GetNotificationsUseCase
import com.project.e_commerce.android.domain.usecase.GetUnreadCountUseCase
import com.project.e_commerce.android.domain.usecase.MarkAsReadUseCase
import com.project.e_commerce.android.domain.usecase.CreateNotificationUseCase
import com.project.e_commerce.android.presentation.services.NotificationManagerService
import com.project.e_commerce.android.data.remote.api.CountriesApi
import com.project.e_commerce.android.data.remote.interceptor.Interceptor
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
import com.project.e_commerce.android.presentation.viewModel.reelsScreenViewModel.ReelsScreenViewModel
import com.project.e_commerce.android.presentation.viewModel.restPasswordViewModel.RestPasswordViewModel
import com.project.e_commerce.android.presentation.viewModel.followingViewModel.FollowingViewModel
import com.project.e_commerce.android.presentation.viewModel.AuthViewModel
import com.project.e_commerce.android.presentation.viewModel.otherUserProfile.OtherUserProfileViewModel
import com.project.e_commerce.android.presentation.viewModel.searchViewModel.SearchViewModel
import com.project.e_commerce.android.presentation.viewModel.NotificationViewModel
import com.project.e_commerce.android.presentation.viewModel.OrderHistoryViewModel
import com.project.e_commerce.android.presentation.viewModel.RecentlyViewedViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.project.e_commerce.android.domain.usecase.FCMTokenUseCase
import com.project.e_commerce.android.domain.usecase.CreateOrderUseCase
import com.project.e_commerce.android.domain.usecase.GetUserOrdersUseCase
import com.project.e_commerce.android.domain.usecase.UpdateOrderStatusUseCase
import com.project.e_commerce.android.domain.usecase.CancelOrderUseCase
import com.project.e_commerce.android.data.repository.FirebaseOrderRepository
import com.project.e_commerce.android.domain.usecase.GoogleSignInUseCase
import com.project.e_commerce.android.data.helper.GoogleSignInHelper

val viewModelModule = module {
    try {
        // Firebase & Repo
        single {
            val firebaseAuth = FirebaseAuth.getInstance()
            android.util.Log.d("CrashDebug", "AppModule: FirebaseAuth created")
            firebaseAuth
        }
        single {
            val firebaseFirestore = FirebaseFirestore.getInstance()
            android.util.Log.d("CrashDebug", "AppModule: FirebaseFirestore created")
            firebaseFirestore
        }
        single<AuthRepository> {
            val repo = FirebaseAuthRepository(get())
            android.util.Log.d("CrashDebug", "AppModule: FirebaseAuthRepository created")
            repo
        }
        single<UserProfileRepository> {
            val repo = FirebaseUserProfileRepository(get(), get())
            android.util.Log.d("CrashDebug", "AppModule: FirebaseUserProfileRepository created")
            repo
        }
        single<FollowingRepository> {
            val repo = FirebaseFollowingRepository(get(), get())
            android.util.Log.d("CrashDebug", "AppModule: FirebaseFollowingRepository created")
            repo
        }
        single<CartRepository> {
            val auth: FirebaseAuth = get()
            val firestore: FirebaseFirestore = get()
            android.util.Log.d(
                "CrashDebug",
                "AppModule: Registering CartRepository with auth=$auth firestore=$firestore"
            )
            val repo = FirebaseCartRepository(auth, firestore)
            android.util.Log.d(
                "CrashDebug",
                "AppModule: FirebaseCartRepository constructed successfully"
            )
            repo
        }

        // Order Repository
        single<OrderRepository> {
            val repo = FirebaseOrderRepository(get(), get())
            android.util.Log.d("CrashDebug", "AppModule: FirebaseOrderRepository created")
            repo
        }

        // Notification Repositories
        single<NotificationRepository> {
            val repo = FirebaseNotificationRepository(get(), get())
            android.util.Log.d("CrashDebug", "AppModule: FirebaseNotificationRepository created")
            repo
        }

        single<NotificationSettingsRepository> {
            val repo = FirebaseNotificationSettingsRepository(get(), get())
            android.util.Log.d("CrashDebug", "AppModule: FirebaseNotificationSettingsRepository created")
            repo
        }

        // Notification Services
        single {
            val service = NotificationManagerService(androidContext())
            android.util.Log.d("CrashDebug", "AppModule: NotificationManagerService created")
            service
        }

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
        single {
            val useCase = LoginByEmailAndPasswordUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: LoginByEmailAndPasswordUseCase created")
            useCase
        }

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

        // Like-related Use Cases
        single {
            val useCase = UpdateUserLikeCountUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: UpdateUserLikeCountUseCase created")
            useCase
        }
        single {
            val useCase = LikeReelUseCase(get(), get())
            android.util.Log.d("CrashDebug", "AppModule: LikeReelUseCase created")
            useCase
        }
        single {
            val useCase = GetReelLikeStatusUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetReelLikeStatusUseCase created")
            useCase
        }
        single {
            val useCase = GetReelLikeCountUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetReelLikeCountUseCase created")
            useCase
        }

        // Comment-related Use Cases
        single {
            val useCase = AddCommentUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: AddCommentUseCase created")
            useCase
        }
        single {
            val useCase = GetReelCommentsUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetReelCommentsUseCase created")
            useCase
        }
        single {
            val useCase = LikeCommentUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: LikeCommentUseCase created")
            useCase
        }
        single {
            val useCase = GetCommentLikeStatusUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetCommentLikeStatusUseCase created")
            useCase
        }
        single {
            val useCase = GetCommentLikeCountUseCase(get())
            android.util.Log.d("CrashDebug", "AppModule: GetCommentLikeCountUseCase created")
            useCase
        }

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
                repo = get(),
                checkEmailValidation = get(),
                checkPasswordValidation = get(),
                checkMatchedPassword = get(),
                googleSignInUseCase = get(),
                googleSignInHelper = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: LoginScreenViewModel created")
            LoginScreenViewModel(
                checkLoginValidation = get(),
                checkEmailValidation = get(),
                checkPasswordValidation = get(),
                loginByEmailAndPasswordUseCase = get()
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
                getFollowersUseCase = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: EditProfileViewModel created")
            EditProfileViewModel(
                getUserProfileUseCase = get(),
                updateUserProfileUseCase = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: RestPasswordViewModel created")
            RestPasswordViewModel(get(), get(), get())
        }
        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: ProductViewModel created")
            ProductViewModel()
        }
        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: ReelsScreenViewModel creation")
            ReelsScreenViewModel(
                productViewModel = get(),
                getUserProfileUseCase = get(),
                likeReelUseCase = get(),
                getReelLikeStatusUseCase = get(),
                getReelLikeCountUseCase = get(),
                updateUserLikeCountUseCase = get(),
                addCommentUseCase = get(),
                getReelCommentsUseCase = get(),
                likeCommentUseCase = get(),
                getCommentLikeStatusUseCase = get(),
                getCommentLikeCountUseCase = get(),
                firestore = get(),
                cartRepository = get()
            )
        }
        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: CartViewModel created")
            CartViewModel(
                cartRepository = get<CartRepository>(),
                auth = get<FirebaseAuth>()
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
                getFollowersUseCase = get(),
                firestore = get()
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
                getFollowingUsersUseCase = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: SearchViewModel created")
            com.project.e_commerce.android.presentation.viewModel.searchViewModel.SearchViewModel(
                productViewModel = get(),
                firestore = get(),
                followUserUseCase = get(),
                unfollowUserUseCase = get(),
                getFollowingStatusUseCase = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: NotificationViewModel created")
            NotificationViewModel(
                getNotificationsUseCase = get(),
                getUnreadCountUseCase = get(),
                markAsReadUseCase = get(),
                createNotificationUseCase = get(),
                auth = get()
            )
        }

        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: OrderHistoryViewModel created")
            OrderHistoryViewModel(
                getUserOrdersUseCase = get(),
                cancelOrderUseCase = get(),
                auth = get()
            )
        }

        // RecentlyViewedRepository
        single {
            val repo = RecentlyViewedRepository(get(), get())
            android.util.Log.d("CrashDebug", "AppModule: RecentlyViewedRepository created")
            repo
        }
        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: RecentlyViewedViewModel created")
            RecentlyViewedViewModel(
                recentlyViewedRepository = get()
            )
        }

        // Retrofit
        single {
            val okHttp = OkHttpClient.Builder()
                .addInterceptor(Interceptor())
                .build()
            android.util.Log.d("CrashDebug", "AppModule: OkHttpClient created")
            Retrofit.Builder()
                .client(okHttp)
                .baseUrl("https://restcountries.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        single {
            val countriesApi = get<Retrofit>().create(CountriesApi::class.java)
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
