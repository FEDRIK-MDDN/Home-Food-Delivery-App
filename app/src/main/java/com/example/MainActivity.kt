package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.data.model.UserRole
import com.example.ui.components.HomeChefBottomNavBar
import com.example.ui.screens.*
import com.example.ui.theme.HomeChefConnectTheme
import com.example.ui.viewmodel.HomeChefViewModel
import com.example.util.GoogleAuthHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: HomeChefViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HomeChefConnectTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: HomeChefViewModel) {
    val currentScreen   by viewModel.currentScreen.collectAsState()
    val foods           by viewModel.foods.collectAsState()
    val favoriteFoodIds by viewModel.favoriteFoodIds.collectAsState()
    val searchQuery     by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedFood    by viewModel.selectedFood.collectAsState()
    val cartItems       by viewModel.cartItems.collectAsState()

    // currentUser is nullable — null means logged out
    val currentUser by viewModel.currentUser.collectAsState()
    val orders      by viewModel.orders.collectAsState()
    val totalCartCount = cartItems.sumOf { it.quantity }

    val selectedOrder by viewModel.selectedOrder.collectAsState()
    val allUsers      by viewModel.users.collectAsState()
    val authError     by viewModel.authError.collectAsState()

    val favoriteFoods = remember(foods, favoriteFoodIds) {
        foods.filter { favoriteFoodIds.contains(it.foodId) && it.isAvailable }
    }

    // ── Reactive navigation: fires whenever currentUser changes ──────────────
    LaunchedEffect(currentUser) {
        when {
            // Just logged in / registered → go to role dashboard
            currentUser != null && currentScreen == "auth" -> {
                when (currentUser!!.role) {
                    UserRole.CUSTOMER -> viewModel.navigateTo("customer_home")
                    UserRole.COOK     -> viewModel.navigateTo("cook_dashboard")
                    UserRole.DELIVERY -> viewModel.navigateTo("delivery_dashboard")
                    UserRole.ADMIN    -> viewModel.navigateTo("admin_dashboard")
                }
            }
            // Logged out → go to auth
            currentUser == null && currentScreen !in setOf("auth", "splash") -> {
                viewModel.navigateTo("auth")
            }
        }
    }

    val screensWithNav = setOf(
        "customer_home", "favorites", "cart", "orders", "profile",
        "chef_ai", "cook_dashboard", "delivery_dashboard", "admin_dashboard"
    )

    Scaffold(
        bottomBar = {
            if (currentScreen in screensWithNav && currentUser != null) {
                HomeChefBottomNavBar(
                    currentScreen  = currentScreen,
                    userRole       = currentUser!!.role,
                    cartBadgeCount = totalCartCount,
                    favoriteBadgeCount = favoriteFoodIds.size,
                    onNavigate     = { viewModel.navigateTo(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                "splash" -> SplashScreen(
                    onStartClick = { viewModel.navigateTo("auth") }
                )

                "auth" -> {
                    val context = LocalContext.current
                    val activity = context as? androidx.activity.ComponentActivity
                    val coroutineScope = rememberCoroutineScope()
                    val webClientId = stringResource(R.string.default_web_client_id)
                    var currentRole by remember { mutableStateOf(UserRole.CUSTOMER) }

                    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
                    ) { actResult ->
                        val res = GoogleAuthHelper.extractIdTokenFromIntent(actResult.data)
                        res.onSuccess { idToken ->
                            viewModel.signInWithGoogle(idToken, currentRole)
                        }.onFailure { ex ->
                            if (ex !is kotlinx.coroutines.CancellationException) {
                                viewModel.setAuthError(ex.localizedMessage ?: "Google Sign-In failed.")
                            }
                        }
                    }

                    AuthScreen(
                        onLogin    = { email, password -> viewModel.login(email, password) },
                        onRegister = { name, email, phone, password, role ->
                            viewModel.register(name, email, phone, password, role)
                        },
                        onGoogleSignIn = { role ->
                            currentRole = role
                            if (activity != null) {
                                coroutineScope.launch {
                                    val result = GoogleAuthHelper.launchGoogleSignIn(activity, webClientId)
                                    result.onSuccess { idToken ->
                                        viewModel.signInWithGoogle(idToken, role)
                                    }.onFailure { ex ->
                                        if (ex is androidx.credentials.exceptions.NoCredentialException) {
                                            // Fallback to standard GoogleSignIn activity intent
                                            googleSignInLauncher.launch(
                                                GoogleAuthHelper.getGoogleSignInIntent(context, webClientId)
                                            )
                                        } else if (ex !is kotlinx.coroutines.CancellationException) {
                                            viewModel.setAuthError(ex.localizedMessage ?: "Google Sign-In failed.")
                                        }
                                    }
                                }
                            }
                        },
                        authError  = authError,
                        onClearError = { viewModel.clearAuthError() }
                    )
                }

                "customer_home" -> currentUser?.let { user ->
                    CustomerHomeScreen(
                        currentUserName  = user.name,
                        currentUserAddress = user.address,
                        foods            = foods,
                        favoriteFoodIds  = favoriteFoodIds,
                        searchQuery      = searchQuery,
                        selectedCategory = selectedCategory,
                        onSearchChange   = { viewModel.setSearchQuery(it) },
                        onCategorySelect = { viewModel.selectCategory(it) },
                        onFoodClick      = { viewModel.selectFood(it) },
                        onFavoriteClick  = { viewModel.toggleFavorite(it) },
                        onAddToCart      = { viewModel.addToCart(it) },
                        onOpenAiRecommendation = { },
                        onOpenNotifications    = { }
                    )
                }

                "favorites" -> FavoritesScreen(
                    favoriteFoods = favoriteFoods,
                    favoriteIds   = favoriteFoodIds,
                    onFoodClick   = { viewModel.selectFood(it) },
                    onFavoriteClick = { viewModel.toggleFavorite(it) },
                    onAddToCart   = { viewModel.addToCart(it) }
                )

                "chef_ai" -> ChefAiScreen(
                    foods       = foods,
                    onBackClick = { viewModel.navigateTo("customer_home") },
                    onFoodClick = { viewModel.selectFood(it) },
                    onAddToCart = { viewModel.addToCart(it) }
                )

                "orders" -> currentUser?.let { user ->
                    OrdersScreen(
                        orders      = orders,
                        currentUser = user,
                        onOrderClick = { viewModel.selectOrderForTracking(it) }
                    )
                }

                "order_tracking" -> {
                    selectedOrder?.let { order ->
                        OrderTrackingScreen(
                            order       = order,
                            onBackClick = { viewModel.navigateTo("orders") }
                        )
                    } ?: viewModel.navigateTo("orders")
                }

                "cook_dashboard" -> currentUser?.let { user ->
                    CookDashboardScreen(
                        currentUser          = user,
                        orders               = orders,
                        foods                = foods,
                        onUpdateOrderStatus  = { orderId, status -> viewModel.updateOrderStatus(orderId, status) },
                        onAddFood            = { viewModel.addFoodByCook(it) },
                        onUpdateFood         = { viewModel.updateFood(it) },
                        onToggleFoodAvailability = { viewModel.toggleFoodAvailability(it) },
                        onDeleteFood         = { viewModel.deleteFood(it) }
                    )
                }

                "delivery_dashboard" -> currentUser?.let { user ->
                    DeliveryDashboardScreen(
                        currentUser        = user,
                        orders             = orders,
                        onClaimDelivery    = { viewModel.claimDelivery(it) },
                        onUpdateOrderStatus = { orderId, status -> viewModel.updateOrderStatus(orderId, status) },
                        onOpenMap          = { viewModel.openDeliveryMap(it) }
                    )
                }

                "delivery_map" -> {
                    selectedOrder?.let { order ->
                        DeliveryMapScreen(
                            order           = order,
                            onBackClick     = { viewModel.navigateTo("delivery_dashboard") },
                            onUpdateOrderStatus = { orderId, status -> viewModel.updateOrderStatus(orderId, status) }
                        )
                    } ?: viewModel.navigateTo("delivery_dashboard")
                }

                "admin_dashboard" -> currentUser?.let { user ->
                    AdminDashboardScreen(
                        currentUser       = user,
                        users             = allUsers,
                        orders            = orders,
                        onApproveCook     = { viewModel.approveCook(it) },
                        onToggleSuspendUser = { viewModel.toggleSuspendUser(it) }
                    )
                }

                "food_details" -> {
                    selectedFood?.let { food ->
                        FoodDetailsScreen(
                            food        = food,
                            onBackClick = { viewModel.navigateTo("customer_home") },
                            onAddToCart = { selectedFoodItem, qty, note ->
                                viewModel.addToCart(selectedFoodItem, qty, note)
                                viewModel.navigateTo("cart")
                            }
                        )
                    } ?: viewModel.navigateTo("customer_home")
                }

                "cart" -> CartScreen(
                    cartItems       = cartItems,
                    appliedPromoCode = viewModel.appliedPromoCode.collectAsState().value,
                    promoDiscount   = viewModel.promoDiscountAmount.collectAsState().value,
                    onBackClick     = { viewModel.navigateTo("customer_home") },
                    onUpdateQuantity = { foodId, qty -> viewModel.updateCartQuantity(foodId, qty) },
                    onRemoveItem    = { foodId -> viewModel.removeCartItem(foodId) },
                    onApplyPromo    = { code -> viewModel.applyPromoCode(code) },
                    onProceedToCheckout = { viewModel.navigateTo("checkout") }
                )

                "checkout" -> currentUser?.let { user ->
                    CheckoutScreen(
                        cartItems      = cartItems,
                        defaultAddress = user.address,
                        promoDiscount  = viewModel.promoDiscountAmount.collectAsState().value,
                        onBackClick    = { viewModel.navigateTo("cart") },
                        onConfirmOrder = { address, payment, notes ->
                            viewModel.checkout(address, payment, notes)
                        }
                    )
                }

                "profile" -> currentUser?.let { user ->
                    ProfileScreen(
                        user             = user,
                        favoriteCount    = favoriteFoodIds.size,
                        orderCount       = when (user.role) {
                            UserRole.CUSTOMER -> orders.count { it.customerId == user.userId }
                            UserRole.COOK     -> orders.count { it.cookId == user.userId }
                            UserRole.DELIVERY -> orders.count { it.deliveryId == user.userId }
                            UserRole.ADMIN    -> orders.size
                        },
                        onUpdateProfile  = { name, email, phone, address ->
                            viewModel.repository.updateUserProfile(name, email, phone, address)
                        },
                        onUpdatePhoto    = { photoUri ->
                            viewModel.repository.updateUserPhoto(photoUri)
                        },
                        onNavigateToFavorites = { viewModel.navigateTo("favorites") },
                        onNavigateToOrders    = { viewModel.navigateTo("orders") },
                        onLogout         = { viewModel.logout() }
                    )
                }

                else -> if (currentUser != null) {
                    viewModel.navigateTo("customer_home")
                } else {
                    viewModel.navigateTo("auth")
                }
            }
        }
    }
}
