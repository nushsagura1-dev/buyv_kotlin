package com.project.e_commerce.android.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.marketplace.MarketplaceProduct
import com.project.e_commerce.domain.model.marketplace.ProductSortBy
import com.project.e_commerce.domain.usecase.marketplace.GetProductsUseCase
import kotlinx.coroutines.flow.firstOrNull

/**
 * PagingSource pour la pagination des produits du Marketplace.
 * 
 * Gère le chargement paginé des produits avec support des filtres :
 * - Catégorie
 * - Prix min/max
 * - Commission min
 * - Recherche
 * - Tri
 * 
 * Utilise l'API backend qui supporte déjà page/limit.
 */
class MarketplaceProductPagingSource(
    private val getProductsUseCase: GetProductsUseCase,
    private val categoryId: String? = null,
    private val minPrice: Double? = null,
    private val maxPrice: Double? = null,
    private val minCommission: Double? = null,
    private val searchQuery: String? = null,
    private val sortBy: ProductSortBy? = null
) : PagingSource<Int, MarketplaceProduct>() {

    companion object {
        private const val STARTING_PAGE_INDEX = 1
        private const val PAGE_SIZE = 20
    }

    /**
     * Charge une page de produits.
     * 
     * @param params Paramètres de chargement (page, taille)
     * @return LoadResult avec les données ou l'erreur
     */
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MarketplaceProduct> {
        val page = params.key ?: STARTING_PAGE_INDEX

        return try {
            // Appel à l'API via UseCase
            val result = getProductsUseCase(
                categoryId = categoryId,
                minPrice = minPrice,
                maxPrice = maxPrice,
                minCommission = minCommission,
                search = searchQuery,
                sortBy = sortBy?.value ?: "relevance",
                page = page,
                limit = params.loadSize
            ).firstOrNull()

            when (result) {
                is Result.Success -> {
                    val response = result.data
                    val products = response.items
                    
                    // Calculer les clés prev/next
                    val prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1
                    val nextKey = if (page < response.totalPages) page + 1 else null

                    LoadResult.Page(
                        data = products,
                        prevKey = prevKey,
                        nextKey = nextKey
                    )
                }
                is Result.Error -> {
                    LoadResult.Error(
                        Exception(result.error.message)
                    )
                }
                else -> {
                    LoadResult.Error(
                        Exception("Unexpected result type")
                    )
                }
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    /**
     * Retourne la clé pour rafraîchir depuis une position donnée.
     * 
     * @param state État actuel du paging
     * @return Clé de la page la plus proche de anchorPosition
     */
    override fun getRefreshKey(state: PagingState<Int, MarketplaceProduct>): Int? {
        // Retourne la page la plus proche de la position d'ancrage
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
