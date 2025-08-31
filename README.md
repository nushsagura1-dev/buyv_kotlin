# E-Commerce Mobile Application (Current State, August 2024)

A modern, feature-rich e-commerce app with TikTok-inspired vertical product reels, social
interactions, cloud video, and robust Firebase+Cloudinary backend.

---

## 🚀 Overview

- TikTok-like reels feed (For you/Following/Explore tabs) with videos/images for social shopping.
- Classic right-side (vertical) engagement bar: Like, Comment, Cart, Share, Music, each with counts.
- Product overlays, hashtags, and info on the left, always above the tab bar.
- Classic "Buy" FAB, docked, visible only on Reels screen, above the bottom navigation bar.
- Fully reactive UI state with Compose/Koin, no infinite loops or navigation crashes.

## 📦 Project Structure & Core Tech

- **Kotlin Multiplatform, Compose, Koin 3.x** for DI
- **Firebase Auth/Firestore** for user/data, **Cloudinary** for all media assets
- **Media3/ExoPlayer** for video
- **One navController** for the entire app (single source of tab/screen truth)
- Modern Compose state: all overlays, prompts, and engagement use robust, single-source `remember`/
  `MutableState`.

## 🔔 Notifications System Status

### ✅ **What's Currently Implemented (95% Complete)**

#### **Firebase Cloud Messaging (FCM) Integration**

- **MyFirebaseMessagingService**: Complete FCM service for receiving push notifications
- **Token Management**: Automatic FCM token registration and refresh
- **Message Processing**: Handles both data and notification payloads from Firebase
- **Firestore Integration**: Saves all notifications to user-specific collections
- **System Notifications**: Shows Android system notifications with custom styling

#### **Notification Data Layer**

- **Complete Repository Pattern**: `NotificationRepository` with full CRUD operations
- **Firebase Backend**: `FirebaseNotificationRepository` with real-time Firestore integration
- **Notification Models**: Comprehensive `FirebaseNotification` data model with 28+ notification
  types
- **Settings Management**: `NotificationSettingsRepository` for user preferences
- **Use Cases**: All notification business logic encapsulated (Get, Create, Mark as Read, etc.)

#### **Rich Notification Features**

- **28 Notification Types**: Orders, Social, Promotions, Security, App Updates, etc.
- **6 Categories**: Orders/Shipping, Social Activity, Promotions/Deals, Account/Security, App
  Updates, General
- **Priority Levels**: High, Medium, Low with appropriate Android notification channels
- **Deep Linking**: Support for navigation to specific screens from notifications
- **User Targeting**: FCM token-based user-specific notifications
- **Expiration**: Optional notification expiration dates

#### **Notification UI (Complete)**

- **NotificationScreen**: Beautiful, fully-functional notification display screen
- **Category Filtering**: Filter notifications by type (Orders, Social, Deals, Account, Updates,
  General)
- **Time-based Grouping**: Organized by Today, Yesterday, Earlier sections
- **Real-time Updates**: Live notification updates via ViewModel and StateFlow
- **Visual States**: Different styling for read/unread notifications
- **Individual Actions**: Tap to mark as read functionality
- **Loading & Error States**: Proper error handling and loading indicators
- **Empty State**: Elegant empty state design

#### **System Integration**

- **Android Permissions**: Notification permissions for Android 13+
- **Notification Channels**: Separate channels for different priority levels
- **Firebase Initialization**: Complete Firebase setup and token management
- **Dependency Injection**: Fully integrated with Koin DI system
- **Navigation**: Accessible from Profile screen and multiple entry points

#### **Development & Testing Tools**

- **NotificationTestHelper**: Create sample notifications for testing
- **FCMTokenUseCase**: Token management and topic subscription utilities
- **Runtime Permissions**: Automatic permission requests for modern Android versions
- **Logging**: Comprehensive logging for debugging and monitoring

### ⚠️ **What's Still Needed (5% Remaining)**

#### **High Priority - Quick Wins**

1. **Deep Link Navigation** (30 min implementation)
  - Navigation to specific screens when notifications are tapped
  - Currently only marks as read, doesn't navigate to `deepLink`

2. **"Mark All as Read" Button** (15 min implementation)
  - Header action button in NotificationScreen
  - Use case already exists, just needs UI button

#### **Medium Priority - UX Enhancements**

3. **Notification Badge Count** (45 min implementation)
  - Show unread count on notification icon in navigation
  - Badge logic exists in bottom bar, just needs connection

4. **Individual Notification Actions** (1 hour implementation)
  - Long-press or swipe actions for delete/mark as unread
  - Menu options for individual notifications

5. **Notification Images** (30 min implementation)
  - Display notification images using Coil
  - Currently shows icons only

#### **Low Priority - Advanced Features**

