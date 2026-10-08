package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
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
import com.example.data.model.CartItem
import com.example.ui.theme.CoralPrice
import com.example.ui.theme.DarkPill
import com.example.ui.theme.GreenPrimary
import com.example.util.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartItems: List<CartItem>,
    appliedPromoCode: String,
    promoDiscount: Double,
    onBackClick: () -> Unit,
    onUpdateQuantity: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onApplyPromo: (String) -> Boolean,
    onProceedToCheckout: () -> Unit
) {
    var promoInput by remember { mutableStateOf(appliedPromoCode) }
    var promoMessage by remember { mutableStateOf<String?>(null) }

    val subtotal = cartItems.sumOf { it.price * it.quantity }
    val deliveryFee = if (subtotal > 15.0) 0.0 else 2.00
    val total = (subtotal + deliveryFee - promoDiscount).coerceAtLeast(0.0)

    // Grouping by cook for information display
    val cookCount = cartItems.map { it.cookId }.distinct().size

    Scaffold(
        topBar = {
            Surface(color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF2F4F7))
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E1E1E),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Cart",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E1E1E)
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF2F4F7))
                            .clickable { /* Search or clear */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF1E1E1E),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Button(
                            onClick = onProceedToCheckout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkPill)
                        ) {
                            Text(
                                text = "Checkout",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingCart,
                        contentDescription = null,
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your cart is empty",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E1E1E)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Explore local home cooks and add meals!",
                        fontSize = 13.sp,
                        color = Color(0xFF737880)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Multi-Cook Automatic Split Banner
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F6EF)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Items from $cookCount cook(s). System will create $cookCount separate order(s) automatically upon checkout.",
                                fontSize = 12.sp,
                                color = Color(0xFF003820),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Cart Item Cards (Matching uploaded UI image 2 left screen)
                items(cartItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF2F4F7))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(ImageUtils.resolveImageModel(item.imageUrl))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1E1E1E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Cook: ${item.cookName}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF737880)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$${String.format("%.2f", item.price)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = CoralPrice
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Quantity Stepper or Delete button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFF5F6F8))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                if (item.quantity == 1) {
                                    IconButton(
                                        onClick = { onRemoveItem(item.foodId) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Remove",
                                            tint = CoralPrice,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { onUpdateQuantity(item.foodId, item.quantity - 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Remove,
                                            contentDescription = "Decrease",
                                            tint = Color(0xFF1E1E1E),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "${item.quantity}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E1E1E),
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                        .clickable { onUpdateQuantity(item.foodId, item.quantity + 1) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Increase",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Promo Code Section (Matching uploaded UI image 2 left screen)
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = promoInput,
                                onValueChange = { promoInput = it },
                                placeholder = { Text("Promo code", color = Color(0xFF9EA3AE), fontSize = 14.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GreenPrimary,
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    unfocusedContainerColor = Color.White,
                                    focusedContainerColor = Color.White
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = {
                                    val ok = onApplyPromo(promoInput)
                                    promoMessage = if (ok) "Promo HOMECOOK10 Applied! -$2.00" else "Invalid Promo Code"
                                },
                                modifier = Modifier.height(52.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkPill)
                            ) {
                                Text(
                                    text = "Apply",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        promoMessage?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = it,
                                fontSize = 12.sp,
                                color = if (it.contains("Applied")) GreenPrimary else CoralPrice,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Order Summary Card (Matching uploaded UI image 2 left screen)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Order Summary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1E1E1E)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Subtotals for products:", fontSize = 13.sp, color = Color(0xFF737880))
                                Text(text = "$${String.format("%.2f", subtotal)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E1E1E))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Delivery shipping:", fontSize = 13.sp, color = Color(0xFF737880))
                                Text(
                                    text = if (deliveryFee == 0.0) "Free" else "$${String.format("%.2f", deliveryFee)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E1E1E)
                                )
                            }

                            if (promoDiscount > 0.0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Promo Discount:", fontSize = 13.sp, color = GreenPrimary)
                                    Text(text = "-$${String.format("%.2f", promoDiscount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GreenPrimary)
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEAEAEA))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Total", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E1E1E))
                                Text(text = "$${String.format("%.2f", total)}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF1E1E1E))
                            }
                        }
                    }
                }
            }
        }
    }
}
