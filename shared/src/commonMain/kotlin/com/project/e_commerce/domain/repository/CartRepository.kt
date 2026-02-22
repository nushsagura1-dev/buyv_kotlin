package com.project.e_commerce.domain.repository

import com.project.e_commerce.domain.model.Cart
import com.project.e_commerce.domain.model.CartItem
import com.project.e_commerce.domain.model.Result

/**
 * Interface du repository de panier.
 * 
 * Gère les opérations CRUD sur le panier utilisateur.
 */
interface CartRepository {
    
    /**
     * Récupère le panier d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result contenant le panier
     */
    suspend fun getUserCart(userId: String): Result<Cart>
    
    /**
     * Récupère le panier d'un utilisateur sous forme de flux (Flow).
     * Permet d'écouter les mises à jour en temps réel.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Flow<Cart> Flux du panier
     */
    fun getUserCartFlow(userId: String): kotlinx.coroutines.flow.Flow<Cart?>
    
    /**
     * Ajoute un article au panier.
     * Si l'article existe déjà (même productId, size, color), incrémente la quantité.
     * 
     * @param userId Identifiant de l'utilisateur
     * @param item Article à ajouter
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun addToCart(userId: String, item: CartItem): Result<Unit>
    
    /**
     * Supprime un article du panier.
     * 
     * @param userId Identifiant de l'utilisateur
     * @param lineId Identifiant de l'article dans le panier
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun removeFromCart(userId: String, lineId: String): Result<Unit>
    
    /**
     * Met à jour la quantité d'un article.
     * 
     * @param userId Identifiant de l'utilisateur
     * @param lineId Identifiant de l'article dans le panier
     * @param quantity Nouvelle quantité
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun updateQuantity(userId: String, lineId: String, quantity: Int): Result<Unit>
    
    /**
     * Vide complètement le panier.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun clearCart(userId: String): Result<Unit>
    
    /**
     * Synchronise le panier avec le serveur.
     * Utile après une connexion pour récupérer le panier sauvegardé.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result<Cart> contenant le panier synchronisé
     */
    suspend fun syncCart(userId: String): Result<Cart>
}
