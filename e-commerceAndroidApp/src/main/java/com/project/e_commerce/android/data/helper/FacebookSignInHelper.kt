package com.project.e_commerce.android.data.helper

import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultRegistryOwner
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * AUTH-002 — Facebook Sign-In helper.
 *
 * Uses the modern Activity Result API (no onActivityResult override needed).
 * The [callbackManager] handles the response internally.
 *
 * Prerequisites:
 *   - Add your Facebook App ID to res/values/strings.xml (replace PLACEHOLDER_FACEBOOK_APP_ID)
 *   - Add your Client Token to res/values/strings.xml (replace PLACEHOLDER_FACEBOOK_CLIENT_TOKEN)
 *   - Both values are available at developers.facebook.com → your app → Settings → Basic
 */
class FacebookSignInHelper(private val context: Context) {

    companion object {
        private const val TAG = "FacebookSignInHelper"
        private val PERMISSIONS = listOf("email", "public_profile")
    }

    val callbackManager: CallbackManager = CallbackManager.Factory.create()

    private var pendingContinuation: ((Result<String>) -> Unit)? = null

    init {
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    val token = result.accessToken.token
                    Log.d(TAG, "Facebook Sign-In success")
                    pendingContinuation?.invoke(Result.success(token))
                    pendingContinuation = null
                }

                override fun onCancel() {
                    Log.d(TAG, "Facebook Sign-In cancelled")
                    pendingContinuation?.invoke(
                        Result.failure(Exception("Facebook Sign-In was cancelled"))
                    )
                    pendingContinuation = null
                }

                override fun onError(error: FacebookException) {
                    Log.e(TAG, "Facebook Sign-In error: ${error.message}")
                    pendingContinuation?.invoke(Result.failure(error))
                    pendingContinuation = null
                }
            }
        )
    }

    /**
     * Launch the Facebook login dialog and suspend until the result arrives.
     * Uses the modern [ActivityResultRegistryOwner] API — no onActivityResult override needed.
     *
     * @param owner The ComponentActivity (or Fragment) hosting the Compose UI.
     * @return [Result] containing the Facebook access token, or an error.
     */
    suspend fun signIn(owner: ActivityResultRegistryOwner): Result<String> =
        suspendCancellableCoroutine { cont ->
            pendingContinuation = { result ->
                if (cont.isActive) cont.resume(result)
            }

            cont.invokeOnCancellation {
                pendingContinuation = null
                LoginManager.getInstance().logOut()
            }

            // Modern API: no onActivityResult override required
            LoginManager.getInstance().logIn(owner, callbackManager, PERMISSIONS)
        }

    /**
     * Sign out from Facebook.
     */
    fun signOut() {
        LoginManager.getInstance().logOut()
        Log.d(TAG, "Facebook Sign-Out completed")
    }
}

