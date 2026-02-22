package com.project.e_commerce.android.presentation.viewModel.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.project.e_commerce.android.data.api.AdminAffiliateSaleResponse
import com.project.e_commerce.android.data.api.SaleStatusUpdateRequest
import com.project.e_commerce.android.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sprint 21: Admin Affiliate Sales Management ViewModel
 * View and manage all affiliate sales and commissions
 */
class AdminAffiliateSalesViewModel(
    private val repository: AdminRepository,
    private val context: Context
) : ViewModel() {

    private val _sales = MutableStateFlow<List<AdminAffiliateSaleResponse>>(emptyList())
    val sales: StateFlow<List<AdminAffiliateSaleResponse>> = _sales.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<String?>(null)
    val selectedStatusFilter: StateFlow<String?> = _selectedStatusFilter.asStateFlow()

    private val adminToken: String by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                context,
                "admin_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.getString("admin_token", "") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    init {
        loadSales()
    }

    fun loadSales(statusFilter: String? = _selectedStatusFilter.value) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _selectedStatusFilter.value = statusFilter
            try {
                val result = repository.getAdminAffiliateSales(adminToken, statusFilter)
                _sales.value = result
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading sales"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterByStatus(status: String?) {
        loadSales(status)
    }

    fun approveSale(saleId: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                repository.updateAffiliateSaleStatus(
                    adminToken,
                    saleId,
                    SaleStatusUpdateRequest(status = "approved")
                )
                _successMessage.value = "Sale approved"
                loadSales()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Approval error"
            }
        }
    }

    fun markAsPaid(saleId: String, paymentReference: String, paymentNotes: String? = null) {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                repository.updateAffiliateSaleStatus(
                    adminToken,
                    saleId,
                    SaleStatusUpdateRequest(
                        status = "paid",
                        payment_reference = paymentReference,
                        payment_notes = paymentNotes
                    )
                )
                _successMessage.value = "Sale marked as paid"
                loadSales()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Payment error"
            }
        }
    }

    fun getCountByStatus(status: String): Int {
        return _sales.value.count { it.commission_status == status }
    }

    fun getTotalCommission(): Double {
        return _sales.value.sumOf { it.commission_amount }
    }

    fun getPendingCommission(): Double {
        return _sales.value.filter { it.commission_status == "pending" }.sumOf { it.commission_amount }
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}
