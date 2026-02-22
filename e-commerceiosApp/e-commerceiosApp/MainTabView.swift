import SwiftUI
import Shared

struct MainTabView: View {
    @StateObject private var navigationManager = NavigationManager.shared
    
    var body: some View {
        TabView(selection: $navigationManager.selectedTab) {
            NavigationView {
                ProductListView()
                    .toolbar {
                        ToolbarItem(placement: .navigationBarLeading) {
                            NavigationLink(destination: MarketplaceView()) {
                                Image(systemName: "storefront")
                                    .foregroundColor(AppColors.primary)
                            }
                        }
                        ToolbarItem(placement: .navigationBarTrailing) {
                            NavigationLink(destination: NotificationView()) {
                                Image(systemName: "bell")
                                    .foregroundColor(AppColors.primary)
                            }
                        }
                    }
                    // Deep link: navigate to product detail
                    .background(
                        Group {
                            if let productId = navigationManager.targetProductId {
                                NavigationLink(
                                    destination: ProductDetailView(productId: productId),
                                    isActive: $navigationManager.showProductDetail
                                ) { EmptyView() }
                            }
                            if let userId = navigationManager.targetProfileUserId {
                                NavigationLink(
                                    destination: UserProfileView(userId: userId),
                                    isActive: $navigationManager.showProfileSheet
                                ) { EmptyView() }
                            }
                        }
                    )
            }
            .tabItem {
                Image(systemName: "house.fill")
                Text("Home")
            }
            .tag(0)
            
            NavigationView {
                ExploreView()
            }
            .tabItem {
                Image(systemName: "magnifyingglass")
                Text("Discover")
            }
            .tag(1)
            
            NavigationView {
                ReelsView()
            }
            .tabItem {
                Image(systemName: "play.rectangle.fill")
                Text("Reels")
            }
            .tag(2)
            
            NavigationView {
                CartView()
            }
            .tabItem {
                Image(systemName: "cart.fill")
                Text("Cart")
            }
            .tag(3)
                
            NavigationView {
                ProfileView()
                    .toolbar {
                        ToolbarItem(placement: .navigationBarTrailing) {
                            NavigationLink(destination: SettingsView()) {
                                Image(systemName: "gearshape")
                                    .foregroundColor(AppColors.primary)
                            }
                        }
                    }
                    // Deep link: navigate to order detail
                    .background(
                        Group {
                            if let orderId = navigationManager.targetOrderId {
                                NavigationLink(
                                    destination: OrderDetailsView(orderId: orderId),
                                    isActive: $navigationManager.showOrderDetail
                                ) { EmptyView() }
                            }
                        }
                    )
            }
            .tabItem {
                Image(systemName: "person.fill")
                Text("Profile")
            }
            .tag(4)
        }
        .accentColor(AppColors.primary)
        .navigationBarBackButtonHidden(true)
    }
}

struct HomeView: View {
    var body: some View {
        ZStack {
            AppColors.background.ignoresSafeArea()
            Text("Home Screen").font(Typography.h2)
        }
    }
}


// CartView is now in Views/Cart/CartView.swift

// ProfileView is now in Views/Profile/ProfileView.swift
