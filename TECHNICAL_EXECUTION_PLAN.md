# Plan d'Exécution Technique — BuyV KMP
**Architecte :** Senior Software Architect (KMP & Mobile Commerce)  
**Date :** 1er Mars 2026  
**Référence QA :** QA_REPORT_BUYV.md (32 tickets, 5 CRITICAL / 13 HIGH / 11 MEDIUM / 3 LOW)

---

## 1. Analyse Critique de l'Architecture Actuelle

### 1.1 Points Forts
| Aspect | Évaluation |
|--------|-----------|
| **Clean Architecture** | Bonne séparation `domain/model`, `domain/usecase`, `data/repository`, `data/remote/dto` dans `shared/commonMain`. |
| **DI Koin** | Module Koin centralisé (`SharedModule.kt`) correct — `networkModule`, `repositoryModule`, `useCaseModule` séparés. |
| **Ktor Client** | Configuration dual (public/authenticated), Content Negotiation JSON, token Bearer, timeouts — solide. |
| **Parité structurelle** | iOS possède des ViewModels/Views miroirs pour chaque écran Android. Le `shared` framework est consommé des deux côtés. |

### 1.2 Problèmes Architecturaux Critiques

#### P1 — Sérialisation `SoundDto` fragile (CRITICAL — QA #26)
```
SoundDto exige TOUS les champs comme required (val id: Int, val uid: String, …).
→ Si le backend retourne un champ manquant, Kotlinx.serialization throw MissingFieldException.
→ L'erreur brute est affichée à l'utilisateur : "Fields [id, uid, title…] are required".
```
**Impact :** iOS + Android identique (shared DTO).  
**Root Cause :** Aucun `default value` sur les champs required du `SoundDto`. Pas de `try/catch` dans `SoundNetworkRepository`.

#### P2 — Absence de Auth Guard / Guest Mode (HIGH — QA #19, #21)
```
MyNavHost.kt → isLoggedIn = true (hardcodé)
→ Aucun mode guest. L'app force Login/Onboarding avant contenu.
→ Pas d'intercepteur d'action sensible (Like, Comment, Cart, Follow → login requis).
```
**Impact :** Le ratio de conversion first-open → retention chute. Bloqueur commercial.

#### P3 — HTML brut dans caption/description produit (CRITICAL — QA #23, #25)
```
Le champ description des produits CJ contient du HTML brut (<img>, <br/>, <b>).
→ Affiché tel quel dans les TextFields caption et les overlays produit.
→ Pas de pipeline de sanitization entre DTO → Domain Model.
```
**Impact :** Android + iOS. Le `ProductMappers.kt` et `DtoMappers.kt` ne filtrent pas le HTML.

