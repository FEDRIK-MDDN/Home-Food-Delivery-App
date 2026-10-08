package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException

object GoogleAuthHelper {

    /**
     * Triggers Google Sign-In via Android Credential Manager and returns the ID token.
     */
    suspend fun launchGoogleSignIn(activity: Activity, webClientId: String): Result<String> {
        return try {
            val credentialManager = CredentialManager.create(activity)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                Result.success(idToken)
            } else {
                Result.failure(Exception("Unsupported credential type received."))
            }
        } catch (e: GetCredentialCancellationException) {
            // User cancelled account selection — silent cancel, no error prompt needed
            Result.failure(CancellationException("Sign-in cancelled by user."))
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            android.util.Log.e("HomeChef", "No Google credentials available: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            android.util.Log.e("HomeChef", "Google Sign-In failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Builds standard Google Sign-In intent for fallback.
     */
    fun getGoogleSignInIntent(context: Context, webClientId: String): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        return client.signInIntent
    }

    /**
     * Extracts ID token from Google Sign-In Intent result.
     */
    fun extractIdTokenFromIntent(data: Intent?): Result<String> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrBlank()) {
                Result.success(idToken)
            } else {
                Result.failure(Exception("Google Sign-In succeeded, but ID token is empty."))
            }
        } catch (e: ApiException) {
            if (e.statusCode == 12501) {
                Result.failure(CancellationException("Sign-in cancelled."))
            } else {
                val msg = when (e.statusCode) {
                    10 -> "Configuration error (Code 10). Make sure SHA-1 and Web Client ID match Firebase Console."
                    7 -> "Network error. Please check your internet connection."
                    else -> "Google Sign-In error (${e.statusCode}): ${e.message}"
                }
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clears cached Google credential state upon logout so the account chooser appears next time.
     */
    suspend fun clearCredentialState(context: Context) {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            android.util.Log.w("HomeChef", "Error clearing credential state: ${e.message}")
        }
        try {
            val client = GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN)
            client.signOut()
        } catch (e: Exception) {
            android.util.Log.w("HomeChef", "Error signing out Google client: ${e.message}")
        }
    }
}
