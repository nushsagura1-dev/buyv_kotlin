# BuyV - Modern E-Commerce Mobile App

A **Kotlin Multiplatform** e-commerce application with TikTok-inspired vertical product reels,
social commerce, and dual-platform support (Android + iOS).

---

## 🎯 Current Priorities (Client Requirements) - ✅ COMPLETED

### Feature 1: User Experience Completion
| Element | Android | iOS | Status |
|---------|---------|-----|--------|
| Product Detail Pages | ✅ Complete (858L) | ✅ Complete (107L) | Done |
| Search Filters (categories/price) | ✅ **Added** | ✅ **ProductSearchView** | ✅ **DONE** |
| Video Upload/Post Creation | ✅ Complete (1157L) | ✅ **CreatePostView** | ✅ **DONE** |
| Followers/Following Lists | ✅ Complete | ✅ Complete | Done |

### Feature 2: Mobile Admin Panel
| Element | Android | iOS | Status |
|---------|---------|-----|--------|
| Admin Dashboard | ✅ 18 screens | ✅ **AdminDashboardView** | ✅ **DONE** |
| Product Management | ✅ Complete | ✅ **AdminProductsView** | ✅ **DONE** |
| Order Tracking | ✅ Complete | ✅ **AdminOrdersView** | ✅ **DONE** |

**✅ Implementation completed on February 15, 2026**

> See [ROADMAP.md](ROADMAP.md) for detailed implementation plan.

---

## Architecture

