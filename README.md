# E-Commerce Mobile Application

A modern, feature-rich e-commerce mobile application built with **Kotlin Multiplatform** and **Jetpack Compose**, featuring a unique reels-based shopping experience similar to TikTok/Instagram Reels.

## 🚀 Project Overview

This is a comprehensive e-commerce platform that combines traditional online shopping with modern social media features. The app allows users to discover products through engaging video reels, browse catalogs, manage shopping carts, and complete purchases with a seamless user experience.

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
│   │   ├── ui/                 # UI components
│   │   │   ├── composable/     # Reusable composables
│   │   │   ├── navigation/     # Navigation components
│   │   │   ├── screens/        # Screen implementations
│   │   │   └── utail/          # UI utilities and helpers
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

// Media & UI
implementation("androidx.media3:media3-exoplayer:1.8.0")
implementation("com.airbnb.android:lottie-compose:6.6.7")
```

## 🌐 Backend Integration

### Firebase Services
- **Authentication**: User management and security
- **Firestore**: NoSQL database for products and orders
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
- Enable required Firebase services

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

### iOS (Kotlin Multiplatform)
- Basic iOS app structure included
- Shared business logic
- Platform-specific UI implementations

## 🔒 Security Features

- **Firebase Authentication**: Secure user management
- **Network Security**: HTTPS enforcement
- **Data Validation**: Input sanitization
- **Permission Management**: Minimal required permissions

## 📊 Performance Optimizations

- **Lazy Loading**: Efficient list rendering
- **Image Caching**: Coil optimization (Glide removed for 16KB compatibility)
- **Video Streaming**: ExoPlayer for smooth playback
- **Memory Management**: Proper lifecycle handling
- **Network Optimization**: Retrofit with interceptors

## 🧪 Testing Strategy

- **Unit Testing**: ViewModel and Use Case testing
- **UI Testing**: Compose UI testing
- **Integration Testing**: Repository and API testing
- **Manual Testing**: User experience validation

## 📈 Future Enhancements

- **Real-time Chat**: Customer support integration
- **Push Notifications**: Order updates and promotions
- **Offline Support**: Local data caching
- **Multi-language**: Internationalization support
- **AR Shopping**: Augmented reality product preview
- **Voice Search**: Voice-activated product discovery

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