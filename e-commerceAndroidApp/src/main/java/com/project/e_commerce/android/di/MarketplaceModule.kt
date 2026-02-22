package com.project.e_commerce.android.di

import com.project.e_commerce.android.data.api.TrackingApi
import com.project.e_commerce.android.data.api.WithdrawalApi
import com.project.e_commerce.android.data.repository.TrackingRepository
import com.project.e_commerce.android.data.repository.WithdrawalRepository
import com.project.e_commerce.android.presentation.viewModel.marketplace.MarketplaceViewModel
import com.project.e_commerce.android.presentation.viewModel.marketplace.ProductDetailViewModel
import com.project.e_commerce.data.remote.api.MarketplaceApiService
import com.project.e_commerce.data.repository.MarketplaceRepository
import com.project.e_commerce.domain.usecase.marketplace.CreatePromotionUseCase
import com.project.e_commerce.domain.usecase.marketplace.GetMyWalletUseCase
import com.project.e_commerce.domain.usecase.marketplace.GetProductByIdUseCase
import com.project.e_commerce.domain.usecase.marketplace.GetProductsUseCase
// Phase 10: Admin imports
import com.project.e_commerce.android.data.api.AdminApi
import com.project.e_commerce.android.data.repository.AdminRepository
import com.project.e_commerce.android.data.repository.marketplace.AdminMarketplaceRepository
import com.project.e_commerce.android.presentation.viewModel.admin.AdminAuthViewModel
import com.project.e_commerce.android.presentation.viewModel.admin.AdminCJImportViewModel
import com.project.e_commerce.android.presentation.viewModel.admin.AdminCommissionViewModel
import com.project.e_commerce.android.presentation.viewModel.admin.AdminDashboardViewModel
import com.project.e_commerce.android.presentation.viewModel.admin.AdminOrderViewModel
import com.project.e_commerce.android.presentation.viewModel.admin.AdminProductViewModel
import com.project.e_commerce.android.presentation.viewModel.admin.AdminUserManagementViewModel
import com.project.e_commerce.android.presentation.viewModel.admin.AdminWithdrawalViewModel
import com.project.e_commerce.android.presentation.viewModel.admin.AdminPostsViewModel
import com.project.e_commerce.android.presentation.viewModel.admin.AdminCommentsViewModel
import com.project.e_commerce.android.presentation.viewModel.admin.AdminFollowsViewModel
import com.project.e_commerce.android.presentation.viewModel.admin.AdminNotificationsViewModel
import com.project.e_commerce.android.presentation.viewModel.admin.AdminCategoriesViewModel
import com.project.e_commerce.android.presentation.viewModel.admin.AdminAffiliateSalesViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

/**
 * Module Koin pour la fonctionnalité Marketplace
 * Fournit les dépendances pour les API, repositories, use cases et ViewModels
 */
