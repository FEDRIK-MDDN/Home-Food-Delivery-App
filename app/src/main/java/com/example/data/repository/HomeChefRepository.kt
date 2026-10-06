package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.CartItemEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class HomeChefRepository(context: Context) {

    private val db    = AppDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    // ─── Logged-in User State ────────────────────────────────────────────────
    // null means not logged in
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // ─── All registered users (from DB, for admin view) ─────────────────────
    val users: StateFlow<List<User>> = db.userDao().getAllUsers()
        .map { list -> list.map { it.toUser() } }
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    // ─── AUTH: Login ─────────────────────────────────────────────────────────
    /**
     * Returns null on success, or an error message string on failure.
     */
    suspend fun login(email: String, password: String): String? = withContext(Dispatchers.IO) {
        val trimEmail = email.trim().lowercase()
        val entity    = db.userDao().findByEmail(trimEmail)
            ?: return@withContext "No account found with that email."

        if (entity.isSuspended) {
            return@withContext "Your account has been suspended. Contact support."
        }

        val hash = AppDatabase.hashPassword(password)
        if (entity.passwordHash != hash) {
            return@withContext "Incorrect password. Please try again."
        }

        _currentUser.value = entity.toUser()
        null  // success
    }

    // ─── AUTH: Register ───────────────────────────────────────────────────────
    /**
     * Returns null on success, or an error message string on failure.
     * Role is CUSTOMER by default; COOK roles start as unapproved.
     */
    suspend fun register(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: UserRole
    ): String? = withContext(Dispatchers.IO) {
        val trimEmail = email.trim().lowercase()

        // Email must be unique
        if (db.userDao().findByEmail(trimEmail) != null) {
            return@withContext "An account with this email already exists."
        }

        // Admin role cannot be registered — only the hardcoded admin exists
        if (role == UserRole.ADMIN) {
            return@withContext "Admin accounts cannot be registered."
        }

        val userId = UUID.randomUUID().toString()
        val entity = UserEntity(
            userId       = userId,
            name         = name.trim(),
            email        = trimEmail,
            passwordHash = AppDatabase.hashPassword(password),
            phone        = phone.trim(),
            role         = role.name,
            isApprovedCook = role != UserRole.COOK,   // cooks need approval, others auto-approved
            isSuspended    = false
        )

        val rowId = db.userDao().insertUser(entity)
        if (rowId == -1L) {
            return@withContext "Registration failed. Please try again."
        }

        _currentUser.value = entity.toUser()
        null  // success
    }

    // ─── AUTH: Logout ─────────────────────────────────────────────────────────
    fun logout() {
        _currentUser.value = null
    }

    // ─── Initial Foods list ───────────────────────────────────────────────────
    private val _foods = MutableStateFlow<List<Food>>(
        listOf(
            Food(
                foodId = "f_sarah_01",
                cookId = "cook_001",
                cookName = "Chef Maria Rosa",
                isCookVerified = true,
                title = "Indian Biriyani",
                description = "Fragrant royal basmati rice cooked with delicate spices and tender chicken.",
                price = 8.50,
                category = "Biryani",
                imageUrl = "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=600",
                ingredients = listOf("Basmati Rice", "Chicken", "Saffron", "Spices"),
                allergens = listOf("Dairy"),
                calories = 510,
                preparationTimeMins = 25,
                rating = 4.8,
                reviewCount = 124,
                availableQuantity = 25,
                isAvailable = true,
                isFeatured = true
            ),
            Food(
                foodId = "f_001",
                cookId = "cook_001",
                cookName = "Chef Maria Rosa",
                isCookVerified = true,
                title = "French Fries",
                description = "Golden crispy fries lightly salted and served warm with garlic aioli and ketchup.",
                price = 5.60,
                discountPrice = 4.90,
                category = "Snacks",
                imageUrl = "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=600",
                ingredients = listOf("Potatoes", "Sea Salt", "Vegetable Oil", "Garlic Dip"),
                allergens = listOf("None"),
                calories = 380,
                preparationTimeMins = 15,
                rating = 4.9,
                reviewCount = 182,
                availableQuantity = 30,
                isFeatured = true
            ),
            Food(
                foodId = "f_002",
                cookId = "cook_001",
                cookName = "Chef Maria Rosa",
                isCookVerified = true,
                title = "Grilled chicken breast",
                description = "Tender, herb-marinated grilled chicken breast served with steamed veggies.",
                price = 3.97,
                category = "Healthy",
                imageUrl = "https://images.unsplash.com/photo-1532550907401-a500c9a57435?w=600",
                ingredients = listOf("Chicken Breast", "Rosemary", "Olive Oil", "Lemon", "Pepper"),
                allergens = listOf("None"),
                calories = 320,
                preparationTimeMins = 20,
                rating = 4.8,
                reviewCount = 94,
                availableQuantity = 20,
                isFeatured = true,
                isReorder = true
            ),
            Food(
                foodId = "f_003",
                cookId = "cook_002",
                cookName = "Uncle Raj's Kitchen",
                isCookVerified = true,
                title = "Crunchy Taco Supreme",
                description = "A crispy flavor-packed crunchy taco loaded with seasoned beef, shredded cheese, and sour cream.",
                price = 4.00,
                category = "Fast Food",
                imageUrl = "https://images.unsplash.com/photo-1551504734-5ee1c4a1479b?w=600",
                ingredients = listOf("Corn Shell", "Seasoned Beef", "Cheddar Cheese", "Lettuce", "Sour Cream"),
                allergens = listOf("Gluten", "Dairy"),
                calories = 410,
                preparationTimeMins = 15,
                rating = 4.6,
                reviewCount = 77,
                availableQuantity = 15,
                isFeatured = false,
                isReorder = true
            ),
            Food(
                foodId = "f_004",
                cookId = "cook_002",
                cookName = "Uncle Raj's Kitchen",
                isCookVerified = true,
                title = "Spicy Butter Chicken",
                description = "Succulent chicken pieces slow-cooked in a creamy butter tomato gravy.",
                price = 7.20,
                category = "Curry",
                imageUrl = "https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?w=600",
                ingredients = listOf("Chicken", "Butter", "Tomato", "Cream", "Garam Masala"),
                allergens = listOf("Dairy"),
                calories = 490,
                preparationTimeMins = 30,
                rating = 4.9,
                reviewCount = 203,
                availableQuantity = 12,
                isFeatured = true
            ),
            Food(
                foodId = "f_005",
                cookId = "cook_001",
                cookName = "Chef Maria Rosa",
                isCookVerified = true,
                title = "Avocado Toast",
                description = "Perfectly toasted sourdough topped with smashed avocado, cherry tomatoes, and poached eggs.",
                price = 6.50,
                category = "Healthy",
                imageUrl = "https://images.unsplash.com/photo-1588137378633-dea1336ce1e2?w=600",
                ingredients = listOf("Sourdough", "Avocado", "Cherry Tomatoes", "Eggs", "Chili Flakes"),
                allergens = listOf("Gluten", "Eggs"),
                calories = 280,
                preparationTimeMins = 10,
                rating = 4.7,
                reviewCount = 58,
                availableQuantity = 10,
                isFeatured = false
            )
        )
    )
    val foods: StateFlow<List<Food>> = _foods.asStateFlow()

    // Favorites — scoped to the currently logged-in user
    val favoriteFoodIds: StateFlow<List<String>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) db.favoriteDao().getFavoriteFoodIds(user.userId)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    // Cart Items from Room — scoped to the currently logged-in user
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
            } else kotlinx.coroutines.flow.flowOf(emptyList())
        }
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    // Orders state (in-memory, demo orders)
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    // ─── Profile Updates ──────────────────────────────────────────────────────
    fun updateUserProfile(name: String, email: String, phone: String, address: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(name = name, email = email, phone = phone, address = address)
        _currentUser.value = updated
        scope.launch {
            db.userDao().updateProfile(current.userId, name, phone, address)
            db.userProfileDao().saveUserProfile(
                UserProfileEntity(updated.userId, updated.name, updated.email, updated.phone, updated.role.name, updated.photoUrl, updated.address)
            )
        }
    }

    fun updateUserPhoto(photoUri: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(photoUrl = photoUri)
        _currentUser.value = updated
        scope.launch {
            db.userDao().updatePhoto(current.userId, photoUri)
            db.userProfileDao().saveUserProfile(
                UserProfileEntity(updated.userId, updated.name, updated.email, updated.phone, updated.role.name, updated.photoUrl, updated.address)
            )
        }
    }

    // ─── Favorites ────────────────────────────────────────────────────────────
    fun toggleFavorite(foodId: String) {
        val userId = _currentUser.value?.userId ?: return
        scope.launch {
            val isFav = favoriteFoodIds.value.contains(foodId)
            if (isFav) {
                db.favoriteDao().removeFavorite(userId, foodId)
            } else {
                db.favoriteDao().addFavorite(FavoriteEntity(userId, foodId))
            }
        }
    }

    // ─── Cart ─────────────────────────────────────────────────────────────────
    fun addToCart(food: Food, quantity: Int = 1, specialRequest: String = "") {
        val userId = _currentUser.value?.userId ?: return
        scope.launch {
            val existing = cartItems.value.find { it.foodId == food.foodId }
            val newQty   = (existing?.quantity ?: 0) + quantity
            val item = CartItemEntity(
                userId         = userId,
                foodId         = food.foodId,
                cookId         = food.cookId,
                cookName       = food.cookName,
                title          = food.title,
                description    = food.description,
                price          = food.discountPrice ?: food.price,
                imageUrl       = food.imageUrl,
                quantity       = newQty,
                specialRequest = if (specialRequest.isNotBlank()) specialRequest else (existing?.specialRequest ?: "")
            )
            db.cartDao().insertOrUpdate(item)
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
                        CartItemEntity(userId, it.foodId, it.cookId, it.cookName, it.title, it.description, it.price, it.imageUrl, newQty, it.specialRequest)
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

    // ─── Orders ───────────────────────────────────────────────────────────────
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
        val newOrders       = mutableListOf<Order>()
        var remainingPromo  = promoDiscount

        groupedByCook.forEach { (cookId, items) ->
            val cookName           = items.firstOrNull()?.cookName ?: "Home Cook"
            val subtotal           = items.sumOf { it.price * it.quantity }
            val deliveryFee        = 2.00
            val tax                = subtotal * 0.05
            val discountForThisOrder = remainingPromo
            remainingPromo = 0.0
            val total   = (subtotal + deliveryFee + tax - discountForThisOrder).coerceAtLeast(1.0)
            val orderId = "ORD-${(1000..9999).random()}"

            val order = Order(
                orderId          = orderId,
                customerId       = user.userId,
                customerName     = user.name,
                customerPhone    = user.phone,
                deliveryAddress  = deliveryAddress.ifBlank { user.address },
                cookId           = cookId,
                cookName         = cookName,
                items            = items,
                subtotal         = subtotal,
                deliveryFee      = deliveryFee,
                tax              = tax,
                total            = total,
                status           = OrderStatus.PENDING,
                paymentMethod    = paymentMethod,
                notesForCook     = notesForCook,
                createdAt        = System.currentTimeMillis()
            )

            newOrders.add(order)
            createdOrderIds.add(orderId)
        }

        _orders.value = newOrders + _orders.value
        _notifications.value = listOf(
            NotificationItem(
                UUID.randomUUID().toString(),
                user.userId,
                "Orders Placed Successfully!",
                "Created ${createdOrderIds.size} distinct order(s) for your sellers."
            )
        ) + _notifications.value

        clearCart()
        return createdOrderIds
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus, rejectReason: String? = null) {
        val updated = _orders.value.map { order ->
            if (order.orderId == orderId) {
                order.copy(
                    status       = newStatus,
                    rejectReason = rejectReason ?: order.rejectReason
                )
            } else order
        }
        _orders.value = updated

        val targetOrder = updated.find { it.orderId == orderId }
        targetOrder?.let {
            val msg = when (newStatus) {
                OrderStatus.ACCEPTED        -> "Your order #${it.orderId} was accepted by ${it.cookName}!"
                OrderStatus.PREPARING       -> "${it.cookName} is preparing your food now!"
                OrderStatus.READY           -> "Order #${it.orderId} is ready for pickup!"
                OrderStatus.PICKED_UP       -> "Delivery driver picked up your food!"
                OrderStatus.OUT_FOR_DELIVERY -> "Driver is on the way with your food!"
                OrderStatus.DELIVERED       -> "Order #${it.orderId} delivered! Enjoy your meal!"
                OrderStatus.COMPLETED       -> "Order completed. Please leave a rating!"
                OrderStatus.CANCELLED       -> "Order #${it.orderId} was cancelled. Reason: ${rejectReason ?: "N/A"}"
                else                        -> "Status updated to ${newStatus.label}"
            }
            _notifications.value = listOf(
                NotificationItem(UUID.randomUUID().toString(), it.customerId, "Order Update", msg)
            ) + _notifications.value
        }
    }

    fun assignDeliveryPartner(orderId: String, deliveryPartnerName: String, deliveryId: String) {
        _orders.value = _orders.value.map { order ->
            if (order.orderId == orderId) {
                order.copy(
                    deliveryId          = deliveryId,
                    deliveryPartnerName = deliveryPartnerName,
                    status              = OrderStatus.PICKED_UP
                )
            } else order
        }
    }

    // ─── Food Management (Cook) ───────────────────────────────────────────────
    fun addFood(food: Food) {
        _foods.value = listOf(food) + _foods.value
    }

    fun updateFood(food: Food) {
        _foods.value = _foods.value.map { if (it.foodId == food.foodId) food else it }
    }

    fun deleteFood(foodId: String) {
        _foods.value = _foods.value.filter { it.foodId != foodId }
    }

    fun toggleFoodAvailability(foodId: String) {
        _foods.value = _foods.value.map {
            if (it.foodId == foodId) it.copy(isAvailable = !it.isAvailable) else it
        }
    }

    // ─── Admin Actions ────────────────────────────────────────────────────────
    fun approveCook(userId: String) {
        scope.launch { db.userDao().approveCook(userId) }
    }

    fun toggleSuspendUser(userId: String) {
        val target = users.value.find { it.userId == userId } ?: return
        scope.launch { db.userDao().setSuspended(userId, !target.isSuspended) }
    }
}

// ─── Extension: UserEntity → domain User ─────────────────────────────────────
private fun UserEntity.toUser() = User(
    userId        = userId,
    name          = name,
    email         = email,
    phone         = phone,
    role          = UserRole.valueOf(role),
    photoUrl      = photoUrl,
    address       = address,
    isApprovedCook = isApprovedCook,
    isSuspended   = isSuspended
)