| Layer | Technology |
|-------|-----------|
| **Shared Module** | KMP (Kotlin Multiplatform) - Ktor, Koin, Use Cases, Repositories, Models |
| **Android** | Jetpack Compose, Material3, ExoPlayer, Coil, Retrofit (admin) |
| **iOS** | SwiftUI, AVPlayer, shared module via KoinHelper/DependencyWrapper |
| **Backend** | Python FastAPI (http://192.168.11.109:8000) |
| **Admin Web** | Flask Python (buyv_admin/) |

### Project Structure

```
buyv-kotlin/
├── shared/                     # KMP shared code (92 files)
│   └── src/commonMain/kotlin/
│       ├── data/               # DTOs, API services, repositories
│       ├── domain/             # Models, use cases (46 total)
│       └── di/                 # Koin modules, KoinHelper (iOS bridge)
├── e-commerceAndroidApp/       # Android app (55+ screens)
│   └── src/main/kotlin/
│       ├── screens/            # Compose UI screens
│       ├── viewModel/          # Android ViewModels
│       ├── di/                 # AppModule (Koin VM registration)
│       └── admin/              # Admin panel (18 screens, Retrofit-based)
├── e-commerceiosApp/           # iOS app (SwiftUI, 22 screens)
│   └── e-commerceiosApp/
│       ├── Views/              # SwiftUI views
│       ├── ViewModels/         # iOS ViewModels
│       ├── Theme/              # Colors, Typography
│       └── Utils/              # SessionManager, ErrorWrapper
├── buyv_backend/               # FastAPI backend
└── buyv_admin/                 # Flask admin panel (web)
```

## Build Configuration

```
Kotlin: 2.2.10 | AGP: 8.12.3 | Gradle: 8.14.3
Compile SDK: 36 | Target SDK: 34 | Min SDK: 24
Compose BOM: 1.9.0 | Material3: 1.3.2 | Koin: 3.5.3
Coil: 3.3.0 | Media3: 1.2.1 | Ktor: 2.3.x
```

## Features

### Core Commerce
- Product catalog with categories, search, filters
- Smart cart with quantity management and checkout
- Complete order lifecycle (create, track, cancel, history)
- Payment integration (Stripe)

### Reels & Social
- TikTok-style vertical video scrolling (For You / Following / Explore)
- Like, comment, bookmark, share interactions
- User profiles with follow/unfollow system
- Sound page with post grid
- Content creation (post/reel upload)

### User System
- Email/password authentication via FastAPI backend
- Registration, login, password reset, account deletion
- Profile management with photo uploads
- Onboarding flow (3-page with skip/next/get started)

### Shared Use Cases (46 total)
- **Auth (5)**: Login, Register, Logout, GetCurrentUser, SendPasswordReset
- **Cart (5)**: Get, Add, Update, Remove, Clear
- **Order (5)**: Create, GetByUser, GetDetails, Cancel, GetRecent
- **Post (10)**: Like, Unlike, Bookmark, Unbookmark, Create, Delete, GetLiked, GetBookmarked, CheckLike, CheckBookmark
- **Product (3)**: GetProducts, GetProductDetails, GetCategories
- **User (10)**: Follow, Unfollow, GetFollowers, GetFollowing, GetFollowingStatus, GetUserPosts, UpdateProfile, SearchUsers, GetUserProfile, DeleteAccount
- **Comment (4)**: Get, Add, Delete, Like
- **Marketplace (4)**: CreatePromotion, GetMyWallet, GetProductById, GetProducts

## API Services (Shared Module)

| Service | Endpoints |
|---------|-----------|
| **AuthApiService** | login, register, logout, getCurrentUser, sendPasswordReset |
| **PostApiService** | getFeed, searchPosts, getPostById, createPost, deletePost, like/unlike, bookmark/unbookmark, getPostsByUser, getBookmarked, getLiked |
| **ProductApiService** | getProducts, getProductDetails, getCategories, searchProducts |
| **CartApiService** | getCart, addToCart, updateCartItem, removeFromCart, clearCart |
| **OrderApiService** | createOrder, getOrdersByUser, getOrderDetails, cancelOrder, getRecentOrders |
| **UserApiService** | follow, unfollow, getFollowers, getFollowing, searchUsers, getUserProfile, updateProfile, deleteAccount |
| **CommentApiService** | getComments, addComment, deleteComment, likeComment |

## iOS Screens (28 existing - +6 new)

| Category | Screens | Status |
|----------|---------|--------|
| **Auth** | LoginView, CreateAccountView, ForgetPasswordView, OnboardingView | ✅ |
| **Product** | ProductListView, ProductDetailView, **ProductSearchView** | ✅ |
| **Cart** | CartView | ✅ |
| **Order** | OrderListView, OrderDetailView | ✅ |
| **Reels** | ReelsView, **CreatePostView** | ✅ |
| **Profile** | ProfileView, EditProfileView, SettingsView, DeleteAccountView | ✅ |
| **Social** | UserSearchView, UserProfileView, FollowListView, FavouriteView, CommentsView, NotificationView | ✅ |
| **Admin** | **AdminDashboardView**, **AdminProductsView**, **AdminOrdersView** | ✅ **NEW** |

## Android Screens (55+)

Key screens: ReelsScreen, ProductListScreen, ProductDetailScreen, CartScreen, OrderScreen,
ProfileScreen, SoundPageScreen, SearchScreen, CommentsScreen, NotificationScreen,
SettingsScreen, AdminDashboard, PromoterDashboard, WithdrawalScreen + many more.

## Quick Start

```bash
# Android
./gradlew :e-commerceAndroidApp:assembleDebug

# Backend
cd buyv_backend && python -m uvicorn main:app --host 0.0.0.0 --port 8000

# Admin
cd buyv_admin && npm start
```

## DI Architecture

**Android**: Koin ViewModels registered in `AppModule.kt`, injected via `koinViewModel()`
**iOS**: `KoinHelper.kt` (shared) exposes use cases → `DependencyWrapper.swift` wraps for Swift → ViewModels consume

## Color Palette

```
Primary Orange: #F4A032 | Secondary Blue: #0B649B
Text Blue: #114B7F | Success Green: #34BE9D
Error Red: #E46962 | Background: #F5F5F5
```

---

*Last Updated: February 15, 2026*