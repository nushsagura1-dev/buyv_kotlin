package com.project.e_commerce.domain.deeplink

/**
 * Configuration centralisée pour le Deep Linking
 * Supporte Android (Intent) et iOS (URL Scheme)
 *
 * Format des liens:
 * - Android: buyv://app/{path}
 * - iOS: buyv://app/{path}
 *
 * Exemples:
 * - buyv://app/profile/user123
 * - buyv://app/post/post456
 * - buyv://app/product/prod789
 * - buyv://app/order/order101
 */
object DeepLinkConfig {
    
    // Scheme et host
    const val SCHEME = "buyv"
    const val HOST = "app"
    
    // Paths
    object Paths {
        const val PROFILE = "profile"
        const val POST = "post"
        const val PRODUCT = "product"
        const val ORDER = "order"
        const val REELS = "reels"
        const val SEARCH = "search"
    }
    
    // Builders
    object Builder {
        
        /**
         * Construit un deep link vers un profil utilisateur
         * @param userId ID de l'utilisateur
         * @return buyv://app/profile/{userId}
         */
        fun profileLink(userId: String): String {
            return "$SCHEME://$HOST/${Paths.PROFILE}/$userId"
        }
        
        /**
         * Construit un deep link vers un post
         * @param postId ID du post
         * @return buyv://app/post/{postId}
         */
        fun postLink(postId: String): String {
            return "$SCHEME://$HOST/${Paths.POST}/$postId"
        }
        
        /**
         * Construit un deep link vers un produit
         * @param productId ID du produit
         * @return buyv://app/product/{productId}
         */
        fun productLink(productId: String): String {
            return "$SCHEME://$HOST/${Paths.PRODUCT}/$productId"
        }
        
        /**
         * Construit un deep link vers une commande
         * @param orderId ID de la commande
         * @return buyv://app/order/{orderId}
         */
        fun orderLink(orderId: String): String {
            return "$SCHEME://$HOST/${Paths.ORDER}/$orderId"
        }
        
        /**
         * Construit un deep link vers les reels
         * @param reelId ID du reel (optionnel)
         * @return buyv://app/reels ou buyv://app/reels/{reelId}
         */
        fun reelsLink(reelId: String? = null): String {
            return if (reelId != null) {
                "$SCHEME://$HOST/${Paths.REELS}/$reelId"
            } else {
                "$SCHEME://$HOST/${Paths.REELS}"
            }
        }
        
        /**
         * Construit un deep link vers la recherche
         * @param query Terme de recherche (optionnel)
         * @return buyv://app/search?q={query}
         */
        fun searchLink(query: String? = null): String {
            return if (query != null) {
                "$SCHEME://$HOST/${Paths.SEARCH}?q=$query"
            } else {
                "$SCHEME://$HOST/${Paths.SEARCH}"
            }
        }
    }
    
    // Parser
    object Parser {
        
        /**
         * Parse un deep link et retourne le résultat
         * @param url URL complète (buyv://app/...)
         * @return DeepLinkResult ou null si invalide
         */
        fun parse(url: String): DeepLinkResult? {
            // Vérifier le scheme
            if (!url.startsWith("$SCHEME://")) {
                return null
            }
            
            // Extraire le path
            val uri = url.removePrefix("$SCHEME://$HOST/")
            val components = uri.split("/", "?")
            
            if (components.isEmpty()) {
                return null
            }
            
            return when (components[0]) {
                Paths.PROFILE -> {
                    if (components.size > 1) {
                        DeepLinkResult.Profile(userId = components[1])
                    } else null
                }
                Paths.POST -> {
                    if (components.size > 1) {
                        DeepLinkResult.Post(postId = components[1])
                    } else null
                }
                Paths.PRODUCT -> {
                    if (components.size > 1) {
                        DeepLinkResult.Product(productId = components[1])
                    } else null
                }
                Paths.ORDER -> {
                    if (components.size > 1) {
                        DeepLinkResult.Order(orderId = components[1])
                    } else null
                }
                Paths.REELS -> {
                    val reelId = if (components.size > 1) components[1] else null
                    DeepLinkResult.Reels(reelId = reelId)
                }
                Paths.SEARCH -> {
                    // Extraire le query parameter
                    val query = if (uri.contains("?q=")) {
                        uri.substringAfter("?q=").substringBefore("&")
                    } else null
                    DeepLinkResult.Search(query = query)
                }
                else -> null
            }
        }
    }
}

/**
 * Résultat du parsing d'un deep link
 */
sealed class DeepLinkResult {
    data class Profile(val userId: String) : DeepLinkResult()
    data class Post(val postId: String) : DeepLinkResult()
    data class Product(val productId: String) : DeepLinkResult()
    data class Order(val orderId: String) : DeepLinkResult()
    data class Reels(val reelId: String?) : DeepLinkResult()
    data class Search(val query: String?) : DeepLinkResult()
}
