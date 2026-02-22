package com.project.e_commerce.android.presentation.viewModel.admin

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.project.e_commerce.android.data.repository.AdminRepository
import com.project.e_commerce.domain.model.Commission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * État de l'UI pour la gestion des commissions admin.
 * 
 * @property isLoading Indique si des données sont en cours de chargement
 * @property commissions Liste des commissions affichées
 * @property selectedCommission Commission sélectionnée pour les détails
 * @property error Message d'erreur à afficher, null si pas d'erreur
 */
data class AdminCommissionUiState(
    val isLoading: Boolean = false,
    val commissions: List<Commission> = emptyList(),
    val selectedCommission: Commission? = null,
    val error: String? = null
)

/**
 * ViewModel pour la gestion des commissions admin.
 * 
 * Gère le chargement, le filtrage et la mise à jour des statuts de commissions.
 * Utilise EncryptedSharedPreferences pour stocker le token admin de manière sécurisée.
 */
class AdminCommissionViewModel(
    private val repository: AdminRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminCommissionUiState())
    val uiState: StateFlow<AdminCommissionUiState> = _uiState.asStateFlow()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        "admin_encrypted_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        loadCommissions()
    }

    /**
     * Charge les commissions depuis l'API.
     * 
     * @param status Filtre optionnel par statut (pending, approved, rejected, paid)
     */
    fun loadCommissions(status: String? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val adminToken = getAdminToken()
                
                if (adminToken.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Admin token not found. Please login again."
                    )
                    return@launch
                }

                val commissions = if (status.isNullOrEmpty() || status == "all") {
                    repository.getAllCommissions(adminToken)
                } else {
                    repository.getCommissionsByStatus(adminToken, status)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    commissions = commissions,
                    error = null
                )
                Log.d("AdminCommission", "✅ Loaded ${commissions.size} commissions")
            } catch (e: Exception) {
                Log.e("AdminCommission", "❌ Error loading commissions: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load commissions"
                )
            }
        }
    }

    /**
     * Met à jour le statut d'une commission.
     * 
     * @param commissionId ID de la commission
     * @param newStatus Nouveau statut (approved, rejected, paid)
     */
    fun updateCommissionStatus(commissionId: Int, newStatus: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val adminToken = getAdminToken()
                
                if (adminToken.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Admin token not found. Please login again."
                    )
                    return@launch
                }

                repository.updateCommissionStatus(adminToken, commissionId, newStatus)
                Log.d("AdminCommission", "✅ Commission $commissionId updated to $newStatus")
                
                // Recharger les commissions après mise à jour
                loadCommissions()
            } catch (e: Exception) {
                Log.e("AdminCommission", "❌ Error updating commission: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update commission status"
                )
            }
        }
    }

    /**
     * Sélectionne une commission pour afficher les détails.
     * 
     * @param commission Commission à sélectionner, null pour désélectionner
     */
    fun selectCommission(commission: Commission?) {
        _uiState.value = _uiState.value.copy(selectedCommission = commission)
    }

    /**
     * Récupère les statistiques des commissions par statut.
     * 
     * @return Map avec le nombre de commissions par statut
     */
    fun getCommissionStats(): Map<String, Int> {
        val commissions = _uiState.value.commissions
        return mapOf(
            "all" to commissions.size,
            "pending" to commissions.count { it.status.equals("pending", ignoreCase = true) },
            "approved" to commissions.count { it.status.equals("approved", ignoreCase = true) },
            "rejected" to commissions.count { it.status.equals("rejected", ignoreCase = true) },
            "paid" to commissions.count { it.status.equals("paid", ignoreCase = true) }
        )
    }

    private fun getAdminToken(): String? {
        return encryptedPrefs.getString("admin_token", null)
    }
}
