package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import com.example.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
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
private const val COL_ORDERS        = "orders"
private const val COL_NOTIFICATIONS = "notifications"

class HomeChefRepository(context: Context) {

    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val db        = AppDatabase.getDatabase(context)
    private val scope     = CoroutineScope(Dispatchers.IO)

    // ─── Live Listeners ───────────────────────────────────────────────────────
    private var foodsListener: ListenerRegistration? = null
    private var ordersListener: ListenerRegistration? = null
    private var notifsListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null

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

    // ─── Room-backed Flows (offline-capable) ──────────────────────────────────
    val favoriteFoodIds: StateFlow<List<String>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) db.favoriteDao().getFavoriteFoodIds(user.userId)
            else flowOf(emptyList())
        }
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    val cartItems: StateFlow<List<CartItem>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                db.cartDao().getAllCartItems(user.userId).map { list ->
                    list.map {
                        CartItem(
                            foodId         = it.foodId,
                            cookId         = it.cookId,
                            cookName       = it.cookName,
                            title          = it.title,
                            description    = it.description,
                            price          = it.price,
                            imageUrl       = it.imageUrl,
                            quantity       = it.quantity,
                            specialRequest = it.specialRequest
                        )
                    }
                }
            } else flowOf(emptyList())
        }
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    init {
        startGlobalListeners()
    }

    // ─── Firestore Real-time Listeners ────────────────────────────────────────
    private fun startGlobalListeners() {
        // Foods
        foodsListener = firestore.collection(COL_FOODS)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val list = snap.documents.mapNotNull { it.toFood() }
                    _foods.value = list.ifEmpty { defaultFoods() }
                }
            }
        // Orders
        ordersListener = firestore.collection(COL_ORDERS)
            .addSnapshotListener { snap, _ ->
                if (snap != null)
                    _orders.value = snap.documents.mapNotNull { it.toOrder() }
            }
        // Users (admin)
        usersListener = firestore.collection(COL_USERS)
            .addSnapshotListener { snap, _ ->
                if (snap != null)
                    _users.value = snap.documents.mapNotNull { it.toUser() }
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

    // ─────────────────────────────────────────────────────────────────────────
    // AUTH
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val uid    = result.user?.uid ?: return@withContext "Login failed."
            // Load user from Firestore and set immediately
            val doc = firestore.collection(COL_USERS).document(uid).get().await()
            val user = doc.toUser() ?: return@withContext "Account data not found. Please re-register."
            _currentUser.value = user
            startUserNotifListener(uid)
            null
        } catch (e: Exception) {
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

            // Step 2: Set currentUser IMMEDIATELY so navigation fires right away
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
            startUserNotifListener(uid)

            // Step 3: Firestore write in background — non-fatal if it fails
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
            } catch (fsEx: Exception) {
                android.util.Log.w("HomeChef", "Firestore write failed (non-fatal): ${fsEx.message}")
            }

            null // success
        } catch (e: Exception) {
            parseAuthError(e.message)
        }
    }


    fun logout() {
        auth.signOut()
        _currentUser.value = null
        notifsListener?.remove()
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
            msg.contains("email address is already") -> "An account with this email already exists."
            msg.contains("weak-password")          -> "Password must be at least 6 characters."
            msg.contains("network error")          -> "Network error. Check your internet connection."
            else                                   -> "Error: $msg"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROFILE
    // ─────────────────────────────────────────────────────────────────────────

    fun updateUserProfile(name: String, email: String, phone: String, address: String) {
        val current = _currentUser.value ?: return
        _currentUser.value = current.copy(name = name, email = email, phone = phone, address = address)
        scope.launch {
            firestore.collection(COL_USERS).document(current.userId).update(
                mapOf("name" to name, "phone" to phone, "address" to address)
            ).await()
        }
    }

    fun updateUserPhoto(photoUri: String) {
        val current = _currentUser.value ?: return
        _currentUser.value = current.copy(photoUrl = photoUri)
        scope.launch {
            firestore.collection(COL_USERS).document(current.userId)
                .update("photoUrl", photoUri).await()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FAVORITES (Room — offline)
    // ─────────────────────────────────────────────────────────────────────────

    fun toggleFavorite(foodId: String) {
        val userId = _currentUser.value?.userId ?: return
        scope.launch {
            if (favoriteFoodIds.value.contains(foodId)) {
                db.favoriteDao().removeFavorite(userId, foodId)
            } else {
                db.favoriteDao().addFavorite(FavoriteEntity(userId, foodId))
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CART (Room — offline)
    // ─────────────────────────────────────────────────────────────────────────

    fun addToCart(food: Food, quantity: Int = 1, specialRequest: String = "") {
        val userId = _currentUser.value?.userId ?: return
        scope.launch {
            val existing = cartItems.value.find { it.foodId == food.foodId }
            val newQty   = (existing?.quantity ?: 0) + quantity
            db.cartDao().insertOrUpdate(
                CartItemEntity(
                    userId         = userId,
                    foodId         = food.foodId,
                    cookId         = food.cookId,
                    cookName       = food.cookName,
                    title          = food.title,
                    description    = food.description,
                    price          = food.discountPrice ?: food.price,
                    imageUrl       = food.imageUrl,
                    quantity       = newQty,
                    specialRequest = if (specialRequest.isNotBlank()) specialRequest
                                     else (existing?.specialRequest ?: "")
                )
            )
        }
    }

    fun updateCartQuantity(foodId: String, newQty: Int) {
        val userId = _currentUser.value?.userId ?: return
        scope.launch {
            if (newQty <= 0) {
                db.cartDao().deleteByFoodId(userId, foodId)
            } else {
                val item = cartItems.value.find { it.foodId == foodId }
                item?.let {
                    db.cartDao().insertOrUpdate(
                        CartItemEntity(userId, it.foodId, it.cookId, it.cookName,
                            it.title, it.description, it.price, it.imageUrl,
                            newQty, it.specialRequest)
                    )
                }
            }
        }
    }

    fun removeCartItem(foodId: String) {
        val userId = _currentUser.value?.userId ?: return
        scope.launch { db.cartDao().deleteByFoodId(userId, foodId) }
    }

    fun clearCart() {
        val userId = _currentUser.value?.userId ?: return
        scope.launch { db.cartDao().clearCart(userId) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORDERS (Firestore)
    // ─────────────────────────────────────────────────────────────────────────

    fun checkoutOrders(
        deliveryAddress: String,
        paymentMethod: String,
        notesForCook: String,
        promoDiscount: Double = 0.0
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
                    "status"              to OrderStatus.PICKED_UP.name
                )
            ).await()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FOOD MANAGEMENT — Firestore
    // ─────────────────────────────────────────────────────────────────────────

    fun addFood(food: Food) {
        scope.launch {
            firestore.collection(COL_FOODS).document(food.foodId)
                .set(food.toMap()).await()
        }
    }

    fun updateFood(food: Food) {
        scope.launch {
            firestore.collection(COL_FOODS).document(food.foodId)
                .set(food.toMap(), SetOptions.merge()).await()
        }
    }

    fun deleteFood(foodId: String) {
        scope.launch {
            firestore.collection(COL_FOODS).document(foodId).delete().await()
        }
    }

    fun toggleFoodAvailability(foodId: String) {
        val current = _foods.value.find { it.foodId == foodId } ?: return
        scope.launch {
            firestore.collection(COL_FOODS).document(foodId)
                .update("isAvailable", !current.isAvailable).await()
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

    // ─────────────────────────────────────────────────────────────────────────
    // SEED default foods if Firestore is empty
    // ─────────────────────────────────────────────────────────────────────────

    fun seedDefaultFoodsIfEmpty() {
        scope.launch {
            val existing = firestore.collection(COL_FOODS).limit(1).get().await()
            if (existing.isEmpty) {
                defaultFoods().forEach { food ->
                    firestore.collection(COL_FOODS).document(food.foodId)
                        .set(food.toMap()).await()
                }
            }
        }
    }
}

// ─── Firestore DocumentSnapshot → Domain Models ───────────────────────────────

private fun com.google.firebase.firestore.DocumentSnapshot.toUser(): User? = try {
    User(
        userId         = getString("userId") ?: id,
        name           = getString("name") ?: "",
        email          = getString("email") ?: "",
        phone          = getString("phone") ?: "",
        role           = UserRole.valueOf(getString("role") ?: "CUSTOMER"),
        photoUrl       = getString("photoUrl") ?: "",
        address        = getString("address") ?: "",
        isApprovedCook = getBoolean("isApprovedCook") ?: false,
        isSuspended    = getBoolean("isSuspended") ?: false
    )
} catch (e: Exception) { null }

private fun com.google.firebase.firestore.DocumentSnapshot.toFood(): Food? = try {
    @Suppress("UNCHECKED_CAST")
    Food(
        foodId               = getString("foodId") ?: id,
        cookId               = getString("cookId") ?: "",
        cookName             = getString("cookName") ?: "",
        isCookVerified       = getBoolean("isCookVerified") ?: false,
        title                = getString("title") ?: "",
        description          = getString("description") ?: "",
        price                = getDouble("price") ?: 0.0,
        discountPrice        = getDouble("discountPrice"),
        category             = getString("category") ?: "",
        imageUrl             = getString("imageUrl") ?: "",
        ingredients          = get("ingredients") as? List<String> ?: emptyList(),
        allergens            = get("allergens") as? List<String> ?: emptyList(),
        calories             = getLong("calories")?.toInt() ?: 0,
        preparationTimeMins  = getLong("preparationTimeMins")?.toInt() ?: 20,
        rating               = getDouble("rating") ?: 0.0,
        reviewCount          = getLong("reviewCount")?.toInt() ?: 0,
        availableQuantity    = getLong("availableQuantity")?.toInt() ?: 10,
        isAvailable          = getBoolean("isAvailable") ?: true,
        isFeatured           = getBoolean("isFeatured") ?: false,
        isReorder            = getBoolean("isReorder") ?: false
    )
} catch (e: Exception) { null }

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
        createdAt           = getLong("createdAt") ?: 0L
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

// ─── Domain model → Firestore Map ─────────────────────────────────────────────

private fun Food.toMap() = mapOf(
    "foodId"              to foodId,
    "cookId"              to cookId,
    "cookName"            to cookName,
    "isCookVerified"      to isCookVerified,
    "title"               to title,
    "description"         to description,
    "price"               to price,
    "discountPrice"       to discountPrice,
    "category"            to category,
    "imageUrl"            to imageUrl,
    "ingredients"         to ingredients,
    "allergens"           to allergens,
    "calories"            to calories,
    "preparationTimeMins" to preparationTimeMins,
    "rating"              to rating,
    "reviewCount"         to reviewCount,
    "availableQuantity"   to availableQuantity,
    "isAvailable"         to isAvailable,
    "isFeatured"          to isFeatured,
    "isReorder"           to isReorder
)

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
    "createdAt"           to createdAt
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
