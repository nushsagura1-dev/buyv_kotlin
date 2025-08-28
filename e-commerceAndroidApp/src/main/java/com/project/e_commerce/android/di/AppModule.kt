package com.project.e_commerce.android.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.e_commerce.android.data.remote.api.CountriesApi
import com.project.e_commerce.android.data.remote.interceptor.Interceptor
import com.project.e_commerce.android.data.repository.AuthRepository
import com.project.e_commerce.android.data.repository.CountriesRepository
import com.project.e_commerce.android.data.repository.FirebaseAuthRepository
import com.project.e_commerce.android.data.repository.FirebaseUserProfileRepository
import com.project.e_commerce.android.data.repository.FirebaseFollowingRepository
import com.project.e_commerce.android.data.repository.FirebaseCartRepository
import com.project.e_commerce.android.domain.repository.UserProfileRepository
import com.project.e_commerce.android.domain.repository.FollowingRepository
import com.project.e_commerce.android.domain.repository.CartRepository
import com.project.e_commerce.android.domain.usecase.CheckEmailValidation
import com.project.e_commerce.android.domain.usecase.CheckLoginValidation
import com.project.e_commerce.android.domain.usecase.CheckMatchedPasswordUseCase
import com.project.e_commerce.android.domain.usecase.CheckPasswordValidation
import com.project.e_commerce.android.domain.usecase.LoginByEmailAndPasswordUseCase
import com.project.e_commerce.android.domain.usecase.*
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
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

        // ViewModels
        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: AuthViewModel created")
            AuthViewModel(
                repo = get(),
                checkEmailValidation = get(),
                checkPasswordValidation = get(),
                checkMatchedPassword = get()
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
            RestPasswordViewModel(get(), get())
        }
        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: ProductViewModel created")
            ProductViewModel()
        }
        viewModel {
            android.util.Log.d("CrashDebug", "AppModule: ReelsScreenViewModel created")
            ReelsScreenViewModel(get(), get(), get(), get())
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
                getFollowersUseCase = get()
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