6. **Notification Settings Screen** (2 hours implementation)
  - UI for managing notification preferences
  - Repository and data models already exist

7. **Quiet Hours Implementation** (1 hour implementation)
  - Respect user quiet hours settings
  - Logic to suppress notifications during specified times

8. **Rich Media Notifications** (2 hours implementation)
  - Support for images, videos in notifications
  - Enhanced notification layouts

### 🎯 **Notification Flow (Currently Working)**

```
1. App Launch → Firebase initializes → FCM token registered
2. User Authentication → Token linked to user account
3. FCM Message Received → MyFirebaseMessagingService processes
4. Notification Saved → Firestore under users/{userId}/notifications/
5. System Notification → Android notification displayed
6. User Opens App → Real-time notifications loaded in UI
7. Tap Notification → Marks as read (deep link navigation TODO)
```

### 📱 **How to Test Notifications Right Now**

1. **Run the App**: FCM token will be automatically registered
2. **Send Test FCM**: Use Firebase Console to send a test message
3. **Create Test Notifications**: Use the built-in `createTestNotification()` method
4. **Check Firestore**: Verify notifications are stored in the database
5. **View in App**: Navigate to Profile → Notifications to see all notifications

### 🏗️ **Architecture Highlights**

- **Clean Architecture**: Separation of concerns with Repository → Use Case → ViewModel → UI
- **Real-time Updates**: Firebase Firestore listeners for live notification updates
- **Offline Support**: Notifications cached locally and sync when online
- **Scalable Design**: Supports unlimited notification types and categories
- **Type Safety**: Strongly typed notification models and enums
- **Error Handling**: Comprehensive error handling throughout the notification flow

## 🧩 Key Features (LIVE & TESTED)

- For You, Following, and Explore tabs as top tabs in Reels screen—Explore navigates, others are
  state-local.
- **Vertical right sidebar**: TikTok-style engagement with live icons/counters, never clipped,
  always correct spacing/padding.
- **Cart, Profile, and Product tabs**: All Compose/Koin/Firestore logic is error-free and robust.
- **FAB (Buy) logic**: Central floating buy FAB, Compose-docked and correctly animated on only the
  home screen.
- **Real video playback** via Media3/ExoPlayer for Cloudinary MP4 URLs; images and fallback logic in
  place.
- **Robust login/signup overlay handling**, never lost scope, and no shadowed UI state.
- **No infinite spinner or loading loop bug**: Following tab now loads just once, lists only what is
  expected, and never relaunches loading automatically after playing a reel.

## 😬 Problems and Known Issues

- **Following tab infinite loop (now fixed):** Caused by child/subcomponent repeatedly retriggering
  `loadUserData`/isLoading. SOLUTION: Only trigger loading in `LaunchedEffect(currentUserId)`, with
  a proper remember/hasLoaded flag. Removed any other calls in UserInfo etc.
- **FAB Floating Action Button** clipping: Must always be in MainActivity/Scaffold's
  floatingActionButton, never child of AppBottomBar. Avoids all visibility/clipping/overlap issues.
- **Compose navigation splitting:** Only one navController through the whole app. This means
  login/signup and cart tab never crash or break navigation.
- **Cart, Profile, Product no longer crash:** DI and Koin/viewModel wiring now robust; no more
  missing dependencies or multiple VM instance bugs.
- **Overlay engagement/interaction UI:** All like/comment/cart/share/music buttons and counts now
  always vertically stacked on the right in the feed.
- **Media3/ExoPlayer video works for all valid .mp4 URLs; shows error for missing/bad video file.**

## 🛠️ Remaining ToDos / Improvement Areas

- Upgrade all UI logic to latest Compose (migrate from deprecated Accompanist pager as soon as
  possible).
- Continue to test login/logout switching and multi-profile robustness.
- Polish engagement icons, add more nuanced UI/animation if desired.
- Keep refactoring following tab logic as needed for more users and better caching.
- **Complete remaining 5% of notification features** (deep linking, badge count, bulk actions)

## 🛡️ How to Keep It Healthy

- Always use one navController (rememberNavController at top App level).
- Guard all data fetches against recomposition/side effect triggers (never trigger loadUserData from
  UserInfo, only from main Following tab entry).
- For conditional overlays/login, use top-level `remember { mutableStateOf(...) }` as the sole
  control for overlays.
- Use fallback/error visuals for all video/image loads for best UX!
- **Monitor notification permissions** and gracefully handle permission denials on Android 13+

## 🏗️ Architecture & Technology Stack

### Core Technologies
- **Kotlin Multiplatform** - Cross-platform development
- **Jetpack Compose** - Modern declarative UI framework
- **Android SDK 36** - Latest Android platform support
- **Kotlin 2.2.10** - Latest Kotlin version
- **Gradle 8.12.1** - Modern build system

