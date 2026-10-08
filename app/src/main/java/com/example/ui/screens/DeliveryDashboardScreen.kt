package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.User
import com.example.ui.components.CancelDeliveryDialog
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDashboardScreen(
    currentUser: User,
    orders: List<Order>,
    onClaimDelivery: (orderId: String) -> Unit,
    onUpdateOrderStatus: (orderId: String, newStatus: OrderStatus) -> Unit,
    onOpenMap: (Order) -> Unit,
    onCancelDelivery: (orderId: String, onResult: (String?) -> Unit) -> Unit = { _, _ -> },
    onNavigateToIssues: (preselectedOrderId: String?) -> Unit = {}
) {
    var orderToCancel by remember { mutableStateOf<Order?>(null) }
    // Available orders ready for pickup — Bug 8 fix: only READY orders are shown.
    // PREPARING orders must NOT be listed; the cook hasn't finished yet.
    val availablePickups = remember(orders) {
        orders.filter {
            it.status == OrderStatus.READY &&
            it.deliveryId.isNullOrEmpty()
        }.sortedByDescending { it.createdAt }
    }

    // Active deliveries claimed by this partner
    val myDeliveries = remember(orders, currentUser) {
        orders.filter { it.deliveryId == currentUser.userId || ((it.deliveryPartnerName?.contains(currentUser.name, ignoreCase = true) == true) && !it.deliveryId.isNullOrEmpty()) }
            .sortedByDescending { it.createdAt }
    }

    val totalEarnings = myDeliveries.filter { it.status == OrderStatus.DELIVERED || it.status == OrderStatus.COMPLETED }.sumOf { it.deliveryFee }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Delivery Express",
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
                            text = "$${String.format("%.2f", totalEarnings)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GreenPrimary
                        )
                        Text(
                            text = "Delivery Fees",
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
                            text = "${myDeliveries.size}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "My Trips",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // My Active Deliveries Section
                if (myDeliveries.isNotEmpty()) {
                    item {
                        Text(
                            text = "My Active Deliveries (${myDeliveries.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                    }

                    items(myDeliveries, key = { "my_${it.orderId}" }) { order ->
                        DeliveryCard(
                            order = order,
                            isClaimed = true,
                            onClaim = { },
                            onOpenMap = { onOpenMap(order) },
                            onUpdateStatus = { newStatus -> onUpdateOrderStatus(order.orderId, newStatus) },
                            onCancelDelivery = { orderToCancel = order },
                            onReportIssue = { onNavigateToIssues(order.orderId) }
                        )
                    }
                }

                // Available Pickup Requests
                item {
                    Text(
                        text = "Available Pickup Requests (${availablePickups.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                }

                if (availablePickups.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No pending pickups near you", fontSize = 14.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                } else {
                    items(availablePickups, key = { "avail_${it.orderId}" }) { order ->
                        DeliveryCard(
                            order = order,
                            isClaimed = false,
                            onClaim = { onClaimDelivery(order.orderId) },
                            onOpenMap = { onOpenMap(order) },
                            onUpdateStatus = { }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    orderToCancel?.let { ord ->
        CancelDeliveryDialog(
            order = ord,
            onDismiss = { orderToCancel = null },
            onConfirmCancel = { orderId, onResult ->
                onCancelDelivery(orderId, onResult)
            }
        )
    }
}

@Composable
fun DeliveryCard(
    order: Order,
    isClaimed: Boolean,
    onClaim: () -> Unit,
    onOpenMap: () -> Unit,
    onUpdateStatus: (OrderStatus) -> Unit,
    onCancelDelivery: () -> Unit = {},
    onReportIssue: () -> Unit = {}
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
                Text(
                    text = "$${String.format("%.2f", order.deliveryFee)} fee",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = GreenPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storefront, contentDescription = "Cook", tint = GreenPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Pickup: ${order.cookName}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = "Dropoff", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Dropoff: ${order.deliveryAddress}", fontSize = 13.sp, color = Color(0xFF334155))
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            if (!isClaimed) {
                Button(
                    onClick = onClaim,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Accept & Claim Trip", fontWeight = FontWeight.Bold)
                }
            } else {
                // Map Navigation and Report Issue buttons for claimed trips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onOpenMap,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_delivery_map_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, GreenPrimary)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Map Navigation", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Live Map", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onReportIssue,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Icon(Icons.Default.ReportProblem, contentDescription = "Report Issue", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Report Issue", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (order.status) {
                        OrderStatus.READY, OrderStatus.PICKED_UP -> {
                            OutlinedButton(
                                onClick = onCancelDelivery,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onUpdateStatus(OrderStatus.OUT_FOR_DELIVERY) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Start Route")
                            }
                        }
                        OrderStatus.OUT_FOR_DELIVERY -> {
                            Button(
                                onClick = { onUpdateStatus(OrderStatus.DELIVERED) },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Mark Delivered")
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GreenContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Status: ${order.status.label}",
                                    color = GreenPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