#### P4 — `Category` model trop simpliste
```kotlin
// Actuel (Category.kt)
data class Category(val id: String = "", val name: String = "", val image: String = "")
```
- Manque `nameArabic`, `slug`, `iconUrl`, `displayOrder`, `isActive` — champs présents dans le panel admin (QA #28/#29).
- Le backend `MarketplaceApiService` retourne `ProductCategory` (MarketplaceModels.kt) mais le domain model `Category` n'a pas de mapping.

#### P5 — Duplication Retrofit + Ktor
```
e-commerceAndroidApp/build.gradle.kts inclut BOTH:
✗ Retrofit2 + Gson + OkHttp interceptor (Android-only admin/payments/tracking APIs)
✓ Ktor (via shared module)
```
- Double stack réseau → taille APK +2MB, incohérence de gestion d'erreurs, double token management.
- Les endpoints Admin (`AdminApi.kt`, `PaymentsApi.kt`, `CommissionsApi.kt`, `WithdrawalApi.kt`) utilisent Retrofit au lieu du Ktor partagé.

#### P6 — Back Press non géré sur BottomSheet (CRITICAL — QA #20)
```
Pas de OnBackPressedDispatcher callback sur les BottomSheet Buy/Cart.
→ Appui retour → finish() Activity → quitte l'app.
```

#### P7 — Firebase encore dans shared/build.gradle.kts
```
shared/commonMain dépend encore de dev.gitlive:firebase-auth/firestore/storage
→ Code mort depuis la migration backend FastAPI.
→ Augmente le binaire iOS framework (Firebase pods linkés inutilement).
```

### 1.3 Matrice de Risque

| Risque | Probabilité | Impact | Priorité |
|--------|-------------|--------|----------|
| Crash serialization Sound/Product | Haute | Bloquant | SPRINT 1 |
| Back press quitte l'app | Haute | Bloquant UX | SPRINT 1 |
| Pas de Guest Mode | Haute | Conversion -70% | SPRINT 1 |
| HTML brut affiché | Haute | Confiance -50% | SPRINT 1 |
| SHA-1 Google Sign-In | Haute | Auth bloqué | SPRINT 1 |
| Double stack Retrofit/Ktor | Moyenne | Maintenance | SPRINT 3 |
| Firebase dead code | Basse | Taille binaire | SPRINT 4 |

---

## 2. Guide de Modification par Fichier

### Convention de lecture
- 🟢 `shared/commonMain` = Code partagé Android+iOS
- 🔵 `shared/androidMain` = Implémentation platform Android côté shared
- 🟠 `e-commerceAndroidApp` = UI/ViewModel Android Compose
- 🍎 `e-commerceiosApp` = UI/ViewModel iOS SwiftUI
- 🟣 `shared/iosMain` = Implémentation platform iOS côté shared
- 🐍 `buyv_backend` = FastAPI Python

---

### SPRINT 1 — CRITICAL + Stabilisation (Semaines 1-2)

#### 2.1 Fix Serialization SoundDto (CRITICAL) — ✅ Faites
**Ticket:** SOUND-001

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟢 [shared/.../dto/BackendDtos.kt](shared/src/commonMain/kotlin/com/project/e_commerce/data/remote/dto/BackendDtos.kt) | `SoundDto` | Ajout valeurs par défaut sur tous les champs |
| 🟢 [shared/.../repository/SoundNetworkRepository.kt](shared/src/commonMain/kotlin/com/project/e_commerce/data/repository/SoundNetworkRepository.kt) | `getSound()` | Wrapping `try/catch SerializationException` → `Result.Error` |
| 🟢 [shared/.../model/Sound.kt](shared/src/commonMain/kotlin/com/project/e_commerce/domain/model/Sound.kt) | `Sound` | Valeurs par défaut pour résilience |

**Changement `BackendDtos.kt` :**
```kotlin
@Serializable
data class SoundDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("uid") val uid: String = "",
    @SerialName("title") val title: String = "Original Sound",
    @SerialName("artist") val artist: String = "Unknown",
    @SerialName("audioUrl") val audioUrl: String = "",
    @SerialName("coverImageUrl") val coverImageUrl: String? = null,
    @SerialName("duration") val duration: Double = 0.0,
    @SerialName("genre") val genre: String? = null,
    @SerialName("usageCount") val usageCount: Int = 0,
    @SerialName("isFeatured") val isFeatured: Boolean = false,
    @SerialName("createdAt") val createdAt: String = ""
)
```

**Changement `SoundNetworkRepository.kt` :**
```kotlin
override suspend fun getSoundDetails(soundUid: String): Result<Sound> {
    return try {
        val dto = soundApiService.getSound(soundUid)
        Result.Success(dto.toDomain())
    } catch (e: SerializationException) {
        Result.Error("Sound information unavailable")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Unknown error")
    }
}
```

---

#### 2.2 Back Press BottomSheet Fix (CRITICAL) — ✅ Faites
**Ticket:** VIDEO-004

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟠 [e-commerceAndroidApp/.../MainActivity.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/MainActivity.kt) | `onBackPressed` | Ajout `OnBackPressedDispatcher` callback |
| 🟠 [e-commerceAndroidApp/.../ReelsView.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/ui/screens/reelsScreen/ReelsView.kt) | BottomSheet state | Exposure du `BottomSheetState` + back handler |
| 🍎 [e-commerceiosApp/.../ReelsView.swift](e-commerceiosApp/e-commerceiosApp/Views/Reels/ReelsView.swift) | Sheet dismiss | Vérifier `presentationMode.wrappedValue.dismiss()` natif (iOS gère déjà le swipe-down) |

**Implémentation Android (Jetpack):**
```kotlin
// Dans le composable contenant le BottomSheet
val bottomSheetState = rememberModalBottomSheetState()
BackHandler(enabled = bottomSheetState.isVisible) {
    scope.launch { bottomSheetState.hide() }
}
```

**iOS :** Pas de bug — les `.sheet` SwiftUI gèrent nativement le dismiss via swipe/back gesture. Vérifier avec `interactiveDismissDisabled(false)`.

---

#### 2.3 Auth Guard + Guest Mode (HIGH) — ✅ Faites
**Tickets:** AUTH-003, AUTH-004

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟢 [shared/.../local/CurrentUserProvider.kt](shared/src/commonMain/kotlin/com/project/e_commerce/data/local/CurrentUserProvider.kt) | `isGuest()` | Nouvelle méthode `fun isAuthenticated(): Boolean` |
| 🟢 **NOUVEAU** `shared/.../domain/model/AuthState.kt` | — | `sealed class AuthState { Guest, Authenticated(user) }` |
| 🟠 [e-commerceAndroidApp/.../navigation/MyNavHost.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/ui/navigation/MyNavHost.kt) | `startDestination` | Changer vers `ReelsScreen` (skip Onboarding/Login) |
| 🟠 **NOUVEAU** `e-commerceAndroidApp/.../composable/common/AuthGuard.kt` | — | Composable intercepteur `@Composable fun AuthGuard(onNeedLogin: () -> Unit, content: @Composable () -> Unit)` |
| 🟠 [e-commerceAndroidApp/.../ReelsView.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/ui/screens/reelsScreen/ReelsView.kt) | `isLoggedIn` | Remplacer `isLoggedIn = true` par query `CurrentUserProvider.isAuthenticated()` |
| 🍎 [e-commerceiosApp/.../MainTabView.swift](e-commerceiosApp/e-commerceiosApp/MainTabView.swift) | Root view | Naviguer vers ReelsView par défaut, lazy login |
| 🍎 [e-commerceiosApp/.../Views/Auth/RequireLoginPromptView.swift](e-commerceiosApp/e-commerceiosApp/Views/Auth/RequireLoginPromptView.swift) | Prompt | Réutiliser pour chaque action sensible |

**Flow cible :**
```
App Launch → SplashScreen (1s branding) → ReelsScreen (mode Guest)
                                              ↓ (Like/Comment/Cart/Follow tap)
                                         LoginBottomSheet
                                              ↓ (success)
                                         Resume action interrompue
```

**Architecture `AuthGuard` (shared logic) :**
```kotlin
// shared/commonMain — nouveau fichier
sealed class AuthAction {
    data class Like(val postId: String) : AuthAction()
    data class Comment(val postId: String) : AuthAction()
    data class AddToCart(val productId: String) : AuthAction()
    data class Follow(val userId: String) : AuthAction()
    object Upload : AuthAction()
}

// Dans le ViewModel partagé (ou Android-side)
fun requireAuth(action: AuthAction, onNeedLogin: () -> Unit, onAuthed: () -> Unit) {
    if (currentUserProvider.isAuthenticated()) onAuthed()
    else {
        pendingAction = action
        onNeedLogin()
    }
}
```

---

#### 2.4 HTML Sanitization Pipeline (CRITICAL) — ✅ Faites
**Tickets:** PROD-002, UPLOAD-002

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟢 **NOUVEAU** `shared/.../data/util/HtmlSanitizer.kt` | — | Utilitaire multiplateforme de nettoyage HTML |
| 🟢 [shared/.../data/mappers/ProductMappers.kt](shared/src/commonMain/kotlin/com/project/e_commerce/data/mappers/ProductMappers.kt) | mapping | Appliquer `HtmlSanitizer.clean()` sur `description`, `caption` |
| 🟢 [shared/.../data/remote/mapper/DtoMappers.kt](shared/src/commonMain/kotlin/com/project/e_commerce/data/remote/mapper/DtoMappers.kt) | mapping | Appliquer sanitization au mapping DTO→Domain |

**Implémentation `HtmlSanitizer.kt` (commonMain) :**
```kotlin
package com.project.e_commerce.data.util

object HtmlSanitizer {
    
    private val IMG_TAG = Regex("<img[^>]*>", RegexOption.IGNORE_CASE)
    private val BR_TAG = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val ALL_TAGS = Regex("<[^>]+>")
    private val MULTI_SPACES = Regex("\\s{2,}")
    private val MULTI_NEWLINES = Regex("\\n{3,}")
    
    /** Convertit du HTML brut en texte lisible */
    fun toPlainText(html: String?): String {
        if (html.isNullOrBlank()) return ""
        return html
            .replace(IMG_TAG, "")           // Supprime <img>
            .replace(BR_TAG, "\n")          // <br> → newline
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(ALL_TAGS, "")          // Supprime toutes les balises restantes
            .replace(MULTI_SPACES, " ")
            .replace(MULTI_NEWLINES, "\n\n")
            .trim()
    }
    
    /** Extrait les URLs d'images depuis le HTML */
    fun extractImageUrls(html: String?): List<String> {
        if (html.isNullOrBlank()) return emptyList()
        val srcRegex = Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return srcRegex.findAll(html).map { it.groupValues[1] }.toList()
    }
}
```

---

#### 2.5 Google SHA-1 + Social Auth (CRITICAL) — ✅ Faites (code)
**Ticket:** AUTH-001, AUTH-002

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟠 [e-commerceAndroidApp/google-services.json](e-commerceAndroidApp/google-services.json) | Config | Vérifier SHA-1 debug+release dans Firebase Console |
| 🟠 [e-commerceAndroidApp/.../helper/GoogleSignInHelper.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/data/helper/GoogleSignInHelper.kt) | WebClientId | Vérifier OAuth2 client ID matches Firebase |
| 🟢 [shared/.../api/AuthApiService.kt](shared/src/commonMain/kotlin/com/project/e_commerce/data/remote/api/AuthApiService.kt) | Endpoints | Ajouter `POST /auth/apple-signin` + `POST /auth/facebook-signin` |
| 🐍 [buyv_backend/app/auth.py](buyv_backend/app/auth.py) | Routes | Ajouter endpoints Apple/Facebook token verification |
| 🍎 [e-commerceiosApp/.../Services/GoogleSignInService.swift](e-commerceiosApp/e-commerceiosApp/Services/GoogleSignInService.swift) | iOS Auth | Vérifier Apple Sign-In natif (AuthenticationServices) |

**Procédure SHA-1 :**
```bash
# Debug SHA-1
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android

# Release SHA-1 (depuis local.properties keystore)
keytool -list -v -keystore e-commerceAndroidApp/buyv-release.keystore -alias buyv

# → Copier les SHA-1 dans Firebase Console → Project Settings → Android App
```

**Nouveaux DTOs (commonMain):**
```kotlin
@Serializable
data class AppleSignInRequestDto(
    @SerialName("id_token") val idToken: String,
    @SerialName("authorization_code") val authorizationCode: String,
    @SerialName("full_name") val fullName: String? = null
)

@Serializable
data class FacebookSignInRequestDto(
    @SerialName("access_token") val accessToken: String
)
```

---

#### 2.6 Crash Navigation Profil Créateur (CRITICAL) — ✅ Faites
**Ticket:** SET-002

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟠 [e-commerceAndroidApp/.../social/UserProfileScreen.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/ui/screens/social/UserProfileScreen.kt) | Null check | Guard `userId` null/empty avant fetch ViewModel |
| 🟠 [e-commerceAndroidApp/.../ReelsView.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/ui/screens/reelsScreen/ReelsView.kt) | Navigation | Vérifier userId avant `navController.navigate(userProfile/{uid})` |
| 🟢 [shared/.../repository/UserNetworkRepository.kt](shared/src/commonMain/kotlin/com/project/e_commerce/data/repository/UserNetworkRepository.kt) | `getUserProfile()` | Guard clause: `if (uid.isBlank()) return Result.Error("Invalid user")` |
| 🍎 [e-commerceiosApp/.../ViewModels/SocialViewModel.swift](e-commerceiosApp/e-commerceiosApp/ViewModels/SocialViewModel.swift) | `loadProfile()` | Même guard clause côté iOS |

---

### SPRINT 2 — ReelsView + Catégories + Save/Cart (Semaines 3-4)

#### 2.7 ReelsView UI Overlay Fixes (HIGH) — ✅ Faites
**Tickets:** VIDEO-001, VIDEO-002, VIDEO-003, VIDEO-005, VIDEO-006

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟠 [e-commerceAndroidApp/.../reelsScreen/ReelsView.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/ui/screens/reelsScreen/ReelsView.kt) | Pause icon | `AnimatedVisibility` + `LaunchedEffect(delay 1500ms)` pour fadeOut |
| 🟠 [e-commerceAndroidApp/.../reelsScreen/components/ReelContent.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/ui/screens/reelsScreen/components/ReelContent.kt) | Owner mode | `if (isOwner) { ShowEditDeleteOverlay() } else { ShowSocialButtons() }` |
| 🟠 **Drawable assets** `res/drawable/` | Icônes | Remplacer `ic_heart_filled` → `ic_heart_outlined`, `ic_comment_filled` → `ic_comment_outlined` |
| 🟠 Lottie animation | Heart | Utiliser fichier Lottie `lottie_heart_like.json` (déjà dans dépendance `lottie-compose:6.6.7`) — remplacer PNG frame-by-frame |
| 🍎 [e-commerceiosApp/.../Views/Reels/ReelsView.swift](e-commerceiosApp/e-commerceiosApp/Views/Reels/ReelsView.swift) | Même logique | Overlay states, outline icons (SF Symbols `heart` au lieu de `heart.fill`) |

**Play/Pause state machine :**
```kotlin
enum class PlayerOverlayState { HIDDEN, SHOWING_PLAY, SHOWING_PAUSE }

// Quand l'utilisateur tap → toggle play/pause
// Afficher icône pendant 1.5s puis fadeOut
var overlayState by remember { mutableStateOf(PlayerOverlayState.HIDDEN) }

LaunchedEffect(overlayState) {
    if (overlayState != PlayerOverlayState.HIDDEN) {
        delay(1500)
        overlayState = PlayerOverlayState.HIDDEN
    }
}

AnimatedVisibility(
    visible = overlayState != PlayerOverlayState.HIDDEN,
    enter = fadeIn(), exit = fadeOut()
) {
    Icon(
        imageVector = if (overlayState == PlayerOverlayState.SHOWING_PLAY) 
            Icons.Filled.PlayArrow else Icons.Filled.Pause,
        tint = Color.White.copy(alpha = 0.8f),
        modifier = Modifier.size(72.dp)
    )
}
```

**Owner Mode détection :**
```kotlin
val currentUserId = currentUserProvider.getCurrentUserId()
val isOwner = reel.userId == currentUserId

if (isOwner) {
    // Afficher: Edit, Delete, Visibility Toggle
    OwnerOverlay(onEdit = { ... }, onDelete = { ... }, onToggleVisibility = { ... })
} else {
    // Afficher: Like, Comment, Share, Cart (outlined icons)
    SocialActionsColumn(reel = reel, isLoggedIn = isLoggedIn, onNeedLogin = { ... })
}
```

---

#### 2.8 Save & Cart Unifié (HIGH) — ✅ Faites
**Ticket:** SET-004

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟢 [shared/.../usecase/cart/AddToCartUseCase.kt](shared/src/commonMain/kotlin/com/project/e_commerce/domain/usecase/cart/AddToCartUseCase.kt) | Dual action | `execute()` doit aussi appeler `bookmarkPostUseCase` |
| 🟢 **NOUVEAU** `shared/.../usecase/cart/SaveAndCartUseCase.kt` | — | Combine `AddToCart` + `BookmarkPost` en action atomique |
| 🟠 [e-commerceAndroidApp/.../reelsScreen/ReelsView.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/ui/screens/reelsScreen/ReelsView.kt) | UI | Supprimer icône Save séparée, garder uniquement Cart icon |
| 🍎 [e-commerceiosApp/.../Views/Reels/ReelsView.swift](e-commerceiosApp/e-commerceiosApp/Views/Reels/ReelsView.swift) | UI | Idem |

```kotlin
class SaveAndCartUseCase(
    private val addToCart: AddToCartUseCase,
    private val bookmarkPost: BookmarkPostUseCase
) {
    suspend fun execute(productId: String, postId: String, cartItem: CartItem): Result<Unit> {
        return try {
            addToCart.execute(cartItem)
            bookmarkPost.execute(postId) // Non-bloquant si échec
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to add to cart")
        }
    }
}
```

---

#### 2.9 Follow Button State (HIGH) — ✅ Faites
**Ticket:** SET-003

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟠 [e-commerceAndroidApp/.../reelsScreen/components/](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/ui/screens/reelsScreen/components/) | Follow btn | Post-follow success: `visibility = View.GONE` (pas `isEnabled = false`) |
| 🟠 [e-commerceAndroidApp/.../ReelsView.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/ui/screens/reelsScreen/ReelsView.kt) | Follow state | Tracker les follows dans un `SnapshotStateMap<String, Boolean>` |
| 🍎 Reels components | Follow btn | `.opacity(isFollowing ? 0 : 1)` + `.allowsHitTesting(!isFollowing)` |

---

#### 2.10 Categories — Model + Frontend + Auto-CJ Mapping (HIGH) — ✅ Faites (model + icons)
**Tickets:** CAT-001, CAT-002, CAT-005

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟢 [shared/.../model/Category.kt](shared/src/commonMain/kotlin/com/project/e_commerce/domain/model/Category.kt) | Enrichir | Ajouter champs admin |
| 🟢 [shared/.../model/marketplace/MarketplaceModels.kt](shared/src/commonMain/kotlin/com/project/e_commerce/domain/model/marketplace/MarketplaceModels.kt) | `ProductCategory` | Vérifier sync avec backend |
| 🟠 [e-commerceAndroidApp/.../ProductScreen.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/ui/screens/ProductScreen.kt) | Category row | Charger catégories dynamiques, fallback icons par slug |
| 🍎 [e-commerceiosApp/.../Views/Product/ProductListView.swift](e-commerceiosApp/e-commerceiosApp/Views/Product/ProductListView.swift) | Category row | Même intégration |
| 🐍 [buyv_backend/app/marketplace/](buyv_backend/app/marketplace/) | CJ import | Mapping automatique `cj_category → buyv_category` |

**Nouveau `Category.kt` :**
```kotlin
@Serializable
data class Category(
    val id: String = "",
    val name: String = "",
    val nameArabic: String = "",
    val slug: String = "",
    val iconUrl: String = "",
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
    val image: String = "" // legacy fallback
)
```

**Fallback icons Android (expect/actual non requis — ressources drawable) :**
```kotlin
// e-commerceAndroidApp/.../util/CategoryIcons.kt
object CategoryIcons {
    fun getDrawableRes(slug: String): Int = when (slug) {
        "electronics" -> R.drawable.ic_electronics
        "fashion" -> R.drawable.ic_fashion
        "home-garden" -> R.drawable.ic_home
        "beauty-health" -> R.drawable.ic_beauty
        "sports-outdoor" -> R.drawable.ic_sports
        "toys-kids" -> R.drawable.ic_toys
        "automotive" -> R.drawable.ic_car
        else -> R.drawable.ic_category_default
    }
}
```

**Fallback icons iOS :**
```swift
// e-commerceiosApp/Utils/CategoryIcons.swift
struct CategoryIcons {
    static func sfSymbol(for slug: String) -> String {
        switch slug {
        case "electronics": return "desktopcomputer"
        case "fashion": return "tshirt"
        case "home-garden": return "house"
        case "beauty-health": return "heart.circle"
        case "sports-outdoor": return "sportscourt"
        case "toys-kids": return "teddybear"
        case "automotive": return "car"
        default: return "square.grid.2x2"
        }
    }
}
```

---

#### 2.11 Product Card Overlay & Sizing (MEDIUM) — ✅ Faites
**Tickets:** PROD-001, PROD-004

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟠 [e-commerceAndroidApp/.../reelsScreen/components/](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/ui/screens/reelsScreen/components/) | Product card | Réduire `maxWidth` 200dp→160dp, `padding` 12dp→8dp, texte 1 ligne `maxLines=1, overflow=Ellipsis` |
| 🟠 Product overlay text | — | Limiter à `maxLines=2` avec "Tap for details" |
| 🍎 Reels components | Same | Adapter les contraintes SwiftUI correspondantes |

---

### SPRINT 3 — Settings, CJ Import, Unification Réseau (Semaines 5-6)

#### 2.12 Settings — Language Selector (HIGH) — ✅ Faites (AppLocale.kt)
**Ticket:** SET-001

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟠 [e-commerceAndroidApp/.../SettingsScreen.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/ui/screens/SettingsScreen.kt) | Language item | Implémenter `AlertDialog` avec choix locale (ar, en, fr) |
| 🟢 **NOUVEAU** `shared/.../domain/model/AppLocale.kt` | — | `enum class AppLocale(val code: String) { ARABIC("ar"), ENGLISH("en"), FRENCH("fr") }` |
| 🟠 [e-commerceAndroidApp/.../EcommerceApp.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/EcommerceApp.kt) | Locale | `AppCompatDelegate.setApplicationLocales()` on change |
| 🍎 [e-commerceiosApp/.../Views/Profile/SettingsView.swift](e-commerceiosApp/e-commerceiosApp/Views/Profile/SettingsView.swift) | Language | `Bundle.setLanguage()` + relancer UI |

**Implémentation Android :**
```kotlin
fun changeLocale(context: Context, localeCode: String) {
    val appLocale = LocaleListCompat.forLanguageTags(localeCode)
    AppCompatDelegate.setApplicationLocales(appLocale)
    // Persister le choix
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit().putString("app_locale", localeCode).apply()
}
```

---

#### 2.13 CJ Import — Filtres + Tendances (MEDIUM) — ✅ Faites
**Tickets:** CAT-003, CAT-004

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟢 [shared/.../api/MarketplaceApiService.kt](shared/src/commonMain/kotlin/com/project/e_commerce/data/remote/api/MarketplaceApiService.kt) | `getProducts()` | Ajouter paramètres `warehouse`, `shipping_country` |
| 🟠 [e-commerceAndroidApp/.../admin/AdminCJImportScreen.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/ui/screens/admin/AdminCJImportScreen.kt) | Empty state | Charger `getFeaturedProducts()` par défaut au lieu d'un écran vide |
| 🟠 Même écran | Filtres | Ajouter `DropdownMenu` catégorie, pays entrepôt, transporteur |
| 🍎 [e-commerceiosApp/.../Views/Admin/AdminCJImportView.swift](e-commerceiosApp/e-commerceiosApp/Views/Admin/AdminCJImportView.swift) | Même | Picker natifs SwiftUI |

---

#### 2.14 Unification Retrofit → Ktor (MEDIUM-HIGH) — ✅ Faites (tous endpoints — Retrofit supprimé)
**tickets:** Architecture P5

| Fichier | Scope | Modification |
|---------|-------|-------------|
| ✅ [e-commerceAndroidApp/.../data/api/AdminApi.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/data/api/AdminApi.kt) | Migration | Converti en `class AdminApi(HttpClient)` — public client + token manuel |
| ✅ `e-commerceAndroidApp/.../data/api/PaymentsApi.kt` | Migration | Supprimé (mig migré → shared `PaymentsApiService`) |
| ✅ `e-commerceAndroidApp/.../data/api/CommissionsApi.kt` | Migration | Supprimé (migré → shared `CommissionsApiService`) |
| ✅ `e-commerceAndroidApp/.../data/api/NotificationsApi.kt` | Migration | Supprimé (dead code) |
| ✅ [e-commerceAndroidApp/.../data/api/WithdrawalApi.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/data/api/WithdrawalApi.kt) | Migration | Converti en `class WithdrawalApi(HttpClient)` — authenticated client |
| ✅ [e-commerceAndroidApp/.../data/api/TrackingApi.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/data/api/TrackingApi.kt) | Migration | Converti en `class TrackingApi(HttpClient)` — authenticated client |
| ✅ [e-commerceAndroidApp/.../data/remote/api/CountriesApi.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/data/remote/api/CountriesApi.kt) | Migration | Converti en `class CountriesApi(HttpClient)` — URL absolue restcountries.com |
| ✅ [e-commerceAndroidApp/.../data/repository/TrackingRepository.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/data/repository/TrackingRepository.kt) | Mise à jour | Supprimé `Response<T>` — appels directs Ktor |
| ✅ [e-commerceAndroidApp/.../data/repository/WithdrawalRepository.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/data/repository/WithdrawalRepository.kt) | Mise à jour | Supprimé `response.isSuccessful && response.body()` |
| ✅ [e-commerceAndroidApp/.../di/MarketplaceModule.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/di/MarketplaceModule.kt) | DI | `Retrofit.create()` → `ApiClass(get(named(...)))` |
| ✅ [e-commerceAndroidApp/.../di/AppModule.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/di/AppModule.kt) | DI | Supprimé 2 beans Retrofit, `CountriesApi` → Ktor |
| ✅ `e-commerceAndroidApp/build.gradle.kts` | Dépendances | Supprimé `retrofit2`, `converter-gson`, `coroutines-adapter` |
| ✅ Models (`AdminModels`, `Order`, `CountryResponse`, `Name`, `Flags`, `Translation`, `User`) | Annotations | `@SerializedName` → `@SerialName`, `@Serializable` ajouté |

**Stratégie :** Migrer un endpoint à la fois. Créer les ApiService correspondants dans `shared/commonMain` pour que iOS bénéficie aussi des endpoints admin/payment.

---

#### 2.15 Loading State Global (LOW) — ✅ Faites
**Ticket:** UI-002

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟢 **NOUVEAU** `shared/.../domain/model/LoadingState.kt` | — | `sealed class LoadingState { Idle, Loading(message), Error(msg), Success }` |
| 🟠 **NOUVEAU** `e-commerceAndroidApp/.../composable/common/GlobalLoadingOverlay.kt` | — | Composable unique `CircularProgressIndicator`, couleur `@color/buyv_primary` |
| 🟠 [e-commerceAndroidApp/.../viewModel/MainUiStateViewModel.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/viewModel/) | State | `StateFlow<LoadingState>` centralisé |
| 🍎 [e-commerceiosApp/.../Utils/](e-commerceiosApp/e-commerceiosApp/Utils/) | Overlay | `ProgressView()` unique stylisé |

```kotlin
// GlobalLoadingOverlay.kt
@Composable
fun GlobalLoadingOverlay(loadingState: LoadingState) {
    AnimatedVisibility(visible = loadingState is LoadingState.Loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = BuyVOrange, // #FF6D00
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
        }
    }
}
```

---

### SPRINT 4 — Emoji, Sound Reuse, Caméra, Polish (Semaines 7-8)

#### 2.16 EmojiCompat (MEDIUM) — ✅ Faites
**Tickets:** UI-003

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟠 [e-commerceAndroidApp/.../EcommerceApp.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/EcommerceApp.kt) | Init | `EmojiCompat.init(BundledEmojiCompatConfig(this))` |
| 🟠 `build.gradle.kts` | Dep | `implementation("androidx.emoji2:emoji2:1.5.0")` + `emoji2-bundled` |
| 🍎 iOS | N/A | iOS gère nativement les emojis via `Text` SwiftUI. Pas de fix requis. |

#### 2.17 Sound Reuse / Audio Extraction (HIGH) — ✅ Faites
**Tickets:** SOUND-002, SOUND-003

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟢 [shared/.../api/SoundApiService.kt](shared/src/commonMain/kotlin/com/project/e_commerce/data/remote/api/SoundApiService.kt) | OK | Endpoint `incrementUsage` existe déjà |
| 🟢 **NOUVEAU expect/actual** `shared/.../domain/platform/AudioExtractor.kt` | — | `expect class AudioExtractor { suspend fun extractAudio(videoUrl: String): String }` |
| 🔵 `shared/androidMain/.../AudioExtractor.kt` | actual | `MediaExtractor` + `MediaMuxer` pour extraction AAC |
| 🟣 `shared/iosMain/.../AudioExtractor.kt` | actual | `AVAssetExportSession` pour extraction audio |
| 🟠 [e-commerceAndroidApp/.../SoundPageScreen.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/ui/screens/SoundPageScreen.kt) | "Use Sound" | Navigation vers CreatePostScreen avec `soundId` pré-sélectionné |
| 🍎 [e-commerceiosApp/.../Views/Reels/SoundPageView.swift](e-commerceiosApp/e-commerceiosApp/Views/Reels/SoundPageView.swift) | "Use Sound" | Navigation vers CreatePostView avec soundId |
| 🐍 [buyv_backend/app/sounds.py](buyv_backend/app/sounds.py) | Extract endpoint | `POST /api/sounds/extract` — extraire audio d'une vidéo uploadée côté serveur (FFmpeg) |

**expect/actual Audio Extraction :**
```kotlin
// commonMain
expect class AudioExtractor {
    suspend fun extractAudioTrack(videoUri: String): ByteArray
}

// androidMain
actual class AudioExtractor(private val context: Context) {
    actual suspend fun extractAudioTrack(videoUri: String): ByteArray {
        return withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            extractor.setDataSource(videoUri)
            // Find audio track, extract, return bytes
            // ... (MediaExtractor + MediaMuxer pipeline)
        }
    }
}

// iosMain
actual class AudioExtractor {
    actual suspend fun extractAudioTrack(videoUri: String): ByteArray {
        return suspendCancellableCoroutine { cont ->
            val asset = AVURLAsset(url = NSURL(string = videoUri))
            val exportSession = AVAssetExportSession(asset, AVAssetExportPresetAppleM4A)
            // Configure and export...
        }
    }
}
```

#### 2.18 Caméra In-App avec Filtres (Phase 2 — Feature) — ✅ Faites (framework)
**Tickets:** Future Feature

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟢 **NOUVEAU expect/actual** `shared/.../domain/platform/CameraCapture.kt` | — | Interface pour caméra cross-platform |
| 🔵 `shared/androidMain` | actual | CameraX + GPUImage filters |
| 🟣 `shared/iosMain` | actual | AVCaptureSession + CIFilter |
| 🟠 **NOUVEAU** `e-commerceAndroidApp/.../screens/camera/CameraScreen.kt` | — | UI Compose caméra |
| 🍎 **NOUVEAU** `e-commerceiosApp/Views/Camera/CameraView.swift` | — | UI SwiftUI caméra |

**Note Architecture :** La caméra est 100% native. L'`expect/actual` ne partage que l'interface. Le rendu est platform-specific.

```kotlin
// commonMain
expect class CameraController {
    fun startPreview()
    fun captureVideo(maxDurationMs: Long): Flow<CaptureState>
    fun applyFilter(filterName: String)
    fun getAvailableFilters(): List<FilterInfo>
}

data class FilterInfo(val id: String, val name: String, val previewUrl: String)
sealed class CaptureState {
    object Recording : CaptureState()
    data class Completed(val outputUri: String) : CaptureState()
    data class Error(val message: String) : CaptureState()
}
```

---

#### 2.19 Nettoyage Firebase Dead Code — ✅ Faites
**Ticket:** Architecture P7

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟢 [shared/build.gradle.kts](shared/build.gradle.kts) | Dépendances | Supprimer `dev.gitlive:firebase-auth`, `firebase-firestore`, `firebase-storage` |
| 🟠 `e-commerceAndroidApp/build.gradle.kts` | Graduel | Garder `firebase-auth-ktx` (Google Sign-In), `firebase-messaging-ktx` (Push). Supprimer `firebase-firestore-ktx`. |

---

#### 2.20 Thumbnails Profil Rectangulaires (MEDIUM) — ✅ Faites
**Ticket:** UI-004

| Fichier | Scope | Modification |
|---------|-------|-------------|
| 🟠 [e-commerceAndroidApp/.../profileScreen/ProfileScreen.kt](e-commerceAndroidApp/src/main/java/com/project/e_commerce/android/presentation/ui/screens/profileScreen/ProfileScreen.kt) | Grid layout | `GridCells.Fixed(3)` + aspect ratio `9f/16f` |
| 🍎 [e-commerceiosApp/.../Views/Profile/ProfileView.swift](e-commerceiosApp/e-commerceiosApp/Views/Profile/ProfileView.swift) | Grid layout | `LazyVGrid(columns: 3)` + `.aspectRatio(9/16)` |

---

## 3. Stratégie de Tests Backend-UI

### 3.1 Tests Unitaires (shared/commonTest)

| Couche | Quoi tester | Priorité | Fichier |
|--------|------------|----------|---------|
| `HtmlSanitizer` | `toPlainText()` vs entrées CJ réelles, `extractImageUrls()` | P0 | `HtmlSanitizerTest.kt` ✅ |
| `SoundDto` désérialisation | Réponse avec champs manquants, réponse complète, réponse null | P0 | `SoundDtoTest.kt` ✅ |
| `AuthState` / `AuthAction` logic | `isAuthenticated()` true/false, `userOrNull`, tous les `AuthAction` | P0 | `AuthStateTest.kt` ✅ |
| `SaveAndCartUseCase` | Action combinée, fallback si bookmark échoue, cart failure propagates | P1 | `SaveAndCartUseCaseTest.kt` ✅ |
| `CategoryIcons.forSlug()` | Tous slugs connus + fallback drawable | P1 | Android instrumented |
| `ProductMappers` | Mapping avec HTML brut → texte propre | P1 | (à faire si mapper ajouté) |

**Exemple test :**
```kotlin
// shared/commonTest
class HtmlSanitizerTest {
    @Test
    fun `toPlainText removes img tags and converts br to newline`() {
        val html = """<p><b>Product info:</b><br/>Color: Black<br/><img src="https://cdn.cj.com/img.jpg"></p>"""
        val result = HtmlSanitizer.toPlainText(html)
        assertEquals("Product info:\nColor: Black", result)
        assertFalse(result.contains("<img"))
        assertFalse(result.contains("<br"))
    }
    
    @Test
    fun `extractImageUrls finds all src attributes`() {
        val html = """<img src="https://a.com/1.jpg"><img src='https://b.com/2.png'>"""
        val urls = HtmlSanitizer.extractImageUrls(html)
        assertEquals(2, urls.size)
        assertEquals("https://a.com/1.jpg", urls[0])
    }
}
```

### 3.2 Tests Ktor Mock (shared/commonTest)

```kotlin
class SoundApiServiceTest {
    private val mockEngine = MockEngine { request ->
        when {
            request.url.encodedPath.endsWith("/api/sounds/abc") ->
                respond(
                    content = """{"id": 1, "uid": "abc", "title": "Test"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            else -> respondError(HttpStatusCode.NotFound)
        }
    }
    
    @Test
    fun `getSound with partial response uses defaults`() = runTest {
        val client = HttpClient(mockEngine) { install(ContentNegotiation) { json(lenientJson) } }
        val service = SoundApiService(client)
        val sound = service.getSound("abc")
        assertEquals("Test", sound.title)
        assertEquals("", sound.audioUrl) // default
    }
}
```

### 3.3 Tests Backend (buyv_backend/tests/) — ✅ Faites (P0/P1)

| Endpoint | Test | Priorité | Fichier |
|----------|------|----------|--------|
| `GET /api/sounds/{uid}` | Retourner tous les champs non-null | P0 | `test_sounds.py` ✅ |
| `POST /api/sounds/{uid}/use` | increment usageCount | P0 | `test_sounds.py` ✅ |
| `GET /api/sounds` | liste + search + genre + limit | P1 | `test_sounds.py` ✅ |
| `GET /api/sounds/trending` | trié par usageCount desc | P1 | `test_sounds.py` ✅ |
| `POST /api/sounds` | admin create + unauth guard | P1 | `test_sounds.py` ✅ |
| `DELETE /api/sounds/{uid}` | admin delete + 404 after | P1 | `test_sounds.py` ✅ |
| `GET /api/v1/marketplace/categories` | Retourner catégories actives avec tous champs | P1 | `test_marketplace_categories.py` ✅ |
| `POST /admin/marketplace/categories` | admin create + unauth guard | P1 | `test_marketplace_categories.py` ✅ |
| `DELETE /admin/marketplace/categories/{id}` | admin delete + removed from list | P1 | `test_marketplace_categories.py` ✅ |
| `POST /api/sounds/extract` | Upload vidéo → retour audio URL | P2 | — (future) |
| `POST /auth/google-signin` | Token valide → AuthResponse (nouveau user) | P0 | `test_auth_oauth.py` ✅ |
| `POST /auth/google-signin` | Token invalide → 401 | P0 | `test_auth_oauth.py` ✅ |
| `POST /auth/google-signin` | Email non vérifié → 400/401 | P0 | `test_auth_oauth.py` ✅ |
| `POST /auth/google-signin` | Admin bypass guard | P0 | `test_auth_oauth.py` ✅ |
| `POST /auth/login` | Password jamais en clair dans la réponse | P0 | `test_auth_oauth.py` ✅ |
| `POST /auth/login` | SQL injection payload → pas de 500 | P1 | `test_auth_oauth.py` ✅ |

### 3.4 Tests UI (Android Instrumented) — ✅ Faites (P0/P1)

| Scénario | Outil | Priorité | Fichier |
|----------|-------|----------|--------|
| Back press sur BottomSheet ne quitte pas l'app | Compose UI Test | P0 | `BackPressBottomSheetTest.kt` ✅ |
| Guest mode → Like → LoginSheet apparaît | Compose UI Test | P0 | `GuestModeAuthTest.kt` ✅ |
| HTML brut non visible dans caption | Espresso Text assertion | P1 | `HtmlCaptionDisplayTest.kt` ✅ |
| Catégories affichent icônes correctes | Compose UI Test | P1 | `CategoryIconsTest.kt` ✅ |
| Play/Pause icon fadeOut après 1.5s | Compose UI Test + advanceTimeBy | P1 | `PlayPauseFadeTest.kt` ✅ |

### 3.5 Tests iOS (XCTest) — ✅ Faites (P0/P1)

| Scénario | Outil | Priorité | Fichier |
|----------|-------|----------|--------|
| Sheet dismiss on back gesture | XCUITest | P0 | `SheetDismissUITest.swift` ✅ |
| Guest browsing → action → login prompt | XCUITest | P0 | `GuestModeUITest.swift` ✅ |
| Category SF Symbols rendered | XCUITest | P1 | `CategorySFSymbolsUITest.swift` ✅ |

---

## 4. Timeline de Déploiement

```
┌──────────────────────────────────────────────────────────────────────┐
│                    TIMELINE BUYV v2.0 STABILISATION                  │
├──────────┬──────────┬──────────┬──────────┬──────────┬──────────────┤
│ Semaine  │    1     │    2     │    3     │   4      │   5-6        │
│          │          │          │          │          │              │
│ SPRINT 1 │▓▓▓▓▓▓▓▓▓│▓▓▓▓▓▓▓▓▓│          │          │              │
│ CRITICAL │ Sound DTO│ Auth     │          │          │              │
│          │ BackPress│ Guard    │          │          │              │
│          │ SHA-1    │ HTML San.│          │          │              │
│          │ Crash Fix│ Guest    │          │          │              │
│          │          │          │          │          │              │
│ SPRINT 2 │          │          │▓▓▓▓▓▓▓▓▓│▓▓▓▓▓▓▓▓▓│              │
│ HIGH     │          │          │ ReelsView│ Category │              │
│          │          │          │ Save+Cart│ Follow   │              │
│          │          │          │ Overlays │ CJ Map   │              │
│          │          │          │          │          │              │
│ SPRINT 3 │          │          │          │          │▓▓▓▓▓▓▓▓▓▓▓▓▓│
│ MED+ARCH │          │          │          │          │ Settings Lang│
│          │          │          │          │          │ CJ Filters   │
│          │          │          │          │          │ Retrofit→Ktor│
│          │          │          │          │          │ Loading State│
│          │          │          │          │          │              │
│ Sem. 7-8 │                    SPRINT 4                              │
│ POLISH   │          Emoji, Sound Reuse, Thumbnails, Firebase Clean  │
│          │                                                          │
│ Sem. 9+  │                    PHASE 2 (Features)                    │
│ FEATURES │          Caméra In-App, Filtres, Audio Extract Server    │
├──────────┴──────────────────────────────────────────────────────────┤
│ RELEASE GATES                                                       │
│ ✓ RC1 (fin S4): 0 CRITICAL, <3 HIGH ouverts → TestFlight/Beta     │
│ ✓ RC2 (fin S6): 0 HIGH, <5 MEDIUM ouverts → Store Review          │
│ ✓ v2.0 (fin S8): Polish complet → Production Release               │
│ ✓ v2.1 (S9+): Phase 2 features (Caméra, Audio server-side)        │
└─────────────────────────────────────────────────────────────────────┘
```

### Jalons Critiques

| Date | Jalon | Critère Go/No-Go |
|------|-------|-------------------|
| **S2 fin** | RC1 Internal | 5 CRITICAL fermés, Guest Mode fonctionnel, Back press fix validé |
| **S4 fin** | RC2 Beta | ReelsView overlay stable, Categories chargées, Save+Cart unifié |
| **S6 fin** | Store Submission | Retrofit supprimé, Settings langue OK, 0 HIGH ouvert |
| **S8 fin** | v2.0 Production | Tous tickets MEDIUM résolus, polish complete |
| **S10+** | v2.1 Feature Drop | Caméra, Audio Extract, Filtres — via Feature Flags |

### Workflow Git Recommandé

```
main ← release/v2.0 ← develop
                            ├── fix/sound-dto-serialization
                            ├── fix/backpress-bottomsheet
                            ├── feat/guest-mode-authguard
                            ├── feat/html-sanitizer
                            ├── fix/google-sha1
                            ├── fix/crash-profile-navigation
                            ├── feat/reels-overlay-states
                            ├── feat/save-and-cart-unified
                            ├── feat/categories-frontend
                            ├── chore/retrofit-to-ktor-migration
                            └── feat/camera-in-app (Phase 2)
```

Chaque branche = 1 PR avec tests. Merge dans `develop` via squash. Cherry-pick CRITICAL dans `release/v2.0` si nécessaire.

---

## 5. Résumé des Fichiers à Créer

| # | Fichier | Module | Statut | Description |
|---|---------|--------|--------|-------------|
| 1 | `shared/.../data/util/HtmlSanitizer.kt` | 🟢 commonMain | ✅ Créé | Nettoyage HTML cross-platform |
| 2 | `shared/.../domain/model/AuthState.kt` | 🟢 commonMain | ✅ Créé | Sealed class Guest/Authenticated |
| 3 | `shared/.../domain/model/AppLocale.kt` | 🟢 commonMain | ✅ Créé | Enum locales supportées |
| 4 | `shared/.../domain/model/LoadingState.kt` | 🟢 commonMain | ✅ Créé | Sealed class loading centralisé |
| 5 | `shared/.../usecase/cart/SaveAndCartUseCase.kt` | 🟢 commonMain | ✅ Créé | Action combinée Cart+Bookmark |
| 6 | `shared/.../domain/platform/AudioExtractor.kt` | 🟢 expect | ✅ Créé | Interface extraction audio |
| 7 | `shared/androidMain/.../AudioExtractor.android.kt` | 🔵 actual | ✅ Créé | MediaExtractor impl |
| 8 | `shared/iosMain/.../AudioExtractor.ios.kt` | 🟣 actual | ✅ Créé | AVAssetExportSession impl |
| 9 | `e-commerceAndroidApp/.../composable/common/AuthGuard.kt` | 🟠 Android | ✅ Créé | Composable auth interceptor |
| 10 | `e-commerceAndroidApp/.../composable/common/GlobalLoadingOverlay.kt` | 🟠 Android | ✅ Créé | Spinner unique |
| 11 | `e-commerceAndroidApp/.../util/CategoryIcons.kt` | 🟠 Android | ✅ Créé | Fallback drawable par slug |
| 12 | `e-commerceiosApp/Utils/CategoryIcons.swift` | 🍎 iOS | ✅ Créé | Fallback SF Symbols par slug |
| 13 | `shared/commonTest/.../HtmlSanitizerTest.kt` | 🟢 Test | ✅ Créé | Tests unitaires sanitizer |
| 14 | `shared/commonTest/.../SoundDtoTest.kt` | 🟢 Test | ✅ Créé | Tests désérialisation |
| 15 | `shared/commonTest/.../api/SoundApiServiceTest.kt` | 🟢 Test | ✅ Créé | Tests Ktor MockEngine (8 scénarios) |
| 16 | `e-commerceAndroidApp/.../androidTest/ui/BackPressBottomSheetTest.kt` | 🟠 Instrumented | ✅ Créé | P0 — Back press BottomSheet |
| 17 | `e-commerceAndroidApp/.../androidTest/ui/GuestModeAuthTest.kt` | 🟠 Instrumented | ✅ Créé | P0 — Guest mode → Login dialog |
| 18 | `e-commerceAndroidApp/.../androidTest/ui/HtmlCaptionDisplayTest.kt` | 🟠 Instrumented | ✅ Créé | P1 — HTML brut non visible |
| 19 | `e-commerceAndroidApp/.../androidTest/ui/CategoryIconsTest.kt` | 🟠 Instrumented | ✅ Créé | P1 — Category icons correctes |
| 20 | `e-commerceAndroidApp/.../androidTest/ui/PlayPauseFadeTest.kt` | 🟠 Instrumented | ✅ Créé | P1 — Play/Pause icon fadeOut après 1.5s |
| 21 | `e-commerceiosApp/UITests/SheetDismissUITest.swift` | 🍎 XCUITest | ✅ Créé | P0 — Sheet dismiss swipe-down |
| 22 | `e-commerceiosApp/UITests/GuestModeUITest.swift` | 🍎 XCUITest | ✅ Créé | P0 — Guest gated actions → login prompt |
| 23 | `e-commerceiosApp/UITests/CategorySFSymbolsUITest.swift` | 🍎 XCUITest | ✅ Créé | P1 — Category SF Symbols lisibles |
| 24 | `buyv_backend/tests/test_sounds.py` | 🐍 Backend | ✅ Créé | P0/P1 — Sounds API (15 tests) |
| 25 | `buyv_backend/tests/test_marketplace_categories.py` | 🐍 Backend | ✅ Créé | P1 — Marketplace catégories (10 tests) |
| 26 | `shared/.../domain/platform/CameraController.kt` | 🟢 expect | ✅ Créé | 2.18 — Interface caméra cross-platform |
| 27 | `shared/androidMain/.../CameraController.android.kt` | 🔵 actual | ✅ Créé | 2.18 — CameraX + GPUImage impl |
| 28 | `shared/iosMain/.../CameraController.ios.kt` | 🟣 actual | ✅ Créé | 2.18 — AVCaptureSession + CIFilter impl |
| 29 | `e-commerceAndroidApp/.../viewModel/CameraViewModel.kt` | 🟠 Android | ✅ Créé | 2.18 — StateFlow ViewModel for camera |
| 30 | `e-commerceAndroidApp/.../screens/camera/CameraScreen.kt` | 🟠 Android | ✅ Créé | 2.18 — Compose full-screen camera UI |
| 31 | `e-commerceiosApp/Views/Camera/CameraView.swift` | 🍎 iOS | ✅ Créé | 2.18 — SwiftUI camera + filter strip |
| 32 | `buyv_backend/tests/test_auth_oauth.py` | 🐍 Backend | ✅ Créé | P0/P1 — Google Sign-In OAuth mock + login security (11 tests) |
| 33 | `shared/commonTest/.../domain/model/AuthStateTest.kt` | 🟢 Test | ✅ Créé | P0 — AuthState/AuthAction logic (14 tests) |
| 34 | `shared/commonTest/.../domain/usecase/cart/SaveAndCartUseCaseTest.kt` | 🟢 Test | ✅ Créé | P1 — SaveAndCartUseCase avec fakes (8 tests) |

---

## 6. Risques & Mitigations

| Risque | Mitigation |
|--------|-----------|
| Migration Retrofit → Ktor casse les écrans Admin Android | Migrer 1 API à la fois, garder double-implem temporaire, feature flag |
| Apple Sign-In nécessite Apple Developer Account + entitlements | Bloquer si pas de certificat. Préparer le backend en attendant. |
| Suppression Firebase shared casse le build iOS | Tester le framework iOS build après suppression. Garder firebase-auth Android-only (pas dans shared). |
| Audio extraction performante sur vieux devices | Limiter extraction à max 60s vidéo. Offrir fallback serveur (FFmpeg). |
| CJ import auto-categorization pas 100% précise | Mapping manuel via table backend. Flag "uncategorized" pour review admin. |

---

*Document généré le 1er Mars 2026 — Technical Execution Plan v1.0*  
*Mis à jour — 34 fichiers créés · 38 fichiers modifiés · 3 fichiers supprimés · 20 work packages ✅ Faites · Retrofit complètement supprimé*  
*Couvre: 32 tickets QA → 20 work packages → 4 Sprints → 8 semaines → Release v2.0*  
*Tests: 8 commonTest Ktor-mock + 36 unit tests + 5 instrumented Compose UI (P0/P1) + 3 iOS XCUITests (P0/P1) + 36 backend pytest (P0/P1)*
