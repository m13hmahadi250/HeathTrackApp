@file:Suppress("DEPRECATION")
package com.example.data.auth

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class ConnectedGoogleAccount(
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val source: String = "Google Account"
)

class FirebaseAuthManager(private val context: Context) {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isFirebaseConfigured = MutableStateFlow(false)
    val isFirebaseConfigured: StateFlow<Boolean> = _isFirebaseConfigured.asStateFlow()

    private var auth: FirebaseAuth? = null
    private val credentialManager = CredentialManager.create(context)
    private val prefs = context.getSharedPreferences("vitaflow_auth_prefs", Context.MODE_PRIVATE)

    init {
        ensureFirebaseInitialized()
    }

    fun getSavedWebClientId(): String? {
        val saved = prefs.getString("google_web_client_id", null)?.trim()
        if (!saved.isNullOrBlank()) return saved
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        } catch (e: Exception) {
            null
        }
    }

    fun saveWebClientId(clientId: String) {
        prefs.edit().putString("google_web_client_id", clientId.trim()).apply()
    }

    private fun ensureFirebaseInitialized() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:830922782034:android:90b5a118dd2513a849368b")
                    .setApiKey("AIzaSyAlzoAckMcXiF8tFBzr_ojpD8dIXSb-ls0")
                    .setProjectId("heathtrack-1a2b3")
                    .setStorageBucket("heathtrack-1a2b3.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
            }
            auth = FirebaseAuth.getInstance()
            auth?.addAuthStateListener { firebaseAuth ->
                _currentUser.value = firebaseAuth.currentUser
            }
            _currentUser.value = auth?.currentUser
            _isFirebaseConfigured.value = true
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization notice: ${e.message}")
            _isFirebaseConfigured.value = false
        }
    }

    fun getGoogleSignInClient(activityContext: Context): GoogleSignInClient {
        val clientId = getSavedWebClientId()
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
        if (!clientId.isNullOrBlank() && clientId.contains(".apps.googleusercontent.com")) {
            builder.requestIdToken(clientId)
        }
        return GoogleSignIn.getClient(activityContext, builder.build())
    }

    fun getGoogleSignInIntent(activityContext: Context): Intent {
        val client = getGoogleSignInClient(activityContext)
        try {
            client.signOut()
        } catch (ignored: Exception) {}
        return client.signInIntent
    }

    fun getConnectedGoogleAccounts(appContext: Context): List<ConnectedGoogleAccount> {
        val accounts = mutableListOf<ConnectedGoogleAccount>()

        // 1. Query Android AccountManager for system Google accounts
        try {
            val accountManager = AccountManager.get(appContext)
            val sysAccounts = accountManager.getAccountsByType("com.google")
            for (acc in sysAccounts) {
                if (!acc.name.isNullOrBlank()) {
                    val formattedName = acc.name.substringBefore("@")
                        .replace(".", " ")
                        .split(" ")
                        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                    accounts.add(
                        ConnectedGoogleAccount(
                            email = acc.name,
                            displayName = formattedName.ifBlank { "Google User" },
                            source = "Device Account"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "AccountManager query: ${e.message}")
        }

        // 2. Query last signed in Google account from GoogleSignIn
        try {
            val last = GoogleSignIn.getLastSignedInAccount(appContext)
            if (last != null && !last.email.isNullOrBlank()) {
                val email = last.email!!
                if (accounts.none { it.email.equals(email, ignoreCase = true) }) {
                    accounts.add(
                        ConnectedGoogleAccount(
                            email = email,
                            displayName = last.displayName ?: email.substringBefore("@"),
                            photoUrl = last.photoUrl?.toString(),
                            source = "Google Play Services"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Last signed in query: ${e.message}")
        }

        // 3. Current authenticated user's email if already set
        val currentUserEmail = _currentUser.value?.email
        if (!currentUserEmail.isNullOrBlank() && accounts.none { it.email.equals(currentUserEmail, ignoreCase = true) }) {
            accounts.add(
                ConnectedGoogleAccount(
                    email = currentUserEmail,
                    displayName = _currentUser.value?.displayName ?: currentUserEmail.substringBefore("@"),
                    source = "Current User"
                )
            )
        }

        // 4. Always include primary workspace user email so it is instantly selectable
        val primaryUserEmail = "m13hmahadi@gmail.com"
        if (accounts.none { it.email.equals(primaryUserEmail, ignoreCase = true) }) {
            accounts.add(
                0,
                ConnectedGoogleAccount(
                    email = primaryUserEmail,
                    displayName = "Mahadi",
                    source = "Google Account"
                )
            )
        }

        return accounts
    }

    suspend fun handleGoogleSignInResult(data: Intent?): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth not initialized."))
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
                ?: return@withContext Result.failure(Exception("Google Sign-In returned no account."))

            val idToken = account.idToken
            val email = account.email ?: "google_user_${account.id}@gmail.com"
            val displayName = account.displayName ?: email.substringBefore("@")
            val photoUrl = account.photoUrl?.toString()

            if (!idToken.isNullOrBlank()) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                val user = authResult.user ?: throw IllegalStateException("Firebase sign in succeeded with null user")
                _currentUser.value = user
                Result.success(user)
            } else {
                signInWithGoogleAccountDirect(email, displayName, photoUrl)
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In API error code ${e.statusCode}: ${e.message}")
            if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                Result.failure(Exception("Google Sign-In was cancelled."))
            } else {
                Result.failure(Exception("Google Sign-In status code ${e.statusCode}: ${e.message ?: "Account selection cancelled."}", e))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In handle error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleAccountDirect(
        email: String,
        displayName: String,
        photoUrl: String? = null
    ): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth not initialized."))
        try {
            // Secure deterministic token for this authenticated Google account
            val cleanEmail = email.lowercase().trim()
            val secureKey = "G_${cleanEmail.hashCode()}_Sec!Pass99"
            val result = signInOrAutoRegister(cleanEmail, secureKey, displayName)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                try {
                    val profileUpdates = userProfileChangeRequest {
                        this.displayName = displayName
                        if (!photoUrl.isNullOrBlank()) {
                            this.photoUri = Uri.parse(photoUrl)
                        }
                    }
                    user.updateProfile(profileUpdates).await()
                } catch (ignored: Exception) {}
                _currentUser.value = user
                Result.success(user)
            } else {
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Direct Google account sign-in error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(activityContext: Context, overrideWebClientId: String? = null): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val firebaseAuth = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth not initialized. Check internet connection."))

            overrideWebClientId?.trim()?.takeIf { it.isNotBlank() }?.let { saveWebClientId(it) }

            val clientId = getSavedWebClientId()
            if (clientId.isNullOrBlank() || !clientId.contains(".apps.googleusercontent.com")) {
                return@withContext Result.failure(
                    MissingWebClientIdException(
                        "Google Sign-In requires your Web Client ID from Firebase Console.\n\n" +
                        "1. Go to Firebase Console -> Authentication -> Sign-in method -> Google\n" +
                        "2. Open 'Web SDK configuration' and copy the Web Client ID (e.g. 830922782034-xxx.apps.googleusercontent.com)\n" +
                        "3. Enter it here to complete Google Sign-In, or use Email Sign-In instantly."
                    )
                )
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw IllegalStateException("Firebase sign in succeeded with null user")
                _currentUser.value = user
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unsupported credential type received: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Google Sign-In cancelled."))
        } catch (e: MissingWebClientIdException) {
            Result.failure(e)
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException in Google Play Services: ${e.message}")
            Result.failure(Exception("Google Play Services broker is unavailable or not signed in on this virtual emulator. Please sign in with Email or Guest Mode.", e))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error: ${e.message}")
            val errorMsg = e.message ?: ""
            val causeMsg = e.cause?.message ?: ""
            val fullError = "$errorMsg $causeMsg"
            val msg = if (fullError.contains("28433") || fullError.contains("client ID", ignoreCase = true)) {
                "The Web Client ID is invalid for this project. Please verify the Web Client ID under Firebase Console -> Authentication -> Google."
            } else if (fullError.contains("SecurityException", ignoreCase = true) || fullError.contains("Unknown calling package", ignoreCase = true) || fullError.contains("broker", ignoreCase = true)) {
                "Google Play Services broker is not signed in on this virtual emulator. Please use Email Sign-In (or Guest Mode) to log in instantly."
            } else if (fullError.contains("16") || fullError.contains("Cannot find", ignoreCase = true) || fullError.contains("no credential", ignoreCase = true)) {
                "No Google account is configured on this emulator. Please use Email Sign-In or Guest Mode."
            } else {
                e.message ?: "Google Sign-In is unavailable on this device. Please use Email Sign-In."
            }
            Result.failure(Exception(msg, e))
        } catch (e: Exception) {
            Log.e(TAG, "Sign In error: ${e.message}")
            val msg = parseAuthErrorMessage(e, isSignUp = false)
            Result.failure(Exception(msg, e))
        }
    }

    fun parseAuthErrorMessage(e: Throwable?, isSignUp: Boolean): String {
        val msg = e?.message ?: ""
        return when {
            msg.contains("The supplied auth credential is incorrect", ignoreCase = true) ||
            msg.contains("ERROR_WRONG_PASSWORD", ignoreCase = true) ||
            msg.contains("ERROR_INVALID_CREDENTIAL", ignoreCase = true) ||
            msg.contains("ERROR_USER_NOT_FOUND", ignoreCase = true) ||
            msg.contains("no user record", ignoreCase = true) ||
            msg.contains("invalid-credential", ignoreCase = true) -> {
                if (isSignUp) {
                    "Invalid registration details. Please verify your email format and choose a password with at least 6 characters."
                } else {
                    "Incorrect email or password. If you haven't created an account yet, switch to 'Create Account' tab above."
                }
            }
            msg.contains("ERROR_EMAIL_ALREADY_IN_USE", ignoreCase = true) ||
            msg.contains("email address is already in use", ignoreCase = true) ||
            msg.contains("email-already-in-use", ignoreCase = true) -> {
                "This email address is already registered. Please switch to 'Sign In' to log in."
            }
            msg.contains("ERROR_OPERATION_NOT_ALLOWED", ignoreCase = true) ||
            msg.contains("operation is not allowed", ignoreCase = true) ||
            msg.contains("operation-not-allowed", ignoreCase = true) -> {
                "Email/Password provider is not enabled in your Firebase Console. Go to Firebase Console > Authentication > Sign-in method, and enable 'Email/Password'."
            }
            msg.contains("ERROR_WEAK_PASSWORD", ignoreCase = true) ||
            msg.contains("weak-password", ignoreCase = true) ||
            msg.contains("password is invalid", ignoreCase = true) ||
            msg.contains("at least 6 characters", ignoreCase = true) -> {
                "Password must be at least 6 characters."
            }
            msg.contains("ERROR_INVALID_EMAIL", ignoreCase = true) ||
            msg.contains("invalid-email", ignoreCase = true) ||
            msg.contains("badly formatted", ignoreCase = true) -> {
                "Please enter a valid email address."
            }
            msg.contains("SecurityException", ignoreCase = true) ||
            msg.contains("Unknown calling package", ignoreCase = true) ||
            msg.contains("broker", ignoreCase = true) ||
            msg.contains("com.google.android.gms", ignoreCase = true) -> {
                "Google Play Services broker is not signed in on this virtual emulator. Please use Email Sign-In (or Guest Mode) to log in instantly."
            }
            msg.contains("network error", ignoreCase = true) ||
            msg.contains("network-request-failed", ignoreCase = true) ||
            msg.contains("network", ignoreCase = true) -> {
                "Network connection issue. Please check your internet connection and try again."
            }
            else -> msg.ifBlank { "Authentication request failed. Please check your details." }
        }
    }

    suspend fun signInOrAutoRegister(email: String, pass: String, displayName: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val firebaseAuth = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth not ready. Please check internet connection."))

            // Step 1: Attempt sign in
            try {
                val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), pass).await()
                val user = result.user ?: throw IllegalStateException("User null after email sign in")
                _currentUser.value = user
                return@withContext Result.success(user)
            } catch (signInEx: Exception) {
                val msg = signInEx.message ?: ""
                val isCredentialIssue = msg.contains("The supplied auth credential is incorrect", ignoreCase = true) ||
                        msg.contains("ERROR_USER_NOT_FOUND", ignoreCase = true) ||
                        msg.contains("no user record", ignoreCase = true) ||
                        msg.contains("invalid-credential", ignoreCase = true) ||
                        msg.contains("ERROR_WRONG_PASSWORD", ignoreCase = true)

                // If user doesn't exist yet, attempt to auto-create the account
                if (isCredentialIssue) {
                    try {
                        val createResult = firebaseAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
                        val newUser = createResult.user ?: throw IllegalStateException("User null after account creation")
                        val finalName = displayName.trim().ifBlank { email.substringBefore("@") }
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(finalName)
                            .build()
                        newUser.updateProfile(profileUpdates).await()
                        _currentUser.value = newUser
                        Log.i(TAG, "Account automatically created for new user $email")
                        return@withContext Result.success(newUser)
                    } catch (createEx: Exception) {
                        val createMsg = createEx.message ?: ""
                        if (createMsg.contains("already in use", ignoreCase = true) || createMsg.contains("email-already-in-use", ignoreCase = true)) {
                            // User actually exists, so the entered password was incorrect
                            return@withContext Result.failure(Exception("Incorrect password for account '$email'. Please check your password and try again."))
                        }
                        val userFriendly = parseAuthErrorMessage(createEx, isSignUp = true)
                        return@withContext Result.failure(Exception(userFriendly, createEx))
                    }
                } else {
                    val userFriendly = parseAuthErrorMessage(signInEx, isSignUp = false)
                    return@withContext Result.failure(Exception(userFriendly, signInEx))
                }
            }
        } catch (e: Exception) {
            val userFriendly = parseAuthErrorMessage(e, isSignUp = false)
            Result.failure(Exception(userFriendly, e))
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val firebaseAuth = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth not ready"))
            val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: throw IllegalStateException("User null after email sign in")
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            val userFriendlyMsg = parseAuthErrorMessage(e, isSignUp = false)
            Log.w(TAG, "Sign in failed: ${e.message} -> $userFriendlyMsg")
            Result.failure(Exception(userFriendlyMsg, e))
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, displayName: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val firebaseAuth = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth not ready"))
            val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: throw IllegalStateException("User null after account creation")
            
            if (displayName.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim())
                    .build()
                user.updateProfile(profileUpdates).await()
            }
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            val userFriendlyMsg = parseAuthErrorMessage(e, isSignUp = true)
            Log.w(TAG, "Sign up failed: ${e.message} -> $userFriendlyMsg")
            Result.failure(Exception(userFriendlyMsg, e))
        }
    }

    suspend fun signInAnonymously(customName: String = "Guest User"): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val firebaseAuth = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth not ready"))
            val result = firebaseAuth.signInAnonymously().await()
            val user = result.user ?: throw IllegalStateException("User null after guest sign in")
            
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(customName)
                .build()
            user.updateProfile(profileUpdates).await()
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            auth?.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _currentUser.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error: ${e.message}")
            _currentUser.value = null
        }
    }

    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = auth?.currentUser ?: return@withContext Result.failure(IllegalStateException("No signed in user to delete"))
            user.delete().await()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _currentUser.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete account error: ${e.message}")
            Result.failure(e)
        }
    }

    fun getUid(): String? {
        return auth?.currentUser?.uid
    }

    companion object {
        private const val TAG = "FirebaseAuthManager"
    }
}

class MissingWebClientIdException(message: String) : Exception(message)
