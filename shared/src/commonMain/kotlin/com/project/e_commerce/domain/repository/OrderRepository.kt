package com.project.e_commerce.domain.repository

import com.project.e_commerce.domain.model.Order
import com.project.e_commerce.domain.model.OrderStatus
import com.project.e_commerce.domain.model.Result

/**
 * Interface du repository de commandes.
 * 
 * Définit les opérations CRUD pour les commandes utilisateur.
 */
interface OrderRepository {
    
    /**
     * Récupère toutes les commandes d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result contenant la liste de commandes
     */
    suspend fun getOrdersByUser(userId: String): Result<List<Order>>
    
    /**
     * Récupère une commande par son identifiant.
     * 
     * @param orderId Identifiant de la commande
     * @return Result contenant la commande
     */
    suspend fun getOrderById(orderId: String): Result<Order>
    
    /**
     * Crée une nouvelle commande.
     * 
     * @param order Commande à créer (les champs doivent être validés)
     * @return Result contenant la commande créée avec son ID
     */
    suspend fun createOrder(order: Order): Result<Order>
    
    /**
     * Met à jour le statut d'une commande.
     * 
     * @param orderId Identifiant de la commande
     * @param status Nouveau statut
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit>
    
    /**
     * Annule une commande.
     * 
     * @param orderId Identifiant de la commande
     * @param reason Raison de l'annulation
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun cancelOrder(orderId: String, reason: String): Result<Unit>
    
    /**
     * Récupère les commandes par statut.
     * 
     * @param userId Identifiant de l'utilisateur
     * @param status Statut des commandes à récupérer
     * @return Result contenant la liste de commandes
     */
    suspend fun getOrdersByStatus(userId: String, status: OrderStatus): Result<List<Order>>
    
    /**
     * Récupère les commandes récentes d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @param limit Nombre maximum de commandes à retourner
     * @return Result contenant la liste de commandes récentes
     */
    suspend fun getRecentOrders(userId: String, limit: Int = 5): Result<List<Order>>
    
    /**
     * Vérifie si une commande peut être annulée.
     * 
     * @param orderId Identifiant de la commande
     * @return Result<Boolean> indiquant si la commande peut être annulée
     */
    suspend fun canCancelOrder(orderId: String): Result<Boolean>
}
