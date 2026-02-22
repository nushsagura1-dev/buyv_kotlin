package com.project.e_commerce.data.local

/**
 * Gestionnaire de stockage local du panier.
 * 
 * Permet de sauvegarder et récupérer le panier de l'utilisateur
 * localement (SharedPreferences sur Android, UserDefaults sur iOS).
 */
expect class CartStorage {
    /**
     * Sauvegarde le panier au format JSON.
     * 
     * @param userId ID de l'utilisateur (pour supporter multi-user sur le même device si besoin)
     * @param cartJson Représentation JSON du panier
     */
    fun saveCart(userId: String, cartJson: String)
    
    /**
     * Récupère le panier stocké.
     * 
     * @param userId ID de l'utilisateur
     * @return JSON du panier ou null si non trouvé
     */
    fun getCart(userId: String): String?
    
    /**
     * Supprime le panier stocké.
     * 
     * @param userId ID de l'utilisateur
     */
    fun clearCart(userId: String)
}