val marketplaceModule = module {
    
    // Repository (MarketplaceApiService provided by shared module)
    single {
        MarketplaceRepository(get())
    }
    
    // Use Cases
    factory {
        GetProductsUseCase(get<MarketplaceRepository>())
    }
    
    factory {
        GetProductByIdUseCase(get<MarketplaceRepository>())
    }
    
    factory {
        CreatePromotionUseCase(get<MarketplaceRepository>())
    }
    
    factory {
        GetMyWalletUseCase(get<MarketplaceRepository>())
    }
    
    // Phase 6: Tracking API & Repository
    single {
        get<Retrofit>().create(TrackingApi::class.java)
    }
    
    single {
        TrackingRepository(
            trackingApi = get(),
            context = androidContext(),
            currentUserProvider = get()
        )
    }
    
    // Phase 8: Withdrawal API & Repository
    single {
        get<Retrofit>().create(WithdrawalApi::class.java)
    }
    
    single {
        WithdrawalRepository(
            withdrawalApi = get()
        )
    }
    
    // Phase 7: Promoter Dashboard Use Cases
    factory {
        com.project.e_commerce.domain.usecase.tracking.GetPromoterAnalyticsUseCase(
            trackingRepository = get()
        )
    }
    
    // Phase 10: Admin API & Repository
    single<AdminApi> {
        get<Retrofit>().create(AdminApi::class.java)
    }
    
    single {
        AdminRepository(
            adminApi = get()
        )
    }
    
    single {
        AdminMarketplaceRepository(
            adminApi = get()
        )
    }
    
    // Payments API (Task 3.2: Stripe Integration)
    single<com.project.e_commerce.android.data.api.PaymentsApi> {
        get<Retrofit>().create(com.project.e_commerce.android.data.api.PaymentsApi::class.java)
    }
    
    // Notifications API (100% Connectivity)
    single<com.project.e_commerce.android.data.api.NotificationsApi> {
        get<Retrofit>().create(com.project.e_commerce.android.data.api.NotificationsApi::class.java)
    }
    
    // Commissions API (100% Connectivity)
    single<com.project.e_commerce.android.data.api.CommissionsApi> {
        get<Retrofit>().create(com.project.e_commerce.android.data.api.CommissionsApi::class.java)
    }
    
    // ViewModels
    viewModel {
        com.project.e_commerce.android.presentation.viewModel.payment.PaymentViewModel(
            paymentsApi = get(),
            currentUserProvider = get(),
            tokenManager = get()
        )
    }
    
    viewModel {
        com.project.e_commerce.android.presentation.viewModel.promoter.PromoterDashboardViewModel(
            getPromoterAnalyticsUseCase = get(),
            currentUserProvider = get()
        )
    }
    
    viewModel {
        com.project.e_commerce.android.presentation.viewModel.promoter.MyCommissionsViewModel(
            commissionsApi = get(),
            currentUserProvider = get(),
            tokenManager = get()
        )
    }
    
    viewModel {
        com.project.e_commerce.android.presentation.viewModel.promoter.MyPromotionsViewModel(
            marketplaceRepository = get(),
            currentUserProvider = get()
        )
    }
    
    viewModel {
        com.project.e_commerce.android.presentation.viewModel.promoter.WalletViewModel(
            marketplaceApiService = get(),
            currentUserProvider = get()
        )
    }
    
    viewModel {
        com.project.e_commerce.android.presentation.viewModel.promoter.AffiliateSalesViewModel(
            marketplaceApiService = get(),
            currentUserProvider = get()
        )
    }
    
    viewModel {
        com.project.e_commerce.android.presentation.viewModel.withdrawal.WithdrawalRequestViewModel(
            withdrawalRepository = get(),
            currentUserProvider = get()
        )
    }
    
    viewModel {
        com.project.e_commerce.android.presentation.viewModel.admin.AdminWithdrawalViewModel(
            withdrawalRepository = get(),
            currentUserProvider = get()
        )
    }
    
    viewModel {
        MarketplaceViewModel(
            getProductsUseCase = get(),
            getCategoriesUseCase = get()
        )
    }
    
    viewModel {
        ProductDetailViewModel(
            getProductByIdUseCase = get(),
            createPromotionUseCase = get(),
            getUserPostsUseCase = get(),
            currentUserProvider = get()
        )
    }
    
    viewModel {
        com.project.e_commerce.android.presentation.viewModel.order.TrackOrderViewModel(
            getOrderDetailsUseCase = get(),
            currentUserProvider = get()
        )
    }
    
    // Phase 10: Admin ViewModels
    viewModel {
        AdminAuthViewModel(
            repository = get<AdminRepository>(),
            context = androidContext()
        )
    }
    
    viewModel {
        AdminDashboardViewModel(
            repository = get<AdminRepository>(),
            context = androidContext()
        )
    }
    
    viewModel {
        AdminProductViewModel(
            adminRepository = get<AdminRepository>(),
            context = androidContext()
        )
    }
    
    viewModel {
        AdminOrderViewModel(
            repository = get<AdminRepository>(),
            context = androidContext()
        )
    }
    
    viewModel {
        AdminCommissionViewModel(
            repository = get<AdminRepository>(),
            context = androidContext()
        )
    }
    
    viewModel {
        AdminCJImportViewModel(
            repository = get(),
            context = androidContext()
        )
    }
    
    viewModel {
        AdminUserManagementViewModel(
            adminRepository = get<AdminRepository>(),
            context = androidContext()
        )
    }

    viewModel {
        AdminPostsViewModel(
            repository = get<AdminRepository>(),
            context = androidContext()
        )
    }

    viewModel {
        AdminCommentsViewModel(
            repository = get<AdminRepository>(),
            context = androidContext()
        )
    }

    viewModel {
        AdminFollowsViewModel(
            repository = get<AdminRepository>(),
            context = androidContext()
        )
    }

    viewModel {
        AdminNotificationsViewModel(
            repository = get<AdminRepository>(),
            context = androidContext()
        )
    }

    // Sprint 21: Category & Affiliate Sales Admin ViewModels
    viewModel {
        AdminCategoriesViewModel(
            repository = get<AdminRepository>(),
            context = androidContext()
        )
    }

    viewModel {
        AdminAffiliateSalesViewModel(
            repository = get<AdminRepository>(),
            context = androidContext()
        )
    }
}
