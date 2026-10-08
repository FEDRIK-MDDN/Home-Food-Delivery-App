package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Food
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.User
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenPrimary
import com.example.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookDashboardScreen(
    currentUser: User,
    orders: List<Order>,
    foods: List<Food>,
    onUpdateOrderStatus: (orderId: String, newStatus: OrderStatus) -> Unit,
    onAddFood: (Food) -> Unit,
    onUpdateFood: (Food) -> Unit = {},
    onToggleFoodAvailability: (foodId: String) -> Unit,
    onDeleteFood: (foodId: String) -> Unit
) {
    var selectedTab by remember { mutableStateOf("Kitchen Orders") }
    var showAddDishDialog by remember { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<Food?>(null) }
    var foodToDelete by remember { mutableStateOf<Food?>(null) }

    // Filter orders for this cook — use cookId only (exact match) to prevent
    // cross-assignment when cook names partially overlap (Bug 9 fix).
    val cookOrders = remember(orders, currentUser) {
        orders.filter { it.cookId == currentUser.userId }
    }

    // Filter foods by this cook — cookId only (Bug 9 fix)
    val cookFoods = remember(foods, currentUser) {
        foods.filter { it.cookId == currentUser.userId }
    }

    val todayRevenue = cookOrders.filter { it.status != OrderStatus.CANCELLED }.sumOf { it.subtotal }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kitchen Dashboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = currentUser.name,
                            fontSize = 12.sp,
                            color = GreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                actions = {
                    // Bug 4 fix: Only approved cooks can add new dishes
                    if (currentUser.isApprovedCook) {
                        IconButton(
                            onClick = { showAddDishDialog = true },
                            modifier = Modifier.testTag("add_dish_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Add Dish",
                                tint = GreenPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAF8)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$${String.format("%.2f", todayRevenue)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GreenPrimary
                        )
                        Text(
                            text = "Earnings",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${cookOrders.size}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Orders",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${cookFoods.size}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Dishes",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // Tabs Selector
            TabRow(
                selectedTabIndex = if (selectedTab == "Kitchen Orders") 0 else 1,
                containerColor = Color.White,
                contentColor = GreenPrimary
            ) {
                Tab(
                    selected = selectedTab == "Kitchen Orders",
                    onClick = { selectedTab = "Kitchen Orders" },
                    text = { Text("Kitchen Orders (${cookOrders.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == "My Menu",
                    onClick = { selectedTab = "My Menu" },
                    text = { Text("My Menu (${cookFoods.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == "Kitchen Orders") {
                // Bug 4 fix: Unapproved cooks see a pending-approval notice instead
                // of being able to operate the kitchen.
                if (!currentUser.isApprovedCook) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.HourglassEmpty,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Pending Admin Approval",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFFD97706)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Your kitchen is under review. You can\u2019t accept orders until an admin approves your account.",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else if (cookOrders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No incoming kitchen orders yet", fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(cookOrders, key = { it.orderId }) { order ->
                            CookOrderCard(
                                order = order,
                                onUpdateStatus = { newStatus -> onUpdateOrderStatus(order.orderId, newStatus) }
                            )
                        }
                    }
                }
            } else if (cookFoods.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No dishes in your menu yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add your signature homemade dishes so customers can order!",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (currentUser.isApprovedCook) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showAddDishDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Your First Dish", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(cookFoods, key = { it.foodId }) { food ->
                        CookFoodItemCard(
                            food = food,
                            onToggleAvailability = { onToggleFoodAvailability(food.foodId) },
                            onEdit = { editingFood = food },
                            onDelete = { foodToDelete = food }
                        )
                    }
                }
            }
        }
    }

    if (showAddDishDialog) {
        AddDishDialog(
            currentUser = currentUser,
            isApprovedCook = currentUser.isApprovedCook,
            onDismiss = { showAddDishDialog = false },
            onAddFood = { food ->
                onAddFood(food)
                selectedTab = "My Menu"
                showAddDishDialog = false
            }
        )
    }

    if (editingFood != null) {
        EditDishDialog(
            food = editingFood!!,
            onDismiss = { editingFood = null },
            onUpdateFood = { updated ->
                onUpdateFood(updated)
                editingFood = null
            }
        )
    }

    if (foodToDelete != null) {
        AlertDialog(
            onDismissRequest = { foodToDelete = null },
            title = { Text("Delete Dish", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove '${foodToDelete?.title}' from your kitchen menu?") },
            confirmButton = {
                Button(
                    onClick = {
                        foodToDelete?.let { onDeleteFood(it.foodId) }
                        foodToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { foodToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CookOrderCard(
    order: Order,
    onUpdateStatus: (OrderStatus) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.orderId}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GreenContainer
                ) {
                    Text(
                        text = order.status.label,
                        color = GreenPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Customer: ${order.customerName} (${order.customerPhone})",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )

            HorizontalDivider(color = Color(0xFFF1F5F9))

            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.quantity}x ${item.title}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "$${String.format("%.2f", item.price * item.quantity)}",
                        fontSize = 14.sp,
                        color = GreenPrimary
                    )
                }
            }

            if (order.notesForCook.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Note: ${order.notesForCook}",
                        fontSize = 12.sp,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Order Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (order.status) {
                    OrderStatus.PENDING -> {
                        Button(
                            onClick = { onUpdateStatus(OrderStatus.ACCEPTED) },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Accept Order")
                        }
                    }
                    OrderStatus.ACCEPTED -> {
                        Button(
                            onClick = { onUpdateStatus(OrderStatus.PREPARING) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Start Preparing")
                        }
                    }
                    OrderStatus.PREPARING -> {
                        Button(
                            onClick = { onUpdateStatus(OrderStatus.READY) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Mark Ready for Pickup")
                        }
                    }
                    else -> {
                        Text(
                            text = "Status: ${order.status.label}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CookFoodItemCard(
    food: Food,
    onToggleAvailability: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Food Image with Floating Badges (Rating & Status)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF1F5F9))
            ) {
                val displayImageUrl = food.imageUrl.ifBlank {
                    if (food.category.contains("biryani", ignoreCase = true) || food.title.contains("biryani", ignoreCase = true)) {
                        "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=600"
                    } else {
                        "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600"
                    }
                }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(ImageUtils.resolveImageModel(displayImageUrl))
                        .crossfade(true)
                        .build(),
                    contentDescription = food.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Rating Pill Top Left (Matching Image 2)
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", food.rating),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF1E1E1E)
                        )
                    }
                }

                // Active / Inactive Badge Top Right
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd),
                    shape = RoundedCornerShape(12.dp),
                    color = if (food.isAvailable) Color(0xFFE8F5E9).copy(alpha = 0.95f) else Color(0xFFF1F5F9).copy(alpha = 0.95f),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (food.isAvailable) GreenPrimary else Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (food.isAvailable) "Active" else "Inactive",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (food.isAvailable) GreenPrimary else Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cook Name & Verified Badge (Matching Image 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = food.cookName,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (food.isCookVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified Cook",
                        tint = GreenPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• ${food.category}",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = " (${food.preparationTimeMins} min)",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Dish Title
            Text(
                text = food.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (food.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = food.description,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Row 1: Price (Left) & Active Status Toggle (Right) - generous space
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price
                Column {
                    Text(
                        text = "Price",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$${String.format(java.util.Locale.US, "%.2f", food.price)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = GreenPrimary
                    )
                }

                // Active Toggle Switch with Label
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (food.isAvailable) Color(0xFFE8F5E9) else Color(0xFFF1F5F9),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onToggleAvailability() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 3.dp, bottom = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (food.isAvailable) GreenPrimary else Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (food.isAvailable) "Active" else "Inactive",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (food.isAvailable) GreenPrimary else Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = food.isAvailable,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GreenPrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Action Buttons (Update & Delete) with comfortable spacing and size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Update / Edit Button
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFF8FAFC),
                        contentColor = Color(0xFF1E293B)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Update",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                // Delete Button
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFFEF2F2),
                        contentColor = Color(0xFFEF4444)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Delete",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDishDialog(
    food: Food,
    onDismiss: () -> Unit,
    onUpdateFood: (Food) -> Unit
) {
    var title by remember { mutableStateOf(food.title) }
    var description by remember { mutableStateOf(food.description) }
    var price by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", food.price)) }
    var category by remember { mutableStateOf(food.category) }
    var prepTime by remember { mutableStateOf(food.preparationTimeMins.toString()) }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var customImageUrl by remember { mutableStateOf(if (food.imageUrl.startsWith("http")) food.imageUrl else "") }
    var isAvailable by remember { mutableStateOf(food.isAvailable) }
    var isImageConverting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                isImageConverting = true
                val base64 = withContext(Dispatchers.IO) {
                    ImageUtils.uriToBase64(context, it)
                }
                selectedImageUri = base64 ?: it.toString()
                isImageConverting = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Dish Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dish Image Picker Box
                Text(
                    text = "Dish Photo",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )

                val activeImage = selectedImageUri
                    ?: customImageUrl.ifBlank { null }
                    ?: food.imageUrl.ifBlank { null }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clickable { imagePickerLauncher.launch("image/*") }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isImageConverting) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = GreenPrimary,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Saving photo...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        } else if (activeImage != null) {
                            AsyncImage(
                                model = ImageUtils.resolveImageModel(activeImage),
                                contentDescription = "Dish Photo Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .clickable {
                                        selectedImageUri = null
                                        customImageUrl = ""
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Image",
                                    tint = Color.White,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Select Photo from Gallery",
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Select Photo from Mobile Gallery",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GreenPrimary
                                )
                                Text(
                                    text = "Tap to open your phone gallery",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customImageUrl,
                    onValueChange = { customImageUrl = it },
                    label = { Text("Or Paste Image Web URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Dish Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price ($)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = prepTime,
                    onValueChange = { prepTime = it },
                    label = { Text("Prep Time (Mins)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Active status switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Status: ${if (isAvailable) "Active (Accepting Orders)" else "Inactive (Hidden)"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isAvailable) GreenPrimary else Color(0xFF64748B)
                        )
                        Text(
                            text = "Show this dish on the customer menu",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GreenPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val finalImage = selectedImageUri
                            ?: customImageUrl.ifBlank { null }
                            ?: food.imageUrl.ifBlank { "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=600" }

                        val updated = food.copy(
                            title = title.trim(),
                            description = description.trim(),
                            price = price.toDoubleOrNull() ?: food.price,
                            category = category.trim().ifBlank { food.category },
                            preparationTimeMins = prepTime.toIntOrNull() ?: food.preparationTimeMins,
                            imageUrl = finalImage,
                            isAvailable = isAvailable
                        )
                        onUpdateFood(updated)
                    }
                },
                enabled = !isImageConverting,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text(if (isImageConverting) "Processing..." else "Update Dish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDishDialog(
    currentUser: User,
    isApprovedCook: Boolean = true,
    onDismiss: () -> Unit,
    onAddFood: (Food) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("8.50") }
    var category by remember { mutableStateOf("Biryani") }
    var prepTime by remember { mutableStateOf("25") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var customImageUrl by remember { mutableStateOf("") }
    var isImageConverting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                isImageConverting = true
                val base64 = withContext(Dispatchers.IO) {
                    ImageUtils.uriToBase64(context, it)
                }
                selectedImageUri = base64 ?: it.toString()
                isImageConverting = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Homemade Dish", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dish Image Picker Box
                Text(
                    text = "Dish Photo",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )

                val activeImage = selectedImageUri ?: customImageUrl.ifBlank { null }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clickable { imagePickerLauncher.launch("image/*") }
                        .testTag("select_dish_image_button")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isImageConverting) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = GreenPrimary,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Saving photo...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        } else if (activeImage != null) {
                            AsyncImage(
                                model = ImageUtils.resolveImageModel(activeImage),
                                contentDescription = "Dish Photo Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .clickable {
                                        selectedImageUri = null
                                        customImageUrl = ""
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Image",
                                    tint = Color.White,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Select Photo from Gallery",
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Select Photo from Mobile Gallery",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GreenPrimary
                                )
                                Text(
                                    text = "Tap to open your phone gallery",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customImageUrl,
                    onValueChange = { customImageUrl = it },
                    label = { Text("Or Paste Image Web URL (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary
                    )
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Dish Title *") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Dish title is required", color = Color.Red) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price ($)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = prepTime,
                    onValueChange = { prepTime = it },
                    label = { Text("Prep Time (Mins)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        val finalImage = selectedImageUri
                            ?: customImageUrl.ifBlank { null }
                            ?: if (category.contains("biryani", ignoreCase = true) || title.contains("biryani", ignoreCase = true)) {
                                "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=600"
                            } else {
                                "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600"
                            }

                        val newFood = Food(
                            foodId = "f_${System.currentTimeMillis()}",
                            cookId = currentUser.userId,
                            cookName = currentUser.name,
                            // Bug 2 fix: isCookVerified reflects actual approval status,
                            // not hardcoded true. Unapproved cooks get isCookVerified=false.
                            isCookVerified = isApprovedCook,
                            title = title.trim(),
                            description = description.trim(),
                            price = price.toDoubleOrNull() ?: 8.50,
                            category = category.trim().ifBlank { "Homemade" },
                            imageUrl = finalImage,
                            preparationTimeMins = prepTime.toIntOrNull() ?: 20,
                            isFeatured = true,
                            isAvailable = true
                        )
                        onAddFood(newFood)
                    }
                },
                enabled = !isImageConverting,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text(if (isImageConverting) "Processing..." else "Add Dish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
