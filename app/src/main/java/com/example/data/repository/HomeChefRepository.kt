package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import com.example.data.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.example.util.GoogleAuthHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

// Firestore collection names
private const val COL_USERS         = "users"
private const val COL_FOODS         = "foods"
private const val COL_ORDERS          = "orders"
private const val COL_NOTIFICATIONS   = "notifications"
private const val COL_DELIVERY_ISSUES = "delivery_issues"
private const val COL_PROMOS          = "kitchen_promos"

private val DUMMY_FOOD_IDS = setOf("f_sarah_01", "f_001", "f_002", "f_003", "f_004", "f_005")
private val DUMMY_COOK_IDS = setOf("cook_001", "cook_002")

class HomeChefRepository(private val context: Context) {

    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val db        = AppDatabase.getDatabase(context)
    private val scope     = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.CoroutineExceptionHandler { _, e ->
        android.util.Log.w("HomeChef", "Coroutine handled error: ${e.message}")
    })

    // ─── Live Listeners ───────────────────────────────────────────────────────
    private var foodsListener: ListenerRegistration? = null
    private var ordersListener: ListenerRegistration? = null
    private var notifsListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private var cartListener: ListenerRegistration? = null
    private var favoritesListener: ListenerRegistration? = null
    private var issuesListener: ListenerRegistration? = null
    private var promosListener: ListenerRegistration? = null

    // ─── State Flows ──────────────────────────────────────────────────────────
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _foods = MutableStateFlow<List<Food>>(emptyList())
    val foods: StateFlow<List<Food>> = _foods.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _deliveryIssues = MutableStateFlow<List<DeliveryIssue>>(emptyList())
    val deliveryIssues: StateFlow<List<DeliveryIssue>> = _deliveryIssues.asStateFlow()

    private val _promos = MutableStateFlow<List<PromoCode>>(emptyList())
    val promos: StateFlow<List<PromoCode>> = _promos.asStateFlow()

    // ─── Cart & Favorites (Firestore-backed, cloud-synced) ────────────────────
    private val _favoriteFoodIds = MutableStateFlow<List<String>>(emptyList())
    val favoriteFoodIds: StateFlow<List<String>> = _favoriteFoodIds.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    init {
        // Automatically start listeners as soon as user is authenticated
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                android.util.Log.d("HomeChef", "Auth state changed: user ${user.uid} signed in. Attaching listeners...")
                startGlobalListeners()
                fetchFoodsOnce()
            } else {
                android.util.Log.d("HomeChef", "Auth state changed: no user signed in.")
                stopAllUserListeners()
            }
        }
        seedAdminAccountIfNeeded()
        restoreSessionIfLoggedIn()
        if (auth.currentUser != null) {
            startGlobalListeners()
            fetchFoodsOnce()
            cleanDummyFoodsFromFirestore()
        }
    }

    private fun cleanDummyFoodsFromFirestore() {
        scope.launch {
            try {
                DUMMY_FOOD_IDS.forEach { id ->
                    firestore.collection(COL_FOODS).document(id).delete().await()
                }
                android.util.Log.d("HomeChef", "Dummy foods removed from Firestore.")
            } catch (e: Exception) {
                android.util.Log.w("HomeChef", "Clean dummy foods skipped: ${e.message}")
            }
        }
    }

    private fun restoreSessionIfLoggedIn() {
        val firebaseUser = auth.currentUser ?: return
        val uid = firebaseUser.uid
        val email = firebaseUser.email.orEmpty().trim().lowercase()
        scope.launch {
            try {
                val doc = firestore.collection(COL_USERS).document(uid).get().await()
                val user = if (doc.exists()) doc.toUser() else null

                val resolvedEmail = when {
                    !user?.email.isNullOrBlank() -> user!!.email
                    email.isNotBlank() -> email
                    else -> ""
                }
                val resolvedName = when {
                    !user?.name.isNullOrBlank() -> user!!.name
                    !firebaseUser.displayName.isNullOrBlank() -> firebaseUser.displayName!!.trim()
                    resolvedEmail.isNotBlank() -> resolvedEmail.substringBefore("@")
                        .replace(".", " ")
                        .replace("_", " ")
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    else -> "Foodie"
                }
                val resolvedPhone = user?.phone?.ifBlank { null } ?: firebaseUser.phoneNumber.orEmpty()
                val resolvedRole = user?.role ?: UserRole.CUSTOMER
                val resolvedAddress = user?.address.orEmpty()
                val resolvedPhoto = user?.photoUrl.orEmpty()
                val resolvedApproved = user?.isApprovedCook ?: (resolvedRole != UserRole.COOK)
                val resolvedSuspended = user?.isSuspended ?: false

                val finalUser = User(
                    userId         = uid,
                    name           = resolvedName,
                    email          = resolvedEmail,
                    phone          = resolvedPhone,
                    role           = resolvedRole,
                    photoUrl       = resolvedPhoto,
                    address        = resolvedAddress,
                    isApprovedCook = resolvedApproved,
                    isSuspended    = resolvedSuspended
                )
                _currentUser.value = finalUser
                startAllUserListeners(uid)
                android.util.Log.d("HomeChef", "Restored session for $resolvedName ($resolvedRole)")
            } catch (e: Exception) {
                android.util.Log.w("HomeChef", "Session restore failed: ${e.message}")
            }
        }
    }

    // ─── Firestore Real-time Listeners ────────────────────────────────────────
    fun startGlobalListeners() {
        if (auth.currentUser == null) {
            android.util.Log.d("HomeChef", "Skipping startGlobalListeners: user not authenticated yet.")
            return
        }
        foodsListener?.remove()
        ordersListener?.remove()
        usersListener?.remove()

        // Foods — live cook-added dishes only
        foodsListener = firestore.collection(COL_FOODS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    android.util.Log.e("HomeChef", "Error listening to foods: ${error.message}")
                    return@addSnapshotListener
                }
                if (snap != null) {
                    val list = snap.documents.mapNotNull { it.toFood() }
                        .filterNot { it.foodId in DUMMY_FOOD_IDS || it.cookId in DUMMY_COOK_IDS }
                    _foods.value = list
                    android.util.Log.d("HomeChef", "Loaded ${_foods.value.size} cook-added dishes from Firestore")
                }
            }
        // Orders
        ordersListener = firestore.collection(COL_ORDERS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    android.util.Log.w("HomeChef", "Orders listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snap != null)
                    _orders.value = snap.documents.mapNotNull { it.toOrder() }
                        .sortedByDescending { it.createdAt }
            }
        // Users (admin)
        usersListener = firestore.collection(COL_USERS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    android.util.Log.w("HomeChef", "Users listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snap != null)
                    _users.value = snap.documents.mapNotNull { it.toUser() }
            }
        // Promos (kitchen promo deals & discount vouchers)
        promosListener?.remove()
        promosListener = firestore.collection(COL_PROMOS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    android.util.Log.w("HomeChef", "Promos listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snap != null) {
                    _promos.value = snap.documents.mapNotNull { it.toPromoCode() }
                        .sortedByDescending { it.createdAt }
                }
            }
    }

    fun fetchFoodsOnce() {
        if (auth.currentUser == null) return
        scope.launch {
            try {
                val snap = firestore.collection(COL_FOODS).get().await()
                val list = snap.documents.mapNotNull { it.toFood() }
                    .filterNot { it.foodId in DUMMY_FOOD_IDS || it.cookId in DUMMY_COOK_IDS }
                _foods.value = list
                android.util.Log.d("HomeChef", "Directly fetched ${list.size} cook-added dishes from Firestore.")
            } catch (e: Exception) {
                android.util.Log.w("HomeChef", "fetchFoodsOnce failed: ${e.message}")
            }
        }
    }

    private fun startUserNotifListener(userId: String) {
        notifsListener?.remove()
        notifsListener = firestore.collection(COL_NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snap, _ ->
                if (snap != null)
                    _notifications.value = snap.documents.mapNotNull { it.toNotification() }
                        .sortedByDescending { it.timestamp }
            }
    }

    private fun startUserCartListener(userId: String) {
        cartListener?.remove()
        cartListener = firestore.collection(COL_USERS).document(userId)
            .collection("cart")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    _cartItems.value = snap.documents.mapNotNull { doc ->
                        try {
                            CartItem(
                                foodId         = doc.getString("foodId") ?: doc.id,
                                cookId         = doc.getString("cookId") ?: "",
                                cookName       = doc.getString("cookName") ?: "",
                                title          = doc.getString("title") ?: "",
                                description    = doc.getString("description") ?: "",
                                price          = doc.getDouble("price") ?: 0.0,
                                imageUrl       = doc.getString("imageUrl") ?: "",
                                quantity       = doc.getLong("quantity")?.toInt() ?: 1,
                                specialRequest = doc.getString("specialRequest") ?: ""
                            )
                        } catch (e: Exception) { null }
                    }
                }
            }
    }

    private fun startUserFavoritesListener(userId: String) {
        favoritesListener?.remove()
        favoritesListener = firestore.collection(COL_USERS).document(userId)
            .collection("favorites")
            .addSnapshotListener { snap, _ ->
                if (snap != null)
                    _favoriteFoodIds.value = snap.documents.map { it.id }
            }
    }

    private fun startDeliveryIssuesListener(userId: String) {
        issuesListener?.remove()
        issuesListener = firestore.collection(COL_DELIVERY_ISSUES)
            .whereEqualTo("driverId", userId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    android.util.Log.w("HomeChef", "Delivery issues listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snap != null) {
                    _deliveryIssues.value = snap.documents.mapNotNull { it.toDeliveryIssue() }
                        .sortedByDescending { it.createdAt }
                }
            }
    }

    private fun startAllUserListeners(uid: String) {
        startGlobalListeners()
        fetchFoodsOnce()
        startUserNotifListener(uid)
        startUserCartListener(uid)
        startUserFavoritesListener(uid)
        startDeliveryIssuesListener(uid)
        cleanDummyFoodsFromFirestore()
    }

    private fun stopAllUserListeners() {
        notifsListener?.remove()
        cartListener?.remove()
        favoritesListener?.remove()
        issuesListener?.remove()
        promosListener?.remove()
        _cartItems.value = emptyList()
        _favoriteFoodIds.value = emptyList()
        _notifications.value = emptyList()
        _deliveryIssues.value = emptyList()
        _promos.value = emptyList()
    }

    // Seeds admin account into Firebase Auth + Firestore on first launch
    private fun seedAdminAccountIfNeeded() {
        scope.launch {
            try {
                // Try to create admin in Firebase Auth
                val result = auth.createUserWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD).await()
                val uid = result.user?.uid ?: return@launch
                // Write admin document to Firestore
                firestore.collection(COL_USERS).document(uid).set(mapOf(
                    "userId"         to uid,
                    "name"           to "Admin",
                    "email"          to ADMIN_EMAIL,
                    "phone"          to "",
                    "role"           to "ADMIN",
                    "photoUrl"       to "",
                    "address"        to "",
                    "isApprovedCook" to false,
                    "isSuspended"    to false
                )).await()
                // Sign out immediately — this was just seeding, not a real login
                auth.signOut()
                android.util.Log.d("HomeChef", "Admin account seeded in Firebase.")
            } catch (e: Exception) {
                // "email already in use" = admin already exists, that's fine
                android.util.Log.d("HomeChef", "Admin seed: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUTH
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): String? = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        val trimmedPass = password.trim()
        return@withContext try {
            val result = try {
                auth.signInWithEmailAndPassword(trimmedEmail, password).await()
            } catch (e: Exception) {
                if (password != trimmedPass) {
                    auth.signInWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                } else {
                    throw e
                }
            }
            val firebaseUser = result.user ?: return@withContext "Login failed."
            val uid = firebaseUser.uid

            // 1. Try reading from Firestore
            var user: User? = null
            try {
                val doc = firestore.collection(COL_USERS).document(uid).get().await()
                if (doc.exists()) {
                    user = doc.toUser()
                }
            } catch (fsEx: Exception) {
                android.util.Log.w("HomeChef", "Firestore read during login failed: ${fsEx.message}")
            }

            // 2. Resolve fields with robust fallbacks
            val resolvedEmail = when {
                !user?.email.isNullOrBlank() -> user!!.email
                !firebaseUser.email.isNullOrBlank() -> firebaseUser.email!!.trim().lowercase()
                else -> trimmedEmail
            }

            val resolvedName = when {
                !user?.name.isNullOrBlank() -> user!!.name
                !firebaseUser.displayName.isNullOrBlank() -> firebaseUser.displayName!!.trim()
                else -> resolvedEmail.substringBefore("@")
                    .replace(".", " ")
                    .replace("_", " ")
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    .ifBlank { "Customer" }
            }

            val resolvedPhone = when {
                !user?.phone.isNullOrBlank() -> user!!.phone
                !firebaseUser.phoneNumber.isNullOrBlank() -> firebaseUser.phoneNumber!!
                else -> ""
            }

            val resolvedRole = user?.role ?: UserRole.CUSTOMER
            val resolvedAddress = user?.address.orEmpty()
            val resolvedPhoto = user?.photoUrl.orEmpty()
            val resolvedApproved = user?.isApprovedCook ?: (resolvedRole != UserRole.COOK)
            val resolvedSuspended = user?.isSuspended ?: false

            val finalUser = User(
                userId         = uid,
                name           = resolvedName,
                email          = resolvedEmail,
                phone          = resolvedPhone,
                role           = resolvedRole,
                photoUrl       = resolvedPhoto,
                address        = resolvedAddress,
                isApprovedCook = resolvedApproved,
                isSuspended    = resolvedSuspended
            )

            _currentUser.value = finalUser

            // 3. Auto-heal profile in Firestore so customer details are saved permanently
            scope.launch {
                try {
                    firestore.collection(COL_USERS).document(uid).set(
                        mapOf(
                            "userId"         to uid,
                            "name"           to resolvedName,
                            "email"          to resolvedEmail,
                            "phone"          to resolvedPhone,
                            "role"           to resolvedRole.name,
                            "photoUrl"       to resolvedPhoto,
                            "address"        to resolvedAddress,
                            "isApprovedCook" to resolvedApproved,
                            "isSuspended"    to resolvedSuspended
                        ),
                        SetOptions.merge()
                    ).await()
                    android.util.Log.d("HomeChef", "Customer profile synced in Firestore: $resolvedName ($uid)")
                } catch (e: Exception) {
                    android.util.Log.w("HomeChef", "Failed to auto-heal profile in Firestore: ${e.message}")
                }
            }

            startAllUserListeners(uid)
            null
        } catch (e: Exception) {
            android.util.Log.e("HomeChef", "Login failed for email '$trimmedEmail': ${e.message}", e)
            parseAuthError(e.message)
        }
    }

    suspend fun register(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: UserRole
    ): String? = withContext(Dispatchers.IO) {
        if (role == UserRole.ADMIN)
            return@withContext "Admin accounts cannot be registered."
        return@withContext try {
            // Step 1: Create Firebase Auth account
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val uid    = result.user?.uid ?: return@withContext "Registration failed."
            val trimmedEmail = email.trim().lowercase()

            // Update Auth displayName
            try {
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = name.trim()
                }
                result.user?.updateProfile(profileUpdates)?.await()
            } catch (_: Exception) {}

            // Step 2: Set currentUser IMMEDIATELY
            val newUser = User(
                userId         = uid,
                name           = name.trim(),
                email          = trimmedEmail,
                phone          = phone.trim(),
                role           = role,
                photoUrl       = "",
                address        = "",
                isApprovedCook = (role != UserRole.COOK),
                isSuspended    = false
            )
            _currentUser.value = newUser
            startAllUserListeners(uid)

            // Step 3: Firestore write with retries
            scope.launch {
                var attempts = 0
                while (attempts < 3) {
                    try {
                        val userDoc = mapOf(
                            "userId"         to uid,
                            "name"           to name.trim(),
                            "email"          to trimmedEmail,
                            "phone"          to phone.trim(),
                            "role"           to role.name,
                            "photoUrl"       to "",
                            "address"        to "",
                            "isApprovedCook" to (role != UserRole.COOK),
                            "isSuspended"    to false
                        )
                        firestore.collection(COL_USERS).document(uid).set(userDoc).await()
                        android.util.Log.d("HomeChef", "User profile written to Firestore.")
                        break
                    } catch (e: Exception) {
                        attempts++
                        android.util.Log.w("HomeChef", "Registration Firestore write attempt $attempts failed: ${e.message}")
                        if (attempts < 3) kotlinx.coroutines.delay(500)
                    }
                }
            }

            null // success
        } catch (e: Exception) {
            parseAuthError(e.message)
        }
    }


    suspend fun signInWithGoogle(idToken: String, selectedRole: UserRole = UserRole.CUSTOMER): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: return@withContext "Google Sign-In failed: No user returned."
            val uid = firebaseUser.uid
            val email = firebaseUser.email.orEmpty().trim().lowercase()
            val displayName = firebaseUser.displayName.orEmpty().trim().ifBlank {
                email.substringBefore("@")
                    .replace(".", " ")
                    .replace("_", " ")
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    .ifBlank { "User" }
            }
            val photoUrl = firebaseUser.photoUrl?.toString().orEmpty()
            val phone = firebaseUser.phoneNumber.orEmpty()

            // Check if user already exists in Firestore
            var existingUser: User? = null
            try {
                val doc = firestore.collection(COL_USERS).document(uid).get().await()
                if (doc.exists()) {
                    existingUser = doc.toUser()
                }
            } catch (fsEx: Exception) {
                android.util.Log.w("HomeChef", "Firestore check for Google user failed: ${fsEx.message}")
            }

            val finalUser = if (existingUser != null) {
                // Existing user — retain their existing role and details, update photo if available
                existingUser.copy(
                    photoUrl = existingUser.photoUrl.ifBlank { photoUrl },
                    email = existingUser.email.ifBlank { email }
                )
            } else {
                // First-time Google user — initialize with chosen role
                User(
                    userId         = uid,
                    name           = displayName,
                    email          = email,
                    phone          = phone,
                    role           = selectedRole,
                    photoUrl       = photoUrl,
                    address        = "",
                    isApprovedCook = (selectedRole != UserRole.COOK),
                    isSuspended    = false
                )
            }

            _currentUser.value = finalUser

            // Sync profile into Firestore
            scope.launch {
                try {
                    val userDoc = mapOf(
                        "userId"         to finalUser.userId,
                        "name"           to finalUser.name,
                        "email"          to finalUser.email,
                        "phone"          to finalUser.phone,
                        "role"           to finalUser.role.name,
                        "photoUrl"       to finalUser.photoUrl,
                        "address"        to finalUser.address,
                        "isApprovedCook" to finalUser.isApprovedCook,
                        "isSuspended"    to finalUser.isSuspended
                    )
                    firestore.collection(COL_USERS).document(uid).set(userDoc, SetOptions.merge()).await()
                    android.util.Log.d("HomeChef", "Google user synced to Firestore: ${finalUser.name} ($uid)")
                } catch (e: Exception) {
                    android.util.Log.w("HomeChef", "Failed to save Google user in Firestore: ${e.message}")
                }
            }

            startAllUserListeners(uid)
            null // success
        } catch (e: Exception) {
            android.util.Log.e("HomeChef", "signInWithGoogle error: ${e.message}", e)
            parseAuthError(e.message)
        }
    }

    fun logout() {
        auth.signOut()
        scope.launch {
            GoogleAuthHelper.clearCredentialState(context)
        }
        _currentUser.value = null
        stopAllUserListeners()
        foodsListener?.remove()
        ordersListener?.remove()
        usersListener?.remove()
        _foods.value = emptyList()
    }

    private suspend fun loadUserFromFirestore(uid: String) {
        val doc = firestore.collection(COL_USERS).document(uid).get().await()
        val user = doc.toUser() ?: return
        _currentUser.value = user
        startUserNotifListener(uid)
    }

    private fun parseAuthError(msg: String?): String {
        return when {
            msg == null                            -> "An unexpected error occurred."
            msg.contains("email address is badly") -> "Invalid email address format."
            msg.contains("no user record")         -> "No account found with that email."
            msg.contains("password is invalid")    -> "Incorrect password. Please try again."
            msg.contains("The supplied auth credential is incorrect", ignoreCase = true) ->
                "Incorrect password. Please check your password and try again."
            msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
                "Incorrect email or password. Please check and try again."
            msg.contains("email address is already") -> "An account with this email already exists."
            msg.contains("weak-password")          -> "Password must be at least 6 characters."
            msg.contains("network error")          -> "Network error. Check your internet connection."
            msg.contains("account-exists-with-different-credential", ignoreCase = true) ->
                "An account already exists with this email using a different sign-in method."
            msg.contains("invalid-credential", ignoreCase = true) ->
                "Incorrect email or password. Please try again."
            else                                   -> "Error: $msg"
        }
    }

    suspend fun sendPasswordReset(email: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            auth.sendPasswordResetEmail(email.trim()).await()
            null
        } catch (e: Exception) {
            parseAuthError(e.message)
        }
    }

    suspend fun adminDeleteUser(userId: String): String? = withContext(Dispatchers.IO) {
        val caller = _currentUser.value
        if (caller?.role != UserRole.ADMIN) {
            return@withContext "Permission denied: Only Admin can delete users."
        }
        return@withContext try {
            firestore.collection(COL_USERS).document(userId).delete().await()
            null
        } catch (e: Exception) {
            "Failed to delete user: ${e.message}"
        }
    }

    suspend fun adminUpdateUser(
        userId: String,
        name: String,
        email: String,
        phone: String,
        role: UserRole,
        isApprovedCook: Boolean,
        isSuspended: Boolean
    ): String? = withContext(Dispatchers.IO) {
        val caller = _currentUser.value
        if (caller?.role != UserRole.ADMIN) {
            return@withContext "Permission denied: Only Admin can update users."
        }

        val trimmedName = name.trim()
        val trimmedEmail = email.trim().lowercase()
        if (trimmedName.isBlank()) {
            return@withContext "Name cannot be empty."
        }
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            return@withContext "Please enter a valid email address."
        }

        return@withContext try {
            val updates = mapOf(
                "name"           to trimmedName,
                "email"          to trimmedEmail,
                "phone"          to phone.trim(),
                "role"           to role.name,
                "isApprovedCook" to if (role == UserRole.COOK) isApprovedCook else true,
                "isSuspended"    to isSuspended
            )
            firestore.collection(COL_USERS).document(userId)
                .set(updates, SetOptions.merge()).await()
            android.util.Log.d("HomeChef", "Admin updated user $userId: $trimmedName ($trimmedEmail), role: ${role.name}")
            null
        } catch (e: Exception) {
            android.util.Log.e("HomeChef", "adminUpdateUser failed: ${e.message}", e)
            "Failed to update user: ${e.message}"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROFILE
    // ─────────────────────────────────────────────────────────────────────────

    fun updateUserProfile(name: String, email: String, phone: String, address: String) {
        val current = _currentUser.value ?: return
        _currentUser.value = current.copy(name = name, email = email, phone = phone, address = address)
        scope.launch {
            try {
                firestore.collection(COL_USERS).document(current.userId).set(
                    mapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "address" to address
                    ),
                    SetOptions.merge()
                ).await()
                android.util.Log.d("HomeChef", "User profile updated in Firestore.")
            } catch (e: Exception) {
                android.util.Log.e("HomeChef", "Failed to update user profile: ${e.message}")
            }
        }
    }

    fun updateUserPhoto(photoUri: String) {
        val current = _currentUser.value ?: return
        _currentUser.value = current.copy(photoUrl = photoUri)
        scope.launch {
            try {
                firestore.collection(COL_USERS).document(current.userId).set(
                    mapOf("photoUrl" to photoUri),
                    SetOptions.merge()
                ).await()
                android.util.Log.d("HomeChef", "User photo updated in Firestore.")
            } catch (e: Exception) {
                android.util.Log.e("HomeChef", "Failed to update user photo: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FAVORITES (Firestore — cloud-synced)
    // ─────────────────────────────────────────────────────────────────────────

    fun toggleFavorite(foodId: String) {
        val userId = _currentUser.value?.userId ?: return
        val ref = firestore.collection(COL_USERS).document(userId)
            .collection("favorites").document(foodId)
        scope.launch {
            try {
                if (favoriteFoodIds.value.contains(foodId)) ref.delete().await()
                else ref.set(mapOf("foodId" to foodId, "addedAt" to System.currentTimeMillis())).await()
            } catch (e: Exception) {
                android.util.Log.w("HomeChef", "Favorite toggle failed: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CART (Firestore — cloud-synced)
    // ─────────────────────────────────────────────────────────────────────────

    private fun cartRef(userId: String) =
        firestore.collection(COL_USERS).document(userId).collection("cart")

    fun addToCart(food: Food, quantity: Int = 1, specialRequest: String = "") {
        val userId = _currentUser.value?.userId ?: return
        scope.launch {
            try {
                val existing = cartItems.value.find { it.foodId == food.foodId }
                val newQty   = (existing?.quantity ?: 0) + quantity
                cartRef(userId).document(food.foodId).set(mapOf(
                    "foodId"         to food.foodId,
                    "cookId"         to food.cookId,
                    "cookName"       to food.cookName,
                    "title"          to food.title,
                    "description"    to food.description,
                    "price"          to (food.discountPrice ?: food.price),
                    "imageUrl"       to food.imageUrl,
                    "quantity"       to newQty,
                    "specialRequest" to if (specialRequest.isNotBlank()) specialRequest
                                        else (existing?.specialRequest ?: "")
                )).await()
            } catch (e: Exception) {
                android.util.Log.w("HomeChef", "Add to cart failed: ${e.message}")
            }
        }
    }

    fun updateCartQuantity(foodId: String, newQty: Int) {
        val userId = _currentUser.value?.userId ?: return
        scope.launch {
            try {
                if (newQty <= 0) cartRef(userId).document(foodId).delete().await()
                else cartRef(userId).document(foodId).update("quantity", newQty).await()
            } catch (e: Exception) {
                android.util.Log.w("HomeChef", "Update cart failed: ${e.message}")
            }
        }
    }

    fun removeCartItem(foodId: String) {
        val userId = _currentUser.value?.userId ?: return
        scope.launch {
            try { cartRef(userId).document(foodId).delete().await() }
            catch (e: Exception) { android.util.Log.w("HomeChef", "Remove cart failed: ${e.message}") }
        }
    }

    fun clearCart() {
        val userId = _currentUser.value?.userId ?: return
        scope.launch {
            try {
                cartRef(userId).get().await().documents.forEach { it.reference.delete() }
            } catch (e: Exception) {
                android.util.Log.w("HomeChef", "Clear cart failed: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORDERS (Firestore)
    // ─────────────────────────────────────────────────────────────────────────

    fun checkoutOrders(
        deliveryAddress: String,
        paymentMethod: String,
        notesForCook: String,
        promoDiscount: Double = 0.0,
        appliedPromoCode: String = ""
    ): List<String> {
        val user        = _currentUser.value ?: return emptyList()
        val currentCart = cartItems.value
        if (currentCart.isEmpty()) return emptyList()

        val groupedByCook   = currentCart.groupBy { it.cookId }
        val createdOrderIds = mutableListOf<String>()
        var remainingPromo  = promoDiscount

        groupedByCook.forEach { (cookId, items) ->
            val cookName   = items.firstOrNull()?.cookName ?: "Home Cook"
            val subtotal   = items.sumOf { it.price * it.quantity }
            val deliveryFee = 2.00
            val tax        = subtotal * 0.05
            val discount   = remainingPromo
            remainingPromo = 0.0
            val total   = (subtotal + deliveryFee + tax - discount).coerceAtLeast(1.0)
            val orderId = "ORD-${(1000..9999).random()}"

            val order = Order(
                orderId         = orderId,
                customerId      = user.userId,
                customerName    = user.name,
                customerPhone   = user.phone,
                deliveryAddress = deliveryAddress.ifBlank { user.address },
                cookId          = cookId,
                cookName        = cookName,
                items           = items,
                subtotal        = subtotal,
                deliveryFee     = deliveryFee,
                tax             = tax,
                total           = total,
                status          = OrderStatus.PENDING,
                paymentMethod   = paymentMethod,
                notesForCook    = notesForCook,
                createdAt       = System.currentTimeMillis()
            )

            scope.launch {
                firestore.collection(COL_ORDERS).document(orderId)
                    .set(order.toMap()).await()
                val notifId = UUID.randomUUID().toString()
                firestore.collection(COL_NOTIFICATIONS).document(notifId).set(
                    mapOf(
                        "notificationId" to notifId,
                        "userId"         to user.userId,
                        "title"          to "Order Placed!",
                        "message"        to "Your order #$orderId has been placed successfully.",
                        "timestamp"      to System.currentTimeMillis(),
                        "isRead"         to false,
                        "type"           to "ORDER"
                    )
                ).await()
            }
            createdOrderIds.add(orderId)
        }

        // Auto-update promo redeem count when customer applied promo code
        if (appliedPromoCode.isNotBlank()) {
            val codeToRedeem = appliedPromoCode.trim().uppercase()
            scope.launch {
                try {
                    val promoQuery = firestore.collection(COL_PROMOS)
                        .whereEqualTo("code", codeToRedeem)
                        .limit(1)
                        .get()
                        .await()
                    if (!promoQuery.isEmpty) {
                        val doc = promoQuery.documents.first()
                        doc.reference.update("usageCount", FieldValue.increment(1)).await()
                        android.util.Log.d("HomeChef", "Customer order auto updated redeem count for promo: $codeToRedeem")
                    } else {
                        android.util.Log.w("HomeChef", "Promo $codeToRedeem not found in $COL_PROMOS to increment.")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeChef", "Error auto-incrementing promo redeem count: ${e.message}", e)
                }

                // Immediately update local StateFlow so cook dashboard updates in real time
                _promos.value = _promos.value.map { promo ->
                    if (promo.code.equals(codeToRedeem, ignoreCase = true)) {
                        promo.copy(usageCount = promo.usageCount + 1)
                    } else promo
                }
            }
        }

        clearCart()
        return createdOrderIds
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus, rejectReason: String? = null) {
        scope.launch {
            val updates = mutableMapOf<String, Any>("status" to newStatus.name)
            if (rejectReason != null) updates["rejectReason"] = rejectReason
            firestore.collection(COL_ORDERS).document(orderId).update(updates).await()

            val order = _orders.value.find { it.orderId == orderId } ?: return@launch
            val msg = when (newStatus) {
                OrderStatus.ACCEPTED         -> "Your order #$orderId was accepted by ${order.cookName}!"
                OrderStatus.PREPARING        -> "${order.cookName} is preparing your food now!"
                OrderStatus.READY            -> "Order #$orderId is ready for pickup!"
                OrderStatus.PICKED_UP        -> "Delivery driver picked up your food!"
                OrderStatus.OUT_FOR_DELIVERY -> "Driver is on the way with your food!"
                OrderStatus.DELIVERED        -> "Order #$orderId delivered! Enjoy your meal!"
                OrderStatus.COMPLETED        -> "Order completed. Please leave a rating!"
                OrderStatus.CANCELLED        -> "Order #$orderId was cancelled. Reason: ${rejectReason ?: "N/A"}"
                else                         -> "Status updated to ${newStatus.label}"
            }
            val notifId = UUID.randomUUID().toString()
            firestore.collection(COL_NOTIFICATIONS).document(notifId).set(
                mapOf(
                    "notificationId" to notifId,
                    "userId"         to order.customerId,
                    "title"          to "Order Update",
                    "message"        to msg,
                    "timestamp"      to System.currentTimeMillis(),
                    "isRead"         to false,
                    "type"           to "ORDER"
                )
            ).await()
        }
    }

    fun assignDeliveryPartner(orderId: String, deliveryPartnerName: String, deliveryId: String) {
        scope.launch {
            firestore.collection(COL_ORDERS).document(orderId).update(
                mapOf(
                    "deliveryId"          to deliveryId,
                    "deliveryPartnerName" to deliveryPartnerName,
                    "status"              to OrderStatus.PICKED_UP.name,
                    "cancelledDriverIds"  to FieldValue.arrayRemove(deliveryId)
                )
            ).await()
        }
    }

    suspend fun cancelDeliveryAssignment(orderId: String): String? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection(COL_ORDERS).document(orderId).get().await()
            if (!doc.exists()) {
                return@withContext "Order does not exist."
            }
            val currentStatus = doc.getString("status") ?: ""
            // Ensure delivery cannot be cancelled once route has started or completed
            if (currentStatus == OrderStatus.OUT_FOR_DELIVERY.name ||
                currentStatus == OrderStatus.DELIVERED.name ||
                currentStatus == OrderStatus.COMPLETED.name ||
                currentStatus == OrderStatus.CANCELLED.name) {
                return@withContext "Cannot cancel delivery: Route has already started or order is completed."
            }

            val cancellingDriverId = doc.getString("deliveryId") ?: auth.currentUser?.uid ?: ""

            val updates = mutableMapOf<String, Any>(
                "deliveryId"          to FieldValue.delete(),
                "deliveryPartnerName" to FieldValue.delete(),
                "status"              to OrderStatus.READY.name
            )
            if (cancellingDriverId.isNotEmpty()) {
                updates["cancelledDriverIds"] = FieldValue.arrayUnion(cancellingDriverId)
            }
            firestore.collection(COL_ORDERS).document(orderId).update(updates).await()

            _orders.value = _orders.value.map { ord ->
                if (ord.orderId == orderId) {
                    val updatedCancelledDrivers = if (cancellingDriverId.isNotEmpty() && !ord.cancelledDriverIds.contains(cancellingDriverId)) {
                        ord.cancelledDriverIds + cancellingDriverId
                    } else {
                        ord.cancelledDriverIds
                    }
                    ord.copy(
                        deliveryId = null,
                        deliveryPartnerName = null,
                        status = OrderStatus.READY,
                        cancelledDriverIds = updatedCancelledDrivers
                    )
                } else ord
            }

            android.util.Log.d("HomeChef", "Delivery cancelled for order $orderId, returned to available pickup requests.")
            null
        } catch (e: Exception) {
            android.util.Log.e("HomeChef", "cancelDeliveryAssignment error: ${e.message}", e)
            e.localizedMessage ?: "Failed to cancel delivery."
        }
    }

    suspend fun createDeliveryIssue(
        orderId: String?,
        category: String,
        description: String,
        priority: String = "MEDIUM"
    ): String? = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext "You must be logged in."
        try {
            val issueId = "ISSUE-${System.currentTimeMillis().toString().takeLast(6)}"
            val issue = DeliveryIssue(
                issueId     = issueId,
                driverId    = user.userId,
                driverName  = user.name,
                orderId     = orderId?.takeIf { it.isNotBlank() },
                category    = category,
                description = description.trim(),
                priority    = priority,
                status      = "OPEN",
                createdAt   = System.currentTimeMillis(),
                updatedAt   = System.currentTimeMillis()
            )
            firestore.collection(COL_DELIVERY_ISSUES).document(issueId).set(issue.toMap()).await()
            _deliveryIssues.value = (listOf(issue) + _deliveryIssues.value.filterNot { it.issueId == issueId })
            android.util.Log.d("HomeChef", "Delivery issue created: $issueId")
            null
        } catch (e: Exception) {
            android.util.Log.e("HomeChef", "createDeliveryIssue error: ${e.message}", e)
            e.localizedMessage ?: "Failed to report issue."
        }
    }

    suspend fun updateDeliveryIssue(
        issueId: String,
        category: String,
        description: String,
        priority: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val updates = mapOf(
                "category"    to category,
                "description" to description.trim(),
                "priority"    to priority,
                "updatedAt"   to System.currentTimeMillis()
            )
            firestore.collection(COL_DELIVERY_ISSUES).document(issueId).update(updates).await()
            _deliveryIssues.value = _deliveryIssues.value.map {
                if (it.issueId == issueId) {
                    it.copy(
                        category = category,
                        description = description.trim(),
                        priority = priority,
                        updatedAt = System.currentTimeMillis()
                    )
                } else it
            }
            android.util.Log.d("HomeChef", "Delivery issue updated: $issueId")
            null
        } catch (e: Exception) {
            android.util.Log.e("HomeChef", "updateDeliveryIssue error: ${e.message}", e)
            e.localizedMessage ?: "Failed to update issue."
        }
    }

    suspend fun deleteDeliveryIssue(issueId: String): String? = withContext(Dispatchers.IO) {
        try {
            firestore.collection(COL_DELIVERY_ISSUES).document(issueId).delete().await()
            _deliveryIssues.value = _deliveryIssues.value.filterNot { it.issueId == issueId }
            android.util.Log.d("HomeChef", "Delivery issue deleted: $issueId")
            null
        } catch (e: Exception) {
            android.util.Log.e("HomeChef", "deleteDeliveryIssue error: ${e.message}", e)
            e.localizedMessage ?: "Failed to withdraw issue report."
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COOK PROMOTIONS & DEALS — Firestore CRUD
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun createPromo(
        code: String,
        title: String,
        description: String,
        discountType: String = "PERCENTAGE",
        discountValue: Double = 10.0,
        minOrderValue: Double = 0.0,
        durationDays: Int = 7,
        maxUsageLimit: Int = 50
    ): String? = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: return@withContext "You must be logged in to create promotions."
            val cleanCode = code.trim().uppercase().replace(" ", "")
            if (cleanCode.length < 3) return@withContext "Promo code must be at least 3 characters."
            if (discountValue <= 0) return@withContext "Discount amount must be greater than 0."
            if (discountType == "PERCENTAGE" && discountValue > 90) return@withContext "Percentage discount cannot exceed 90%."

            // Check duplicate code
            val existing = _promos.value.find { it.code.equals(cleanCode, ignoreCase = true) && !it.isExpired }
            if (existing != null) {
                return@withContext "An active promo with code '$cleanCode' already exists."
            }

            val promoId = "promo_${UUID.randomUUID().toString().take(8)}"
            val now = System.currentTimeMillis()
            val end = now + (durationDays.toLong() * 24 * 60 * 60 * 1000)

            val promo = PromoCode(
                promoId        = promoId,
                cookId         = user.userId,
                cookName       = user.name,
                code           = cleanCode,
                title          = title.trim().ifBlank { "$cleanCode Special" },
                description    = description.trim(),
                discountType   = discountType,
                discountValue  = discountValue,
                minOrderValue  = minOrderValue,
                startDate      = now,
                endDate        = end,
                isActive       = true,
                usageCount     = 0,
                maxUsageLimit  = maxUsageLimit,
                createdAt      = now
            )

            firestore.collection(COL_PROMOS).document(promoId).set(promo.toMap()).await()
            _promos.value = (listOf(promo) + _promos.value.filterNot { it.promoId == promoId })
            android.util.Log.d("HomeChef", "Promo created: $promoId ($cleanCode)")
            null
        } catch (e: Exception) {
            android.util.Log.e("HomeChef", "createPromo error: ${e.message}", e)
            e.localizedMessage ?: "Failed to create promo code."
        }
    }

    suspend fun updatePromo(
        promoId: String,
        title: String,
        description: String,
        discountType: String,
        discountValue: Double,
        minOrderValue: Double,
        extendDays: Int = 0,
        isActive: Boolean = true
    ): String? = withContext(Dispatchers.IO) {
        try {
            val current = _promos.value.find { it.promoId == promoId }
                ?: return@withContext "Promo deal not found."

            val newEndDate = if (extendDays > 0) {
                val base = if (current.isExpired) System.currentTimeMillis() else current.endDate
                base + (extendDays.toLong() * 24 * 60 * 60 * 1000)
            } else current.endDate

            val updates = mapOf(
                "title"         to title.trim(),
                "description"   to description.trim(),
                "discountType"  to discountType,
                "discountValue" to discountValue,
                "minOrderValue" to minOrderValue,
                "endDate"       to newEndDate,
                "isActive"      to isActive
            )

            firestore.collection(COL_PROMOS).document(promoId).update(updates).await()
            _promos.value = _promos.value.map {
                if (it.promoId == promoId) {
                    it.copy(
                        title         = title.trim(),
                        description   = description.trim(),
                        discountType  = discountType,
                        discountValue = discountValue,
                        minOrderValue = minOrderValue,
                        endDate       = newEndDate,
                        isActive      = isActive
                    )
                } else it
            }
            android.util.Log.d("HomeChef", "Promo updated: $promoId")
            null
        } catch (e: Exception) {
            android.util.Log.e("HomeChef", "updatePromo error: ${e.message}", e)
            e.localizedMessage ?: "Failed to update promo."
        }
    }

    suspend fun togglePromoStatus(promoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val current = _promos.value.find { it.promoId == promoId }
                ?: return@withContext "Promo deal not found."
            val newStatus = !current.isActive
            firestore.collection(COL_PROMOS).document(promoId).update("isActive", newStatus).await()
            _promos.value = _promos.value.map {
                if (it.promoId == promoId) it.copy(isActive = newStatus) else it
            }
            null
        } catch (e: Exception) {
            e.localizedMessage ?: "Failed to toggle promo status."
        }
    }

    suspend fun deletePromo(promoId: String): String? = withContext(Dispatchers.IO) {
        try {
            firestore.collection(COL_PROMOS).document(promoId).delete().await()
            _promos.value = _promos.value.filterNot { it.promoId == promoId }
            android.util.Log.d("HomeChef", "Promo deleted: $promoId")
            null
        } catch (e: Exception) {
            android.util.Log.e("HomeChef", "deletePromo error: ${e.message}", e)
            e.localizedMessage ?: "Failed to delete promo."
        }
    }

    suspend fun findPromoInFirestore(code: String): PromoCode? = withContext(Dispatchers.IO) {
        try {
            val clean = code.trim().uppercase().replace(" ", "")
            val query = firestore.collection(COL_PROMOS)
                .whereEqualTo("code", clean)
                .limit(1)
                .get()
                .await()
            if (!query.isEmpty) {
                val promo = query.documents.first().toPromoCode()
                if (promo != null) {
                    _promos.value = (listOf(promo) + _promos.value.filterNot { it.promoId == promo.promoId })
                    return@withContext promo
                }
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("HomeChef", "findPromoInFirestore failed: ${e.message}", e)
            null
        }
    }


    suspend fun customerUpdateOrder(
        orderId: String,
        deliveryAddress: String,
        customerPhone: String,
        notesForCook: String,
        updatedItems: List<CartItem>
    ): String? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection(COL_ORDERS).document(orderId).get().await()
            if (!doc.exists()) {
                return@withContext "Order does not exist."
            }
            val currentStatus = doc.getString("status") ?: ""
            if (currentStatus != OrderStatus.PENDING.name) {
                return@withContext "Cannot update: Order has already been accepted or processed by the cook."
            }
            if (updatedItems.isEmpty()) {
                return@withContext "Order must contain at least one item."
            }

            val subtotal = updatedItems.sumOf { it.price * it.quantity }
            val deliveryFee = doc.getDouble("deliveryFee") ?: 2.0
            val tax = subtotal * 0.05
            val total = subtotal + deliveryFee + tax

            val itemsData = updatedItems.map { item ->
                mapOf(
                    "foodId"         to item.foodId,
                    "cookId"         to item.cookId,
                    "cookName"       to item.cookName,
                    "title"          to item.title,
                    "description"    to item.description,
                    "price"          to item.price,
                    "imageUrl"       to item.imageUrl,
                    "quantity"       to item.quantity,
                    "specialRequest" to item.specialRequest
                )
            }

            val updates = mapOf(
                "deliveryAddress" to deliveryAddress.trim(),
                "customerPhone"   to customerPhone.trim(),
                "notesForCook"    to notesForCook.trim(),
                "items"           to itemsData,
                "subtotal"        to subtotal,
                "tax"             to tax,
                "total"           to total
            )

            firestore.collection(COL_ORDERS).document(orderId).update(updates).await()

            val customerId = doc.getString("customerId") ?: ""
            if (customerId.isNotBlank()) {
                val notifId = UUID.randomUUID().toString()
                firestore.collection(COL_NOTIFICATIONS).document(notifId).set(
                    mapOf(
                        "notificationId" to notifId,
                        "userId"         to customerId,
                        "title"          to "Order Updated",
                        "message"        to "Your order #$orderId was updated successfully.",
                        "timestamp"      to System.currentTimeMillis(),
                        "isRead"         to false,
                        "type"           to "ORDER"
                    )
                ).await()
            }

            android.util.Log.d("HomeChef", "Customer updated order $orderId successfully.")
            null
        } catch (e: Exception) {
            android.util.Log.e("HomeChef", "customerUpdateOrder error: ${e.message}", e)
            e.localizedMessage ?: "Failed to update order."
        }
    }

    suspend fun customerDeleteOrder(
        orderId: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection(COL_ORDERS).document(orderId).get().await()
            if (!doc.exists()) {
                return@withContext "Order does not exist."
            }
            val currentStatus = doc.getString("status") ?: ""
            if (currentStatus != OrderStatus.PENDING.name) {
                return@withContext "Cannot cancel/delete: Order has already been accepted or processed by the cook."
            }

            val customerId = doc.getString("customerId") ?: ""

            // Delete order document from Firestore
            firestore.collection(COL_ORDERS).document(orderId).delete().await()

            // Optimistically remove from local state
            _orders.value = _orders.value.filter { it.orderId != orderId }

            if (customerId.isNotBlank()) {
                val notifId = UUID.randomUUID().toString()
                firestore.collection(COL_NOTIFICATIONS).document(notifId).set(
                    mapOf(
                        "notificationId" to notifId,
                        "userId"         to customerId,
                        "title"          to "Order Cancelled",
                        "message"        to "Your order #$orderId was cancelled and deleted.",
                        "timestamp"      to System.currentTimeMillis(),
                        "isRead"         to false,
                        "type"           to "ORDER"
                    )
                ).await()
            }

            android.util.Log.d("HomeChef", "Customer deleted order $orderId successfully.")
            null
        } catch (e: Exception) {
            android.util.Log.e("HomeChef", "customerDeleteOrder error: ${e.message}", e)
            e.localizedMessage ?: "Failed to delete order."
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // FOOD MANAGEMENT — Firestore
    // ─────────────────────────────────────────────────────────────────────────

    fun addFood(food: Food) {
        // Optimistic update: immediately show in UI (Cook's My Menu & Customer Menu)
        _foods.value = (_foods.value + food).distinctBy { it.foodId }

        scope.launch {
            try {
                android.util.Log.d("HomeChef", "Writing food ${food.foodId} to Firestore...")
                firestore.collection(COL_FOODS).document(food.foodId)
                    .set(food.toMap()).await()
                android.util.Log.d("HomeChef", "Food ${food.foodId} successfully saved to Firestore.")
            } catch (e: Exception) {
                android.util.Log.e("HomeChef", "Failed to save food ${food.foodId}: ${e.message}", e)
                // Revert optimistic update on failure
                _foods.value = _foods.value.filter { it.foodId != food.foodId }
            }
        }
    }

    fun updateFood(food: Food) {
        // Optimistic update
        _foods.value = _foods.value.map { if (it.foodId == food.foodId) food else it }

        scope.launch {
            try {
                firestore.collection(COL_FOODS).document(food.foodId)
                    .set(food.toMap(), SetOptions.merge()).await()
                android.util.Log.d("HomeChef", "Food ${food.foodId} updated in Firestore.")
            } catch (e: Exception) {
                android.util.Log.e("HomeChef", "Failed to update food: ${e.message}", e)
            }
        }
    }

    fun deleteFood(foodId: String) {
        // Optimistic update
        _foods.value = _foods.value.filter { it.foodId != foodId }

        scope.launch {
            try {
                firestore.collection(COL_FOODS).document(foodId).delete().await()
                android.util.Log.d("HomeChef", "Food $foodId deleted from Firestore.")
            } catch (e: Exception) {
                android.util.Log.e("HomeChef", "Failed to delete food: ${e.message}", e)
            }
        }
    }

    fun toggleFoodAvailability(foodId: String) {
        val current = _foods.value.find { it.foodId == foodId } ?: return
        val newAvailable = !current.isAvailable
        _foods.value = _foods.value.map { if (it.foodId == foodId) it.copy(isAvailable = newAvailable) else it }

        scope.launch {
            try {
                firestore.collection(COL_FOODS).document(foodId)
                    .update("isAvailable", newAvailable).await()
            } catch (e: Exception) {
                android.util.Log.e("HomeChef", "Failed to toggle availability: ${e.message}", e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────────────────────────────────

    fun approveCook(userId: String) {
        scope.launch {
            firestore.collection(COL_USERS).document(userId)
                .update("isApprovedCook", true).await()
        }
    }

    fun toggleSuspendUser(userId: String) {
        val target = _users.value.find { it.userId == userId } ?: return
        scope.launch {
            firestore.collection(COL_USERS).document(userId)
                .update("isSuspended", !target.isSuspended).await()
        }
    }

    suspend fun adminCreateUser(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: UserRole,
        isApprovedCook: Boolean = true
    ): String? = withContext(Dispatchers.IO) {
        val caller = _currentUser.value
        if (caller?.role != UserRole.ADMIN) {
            return@withContext "Permission denied: Only Admin can create users."
        }

        val trimmedEmail = email.trim().lowercase()
        val trimmedPass = password.trim()
        val trimmedName = name.trim().ifBlank {
            trimmedEmail.substringBefore("@")
                .replace(".", " ")
                .replace("_", " ")
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                .ifBlank { "User" }
        }

        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            return@withContext "Please enter a valid email address."
        }
        if (trimmedPass.length < 6) {
            return@withContext "Password must be at least 6 characters."
        }

        // Secondary FirebaseApp allows creating a new Firebase Auth account without logging out the active admin
        val tempAppName = "AdminUserCreator_${UUID.randomUUID()}"
        var secondaryApp: FirebaseApp? = null
        return@withContext try {
            val options = FirebaseApp.getInstance().options
            secondaryApp = FirebaseApp.initializeApp(context, options, tempAppName)
            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

            val authResult = secondaryAuth.createUserWithEmailAndPassword(trimmedEmail, trimmedPass).await()
            val newUid = authResult.user?.uid ?: return@withContext "Failed to obtain new user ID."

            // Set display name in Firebase Auth
            try {
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = trimmedName
                }
                authResult.user?.updateProfile(profileUpdates)?.await()
            } catch (_: Exception) {}

            // Store user document in Firestore users collection
            val userDoc = mapOf(
                "userId"         to newUid,
                "name"           to trimmedName,
                "email"          to trimmedEmail,
                "phone"          to phone.trim(),
                "role"           to role.name,
                "photoUrl"       to "",
                "address"        to "",
                "isApprovedCook" to if (role == UserRole.COOK) isApprovedCook else true,
                "isSuspended"    to false
            )
            firestore.collection(COL_USERS).document(newUid).set(userDoc).await()
            android.util.Log.d("HomeChef", "Admin created user $trimmedEmail ($newUid) with role ${role.name}")
            null // Success
        } catch (e: Exception) {
            android.util.Log.e("HomeChef", "adminCreateUser failed: ${e.message}", e)
            parseAuthError(e.message)
        } finally {
            try {
                secondaryApp?.let {
                    FirebaseAuth.getInstance(it).signOut()
                    it.delete()
                }
            } catch (delEx: Exception) {
                android.util.Log.w("HomeChef", "Error cleaning up secondary FirebaseApp: ${delEx.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEED default foods if Firestore is empty
    // ─────────────────────────────────────────────────────────────────────────

    fun removeDummyFoods() {
        cleanDummyFoodsFromFirestore()
    }
}

// ─── Firestore DocumentSnapshot → Domain Models ───────────────────────────────

private fun com.google.firebase.firestore.DocumentSnapshot.toUser(): User? {
    return try {
        if (!exists()) return null
        val docName = getString("name")?.trim().orEmpty()
        val docEmail = getString("email")?.trim().orEmpty()
        if (docName.isBlank() && docEmail.isBlank()) return null
        User(
            userId         = getString("userId") ?: id,
            name           = docName,
            email          = docEmail,
            phone          = getString("phone")?.trim().orEmpty(),
            role           = try { UserRole.valueOf(getString("role") ?: "CUSTOMER") } catch (_: Exception) { UserRole.CUSTOMER },
            photoUrl       = getString("photoUrl")?.trim().orEmpty(),
            address        = getString("address")?.trim().orEmpty(),
            isApprovedCook = getBoolean("isApprovedCook") ?: false,
            isSuspended    = getBoolean("isSuspended") ?: false
        )
    } catch (e: Exception) {
        android.util.Log.e("HomeChef", "Error parsing user doc $id: ${e.message}")
        null
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toFood(): Food? = try {
    @Suppress("UNCHECKED_CAST")
    Food(
        foodId               = getString("foodId") ?: id,
        cookId               = getString("cookId") ?: "",
        cookName             = getString("cookName") ?: "",
        isCookVerified       = getBoolean("isCookVerified") ?: false,
        title                = getString("title") ?: "",
        description          = getString("description") ?: "",
        price                = (get("price") as? Number)?.toDouble() ?: getDouble("price") ?: 0.0,
        discountPrice        = (get("discountPrice") as? Number)?.toDouble() ?: getDouble("discountPrice"),
        category             = getString("category") ?: "",
        imageUrl             = getString("imageUrl") ?: "",
        ingredients          = (get("ingredients") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        allergens            = (get("allergens") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        calories             = (get("calories") as? Number)?.toInt() ?: getLong("calories")?.toInt() ?: 0,
        preparationTimeMins  = (get("preparationTimeMins") as? Number)?.toInt() ?: getLong("preparationTimeMins")?.toInt() ?: 20,
        rating               = (get("rating") as? Number)?.toDouble() ?: getDouble("rating") ?: 0.0,
        reviewCount          = (get("reviewCount") as? Number)?.toInt() ?: getLong("reviewCount")?.toInt() ?: 0,
        availableQuantity    = (get("availableQuantity") as? Number)?.toInt() ?: getLong("availableQuantity")?.toInt() ?: 10,
        isAvailable          = getBoolean("isAvailable") ?: true,
        isFeatured           = getBoolean("isFeatured") ?: false,
        isReorder            = getBoolean("isReorder") ?: false
    )
} catch (e: Exception) {
    android.util.Log.e("HomeChef", "Failed to deserialize food doc $id: ${e.message}")
    null
}

private fun com.google.firebase.firestore.DocumentSnapshot.toOrder(): Order? = try {
    @Suppress("UNCHECKED_CAST")
    val rawItems = get("items") as? List<Map<String, Any>> ?: emptyList()
    val items = rawItems.map { m ->
        CartItem(
            foodId         = m["foodId"] as? String ?: "",
            cookId         = m["cookId"] as? String ?: "",
            cookName       = m["cookName"] as? String ?: "",
            title          = m["title"] as? String ?: "",
            description    = m["description"] as? String ?: "",
            price          = (m["price"] as? Number)?.toDouble() ?: 0.0,
            imageUrl       = m["imageUrl"] as? String ?: "",
            quantity       = (m["quantity"] as? Number)?.toInt() ?: 1,
            specialRequest = m["specialRequest"] as? String ?: ""
        )
    }
    @Suppress("UNCHECKED_CAST")
    val cancelledDriverIds = (get("cancelledDriverIds") as? List<String>) ?: emptyList()

    Order(
        orderId             = getString("orderId") ?: id,
        customerId          = getString("customerId") ?: "",
        customerName        = getString("customerName") ?: "",
        customerPhone       = getString("customerPhone") ?: "",
        deliveryAddress     = getString("deliveryAddress") ?: "",
        cookId              = getString("cookId") ?: "",
        cookName            = getString("cookName") ?: "",
        items               = items,
        subtotal            = getDouble("subtotal") ?: 0.0,
        deliveryFee         = getDouble("deliveryFee") ?: 2.0,
        tax                 = getDouble("tax") ?: 0.0,
        total               = getDouble("total") ?: 0.0,
        status              = OrderStatus.valueOf(getString("status") ?: "PENDING"),
        paymentMethod       = getString("paymentMethod") ?: "",
        notesForCook        = getString("notesForCook") ?: "",
        deliveryId          = getString("deliveryId"),
        deliveryPartnerName = getString("deliveryPartnerName"),
        rejectReason        = getString("rejectReason"),
        createdAt           = getLong("createdAt")?.takeIf { it > 0L } ?: System.currentTimeMillis(),
        cancelledDriverIds  = cancelledDriverIds
    )
} catch (e: Exception) { null }

private fun com.google.firebase.firestore.DocumentSnapshot.toNotification(): NotificationItem? = try {
    NotificationItem(
        notificationId = getString("notificationId") ?: id,
        userId         = getString("userId") ?: "",
        title          = getString("title") ?: "",
        message        = getString("message") ?: "",
        timestamp      = getLong("timestamp") ?: 0L,
        isRead         = getBoolean("isRead") ?: false,
        type           = getString("type") ?: "GENERAL"
    )
} catch (e: Exception) { null }

private fun com.google.firebase.firestore.DocumentSnapshot.toDeliveryIssue(): DeliveryIssue? = try {
    DeliveryIssue(
        issueId     = getString("issueId") ?: id,
        driverId    = getString("driverId") ?: "",
        driverName  = getString("driverName") ?: "",
        orderId     = getString("orderId"),
        category    = getString("category") ?: DeliveryIssueCategories.OTHER,
        description = getString("description") ?: "",
        priority    = getString("priority") ?: "MEDIUM",
        status      = getString("status") ?: "OPEN",
        createdAt   = getLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt   = getLong("updatedAt") ?: System.currentTimeMillis()
    )
} catch (e: Exception) { null }

private fun DeliveryIssue.toMap(): Map<String, Any?> = mapOf(
    "issueId"     to issueId,
    "driverId"    to driverId,
    "driverName"  to driverName,
    "orderId"     to orderId,
    "category"    to category,
    "description" to description,
    "priority"    to priority,
    "status"      to status,
    "createdAt"   to createdAt,
    "updatedAt"   to updatedAt
)

private fun com.google.firebase.firestore.DocumentSnapshot.toPromoCode(): PromoCode? = try {
    val endVal = getLong("endDate") ?: 0L
    val finalEnd = if (endVal > 0L) endVal else (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
    PromoCode(
        promoId       = getString("promoId") ?: id,
        cookId        = getString("cookId") ?: "",
        cookName      = getString("cookName") ?: "",
        code          = getString("code") ?: "",
        title         = getString("title") ?: "",
        description   = getString("description") ?: "",
        discountType  = getString("discountType") ?: "PERCENTAGE",
        discountValue = getDouble("discountValue") ?: 10.0,
        minOrderValue = getDouble("minOrderValue") ?: 0.0,
        startDate     = getLong("startDate") ?: System.currentTimeMillis(),
        endDate       = finalEnd,
        isActive      = getBoolean("isActive") ?: true,
        usageCount    = getLong("usageCount")?.toInt() ?: 0,
        maxUsageLimit = getLong("maxUsageLimit")?.toInt() ?: 50,
        createdAt     = getLong("createdAt") ?: System.currentTimeMillis()
    )
} catch (e: Exception) { null }

private fun PromoCode.toMap(): Map<String, Any?> = mapOf(
    "promoId"       to promoId,
    "cookId"        to cookId,
    "cookName"      to cookName,
    "code"          to code,
    "title"         to title,
    "description"   to description,
    "discountType"  to discountType,
    "discountValue" to discountValue,
    "minOrderValue" to minOrderValue,
    "startDate"     to startDate,
    "endDate"       to endDate,
    "isActive"      to isActive,
    "usageCount"    to usageCount,
    "maxUsageLimit" to maxUsageLimit,
    "createdAt"     to createdAt
)

// ─── Domain model → Firestore Map ─────────────────────────────────────────────

private fun Food.toMap(): Map<String, Any> = buildMap {
    put("foodId", foodId)
    put("cookId", cookId)
    put("cookName", cookName)
    put("isCookVerified", isCookVerified)
    put("title", title)
    put("description", description)
    put("price", price)
    discountPrice?.let { put("discountPrice", it) }
    put("category", category)
    put("imageUrl", imageUrl)
    put("ingredients", ingredients)
    put("allergens", allergens)
    put("calories", calories)
    put("preparationTimeMins", preparationTimeMins)
    put("rating", rating)
    put("reviewCount", reviewCount)
    put("availableQuantity", availableQuantity)
    put("isAvailable", isAvailable)
    put("isFeatured", isFeatured)
    put("isReorder", isReorder)
}

private fun Order.toMap() = mapOf(
    "orderId"             to orderId,
    "customerId"          to customerId,
    "customerName"        to customerName,
    "customerPhone"       to customerPhone,
    "deliveryAddress"     to deliveryAddress,
    "cookId"              to cookId,
    "cookName"            to cookName,
    "items"               to items.map { item ->
        mapOf(
            "foodId"         to item.foodId,
            "cookId"         to item.cookId,
            "cookName"       to item.cookName,
            "title"          to item.title,
            "description"    to item.description,
            "price"          to item.price,
            "imageUrl"       to item.imageUrl,
            "quantity"       to item.quantity,
            "specialRequest" to item.specialRequest
        )
    },
    "subtotal"            to subtotal,
    "deliveryFee"         to deliveryFee,
    "tax"                 to tax,
    "total"               to total,
    "status"              to status.name,
    "paymentMethod"       to paymentMethod,
    "notesForCook"        to notesForCook,
    "deliveryId"          to deliveryId,
    "deliveryPartnerName" to deliveryPartnerName,
    "rejectReason"        to rejectReason,
    "createdAt"           to createdAt,
    "cancelledDriverIds"  to cancelledDriverIds
)

// ─── Default food catalog (seeded to Firestore if empty) ──────────────────────
private fun defaultFoods(): List<Food> = listOf(
    Food(foodId="f_sarah_01", cookId="cook_001", cookName="Chef Maria Rosa", isCookVerified=true,
        title="Indian Biriyani", description="Fragrant royal basmati rice cooked with delicate spices and tender chicken.",
        price=8.50, category="Biryani", imageUrl="https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=600",
        ingredients=listOf("Basmati Rice","Chicken","Saffron","Spices"), allergens=listOf("Dairy"),
        calories=510, preparationTimeMins=25, rating=4.8, reviewCount=124, availableQuantity=25, isAvailable=true, isFeatured=true),
    Food(foodId="f_001", cookId="cook_001", cookName="Chef Maria Rosa", isCookVerified=true,
        title="French Fries", description="Golden crispy fries lightly salted with garlic aioli.",
        price=5.60, discountPrice=4.90, category="Snacks", imageUrl="https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=600",
        ingredients=listOf("Potatoes","Sea Salt","Oil","Garlic Dip"), allergens=listOf("None"),
        calories=380, preparationTimeMins=15, rating=4.9, reviewCount=182, availableQuantity=30, isFeatured=true),
    Food(foodId="f_002", cookId="cook_001", cookName="Chef Maria Rosa", isCookVerified=true,
        title="Grilled Chicken Breast", description="Herb-marinated grilled chicken breast with steamed veggies.",
        price=3.97, category="Healthy", imageUrl="https://images.unsplash.com/photo-1532550907401-a500c9a57435?w=600",
        ingredients=listOf("Chicken Breast","Rosemary","Olive Oil","Lemon"), allergens=listOf("None"),
        calories=320, preparationTimeMins=20, rating=4.8, reviewCount=94, availableQuantity=20, isFeatured=true, isReorder=true),
    Food(foodId="f_003", cookId="cook_002", cookName="Uncle Raj's Kitchen", isCookVerified=true,
        title="Crunchy Taco Supreme", description="Crispy taco loaded with seasoned beef and cheddar.",
        price=4.00, category="Fast Food", imageUrl="https://images.unsplash.com/photo-1551504734-5ee1c4a1479b?w=600",
        ingredients=listOf("Corn Shell","Seasoned Beef","Cheddar","Lettuce","Sour Cream"), allergens=listOf("Gluten","Dairy"),
        calories=410, preparationTimeMins=15, rating=4.6, reviewCount=77, availableQuantity=15, isReorder=true),
    Food(foodId="f_004", cookId="cook_002", cookName="Uncle Raj's Kitchen", isCookVerified=true,
        title="Spicy Butter Chicken", description="Succulent chicken in creamy butter tomato gravy.",
        price=7.20, category="Curry", imageUrl="https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?w=600",
        ingredients=listOf("Chicken","Butter","Tomato","Cream","Garam Masala"), allergens=listOf("Dairy"),
        calories=490, preparationTimeMins=30, rating=4.9, reviewCount=203, availableQuantity=12, isFeatured=true),
    Food(foodId="f_005", cookId="cook_001", cookName="Chef Maria Rosa", isCookVerified=true,
        title="Avocado Toast", description="Sourdough topped with smashed avocado and poached eggs.",
        price=6.50, category="Healthy", imageUrl="https://images.unsplash.com/photo-1588137378633-dea1336ce1e2?w=600",
        ingredients=listOf("Sourdough","Avocado","Cherry Tomatoes","Eggs"), allergens=listOf("Gluten","Eggs"),
        calories=280, preparationTimeMins=10, rating=4.7, reviewCount=58, availableQuantity=10)
)
