package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.HomeChefRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeChefViewModel(application: Application) : AndroidViewModel(application) {

    val repository = HomeChefRepository(application)

    init {
        // Clean any leftover dummy foods so only cook dishes are displayed
        repository.removeDummyFoods()
    }

    // User state — null = not logged in
    val currentUser = repository.currentUser
    val users       = repository.users

    // Foods & Favorites
    val foods          = repository.foods
    val favoriteFoodIds = repository.favoriteFoodIds

    // Cart
    val cartItems = repository.cartItems

    // Orders
    val orders = repository.orders

    // Notifications
    val notifications = repository.notifications

    // Delivery Issues
    val deliveryIssues = repository.deliveryIssues

    // Kitchen Promotions & Deals (Cook CRUD)
    val promos = repository.promos

    // Navigation
    private val _currentScreen = MutableStateFlow("splash")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All In")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFood = MutableStateFlow<Food?>(null)
    val selectedFood: StateFlow<Food?> = _selectedFood.asStateFlow()

    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()

    // Promo
    private val _appliedPromoCode = MutableStateFlow("")
    val appliedPromoCode: StateFlow<String> = _appliedPromoCode.asStateFlow()

    private val _promoDiscountAmount = MutableStateFlow(0.0)
    val promoDiscountAmount: StateFlow<Double> = _promoDiscountAmount.asStateFlow()

    // AI
    private val _aiRecommendationResult = MutableStateFlow<String?>(null)
    val aiRecommendationResult: StateFlow<String?> = _aiRecommendationResult.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Auth error / feedback
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        viewModelScope.launch {
            repository.orders.collect { orderList ->
                val curId = _selectedOrder.value?.orderId ?: return@collect
                val updated = orderList.find { it.orderId == curId }
                _selectedOrder.value = updated
            }
        }
    }

    // ─── Auth ─────────────────────────────────────────────────────────────────

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val error = repository.login(email, password)
            if (error != null) {
                _authError.value = error
            } else {
                _authError.value = null
                // Navigate based on role
                val role = repository.currentUser.value?.role ?: UserRole.CUSTOMER
                navigateAfterLogin(role)
            }
        }
    }

    fun register(name: String, email: String, phone: String, password: String, role: UserRole) {
        viewModelScope.launch {
            val error = repository.register(name, email, phone, password, role)
            if (error != null) {
                _authError.value = error
            } else {
                _authError.value = null
                val actualRole = repository.currentUser.value?.role ?: UserRole.CUSTOMER
                navigateAfterLogin(actualRole)
            }
        }
    }

    fun signInWithGoogle(idToken: String, selectedRole: UserRole = UserRole.CUSTOMER) {
        viewModelScope.launch {
            val error = repository.signInWithGoogle(idToken, selectedRole)
            if (error != null) {
                _authError.value = error
            } else {
                _authError.value = null
                val actualRole = repository.currentUser.value?.role ?: selectedRole
                navigateAfterLogin(actualRole)
            }
        }
    }

    fun logout() {
        repository.logout()
        _currentScreen.value = "auth"
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun setAuthError(message: String) {
        _authError.value = message
    }

    private fun navigateAfterLogin(role: UserRole) {
        when (role) {
            UserRole.CUSTOMER  -> _currentScreen.value = "customer_home"
            UserRole.COOK      -> _currentScreen.value = "cook_dashboard"
            UserRole.DELIVERY  -> _currentScreen.value = "delivery_dashboard"
            UserRole.ADMIN     -> _currentScreen.value = "admin_dashboard"
        }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    fun navigateTo(screen: String) { _currentScreen.value = screen }

    fun selectCategory(category: String) { _selectedCategory.value = category }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun selectFood(food: Food) {
        _selectedFood.value = food
        _currentScreen.value = "food_details"
    }

    fun selectOrderForTracking(order: Order) {
        _selectedOrder.value = order
        _currentScreen.value = "order_tracking"
    }

    // ─── Cart ─────────────────────────────────────────────────────────────────

    fun addToCart(food: Food, quantity: Int = 1, specialRequest: String = "") {
        repository.addToCart(food, quantity, specialRequest)
    }

    fun updateCartQuantity(foodId: String, qty: Int) {
        repository.updateCartQuantity(foodId, qty)
    }

    fun removeCartItem(foodId: String) {
        repository.removeCartItem(foodId)
    }

    fun applyPromoCode(
        code: String,
        onResult: (isSuccess: Boolean, message: String) -> Unit = { _, _ -> }
    ): Boolean {
        val clean = code.trim().uppercase().replace(" ", "")
        if (clean.isBlank()) {
            onResult(false, "Please enter a promo code.")
            return false
        }
        if (clean == "HOMECOOK10") {
            _appliedPromoCode.value    = clean
            _promoDiscountAmount.value = 2.00
            onResult(true, "Promo HOMECOOK10 Applied! -$2.00")
            return true
        }
        if (clean == "CAREEM20") {
            _appliedPromoCode.value    = clean
            _promoDiscountAmount.value = 3.00
            onResult(true, "Promo CAREEM20 Applied! -$3.00")
            return true
        }

        // 1. Try finding in in-memory state
        val inMemPromo = repository.promos.value.find { it.code.trim().uppercase().replace(" ", "") == clean }
        if (inMemPromo != null) {
            return validateAndApplyPromo(inMemPromo, onResult)
        }

        // 2. Query Firestore directly if not yet synced in memory
        viewModelScope.launch {
            val fsPromo = repository.findPromoInFirestore(clean)
            if (fsPromo != null) {
                validateAndApplyPromo(fsPromo, onResult)
            } else {
                onResult(false, "Promo code '$clean' not found.")
            }
        }
        return false
    }

    private fun validateAndApplyPromo(
        promo: PromoCode,
        onResult: (isSuccess: Boolean, message: String) -> Unit
    ): Boolean {
        val clean = promo.code.trim().uppercase()
        if (!promo.isActive) {
            onResult(false, "Promo code '$clean' is currently paused by kitchen.")
            return false
        }
        if (promo.isExpired) {
            onResult(false, "Promo code '$clean' has expired.")
            return false
        }
        if (promo.maxUsageLimit in 1..promo.usageCount) {
            onResult(false, "Promo code '$clean' has reached its usage limit.")
            return false
        }

        val subtotal = repository.cartItems.value.sumOf { it.price * it.quantity }
        if (promo.minOrderValue > 0.0 && subtotal < promo.minOrderValue) {
            onResult(false, "Minimum order of $${String.format("%.2f", promo.minOrderValue)} required (Cart: $${String.format("%.2f", subtotal)}).")
            return false
        }

        val discount = if (promo.discountType == "PERCENTAGE") {
            val base = if (subtotal > 0) subtotal else 20.0
            (base * (promo.discountValue / 100.0)).coerceAtLeast(1.0)
        } else {
            promo.discountValue
        }

        _appliedPromoCode.value = clean
        _promoDiscountAmount.value = discount
        onResult(true, "Promo '$clean' Applied! -$${String.format("%.2f", discount)}")
        return true
    }

    // ─── Checkout ─────────────────────────────────────────────────────────────

    fun checkout(deliveryAddress: String, paymentMethod: String, notesForCook: String): List<String> {
        val appliedCode = _appliedPromoCode.value
        val discount = _promoDiscountAmount.value
        val createdIds = repository.checkoutOrders(
            deliveryAddress  = deliveryAddress,
            paymentMethod    = paymentMethod,
            notesForCook     = notesForCook,
            promoDiscount    = discount,
            appliedPromoCode = appliedCode
        )
        if (createdIds.isNotEmpty()) {
            _appliedPromoCode.value    = ""
            _promoDiscountAmount.value = 0.0
            val firstOrder = repository.orders.value.find { it.orderId == createdIds.first() }
            if (firstOrder != null) _selectedOrder.value = firstOrder
            navigateTo("order_tracking")
        }
        return createdIds
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus, reason: String? = null) {
        repository.updateOrderStatus(orderId, newStatus, reason)
        if (_selectedOrder.value?.orderId == orderId) {
            _selectedOrder.value = repository.orders.value.find { it.orderId == orderId }
        }
    }

    fun openDeliveryMap(order: Order) {
        _selectedOrder.value = order
        _currentScreen.value = "delivery_map"
    }

    fun claimDelivery(orderId: String) {
        val currentPartner = currentUser.value ?: return
        repository.assignDeliveryPartner(orderId, currentPartner.name, currentPartner.userId)
        val updatedOrder = repository.orders.value.find { it.orderId == orderId }
        _selectedOrder.value = updatedOrder
        if (updatedOrder != null) _currentScreen.value = "delivery_map"
    }

    fun cancelDelivery(orderId: String, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val error = repository.cancelDeliveryAssignment(orderId)
            if (error == null) {
                if (_selectedOrder.value?.orderId == orderId) {
                    _selectedOrder.value = repository.orders.value.find { it.orderId == orderId }
                }
            }
            onResult(error)
        }
    }

    fun createDeliveryIssue(
        orderId: String?,
        category: String,
        description: String,
        priority: String = "MEDIUM",
        onResult: (String?) -> Unit = {}
    ) {
        viewModelScope.launch {
            val error = repository.createDeliveryIssue(orderId, category, description, priority)
            onResult(error)
        }
    }

    fun updateDeliveryIssue(
        issueId: String,
        category: String,
        description: String,
        priority: String = "MEDIUM",
        onResult: (String?) -> Unit = {}
    ) {
        viewModelScope.launch {
            val error = repository.updateDeliveryIssue(issueId, category, description, priority)
            onResult(error)
        }
    }

    fun deleteDeliveryIssue(
        issueId: String,
        onResult: (String?) -> Unit = {}
    ) {
        viewModelScope.launch {
            val error = repository.deleteDeliveryIssue(issueId)
            onResult(error)
        }
    }

    // ─── Cook Promotions & Deals CRUD ──────────────────────────────────────────

    fun createPromo(
        code: String,
        title: String,
        description: String,
        discountType: String = "PERCENTAGE",
        discountValue: Double = 10.0,
        minOrderValue: Double = 0.0,
        durationDays: Int = 7,
        maxUsageLimit: Int = 50,
        onResult: (String?) -> Unit = {}
    ) {
        viewModelScope.launch {
            val error = repository.createPromo(
                code = code,
                title = title,
                description = description,
                discountType = discountType,
                discountValue = discountValue,
                minOrderValue = minOrderValue,
                durationDays = durationDays,
                maxUsageLimit = maxUsageLimit
            )
            onResult(error)
        }
    }

    fun updatePromo(
        promoId: String,
        title: String,
        description: String,
        discountType: String,
        discountValue: Double,
        minOrderValue: Double,
        extendDays: Int = 0,
        isActive: Boolean = true,
        onResult: (String?) -> Unit = {}
    ) {
        viewModelScope.launch {
            val error = repository.updatePromo(
                promoId = promoId,
                title = title,
                description = description,
                discountType = discountType,
                discountValue = discountValue,
                minOrderValue = minOrderValue,
                extendDays = extendDays,
                isActive = isActive
            )
            onResult(error)
        }
    }

    fun togglePromoStatus(promoId: String, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val error = repository.togglePromoStatus(promoId)
            onResult(error)
        }
    }

    fun deletePromo(promoId: String, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val error = repository.deletePromo(promoId)
            onResult(error)
        }
    }

    fun customerUpdateOrder(
        orderId: String,
        deliveryAddress: String,
        customerPhone: String,
        notesForCook: String,
        updatedItems: List<CartItem>,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val error = repository.customerUpdateOrder(
                orderId = orderId,
                deliveryAddress = deliveryAddress,
                customerPhone = customerPhone,
                notesForCook = notesForCook,
                updatedItems = updatedItems
            )
            if (error == null) {
                val updated = repository.orders.value.find { it.orderId == orderId }
                if (updated != null) _selectedOrder.value = updated
            }
            onResult(error)
        }
    }

    fun customerDeleteOrder(
        orderId: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val error = repository.customerDeleteOrder(orderId)
            if (error == null) {
                if (_selectedOrder.value?.orderId == orderId) {
                    _selectedOrder.value = null
                }
            }
            onResult(error)
        }
    }

    // ─── Food Management ──────────────────────────────────────────────────────

    fun toggleFavorite(foodId: String)                { repository.toggleFavorite(foodId) }
    fun addFoodByCook(food: Food)                     { repository.addFood(food) }
    fun updateFood(food: Food)                        { repository.updateFood(food) }
    fun toggleFoodAvailability(foodId: String)        { repository.toggleFoodAvailability(foodId) }
    fun deleteFood(foodId: String)                    { repository.deleteFood(foodId) }
    fun refreshFoods()                                { repository.startGlobalListeners(); repository.fetchFoodsOnce() }

    // ─── Admin ────────────────────────────────────────────────────────────────

    fun approveCook(userId: String)     { repository.approveCook(userId) }
    fun toggleSuspendUser(userId: String) { repository.toggleSuspendUser(userId) }

    fun adminCreateUser(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: UserRole,
        isApprovedCook: Boolean = true,
        onResult: (errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val error = repository.adminCreateUser(name, email, phone, password, role, isApprovedCook)
            onResult(error)
        }
    }

    fun sendPasswordReset(email: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val err = repository.sendPasswordReset(email)
            onResult(err)
        }
    }

    fun adminDeleteUser(userId: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val err = repository.adminDeleteUser(userId)
            onResult(err)
        }
    }

    fun adminUpdateUser(
        userId: String,
        name: String,
        email: String,
        phone: String,
        role: UserRole,
        isApprovedCook: Boolean,
        isSuspended: Boolean,
        onResult: (errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val err = repository.adminUpdateUser(userId, name, email, phone, role, isApprovedCook, isSuspended)
            onResult(err)
        }
    }

    // ─── AI Recommendation ────────────────────────────────────────────────────

    fun generateAiRecommendation(userPreference: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiRecommendationResult.value = null
            kotlinx.coroutines.delay(1000)
            val matchingFood = foods.value.filter { food ->
                food.title.contains(userPreference, ignoreCase = true) ||
                food.category.contains(userPreference, ignoreCase = true) ||
                food.description.contains(userPreference, ignoreCase = true)
            }.firstOrNull() ?: foods.value.randomOrNull()

            _aiRecommendationResult.value = if (matchingFood != null) {
                "Chef AI Recommendation based on '${userPreference}': Try our high-protein '${matchingFood.title}' by ${matchingFood.cookName}! Freshly prepared in ${matchingFood.preparationTimeMins} mins for only $${String.format("%.2f", matchingFood.price)}."
            } else {
                "Based on your craving, we recommend Chef Maria Rosa's French Fries or Royal Chicken Biryani!"
            }
            _isAiLoading.value = false
        }
    }
}
