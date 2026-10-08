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

    fun applyPromoCode(code: String): Boolean {
        return if (code.trim().equals("HOMECOOK10", ignoreCase = true) ||
                   code.trim().equals("CAREEM20", ignoreCase = true)) {
            _appliedPromoCode.value    = code.uppercase()
            _promoDiscountAmount.value = 2.00
            true
        } else false
    }

    // ─── Checkout ─────────────────────────────────────────────────────────────

    fun checkout(deliveryAddress: String, paymentMethod: String, notesForCook: String): List<String> {
        val createdIds = repository.checkoutOrders(
            deliveryAddress = deliveryAddress,
            paymentMethod   = paymentMethod,
            notesForCook    = notesForCook,
            promoDiscount   = _promoDiscountAmount.value
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