### Architecture Pattern
- **MVVM (Model-View-ViewModel)** - Clean architecture implementation
- **Repository Pattern** - Data layer abstraction
- **Use Case Pattern** - Business logic encapsulation
- **Dependency Injection** - Koin for dependency management

### Key Libraries & Dependencies
- **UI & Animation**: Compose Material3, Lottie, Coil (Glide removed for 16KB compatibility)
- **Navigation**: Navigation Compose 2.9.3
- **Networking**: Retrofit 3.0.0, OkHttp 5.1.0
- **State Management**: Kotlin Coroutines, StateFlow
- **Media**: Media3 ExoPlayer for video playback
- **Image Management**: Cloudinary for cloud storage
- **Authentication**: Firebase Auth
- **Database**: Firebase Firestore
- **Push Notifications**: Firebase Cloud Messaging (FCM)

## 📱 Application Features

### 🎬 Reels-Based Shopping Experience
- **Video Reels**: Product showcase through engaging video content
- **Interactive Elements**: Like, comment, and share functionality
- **Product Integration**: Direct product information display on reels
- **Swipe Navigation**: Vertical scrolling through product reels

### 🛍️ E-Commerce Core Features
- **Product Catalog**: Comprehensive product browsing
- **Category Management**: Organized product categorization
- **Search & Filter**: Advanced product discovery
- **Shopping Cart**: Full cart management with quantity controls
- **Payment Integration**: Secure checkout process
- **Order Tracking**: Real-time order status monitoring

### 👤 User Management
- **Authentication**: Firebase-based user registration and login
- **Profile Management**: Comprehensive user profiles
- **Favorites**: Wishlist and saved items
- **Order History**: Complete purchase history
- **Settings**: User preferences and account management

### 🔔 Notification System

- **Push Notifications**: Firebase Cloud Messaging integration
- **Real-time Updates**: Live notification feed with category filtering
- **Rich Notifications**: Support for images, deep links, and action buttons
- **Notification History**: Complete notification management and history
- **User Preferences**: Customizable notification settings per category

### 🎨 User Interface
- **Modern Design**: Material Design 3 principles
- **Responsive Layout**: Adaptive to different screen sizes
- **Dark/Light Theme**: Theme customization support
- **Smooth Animations**: Lottie animations and transitions
- **Bottom Navigation**: Intuitive app navigation

## 🏛️ Project Structure

```
e-commerceAndroidApp/
├── src/main/java/com/project/e_commerce/android/
│   ├── data/                    # Data layer
│   │   ├── model/              # Data models
│   │   ├── remote/             # API and network layer
│   │   │   ├── api/            # REST API interfaces
│   │   │   ├── interceptor/    # Network interceptors
│   │   │   └── response/       # API response models
│   │   └── repository/         # Repository implementations
│   ├── di/                     # Dependency injection
│   ├── domain/                 # Business logic layer
│   │   ├── model/              # Domain models
│   │   ├── repository/         # Repository interfaces
│   │   └── usecase/            # Business use cases
│   ├── presentation/           # UI layer
│   │   ├── services/           # Background services (FCM)
│   │   ├── ui/                 # UI components
│   │   │   ├── composable/     # Reusable composables
│   │   │   ├── navigation/     # Navigation components
│   │   │   ├── screens/        # Screen implementations
│   │   │   └── utail/          # UI utilities and helpers
│   │   ├── utils/              # Utilities and helpers
│   │   └── viewModel/          # ViewModels for each screen
│   ├── EcommerceApp.kt         # Application class
│   └── MainActivity.kt         # Main activity
```

## 🎯 Key Screens & Features

### 1. **Splash Screen**
- App branding and initialization
- First-time user detection
- Navigation routing logic

### 2. **Onboarding**
- Multi-step user introduction
- Feature highlights
- Skip functionality

### 3. **Authentication**
- Login/Registration forms
- Password reset functionality
- Email verification
- Social media integration

### 4. **Reels Screen** (Main Feature)
- Vertical video scrolling
- Product information overlay
- Interactive engagement features
- Add to cart functionality

### 5. **Product Catalog**
- Category-based browsing
- Featured products
- Best seller highlights
- Search and filtering

### 6. **Shopping Cart**
- Item management
- Quantity controls
- Price calculations
- Checkout process

### 7. **User Profile**
- Personal information
- Order history
- Favorites and saved items
- Account settings

### 8. **Notifications** (NEW)

- Real-time notification feed
- Category-based filtering
- Time-based organization
- Mark as read functionality
- Push notification integration

## 🔧 Configuration & Setup

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 36
- Kotlin 2.2.10+
- Java 17

