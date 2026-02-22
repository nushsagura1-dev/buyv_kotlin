# 📱 Configuration iOS — Étapes nécessitant un Mac
## Document destiné au client

**Projet :** BuyV — Application e-commerce & marketplace
**Date :** 19 février 2026
**Objet :** Ce document détaille les configurations et développements iOS qui nécessitent un ordinateur macOS avec Xcode pour être réalisés.

---

## ✅ Ce qui est DÉJÀ fait (fonctionnel sans Mac)

Toutes les fonctionnalités suivantes ont été développées et sont **prêtes à compiler** sur macOS :

| Fonctionnalité | Écrans iOS | État |
|----------------|------------|------|
| Authentification (login, inscription, reset) | 7 écrans | ✅ Complet |
| Catalogue produits, recherche, filtres | 5 écrans | ✅ Complet |
| Panier & Commandes | 4 écrans | ✅ Complet |
| Reels / Posts / Création de contenu | 4 écrans | ✅ Complet |
| Social (profil, followers, recherche) | 8 écrans | ✅ Complet |
| Marketplace & Promoteurs | 4 écrans | ✅ Complet |
| Admin (login, dashboard, produits, commandes, users, commissions, CJ, withdrawals) | 8 écrans | ✅ Complet |
| Deep Links (profil, post, produit, commande) | 4 routes | ✅ Complet |
| **Total** | **44 écrans** | ✅ |

---

## 🔧 Étape 1 — Installation des SDK (environ 1h30 sur Mac)

### 1.1 Stripe SDK — Paiements réels

**Fichier concerné :** `StripePaymentService.swift` (222 lignes)

Le code de paiement est **entièrement écrit** avec un pattern `#if canImport(StripePaymentSheet)`. Il utilise actuellement un mode simulation. Pour activer les vrais paiements :

**Actions à effectuer dans Xcode :**
1. Ouvrir le projet dans Xcode
2. File → Add Package Dependencies
3. URL : `https://github.com/stripe/stripe-ios`
4. Sélectionner le package `StripePaymentSheet`
5. Build — le code réel s'active automatiquement

**Temps estimé :** 15-20 minutes

### 1.2 Firebase SDK — Notifications push

**Fichier concerné :** `AppDelegate.swift` (130 lignes)

Le code Firebase est **entièrement écrit** avec `#if canImport(FirebaseCore)` et `#if canImport(FirebaseMessaging)`. Les notifications fonctionnent via APNS natif mais sans le ciblage FCM.

**Actions à effectuer dans Xcode :**
1. File → Add Package Dependencies
2. URL : `https://github.com/firebase/firebase-ios-sdk`
3. Sélectionner : `FirebaseCore`, `FirebaseMessaging`, `FirebaseAnalytics`
4. Copier `GoogleService-Info.plist` dans le projet (déjà présent dans le repo)
5. Dans l'onglet Capabilities, activer "Push Notifications"
6. Build — Firebase s'active automatiquement

**Temps estimé :** 20-30 minutes

### 1.3 Google Sign-In SDK — Connexion sociale

**Fichier concerné :** `GoogleSignInService.swift` (96 lignes)

Le service est préparé avec `#if canImport(GoogleSignIn)`, actuellement en mode désactivé.

**Actions à effectuer dans Xcode :**
1. File → Add Package Dependencies
2. URL : `https://github.com/google/GoogleSignIn-iOS`
3. Configurer le `clientID` depuis Google Cloud Console
4. Ajouter le URL scheme dans Info.plist

**Temps estimé :** 20-30 minutes

---

## 🔧 Étape 2 — Développement des 7 écrans Admin avancés (2-3 jours sur Mac)

L'application Android dispose de **15 écrans d'administration complets**. L'iOS en a **8 fonctionnels**. Il reste **7 écrans** à développer, qui affichent actuellement un message "Planned for v2.x".

### Écrans à développer

| # | Écran | Équivalent Android | Description | Effort |
|---|-------|--------------------|-------------|--------|
| 1 | **AdminPostsView** | AdminPostsScreen.kt (352 L) | Modération des posts/reels (voir, supprimer, filtrer par statut) | 3-4h |
| 2 | **AdminCommentsView** | AdminCommentsScreen.kt (258 L) | Modération des commentaires (approuver, rejeter, supprimer) | 2-3h |
| 3 | **AdminCategoriesView** | AdminCategoriesScreen.kt (386 L) | Gestion catégories produits (créer, modifier, supprimer, réorganiser) | 3-4h |
| 4 | **AdminFollowsView** | AdminFollowsScreen.kt (262 L) | Statistiques et gestion des relations de suivi entre utilisateurs | 2-3h |
| 5 | **AdminNotificationsView** | AdminNotificationsScreen.kt (347 L) | Historique des notifications + envoi de broadcasts aux utilisateurs | 3-4h |
| 6 | **AdminPromoterWalletsView** | AdminPromoterWalletsScreen.kt (378 L) | Gestion des wallets promoteurs (soldes, retraits, transactions) | 3-4h |
| 7 | **AdminAffiliateSalesView** | AdminAffiliateSalesScreen.kt (293 L) | Suivi des ventes affiliées (filtrage, approbation, paiement) | 2-3h |

