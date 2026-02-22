package com.project.e_commerce.domain.repository

import com.project.e_commerce.domain.model.Category
import com.project.e_commerce.domain.model.Product
import com.project.e_commerce.domain.model.Result

/**
 * Interface du repository de produits.
 * 
 * Définit les opérations CRUD pour les produits et les catégories.
 */
interface ProductRepository {
    
    /**
     * Récupère tous les produits.
     * 
     * @return Result contenant la liste de produits ou une erreur
     */
    suspend fun getAllProducts(): Result<List<Product>>
    
    /**
     * Récupère les produits d'une catégorie spécifique.
     * 
     * @param categoryId Identifiant de la catégorie
     * @return Result contenant la liste de produits ou une erreur
     */
    suspend fun getProductsByCategory(categoryId: String): Result<List<Product>>
    
    /**
     * Récupère un produit par son identifiant.
     * 
     * @param productId Identifiant du produit
     * @return Result contenant le produit ou une erreur
     */
    suspend fun getProductById(productId: String): Result<Product>
    
    /**
     * Recherche des produits par mots-clés.
     * 
     * @param query Requête de recherche (sera sanitisée)
     * @return Result contenant la liste de produits correspondants
     */
    suspend fun searchProducts(query: String): Result<List<Product>>
    
    /**
     * Récupère les produits d'un utilisateur spécifique.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result contenant la liste de produits de l'utilisateur
     */
    suspend fun getProductsByUser(userId: String): Result<List<Product>>
    
    /**
     * Crée un nouveau produit.
     * 
     * @param product Produit à créer (les champs doivent être validés)
     * @return Result contenant le produit créé avec son ID
     */
    suspend fun createProduct(product: Product): Result<Product>
    
    /**
     * Met à jour un produit existant.
     * 
     * @param product Produit avec les nouvelles données
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun updateProduct(product: Product): Result<Unit>
    
    /**
     * Supprime un produit.
     * 
     * @param productId Identifiant du produit à supprimer
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun deleteProduct(productId: String): Result<Unit>
    
    /**
     * Récupère toutes les catégories.
     * 
     * @return Result contenant la liste de catégories
     */
    suspend fun getAllCategories(): Result<List<Category>>
    
    /**
     * Récupère une catégorie par son identifiant.
     * 
     * @param categoryId Identifiant de la catégorie
     * @return Result contenant la catégorie
     */
    suspend fun getCategoryById(categoryId: String): Result<Category>
    
    /**
     * Récupère les produits les plus populaires (basés sur le rating).
     * 
     * @param limit Nombre maximum de produits à retourner
     * @return Result contenant la liste de produits populaires
     */
    suspend fun getPopularProducts(limit: Int = 10): Result<List<Product>>
    
    /**
     * Récupère les produits récents.
     * 
     * @param limit Nombre maximum de produits à retourner
     * @return Result contenant la liste de produits récents
     */
    suspend fun getRecentProducts(limit: Int = 10): Result<List<Product>>
}
