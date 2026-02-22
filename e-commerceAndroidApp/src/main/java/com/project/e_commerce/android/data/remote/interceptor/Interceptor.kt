package com.project.e_commerce.android.data.remote.interceptor

import com.project.e_commerce.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Auth interceptor: automatically adds "Authorization: Bearer <token>" to every
 * outgoing request that does not already carry an Authorization header.
 * This covers TrackingApi, WithdrawalApi and any other Retrofit API that omits
 * the explicit @Header parameter.
 */
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        // If the call already carries a token (e.g. AdminApi explicit header)
        // don't add a second one — just forward as-is.
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }
        val token = tokenManager.getAccessToken()
            ?: return chain.proceed(originalRequest) // not logged-in, send unauthenticated
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(newRequest)
    }
}
