package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Food
import com.example.ui.components.ChefPromoBanner
import com.example.ui.components.FoodCard
import com.example.ui.theme.DarkPill
import com.example.ui.theme.GreenPrimary
import com.example.util.ImageUtils

data class CategoryItem(val name: String, val icon: String, val isAllIn: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    currentUserName: String = "Friend",
    currentUserAddress: String = "",
    foods: List<Food>,
    favoriteFoodIds: List<String>,
    searchQuery: String,
    selectedCategory: String,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onFoodClick: (Food) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onAddToCart: (Food) -> Unit,
    onOpenAiRecommendation: () -> Unit,
    onOpenNotifications: () -> Unit,
    onUpdateAddress: (String) -> Unit = {}
) {
    val categories = listOf(
        CategoryItem("Offers", "local_offer"),
        CategoryItem("Asian", "ramen_dining"),
        CategoryItem("Biryani", "rice_bowl"),
        CategoryItem("Indian", "curry"),
        CategoryItem("Gotyou", "card_giftcard"),
        CategoryItem("Box", "takeout_dining"),
        CategoryItem("Burger", "lunch_dining"),
        CategoryItem("All In", "apps", isAllIn = true)
    )

    var activeFilterPill by remember { mutableStateOf("GOTYOU") }
    var showLocationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Top Header Bar (Editorial Location & Greeting Header)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Location Header
                    Column(
                        modifier = Modifier
                            .clickable { showLocationDialog = true }
                    ) {
                        Text(
                            text = "CURRENT LOCATION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary,
                            letterSpacing = 1.5.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentUserAddress.ifBlank { "Add delivery address" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Change location",
                                tint = GreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Notification Bell Button
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFECFDF5))
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsNone,
                            contentDescription = "Notifications",
                            tint = GreenPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dynamic greeting using real user name and time of day
                val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val timeGreeting = when {
                    currentHour < 12 -> "Morning"
                    currentHour < 17 -> "Afternoon"
                    else -> "Evening"
                }
                // Show first name, with fallback to Foodie
                val firstName = currentUserName.trim().split(" ").firstOrNull()?.ifBlank { "Foodie" } ?: "Foodie"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$timeGreeting, ",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = firstName,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GreenPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search homemade pasta...", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color(0xFF94A3B8))
                    },
                    trailingIcon = {
                        IconButton(onClick = { /* Voice Search */ }) {
                            Icon(Icons.Filled.Mic, contentDescription = "Voice Search", tint = GreenPrimary)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF1E1E1E), fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1E1E1E),
                        unfocusedTextColor = Color(0xFF1E1E1E),
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF8FAF8),
                        focusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter pills row (Matching uploaded UI image 1 middle screen: GOTYOU, 4.5+ Rated, 30 mins)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkPill),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Filter",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    listOf("GOTYOU", "4.5+ Rated", "30 mins").forEach { filter ->
                        val isSelected = activeFilterPill == filter
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { activeFilterPill = filter },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) Color.White else Color(0xFFF2F4F7),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary) else null
                        ) {
                            Text(
                                text = filter,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) GreenPrimary else Color(0xFF1E1E1E),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Main Scrollable Home Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Chef Promo Banner
            ChefPromoBanner(onBannerClick = { onCategorySelect("Offers") })

            Spacer(modifier = Modifier.height(20.dp))

            // AI Meal Recommendation Callout Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAiRecommendation() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F6EF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "AI Recommendation",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ask Chef AI Advisor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF003820)
                            )
                            Text(
                                text = "Get personalized meal suggestions in seconds!",
                                fontSize = 11.sp,
                                color = Color(0xFF005430)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open AI",
                        tint = GreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Food Categories 2x4 Grid (Matching uploaded UI image 1)
            Text(
                text = "Food Categories",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1A1D1E)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2 rows of 4 category tiles
            val firstRow = categories.take(4)
            val secondRow = categories.drop(4)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                firstRow.forEach { item ->
                    CategoryTile(
                        item = item,
                        isSelected = selectedCategory == item.name,
                        onClick = { onCategorySelect(item.name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                secondRow.forEach { item ->
                    CategoryTile(
                        item = item,
                        isSelected = selectedCategory == item.name,
                        onClick = { onCategorySelect(item.name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val reorderFoods = foods.filter { it.isAvailable && (it.isReorder || it.isFeatured) }
            if (reorderFoods.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Featured Homemade Dishes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1A1D1E)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(reorderFoods, key = { it.foodId }) { food ->
                        Card(
                            modifier = Modifier
                                .width(160.dp)
                                .clickable { onFoodClick(food) },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = GreenPrimary)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = food.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = food.description,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(ImageUtils.resolveImageModel(food.imageUrl))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = food.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Popular Home Cook Meals Section
            Text(
                text = "Popular Homemade Foods",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1A1D1E)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filtered foods by search, category, and availability (only active foods are displayed to customers)
            val filteredFoods = foods.filter { food ->
                food.isAvailable &&
                (selectedCategory == "All In" || food.category.equals(selectedCategory, ignoreCase = true)) &&
                (searchQuery.isEmpty() || food.title.contains(searchQuery, ignoreCase = true) || food.cookName.contains(searchQuery, ignoreCase = true))
            }

            if (filteredFoods.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No homemade meals found in this category.",
                        color = Color(0xFF737880),
                        fontSize = 14.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    filteredFoods.forEach { food ->
                        FoodCard(
                            food = food,
                            isFavorite = favoriteFoodIds.contains(food.foodId),
                            onFoodClick = { onFoodClick(food) },
                            onFavoriteClick = { onFavoriteClick(food.foodId) },
                            onAddToCartClick = { onAddToCart(food) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ─── Set / Change Delivery Address Dialog ────────────────────────────
        if (showLocationDialog) {
            var tempAddress by remember(currentUserAddress) { mutableStateOf(currentUserAddress) }
            val inputColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = GreenPrimary,
                unfocusedBorderColor = Color(0xFFCBD5E1),
                cursorColor = GreenPrimary
            )

            AlertDialog(
                onDismissRequest = { showLocationDialog = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = if (currentUserAddress.isBlank()) "Set Delivery Address" else "Change Delivery Address",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Enter your primary address for food deliveries:",
                            fontSize = 13.5.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp
                        )

                        OutlinedTextField(
                            value = tempAddress,
                            onValueChange = { tempAddress = it },
                            placeholder = { Text("e.g. 123 Green Street, Tech City, Apt 4B") },
                            label = { Text("Delivery Address") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = inputColors,
                            maxLines = 3,
                            trailingIcon = {
                                if (tempAddress.isNotBlank()) {
                                    IconButton(onClick = { tempAddress = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear address",
                                            tint = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = tempAddress.trim()
                            if (trimmed.isNotBlank()) {
                                onUpdateAddress(trimmed)
                                showLocationDialog = false
                            }
                        },
                        enabled = tempAddress.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Address", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLocationDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                }
            )
        }
    }
}

@Composable
fun CategoryTile(
    item: CategoryItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(4.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (item.isAllIn) DarkPill else Color.White)
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) GreenPrimary else Color.Transparent,
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (item.name) {
                    "Offers" -> Icons.Filled.LocalOffer
                    "Asian" -> Icons.Filled.RamenDining
                    "Biryani" -> Icons.Filled.RiceBowl
                    "Indian" -> Icons.Filled.Restaurant
                    "Gotyou" -> Icons.Filled.CardGiftcard
                    "Box" -> Icons.Filled.TakeoutDining
                    "Burger" -> Icons.Filled.LunchDining
                    else -> Icons.Filled.Apps
                },
                contentDescription = item.name,
                tint = if (item.isAllIn) Color.White else GreenPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.name,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (item.isAllIn) Color(0xFF1E1E1E) else Color(0xFF1E1E1E)
        )
    }
}
