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
import com.project.e_commerce.android.domain.repository.UserProfileRepository
import com.project.e_commerce.android.domain.repository.FollowingRepository
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
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val viewModelModule = module {
    // Firebase & Repo
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single<AuthRepository> { FirebaseAuthRepository(get()) }
    single<UserProfileRepository> { FirebaseUserProfileRepository(get(), get()) }
    single<FollowingRepository> { FirebaseFollowingRepository(get(), get()) }

    // Use cases
    single { CheckMatchedPasswordUseCase() }
    single { CheckLoginValidation() }
    single { CheckEmailValidation() }
    single { CheckPasswordValidation() }
    single { LoginByEmailAndPasswordUseCase(get()) }
    
    // User Profile Use Cases
    single { GetUserProfileUseCase(get()) }
    single { UpdateUserProfileUseCase(get()) }
    single { GetUserPostsUseCase(get()) }
    single { GetUserReelsUseCase(get()) }
    single { GetUserProductsUseCase(get()) }
    single { GetUserLikedPostsUseCase(get()) }
    single { GetUserBookmarkedPostsUseCase(get()) }
    
    // Following Use Cases
    single { FollowUserUseCase(get()) }
    single { UnfollowUserUseCase(get()) }
    single { GetFollowingStatusUseCase(get()) }
    single { GetFollowingUsersUseCase(get()) }
    single { GetUserProfilesByIdsUseCase(get()) }
    single { GetFollowersUseCase(get()) }

    // ViewModels
    viewModel {
        AuthViewModel(
            repo = get(),
            checkEmailValidation = get(),
            checkPasswordValidation = get(),
            checkMatchedPassword = get()
        )
    }

    viewModel {
        LoginScreenViewModel(
            checkLoginValidation = get (),
            checkEmailValidation = get(),
            checkPasswordValidation = get(),
            loginByEmailAndPasswordUseCase = get()
        )
    }

    viewModel { 
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
        EditProfileViewModel(
            getUserProfileUseCase = get(),
            updateUserProfileUseCase = get()
        ) 
    }
    
    viewModel { RestPasswordViewModel(get(), get()) }
    viewModel { ProductViewModel() }
    viewModel { ReelsScreenViewModel(get(), get()) }
    viewModel { CartViewModel() }
    viewModel { 
        FollowingViewModel(
            followUserUseCase = get(),
            unfollowUserUseCase = get(),
            getFollowingStatusUseCase = get(),
            getFollowingUsersUseCase = get(),
            getUserProfilesByIdsUseCase = get(),
            getFollowersUseCase = get()
        ) 
    }

    // Retrofit
    single {
        val okHttp = OkHttpClient.Builder()
            .addInterceptor(Interceptor())
            .build()

        Retrofit.Builder()
            .client(okHttp)
            .baseUrl("https://restcountries.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    single { get<Retrofit>().create(CountriesApi::class.java) }
    single { CountriesRepository(get()) }
}
