package com.project.e_commerce.android.data.helper

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.project.e_commerce.android.R
import kotlinx.coroutines.tasks.await
import kotlin.runCatching

class GoogleSignInHelper(private val context: Context) {

    companion object {
        // This should match your Firebase project's Web Client ID
        // You can get this from your google-services.json or Firebase Console
        private const val TAG = "GoogleSignInHelper"
    }

    private val googleSignInClient: GoogleSignInClient by lazy {
        val webClientId = context.getString(R.string.default_web_client_id)
        Log.d(TAG, "Initializing Google Sign-In with Web Client ID: $webClientId")
        Log.d(TAG, "Package name: ${context.packageName}")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    /**
     * Verifies that Google Play Services are available on the device
     * Returns true if available, false otherwise
     */
    fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }

    /**
     * Get the intent to start Google Sign-In flow
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Handle the result from Google Sign-In activity
     * @param data The intent data from the activity result
     * @return Result containing the ID token or error
     */
    suspend fun handleSignInResult(data: Intent?): Result<String> = runCatching {
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d(TAG, "Google Sign-In successful for: ${account?.email}")
            account?.idToken ?: throw Exception("Failed to get ID token from Google account")
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In failed with code: ${e.statusCode}")
            when (e.statusCode) {
                12501 -> throw Exception("Google Sign-In was cancelled by user")
                12500 -> throw Exception("Google Sign-In service is not available. Please check your internet connection.")
                7 -> throw Exception("Network error during Google Sign-In. Please try again.")
                10 -> throw Exception("Developer Error: Check SHA-1 fingerprint in Firebase Console. Code: ${e.statusCode}")
                8 -> throw Exception("Internal Error: Google Sign-In configuration issue. Code: ${e.statusCode}")
                16 -> throw Exception("Google Sign-In cancelled. Code: ${e.statusCode}")
                else -> throw Exception("Google Sign-In failed with error code: ${e.statusCode}. ${e.message}")
            }
        }
    }

    /**
     * Sign out from Google
     */
    suspend fun signOut() {
        try {
            googleSignInClient.signOut().await()
            Log.d(TAG, "Google Sign-Out completed")
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-Out failed: ${e.message}")
        }
    }

    /**
     * Revoke access (stronger than sign out)
     */
    suspend fun revokeAccess() {
        try {
            googleSignInClient.revokeAccess().await()
            Log.d(TAG, "Google access revoked")
        } catch (e: Exception) {
            Log.e(TAG, "Google access revocation failed: ${e.message}")
        }
    }

    /**
     * Check if user is already signed in
     */
    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
}