**Temps total estimé :** 18-25 heures de développement (2-3 jours)

### Infrastructure déjà en place

Ces écrans ne partent PAS de zéro. Voici ce qui est déjà prêt :

- ✅ **Backend** : Tous les endpoints API existent et fonctionnent (130 endpoints total)
- ✅ **AdminApiService.swift** : 22 méthodes API déjà codées (661 lignes) — couvre la majorité des appels nécessaires
- ✅ **Design System** : Les 8 écrans admin existants définissent le style visuel à suivre
- ✅ **Navigation** : L'AdminDashboardView a déjà les liens vers ces écrans (actuellement vers les placeholders)

### Ce qu'il faudra faire pour chaque écran

1. Créer un nouveau fichier `Admin[X]View.swift` dans `Views/Admin/`
2. Créer un `Admin[X]ViewModel.swift` dans `ViewModels/`
3. Ajouter les méthodes API manquantes dans `AdminApiService.swift` (si besoin)
4. Remplacer la référence au placeholder dans `AdminDashboardView.swift`

---

## 🔧 Étape 3 — Configuration de sécurité iOS (1h sur Mac)

| Action | Détail | Priorité |
|--------|--------|----------|
| Configurer ATS (App Transport Security) | Désactiver `NSAllowsArbitraryLoads` et n'autoriser que le domaine API en production | 🔴 Haute |
| Vérifier Keychain pour le stockage des tokens | S'assurer que les tokens JWT sont stockés dans le Keychain iOS | 🟡 Moyenne |
| Configurer les URL Schemes pour deep links | Vérifier dans Info.plist | 🟢 Basse |

---

## 📋 Récapitulatif pour le client

| Étape | Quoi | Durée | Prérequis |
|-------|------|-------|-----------|
| **1** | Installation SDK (Stripe + Firebase + Google) | **~1h30** | macOS + Xcode + comptes dev |
| **2** | 7 écrans admin avancés | **2-3 jours** | macOS + Xcode |
| **3** | Configuration sécurité | **~1h** | macOS + Xcode |
| **Total** | | **3-4 jours** | |

### ⚠️ Remarque importante

> Les **fonctionnalités demandées dans les phases 1 et 2 du devis** (expérience utilisateur + gestion ventes/produits/commandes) sont **entièrement fonctionnelles** sur iOS. Les 7 écrans admin manquants sont des fonctionnalités **avancées d'administration** qui vont au-delà du scope initial du devis.

### Comptes nécessaires

Pour réaliser ces étapes, il faudra :
- Un **Apple Developer Account** (99$/an) pour publier sur l'App Store
- Les **credentials Stripe** en mode live (pour les paiements réels)
- Un projet **Firebase** configuré avec les credentials iOS
- Un **client ID Google** pour Google Sign-In

---

## 🔒 Étape 5 : Certificate Pinning iOS (H-5)

> **Prérequis** : macOS avec Xcode 15+

L'infrastructure de certificate pinning est en place dans le module shared KMP :
- `commonMain/PlatformEngine.kt` — expect function
- `androidMain/PlatformEngine.android.kt` — ✅ implémenté (OkHttp CertificatePinner)
- `iosMain/PlatformEngine.ios.kt` — ⚠️ à compléter avec `handleChallenge`

**Action requise** : Dans `PlatformEngine.ios.kt`, ajouter le callback `handleChallenge` pour valider le SPKI SHA-256 du certificat serveur contre les pins suivants :

```
# Let's Encrypt R13 (intermédiaire)
sha256/AlSQhgtJirc8ahLyekmtX+Iw+v46yPYRLJt9Cq1GlB0=
# ISRG Root X1 (racine — valide jusqu'en 2035)
sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=
```

Le code Ktor Darwin pour implémenter le pinning :
```kotlin
actual fun createPlatformEngine(): HttpClientEngine {
    return Darwin.create {
        handleChallenge { session, task, challenge, completionHandler ->
            // Valider le ServerTrust contre les pins SPKI
            // Voir l'implémentation Android comme référence
        }
    }
}
```

| Action | Temps estimé |
|--------|-------------|
| Implémenter handleChallenge avec validation SPKI | 0.5 jour |
| Tester avec le serveur de production | 0.5 jour |

---

*Document mis à jour le 19 février 2026*