### Build Configuration
```kotlin
android {
    namespace = "com.project.e_commerce.android"
    compileSdk = 36
    minSdk = 24
    targetSdk = 34
    
    buildFeatures {
        compose = true
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

### Key Dependencies
```kotlin
// Compose
implementation("androidx.compose.ui:ui:1.9.0")
implementation("androidx.compose.material3:material3:1.3.2")

// Navigation
implementation("androidx.navigation:navigation-compose:2.9.3")

// Dependency Injection
implementation("io.insert-koin:koin-android:4.1.0")

// Firebase
implementation("com.google.firebase:firebase-auth-ktx:23.2.1")
implementation("com.google.firebase:firebase-firestore-ktx:25.1.4")
implementation("com.google.firebase:firebase-messaging-ktx:24.1.2")

// Media & UI
implementation("androidx.media3:media3-exoplayer:1.8.0")
implementation("com.airbnb.android:lottie-compose:6.6.7")
```

## 🌐 Backend Integration

### Firebase Services
- **Authentication**: User management and security
- **Firestore**: NoSQL database for products, orders, and notifications
- **Cloud Messaging (FCM)**: Push notifications and real-time updates
- **Analytics**: User behavior tracking
- **Storage**: Media file management

### Cloudinary Integration
- **Image Optimization**: Automatic image processing
- **Video Management**: Efficient video storage and delivery
- **Organized Structure**: Folder-based organization
- **File Limits**: Optimized for mobile performance

### REST API Integration
- **Countries API**: Geographic data support
- **Custom Interceptors**: Request/response handling
- **Retrofit**: Type-safe HTTP client

## 🎨 Design System

### Color Palette
```kotlin
val PrimaryColor = Color(0xFFF4A032)        // Orange
val SecondaryColor = Color(0xFF0B649B)      // Blue
val PrimaryColorText = Color(0xFF114B7F)    // Dark Blue
val ChipsColor = Color(0xFF34BE9D)          // Green
val ErrorPrimaryColor = Color(0xFFE46962)   // Red
val NotificationColor = Color(0xFF0066CC)   // Notification Theme
```

### Typography Scale
```kotlin
val tinyFontSize = 8.sp
val smallFontSize = 12.sp
val mediumFontSize = 16.sp
val titleFontSize = 18.sp
val largeFontSize = 54.sp
```

### Spacing System
```kotlin
val tinyUnit = 4.dp
val smallUnit = 8.dp
val mediumUnit = 16.dp
val largeUnit = 24.dp
val xLargeUnit = 32.dp
```

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone [repository-url]
cd e-commerce-master-new-full
```

### 2. Open in Android Studio
- Open the project in Android Studio
- Sync Gradle files
- Install required SDK components

### 3. Configure Firebase
- Add your `google-services.json` file
- Configure Firebase project settings
- Enable Authentication, Firestore, and Cloud Messaging
- Set up FCM server key for sending notifications

### 4. Configure Cloudinary
- Update Cloudinary credentials in `CloudinaryConfig.kt`
- Set up upload presets and folders

### 5. Build and Run
```bash
./gradlew assembleDebug
```

## 📱 Platform Support

### Android
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 36 (Android 15)
- **Notification Support**: Android 13+ notification permissions

### iOS (Kotlin Multiplatform)
- Basic iOS app structure included
- Shared business logic
- Platform-specific UI implementations

## 🔒 Security Features

- **Firebase Authentication**: Secure user management
- **Network Security**: HTTPS enforcement
- **Data Validation**: Input sanitization
- **Permission Management**: Minimal required permissions
- **Notification Security**: User-specific FCM token management

## 📊 Performance Optimizations

- **Lazy Loading**: Efficient list rendering
- **Image Caching**: Coil optimization (Glide removed for 16KB compatibility)
- **Video Streaming**: ExoPlayer for smooth playback
- **Memory Management**: Proper lifecycle handling
- **Network Optimization**: Retrofit with interceptors
- **Notification Efficiency**: Smart notification batching and caching

## 🧪 Testing Strategy

- **Unit Testing**: ViewModel and Use Case testing
- **UI Testing**: Compose UI testing
- **Integration Testing**: Repository and API testing
- **Manual Testing**: User experience validation
- **Notification Testing**: FCM message testing and validation

## 📈 Future Enhancements

- **Real-time Chat**: Customer support integration
- **Enhanced Push Notifications**: Rich media and interactive notifications
- **Offline Support**: Local data caching
- **Multi-language**: Internationalization support
- **AR Shopping**: Augmented reality product preview
- **Voice Search**: Voice-activated product discovery
- **Smart Notifications**: AI-powered personalized notifications

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the terms specified in the LICENSE file.

## 📞 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation

---

**Built with ❤️ using modern Android development practices and cutting-edge technologies.**