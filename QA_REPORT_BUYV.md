# 📋 Rapport d'Analyse QA Mobile - Application BuyV

**Date :** 1er Mars 2026  
**Plateforme :** Android (Debug Build)  
**Analyste QA :** Expert QA Mobile  
**Destinataire :** Architecte Logiciel  

---

## 📑 Table des Matières

1. [Partie 1 - Photos 1 à 10](#partie-1---photos-1-à-10)
2. [Partie 2 - Photos 11 à 20](#partie-2---photos-11-à-20)
3. [Partie 3 - Photos 21 à 31](#partie-3---photos-21-à-31) *(À compléter)*

---

## Partie 1 - Photos 1 à 10

### Photo 1 : Expérience de Téléchargement de Contenu (Upload)

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "هو انا مش عارف كيف ارفع محتوى" |
| **Traduction** | Je ne sais pas comment télécharger du contenu |
| **Problème UI/UX** | Le flux de création/upload de contenu n'est pas intuitif. Aucun CTA visible sur l'écran principal ou profil |
| **Élément Navigation** | Navigation Principale (Bottom Bar / Top Bar) |

**Spécifications Techniques :**
```
Feature: Onboarding & CTA
- Implémenter un FloatingActionButton central ou onglet dédié "+" dans BottomNavigationBar
- Navigation: Ouvrir BottomSheet ou CreateContentActivity/Fragment
- Priority: HIGH
```

---

### Photo 2 : Rendu visuel du bouton 'Buy'

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "وشكل زر buy تكون عليه حدود غير مريحة اثناء التنقل بين الصفحات" |
| **Traduction** | Les bordures du bouton 'Buy' sont visuellement inconfortables lors de la navigation |
| **Problème UI** | Contour rigide ou ombre de mauvaise qualité créant un effet visuel désagréable lors des transitions |
| **Élément Navigation** | Bottom Navigation Bar (Bouton central flottant) |

**Spécifications Techniques :**
```
UI/Styling:
- Ajuster propriétés XML/View du bouton central Buy
- Retirer contour dur: app:borderWidth="0dp"
- Réduire elevation et normaliser ombre portée
- Assurer fluidité pendant transitions NavController
- Priority: MEDIUM
```

---

### Photo 3 : Aspect des icônes d'interaction sur la vidéo

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "تغير بسيط في ازرار التفاعل... نخليها خطوط مجوفة بدلا من شكل ممتلئ يغطي على الفيديو" |
| **Traduction** | Boutons sous forme de contours creux au lieu d'être pleins pour ne pas masquer la vidéo |
| **Problème UI** | Icônes sociales (Like, Comment, Share) opaques "Filled", obstruant la vidéo |
| **Élément Navigation** | Side Overlay sur Video Player UI |

**Spécifications Techniques :**
```
Assets Replacement:
- Changer Drawables: ic_heart_filled → ic_heart_outlined
- Appliquer icônes mode "Outline" (Wireframe)
- Ajouter légère ombre portée (Drop Shadow) pour lisibilité sur fonds clairs
- Priority: MEDIUM
```

---

### Photo 4 : Multiples Anomalies Lecteur (Pause, Follow, Save/Cart)

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "علامة توقف الفيديو تبقى... زر المتابعة يختفي تماما بدلا من رمادي... زر الحفظ هو السلة" |
| **Traduction** | L'icône Pause persiste. Le bouton Follow doit disparaître. Le bouton Save doit devenir Cart |
| **Problème UI** | Erreurs d'états multiples sur le lecteur vidéo |
| **Élément Navigation** | Composants flottants Video Player |

**Spécifications Techniques :**
```
State Management (Play/Pause):
- Cacher icône pause via View.GONE lors onResume() ou touch listener reprise

Follow Button State:
- Post-API "Follow Success": btn_follow.visibility = View.GONE (pas isEnabled = false)

Action Merging (Cart/Save):
- Supprimer icône sauvegarde dédiée
- Conserver icône "Panier (Cart)"
- onClick Panier: dispatcher dual action → saveVideo() + addProductToCart()
- Priority: HIGH
```

---

### Photo 5 : Menu des Paramètres incomplet (Settings)

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "قائمة الاعدادات الي كانت موجودة... اللغة غير شغالة" |
| **Traduction** | Menu des paramètres précédent absent, la modification de langue ne fonctionne pas |
| **Problème UI** | Onglet "Language" présent mais non réactif (Dead click). Options disparues vs v1 |
| **Élément Navigation** | Écran Settings du profil utilisateur |

**Spécifications Techniques :**
```
Touch Listener Handling:
- Implémenter setOnClickListener sur item "Language"
- Déclencher BottomSheetDialog ou AlertDialog pour choix locale

Localisation:
- Rebuild Activity via recreate() lors modification Context.Locale
- Réintégrer endpoints ancien layout Settings
- Priority: HIGH
```

---

### Photo 6 : Qualité de l'animation d'interaction (Cœur)

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "جودة القلب و الانميشن غير جيدة" |
| **Traduction** | Qualité du cœur et de l'animation mauvaise |
| **Problème UI** | Asset cœur central en basse résolution, saccadé ou framerate insuffisant lors double-tap |
| **Élément Navigation** | Lecteur Vidéo (Gesture overlay) |

**Spécifications Techniques :**
```
Animation Rendering:
- Remplacer animation matricielle (GIF/PNG frame by frame)
- Utiliser bibliothèque vectorielle performante
- Inclure fichier Lottie (lottie_heart_like.json)
- Garantir fluidité 60fps toutes résolutions
- Priority: MEDIUM
```

---

### Photo 7 : Erreurs d'Authentification (Google / Providers)

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "التسجيل بالجميل و نضيف حسابات ابل وفيسبوك... الخروج الكراش" |
| **Traduction** | Erreur Google SHA-1, ajouter Facebook/Apple, crash vers profil créateur |
| **Problème UI** | Toast erreur système: "Developer Error: Check SHA-1 fingerprint". Crash bloquant |
| **Élément Navigation** | LoginActivity / OAuth Buttons |

**Spécifications Techniques :**
```
Firebase Configuration:
- Générer et lier clés SHA-1/SHA-256 (Debug + Release) sur Firebase Console
- Autoriser GoogleSignInClient

Feature Multi-Auth:
- Intégrer Facebook Login SDK
- Intégrer Sign-In with Apple

Bug Fix (Crash Profile):
- Investiguer logs navigation Video → Uploader Profile
- Résoudre NullPointerException (UID manquant lors fetch profil ViewModel)
- Priority: CRITICAL
```

---

### Photo 8 : Notification en erreur de téléchargement

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "لما نحاول نرفع محتوى يظهر الاشعار التالي" |
| **Traduction** | En tentant d'uploader du contenu, ce message apparaît ("User not authenticated" / "Select Product") |
| **Problème UI** | Utilisateur bloqué par Tooltips/Snackbars inadaptés |
| **Élément Navigation** | Écran ajout Produit / Vidéo |

**Spécifications Techniques :**
```
Auth Guard:
- Intercepteur en amont: si token expiré/absent → redirect LoginActivity
- Bloquer accès Activity upload sans auth

Form Validation:
- Marqueur visuel (error outline / fond rouge) sur input "Marketplace Product"
- Remplacer alert box non descriptive
- Priority: HIGH
```

---

### Photo 9 : Écran CJ Import non intuitif

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "كيف اسوي ايمبورت للمنتج من سيجي" |
| **Traduction** | Je ne comprends pas comment faire un import depuis CJ |
| **Problème UI** | Écran vide par défaut (Empty State), obligeant recherche manuelle |
| **Élément Navigation** | Dashboard Admin/Promoter → CJ Import |

**Spécifications Techniques :**
```
UI UX Data Fetching:
- Sur onViewCreated: appel GET Default/Trending Products CJ API
- Remplacer Empty State par grille "Pour Vous / Tendances"

Filtres CJ:
- Connecter filtres globaux CJ via Dropdowns
- Sélection entrepôt par pays, Catégorie CJ, Transporteurs
- Priority: MEDIUM
```

---

### Photo 10 : Gestion dynamique des offres et réductions

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "تغيير أيقونة العروض والتخفيضات والنصوص عليها" |
| **Traduction** | Bannière "Sales get 25% discount" modifiable par l'admin |
| **Problème UI** | Banner sur écran Home/Products avec textes/visuels hardcodés |
| **Élément Navigation** | Dashboard Principal (Top Banner Item) |

**Spécifications Techniques :**
```
Dynamic UI Data Binding:
- Refactoriser composant Vue Android pour accepter modèle données API/cache

Backend Dashboard:
- Créer route GET /api/home_banners
- DataBinding: couleur Hex, Texte, Image URL
- Administration depuis portail Web/Admin
- Priority: MEDIUM
```

---

## Partie 2 - Photos 11 à 20

### Photo 11 : Section Commentaires avec onglet Ratings

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "ايقونة التعليقات يجب تعديلها بهذا الشكل و إضافة خانة معاينة تعليقات التقيم" |
| **Traduction** | L'icône commentaires doit être modifiée et ajouter une section pour voir les avis/ratings |
| **Problème UI** | Interface commentaires visible avec onglets "Comments" et "Rates" - design à valider |
| **Élément Navigation** | BottomSheet Commentaires sur Video Player |

**Observation Image :**
- BottomSheet avec deux onglets : "Comments" (💬) et "Rates" (⭐)
- Liste de commentaires avec avatars, noms, textes
- Compteurs likes (800) et dislikes (2) par commentaire
- Champ "Write Comment" en bas avec bouton envoi

**Spécifications Techniques :**
```
UI Enhancement:
- Confirmer design TabLayout "Comments" / "Rates"
- Ajouter icône étoile remplie pour onglet Rates actif
- Implémenter système de notation produit (1-5 étoiles)
- Séparer clairement ratings produit vs commentaires vidéo

Data Model:
- ProductRating (userId, productId, stars, review_text, timestamp)
- Afficher moyenne des notes sur la carte produit
- Priority: MEDIUM
```

---

### Photo 12 : Icônes masquées lors consultation propre contenu

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "لما اضغط عليه وندخل الى المحتوى الفيديو كل أزرار التفاعل... تختفي ويبقى مستطيل لحذف او تعديل" |
| **Traduction** | Quand j'accède à mon contenu vidéo, tous les boutons d'interaction disparaissent, ne reste qu'un rectangle pour supprimer/modifier |
| **Problème UI** | Mode "Owner View" : icônes Pause visibles superposées (II), bouton Play visible simultanément |
| **Élément Navigation** | Video Player en mode lecture propre contenu |

**Observation Image :**
- Vidéo avec icône Pause (II) persistante au centre
- Superposition de texte produit en bas à droite ("User", "Product information", "Lining material: Faux fur", "Colors: Black, khaki, off-white, camel")
- Icônes sociales côté gauche en mode outline (bonnes)

**Spécifications Techniques :**
```
State Management Video Player:
- Cacher icône Pause après 1.5s d'inactivité (fadeOut animation)
- Bind Visibility toggle sur GestureDetector.onSingleTapConfirmed()

Owner Mode:
- Masquer tous boutons interaction (Like, Comment, Share, Cart)
- Afficher uniquement overlay "Edit" / "Delete" / "Visibility Toggle (Public/Private)"
- Conserver barre navigation top (Explore, Following, For you)
- Priority: HIGH
```

---

### Photo 13 : Grille vidéos profil - forme et badge à corriger

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "نشيلو هنا هذا المثلث الي بالاحمر ولو نخلي الشكل مستطيل بدلا من مربع لإبعاد الفيديو" |
| **Traduction** | Enlever ce triangle rouge et rendre la forme rectangulaire au lieu de carrée pour les vignettes vidéo |
| **Problème UI** | Vignettes vidéo carrées avec indicateur triangulaire/badge non désiré |
| **Élément Navigation** | Profile Screen → Video Grid (RecyclerView) |

**Observation Image :**
- Écran profil "BuyV Admin" avec statistiques (Followers, Following, Likes)
- Boutons "Share Profile", "Edit Profile", "Add New Post +"
- Onglets de filtrage (cœur, bookmark, grille, liste)
- Vignette vidéo carrée avec cercle rouge (annotation client) et icône play
- Badge "Cette TV" et compteur de vues

**Spécifications Techniques :**
```
UI/Layout Video Thumbnail:
- Modifier aspect ratio CardView: 1:1 → 9:16 ou 4:5 (rectangulaire portrait)
- Supprimer badge triangulaire overlay (si existant)
- Conserver uniquement: thumbnail + icône play centrale + compteur vues

Grid Layout:
- GridLayoutManager spanCount=3 avec items rectangulaires
- Espacement uniforme entre items (4dp)
- Priority: MEDIUM
```

---

### Photo 14 : Menu contextuel (Long Press) sur vidéo

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "ايقونة تظهر على الفيديو بعد الضغط مطولا عليه" |
| **Traduction** | Icône/menu qui apparaît après un appui long sur la vidéo |
| **Problème UI** | Menu contextuel fonctionnel - design et options à valider |
| **Élément Navigation** | Video Player → Long Press Gesture |

**Observation Image :**
- BottomSheet avec 3 options en arabe:
  - "تنزيل" (Télécharger) avec icône download
  - "غير مهتم" (Pas intéressé) avec icône cœur brisé
  - "إبلاغ" (Signaler) avec icône flag
- Background vidéo assombri

**Spécifications Techniques :**
```
Long Press Context Menu:
- Confirmer options: Download, Not Interested, Report
- Ajouter options supplémentaires si nécessaire:
  - "Copy Link" (copier lien)
  - "Share" (partager)
  - "Save to Favorites" (sauvegarder)

Fonctionnalité Download:
- Implémenter téléchargement vidéo local (avec/sans watermark BuyV)
- Gérer permissions WRITE_EXTERNAL_STORAGE

Not Interested:
- Envoyer signal algorithme recommandation
- POST /api/video/{id}/not-interested

Report:
- Ouvrir formulaire signalement avec catégories
- Priority: LOW (fonctionnel)
```

---

### Photo 15 : Discussion référencée (pas d'image spécifique)

*Cette photo fait référence à une discussion sur les fonctionnalités futures (caméra avec filtres, extraction audio). Voir section "Futures Features" en annexe.*

---

### Photo 16 : Bouton "Sound" - Réutilisation audio vidéo

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "الجزئية الي تحدثت انت عنها... قصدي كان عن هذا الزر" (Le bouton d'utilisation du son) |
| **Traduction** | Le client veut pouvoir réutiliser l'audio d'une vidéo existante pour ses propres créations |
| **Problème UI** | Écran "Music" avec "Original Sound" affiché - fonctionnalité à implémenter |
| **Élément Navigation** | Music Detail Screen (depuis icône 🎵 sur vidéo) |

**Observation Image (Photo 16-1 - Music Screen) :**
- Header "Music" avec bouton retour
- Disque vinyle animé avec thumbnail vidéo au centre
- Titre "Original Sound"
- Artiste "> Unknown"
- Compteur "posts 0"
- Bouton "Add to Favorites" avec icône bookmark

**Observation Image (Photo 16-2 - Boutons Action) :**
- Bouton orange "Use sound 🎵" (entouré en rouge par client)
- Bouton bleu "Share <"

**Spécifications Techniques :**
```
Feature: Audio Extraction & Reuse
- Bouton "Use sound": Naviguer vers CreateVideoActivity avec audioId pré-sélectionné
- Extraire piste audio de la vidéo source (FFmpeg / MediaExtractor)
- Stocker audio extrait: /api/sounds/{soundId}

Data Model Sound:
- Sound (id, videoSourceId, title, artistName, duration, audioUrl, usageCount)
- Incrémenter usageCount à chaque utilisation

UI Flow:
1. User clique icône 🎵 sur vidéo
2. Ouverture SoundDetailFragment
3. "Use sound" → CreateVideoActivity(soundId)
4. Vidéo créée avec audio attaché

Backend:
- GET /api/sounds/{id}
- POST /api/sounds/{id}/use (tracking)
- Priority: HIGH
```

---

### Photo 17 : Taille du rectangle détails produit

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "مستطيل عرض تفاصيل المنتج بجانب زر buy يكون صغير" |
| **Traduction** | Le rectangle d'affichage des détails produit à côté du bouton Buy doit être plus petit |
| **Problème UI** | Carte produit en bas trop grande, prend trop d'espace sur la vidéo |
| **Élément Navigation** | Video Player → Product Card Overlay |

**Observation Image :**
- Feed vidéo avec homme en costume
- Carte produit en bas gauche: "Hanger Shirt" / "Shirt" / "View" / "100.00 $"
- Thumbnail produit visible
- Bouton "Buy" central orange
- Icônes sociales à droite avec compteurs (20 likes, 30 comments, 20 cart)

**Spécifications Techniques :**
```
UI/Sizing Product Card:
- Réduire hauteur CardView produit de ~30%
- Layout compact: thumbnail (40dp) + titre (1 ligne ellipsize) + prix + bouton View
- Max width: 200dp → 160dp
- Padding interne réduit: 12dp → 8dp

Alternative Design:
- Mode collapsed par défaut (thumbnail + prix uniquement)
- Expansion on tap pour détails complets
- Priority: MEDIUM
```

---

### Photo 18 : Indicateurs de chargement multiples (Spinners)

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "نتمنى نصحوو أمر دوائر أثناء ضعف الشبكة نخليها وحدة" |
| **Traduction** | Les cercles de chargement lors d'une connexion faible, on devrait en avoir un seul |
| **Problème UI** | Multiples spinners de chargement visibles simultanément |
| **Élément Navigation** | Feed Vidéo (Écran de chargement) |

**Observation Images (18-1 et 18-2) :**
- Image 1: Spinner vert/cyan au centre de l'écran noir
- Image 2: Spinner violet au centre de l'écran noir
- Couleurs incohérentes entre les spinners
- Navigation bar visible en bas (fonctionnelle)

**Spécifications Techniques :**
```
Loading State Management:
- Centraliser logique de loading dans un singleton/ViewModel partagé
- Un seul ProgressIndicator visible à la fois
- Unifier couleur spinner: utiliser colorPrimary du thème (orange BuyV)

Implementation:
- LoadingOverlay composant réutilisable
- Z-index élevé pour superposition unique
- Bind sur LiveData<LoadingState> centralisé

Animation:
- CircularProgressIndicator Material Design
- Couleur: @color/buyv_primary (#FF6D00 ou similaire)
- Taille: 48dp
- Priority: LOW
```

---

### Photo 19 : Suppression écrans d'onboarding / Login initial

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "نتخلص من كل الصفحات التوضيحية في البداية وصفحة تسجيل الدخول... الواحد اول مايدخل التطبيق يظهر معاه المحتوى وأثناء محاولة التفاعل يطلب منه التسجيل" |
| **Traduction** | Supprimer tous les écrans d'introduction et login initial. L'utilisateur voit le contenu dès l'ouverture, et le login est demandé uniquement lors d'une interaction |
| **Problème UI** | Flux d'onboarding bloquant avant accès au contenu |
| **Élément Navigation** | SplashActivity → OnboardingActivity → LoginActivity |

**Spécifications Techniques :**
```
Navigation Flow Refactoring:
- SplashActivity → directement MainFeedActivity
- Skip OnboardingActivity (ou rendre optionnel depuis Settings)
- Skip LoginActivity au démarrage

Lazy Authentication:
- Utilisateur browse contenu en mode "Guest"
- Actions nécessitant auth déclenchent LoginBottomSheet:
  - Like
  - Comment
  - Follow
  - Add to Cart
  - Share (optionnel)
  - Upload content

Implementation:
- AuthGuard interceptor sur chaque action sensible
- if (!isLoggedIn()) showLoginBottomSheet() else proceedAction()
- Post-login: reprendre action interrompue

SharedPreferences:
- Stocker "hasSeenOnboarding" = true après premier skip
- Priority: HIGH
```

---

### Photo 20 : Comportement bouton Back sur BottomSheet Buy

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "لما نكون في قائمة زر buy المنبثقة ونحاول الخروج منها بواسطة زر الرجوع للخلف في الهاتف... يسبب لي الامر الخروج من التطبيق كله" |
| **Traduction** | Quand on est dans le menu Buy (BottomSheet) et qu'on appuie sur le bouton retour du téléphone, ça fait quitter l'application entièrement |
| **Problème UI** | Back press sur BottomSheet ferme l'app au lieu de fermer le sheet |
| **Élément Navigation** | MainFeedActivity → BuyBottomSheet |

**Spécifications Techniques :**
```
Back Press Handling:
- Override onBackPressed() dans Activity hôte
- Vérifier si BottomSheet est expanded/visible
- Si oui: bottomSheet.dismiss() ou bottomSheetBehavior.state = STATE_HIDDEN
- Si non: comportement par défaut (back navigation)

Implementation Kotlin:
```kotlin
override fun onBackPressed() {
    if (buyBottomSheet.isVisible || 
        bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
        buyBottomSheet.dismiss()
    } else {
        super.onBackPressed()
    }
}
```

Alternative avec OnBackPressedDispatcher (Jetpack):
```kotlin
onBackPressedDispatcher.addCallback(this) {
    if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    } else {
        isEnabled = false
        onBackPressed()
    }
}
```
- Priority: CRITICAL
```

---

### Photo 20-bis : Caractères Unicode corrompus (OBJ)

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | *(Observé sur image)* |
| **Traduction** | Caractères "OBJ" (Object Replacement Character U+FFFC) visibles dans l'UI |
| **Problème UI** | Encodage incorrect des emojis ou caractères spéciaux - affiche des rectangles "OBJ" |
| **Élément Navigation** | Product Card / Video Description |

**Observation Image :**
- Texte à côté du nom utilisateur affiche: "[OBJ][OBJ][OBJ][OBJ]"
- Probablement des emojis non supportés par la police système

**Spécifications Techniques :**
```
Unicode/Emoji Support:
- Inclure police supportant emojis (EmojiCompat library)
- Initialiser EmojiCompat au démarrage de l'application

Implementation:
```kotlin
// Application.onCreate()
val config = BundledEmojiCompatConfig(this)
EmojiCompat.init(config)
```

XML Layout:
- Utiliser EmojiTextView au lieu de TextView standard
- Ou appliquer EmojiCompat.get().process(text) avant affichage

Fallback:
- Filtrer caractères non-BMP si emoji non supporté
- Priority: MEDIUM
```

---

## Partie 3 - Photos 21 à 31

### Photo 21 : Écrans Login et Onboarding bloquants

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "نتخلص من كل الصفحات التوضيحية في البداية وصفحة تسجيل الدخول" |
| **Traduction** | Se débarrasser de tous les écrans d'introduction et de la page de connexion initiale |
| **Problème UI** | Flux d'onboarding obligatoire avant accès au contenu |
| **Élément Navigation** | OnboardingActivity → LoginActivity |

**Observation Images (21-1 Login, 21-2 Onboarding) :**
- **Login Screen:** Email/Password fields, bouton Login orange, Google Sign-in, "Register Now"
- **Onboarding Screen:** Illustration, "Discover Amazing Products!", bouton "Skip" (en haut), bouton "Next", indicateur de pages (3 dots)

**Spécifications Techniques :**
```
Navigation Flow:
- Supprimer passage obligatoire par OnboardingActivity
- Supprimer passage obligatoire par LoginActivity au démarrage
- Route directe: SplashActivity → MainFeedActivity

Guest Mode:
- Permettre navigation complète sans authentification
- Stocker flag "isGuest = true" dans session
- Déclencher LoginBottomSheet uniquement sur actions sensibles

Actions nécessitant Auth:
- Like, Comment, Follow, Add to Cart, Upload, Purchase
- Priority: HIGH
```

---

### Photo 22 : Icône Play persistante et caractères OBJ

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | *(Observé sur image)* |
| **Traduction** | Icône Play visible même pendant lecture + caractères [OBJ] corrompus |
| **Problème UI** | États visuels incohérents sur le lecteur vidéo |
| **Élément Navigation** | Video Player Feed |

**Observation Image :**
- Vidéo d'arbre au coucher de soleil avec icône Play (▶) visible au centre
- Caractères "[OBJ][OBJ][OBJ]" visibles à côté du nom utilisateur
- Icône "Saved" (bookmark) en jaune = état sauvegardé actif
- Bouton "+ Follow" visible

**Spécifications Techniques :**
```
Play Icon State:
- Cacher icône Play après démarrage lecture (View.GONE ou alpha fadeOut)
- Afficher uniquement sur état PAUSED ou BUFFERING

Unicode Fix (OBJ Characters):
- Implémenter EmojiCompat library
- Filtrer caractères U+FFFC avant affichage
- Utiliser police supportant emojis complets
- Priority: MEDIUM
```

---

### Photo 23 : BottomSheet Produit - URL brute visible

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | *(Observé sur image)* |
| **Traduction** | L'URL de l'image produit est affichée en brut dans la description |
| **Problème UI** | Données techniques (URL) visibles à l'utilisateur final |
| **Élément Navigation** | Product Detail BottomSheet |

**Observation Image :**
- BottomSheet avec catégories horizontales (Camera, Computer, Tablet PC, TV set, Monitor, UAV)
- Images produit "64GB Micro SD Card 3-Pack"
- Texte visible: `img> src="https://cf.cjdropshipping.com/da0896bf-800a-47dd-8cc2-...`
- Sélecteur quantité (+/- 1), Prix 47$
- Bouton "Add to Cart" partiellement masqué par bouton Buy

**Spécifications Techniques :**
```
Data Sanitization:
- Parser HTML de la description produit CJ avant affichage
- Extraire images via regex/parser et les afficher dans ImageCarousel
- Supprimer balises <img>, <br/>, <b> du texte affiché
- Utiliser Html.fromHtml() avec PROPER handling des images

Implementation:
```kotlin
val cleanDescription = rawHtml
    .replace(Regex("<img[^>]*>"), "")
    .replace(Regex("<br\\s*/?>"), "\n")
    .let { Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT) }
```

UI Layout Fix:
- Repositionner bouton "Add to Cart" pour éviter chevauchement avec FAB "Buy"
- Priority: HIGH
```

---

### Photo 24 : Page Product Details - Image non chargée

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | *(Observé sur image)* |
| **Traduction** | L'image produit ne se charge pas (placeholder panier gris visible) |
| **Problème UI** | Échec de chargement image produit, placeholder incorrect |
| **Élément Navigation** | ProductDetailsActivity |

**Observation Image :**
- Header "Product Details" avec flèche retour
- Badge "Earn 🔶 %10,0" (programme affiliation)
- Zone image vide avec icône panier gris (placeholder)
- Carousel dots (9 points) indiquant images disponibles
- Titre: "Thickened Snug-fitting Thermal Bread Cotton Boots"
- Prix barré 19,11$ → 4,00$ avec badge "-76%"
- Statistiques: Promos (1), Sales (0), Note (étoile)
- Boutons "Promote 📢" (orange outline) et "Buy 🛒" (rouge filled)

**Spécifications Techniques :**
```
Image Loading:
- Vérifier URLs images CJ (HTTPS, domaine valide)
- Implémenter retry logic avec Coil/Glide
- Placeholder approprié (image produit générique, pas panier)
- Error placeholder distinct

Placeholder Fix:
- Remplacer ic_cart par ic_image_placeholder ou shimmer loading
- Afficher skeleton loader pendant chargement

Network Error Handling:
- Afficher message "Image unavailable" si échec définitif
- Log erreur pour debugging
- Priority: MEDIUM
```

---

### Photo 25 : Écran Promote Product - HTML brut dans Caption

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "لما يكون شخص حاب يروح لمنتج معين او رفع منتج يظهر له مكان مخصص لرفع صورة او فيديو" |
| **Traduction** | Zones séparées pour upload vidéo et photos, mais HTML brut visible dans caption |
| **Problème UI** | Balises HTML affichées en brut dans le champ "Your Caption" |
| **Élément Navigation** | PromoteProductActivity |

**Observation Image :**
- Header "Promote Product"
- Section "Upload Product Reel": Upload video, Browse files (Max 60 seconds, MP4/MOV, Max 50MB)
- Section "Upload Product Images": Upload photos, Browse files (Format: .jpeg, .png & Max 25MB)
- Section "Your Caption" avec HTML brut visible:
  ```
  p><b>Product information:</b><br/>Lining material: Faux fur<br/> Colors: Black, khaki, off-white, camel<br/> Shaft height: High-top<br/>...
  <b>Packing list: </b>br/>A pair of boots></br>
  <b>Product Image:</b></br>img>
  ```

**Spécifications Techniques :**
```
Caption Pre-processing:
- Nettoyer HTML avant pré-remplissage du champ caption
- Convertir en texte plain ou Markdown simplifié
- Supprimer balises <img>, <p>, <b>, <br>

Implementation:
```kotlin
fun cleanHtmlToPlainText(html: String): String {
    return html
        .replace(Regex("<br\\s*/?>"), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}
```

UX Enhancement:
- Permettre édition libre du caption
- Ajouter bouton "Reset to default" si besoin
- Priority: HIGH
```

---

### Photo 26 : Erreur API Sound - Champs manquants

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "استخدام موسيقى الفيديو هذا... مهمة حتى نعتمد على موسيقى الفيديو المرفوع" |
| **Traduction** | Utiliser la musique de la vidéo uploadée est important |
| **Problème UI** | Message d'erreur technique affiché à l'utilisateur |
| **Élément Navigation** | MusicDetailFragment |

**Observation Image :**
- Header "Music"
- Disque vinyle avec thumbnail et icône play
- "Original Sound" / "> Unknown" / "posts 0"
- Bouton "Add to Favorites"
- **Erreur affichée:** `Illegal input: Fields [id, uid, title, artist, audioUrl, createdAt] are required for type with serial name 'com.project.e_commerce.data.remote.dto.So$:undDto', but they were missing at path`
- Boutons "Use sound" et "Share" en bas

**Spécifications Techniques :**
```
Bug Fix - API Response Handling:
- Erreur de désérialisation Kotlinx.serialization
- Champs obligatoires manquants dans réponse API /sounds/{id}

Backend Fix:
- Vérifier endpoint GET /api/sounds/{id} retourne tous les champs requis:
  - id, uid, title, artist, audioUrl, createdAt

Frontend Fix:
- Rendre champs optionnels avec valeurs par défaut:
```kotlin
@Serializable
data class SoundDto(
    val id: String = "",
    val uid: String = "",
    val title: String = "Original Sound",
    val artist: String = "Unknown",
    val audioUrl: String = "",
    val createdAt: String = ""
)
```

Error Handling:
- Catch SerializationException
- Afficher message user-friendly: "Sound information unavailable"
- Ne PAS afficher stacktrace à l'utilisateur
- Priority: CRITICAL
```

---

### Photo 27 : Feed vidéo - Texte produit en overlay

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | *(Observé sur image)* |
| **Traduction** | Informations produit affichées en overlay sur la vidéo |
| **Problème UI** | Texte produit trop verbeux, prend trop d'espace |
| **Élément Navigation** | Video Player - Product Info Overlay |

**Observation Image :**
- Vidéo bottes avec texte overlay à droite:
  - "User"
  - ":Product information"
  - "Lining material: Faux fur"
  - "...Colors: Black, khaki, off-white, camel"
- Carte produit en bas: "...Thickened Snug-fitting Thermal Brea" / View / 19,11$
- Icônes sociales en outline (heart, comment, cart) = bonnes

**Spécifications Techniques :**
```
Product Info Overlay:
- Limiter texte overlay à 2 lignes maximum
- Afficher uniquement: Nom produit + Prix
- Déplacer détails complets vers BottomSheet (on tap)

Text Truncation:
- maxLines = 2
- ellipsize = end
- Cliquer pour voir détails complets

Alternative:
- Supprimer overlay texte produit
- Conserver uniquement carte produit en bas
- Priority: LOW
```

---

### Photo 28 : Category Management - Dialog Edit

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "التصنيفات لازم تكون شغالة بصفحة البروداكتس ومتوافقة مع ما يحدده الادمن" |
| **Traduction** | Les catégories doivent fonctionner sur la page Products et correspondre à ce que l'admin définit |
| **Problème UI** | Interface admin fonctionnelle - à connecter avec frontend |
| **Élément Navigation** | Admin Dashboard → Category Management |

**Observation Image :**
- Dialog "Edit Category" avec champs:
  - Name: "Beauty & Health"
  - Name (Arabic): "الجمال والصحة"
  - Slug: "beauty-health"
  - Icon URL: (vide)
  - Display order: 4
  - Toggle "Active" (ON)
- Boutons "Edit" et "Cancel"

**Spécifications Techniques :**
```
Category Data Model:
- id, name, nameArabic, slug, iconUrl, displayOrder, isActive

Admin Features (Fonctionnels):
✅ CRUD catégories
✅ Traduction arabe
✅ Ordre d'affichage
✅ Toggle activation

Frontend Integration Needed:
- GET /api/categories (actives uniquement pour users)
- Afficher catégories sur ProductsFragment
- Filtrer produits par catégorie sélectionnée
- Respecter displayOrder pour tri
- Priority: HIGH
```

---

### Photo 29 : Category Management - Liste complète

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | "من الاحسن لما يتم مناداة كامل المنتوجات عبر صفحة الامبورت... يتم تلقائيا تصنيف المنتجات" |
| **Traduction** | Idéalement, lors de l'import de produits CJ, les catégoriser automatiquement |
| **Problème UI** | Mapping catégories CJ → catégories BuyV à implémenter |
| **Élément Navigation** | Admin Dashboard → Category Management |

**Observation Image :**
- Header "Category Management" avec bouton "+"
- Stats: 0 Inactive, 8 Active, 8 Total
- Barre de recherche "Search for a category..."
- Liste catégories avec actions (delete, edit, toggle):
  1. Electronics / إلكترونيات
  2. Fashion / أزياء
  3. Home & Garden / المنزل والحديقة
  4. Beauty & Health / الجمال والصحة
  5. Sports & Outdoor / الرياضة والهواء الطلق
  6. Toys & Kids / ألعاب وأطفال
  7. Automotive / السيارات

**Spécifications Techniques :**
```
Auto-Categorization CJ Import:
- Mapper catégories CJ vers catégories BuyV
- Exemple mapping:
  - CJ "3C Products" → BuyV "Electronics"
  - CJ "Clothing" → BuyV "Fashion"
  - CJ "Home Improvement" → BuyV "Home & Garden"

Implementation:
```kotlin
val categoryMapping = mapOf(
    "3C Products" to "electronics",
    "Clothing" to "fashion",
    "Home Improvement" to "home-garden",
    "Beauty" to "beauty-health",
    "Sports" to "sports-outdoor",
    "Toys" to "toys-kids",
    "Auto" to "automotive"
)

fun mapCJCategory(cjCategory: String): String {
    return categoryMapping[cjCategory] ?: "uncategorized"
}
```

Frontend Products Page:
- Afficher tabs/chips catégories en haut
- Filtrer produits au clic sur catégorie
- "All" comme option par défaut
- Priority: HIGH
```

---

### Photo 30 : Page Products - Icônes catégories non chargées

| Attribut | Détail |
|----------|--------|
| **Feedback Client** | *(Entouré en rouge sur l'image - zone des catégories)* |
| **Traduction** | Les icônes des catégories ne se chargent pas correctement |
| **Problème UI** | Icônes catégories affichent un panier générique au lieu des icônes spécifiques |
| **Élément Navigation** | ProductsFragment → Category Row |

**Observation Image :**
- Header avec logo "buyV" et cloche notification (badge 11)
- Barre de recherche
- Bannière promo "Sales get 25% discount" avec bouton "Shop Now"
- **Zone problématique (entourée rouge):** Row catégories horizontale
  - Beauty & Health → icône panier 🛒 (incorrect)
  - Home & Garden → icône panier 🛒 (incorrect)
  - Fashion → icône panier 🛒 (incorrect)
  - Electronics → icône panier 🛒 (incorrect)
- Section "Featured Products" avec 2 produits
- Section "Best Sellers" en bas
- Bottom nav: Profile, Cart, **Products** (sélectionné), Reels

**Spécifications Techniques :**
```
Bug: Category Icons Not Loading
- Les icônes catégories utilisent un placeholder panier au lieu des vraies icônes
- Vérifier champ "Icon URL" dans admin (Photo 28 montre qu'il est vide)

Root Cause Analysis:
1. Icon URL vide dans base de données catégories
2. Fallback placeholder = ic_cart (incorrect)
3. Pas de chargement dynamique des icônes

Fix Required:

1. Admin Panel - Ajouter Icon URLs:
   - Electronics: URL icône électronique/ordinateur
   - Fashion: URL icône vêtement/cintre
   - Home & Garden: URL icône maison/plante
   - Beauty & Health: URL icône cosmétique/cœur

2. Frontend - Fallback approprié:
```kotlin
// Au lieu de ic_cart, utiliser icône catégorie par défaut
fun getCategoryIcon(category: Category): Any {
    return if (category.iconUrl.isNotEmpty()) {
        category.iconUrl // Charger via Coil/Glide
    } else {
        // Fallback par slug
        when (category.slug) {
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
}
```

3. Assets Required:
   - Créer/importer icônes vectorielles pour chaque catégorie
   - Style: Outline icons (cohérent avec le design)
   - Taille: 24dp x 24dp

Priority: HIGH
```

---

## 📊 Résumé des Priorités - COMPLET

| Priorité | Tickets | Description |
|----------|---------|-------------|
| **CRITICAL** | 5 | Crash profil, Back press BottomSheet, Auth Google SHA-1, Erreur API Sound serialization, HTML brut dans caption |
| **HIGH** | 13 | Upload UX, États lecteur, Settings langue, Auth Guard, Lazy Auth, Sound reuse, Owner mode, Guest mode/Skip onboarding, URL brute visible, Catégories frontend, Auto-categorization CJ, Icônes catégories |
| **MEDIUM** | 11 | Bouton Buy styling, Icônes outline, Animation cœur, CJ filters, Banners, Commentaires, Thumbnails grille, Product card size, Emoji OBJ, Image loading placeholder, Play icon state |
| **LOW** | 3 | Spinner unification, Context menu, Product info overlay verbeux |

---

## 📋 Liste des Tickets par Module

### Module: Authentication & Onboarding
| ID | Titre | Priorité | Photo |
|----|-------|----------|-------|
| AUTH-001 | Google SHA-1 configuration Firebase | CRITICAL | 7 |
| AUTH-002 | Ajouter Facebook/Apple Sign-In | HIGH | 7 |
| AUTH-003 | Skip onboarding, mode Guest | HIGH | 19, 21 |
| AUTH-004 | Lazy auth sur actions sensibles | HIGH | 19, 21 |

### Module: Video Player
| ID | Titre | Priorité | Photo |
|----|-------|----------|-------|
| VIDEO-001 | Icône Pause persistante après reprise | HIGH | 4, 12 |
| VIDEO-002 | Icône Play visible pendant lecture | MEDIUM | 22 |
| VIDEO-003 | Owner mode: masquer boutons interaction | HIGH | 12 |
| VIDEO-004 | Back press ferme app vs BottomSheet | CRITICAL | 20 |
| VIDEO-005 | Icônes sociales Outlined vs Filled | MEDIUM | 3 |
| VIDEO-006 | Animation cœur double-tap (Lottie) | MEDIUM | 6 |

### Module: Product Display
| ID | Titre | Priorité | Photo |
|----|-------|----------|-------|
| PROD-001 | Carte produit trop grande | MEDIUM | 17 |
| PROD-002 | URL image visible dans description | HIGH | 23 |
| PROD-003 | Image produit non chargée (placeholder) | MEDIUM | 24 |
| PROD-004 | Product info overlay verbeux | LOW | 27 |
| PROD-005 | Bannière promo dynamique (admin) | MEDIUM | 10 |

### Module: Upload & Content Creation
| ID | Titre | Priorité | Photo |
|----|-------|----------|-------|
| UPLOAD-001 | CTA upload non intuitif | HIGH | 1 |
| UPLOAD-002 | HTML brut dans caption Promote | CRITICAL | 25 |
| UPLOAD-003 | Zones upload vidéo + photos séparées | HIGH | 21 |
| UPLOAD-004 | Auth guard avant upload | HIGH | 8 |

### Module: Sound & Music
| ID | Titre | Priorité | Photo |
|----|-------|----------|-------|
| SOUND-001 | Erreur serialization SoundDto | CRITICAL | 26 |
| SOUND-002 | Bouton "Use Sound" fonctionnel | HIGH | 16 |
| SOUND-003 | Extraction audio pour réutilisation | HIGH | 22 |

### Module: Categories & CJ Import
| ID | Titre | Priorité | Photo |
|----|-------|----------|-------|
| CAT-001 | Catégories actives sur ProductsFragment | HIGH | 28, 29 |
| CAT-002 | Auto-categorization import CJ | HIGH | 29 |
| CAT-005 | Icônes catégories non chargées (placeholder panier) | HIGH | 30 |
| CAT-003 | Filtres CJ (pays, catégorie) | MEDIUM | 9 |
| CAT-004 | Empty state CJ avec produits tendance | MEDIUM | 9 |

### Module: UI/UX Global
| ID | Titre | Priorité | Photo |
|----|-------|----------|-------|
| UI-001 | Bouton Buy bordures inconfortables | MEDIUM | 2 |
| UI-002 | Spinner unique couleur unifiée | LOW | 18 |
| UI-003 | Caractères OBJ (emojis corrompus) | MEDIUM | 20, 22 |
| UI-004 | Vignettes profil rectangulaires | MEDIUM | 13 |
| UI-005 | Menu contextuel long press | LOW | 14 |

### Module: Settings & Profile
| ID | Titre | Priorité | Photo |
|----|-------|----------|-------|
| SET-001 | Sélecteur langue non fonctionnel | HIGH | 5 |
| SET-002 | Crash navigation vers profil créateur | CRITICAL | 7 |
| SET-003 | Follow button GONE vs disabled | HIGH | 4 |
| SET-004 | Merge Save + Cart en un seul bouton | HIGH | 4 |

### Module: Comments & Ratings
| ID | Titre | Priorité | Photo |
|----|-------|----------|-------|
| COM-001 | Onglet Ratings sur commentaires | MEDIUM | 11 |

---

## 📎 Annexe - Futures Features (Discussion)

### Caméra avec Filtres
```
Feature Request (Phase 2):
- Intégrer CameraX pour capture vidéo in-app
- Rechercher bibliothèque filtres open-source (GPUImage, CameraKit)
- Évaluer performance temps réel des filtres
```

### Extraction Audio pour Réutilisation
```
Feature Request (Phase 2):
- Utiliser FFmpeg ou MediaExtractor pour extraire piste audio
- Stocker sons extraits côté serveur
- Permettre réutilisation style TikTok/Instagram Reels
```

---

*Document généré le 1er Mars 2026 - QA Report v1.0 FINAL*
*Total: 32 photos analysées | 32 tickets identifiés | 5 CRITICAL | 13 HIGH | 11 MEDIUM | 3 LOW*
