package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
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
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.components.CancelDeliveryDialog
import com.example.ui.components.CustomerDeleteOrderDialog
import com.example.ui.components.CustomerEditOrderDialog
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    orders: List<Order>,
    currentUser: User,
    onOrderClick: (Order) -> Unit,
    onUpdateOrder: (orderId: String, deliveryAddress: String, phone: String, notes: String, items: List<CartItem>, onResult: (String?) -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteOrder: (orderId: String, onResult: (String?) -> Unit) -> Unit = { _, _ -> },
    onCancelDelivery: (orderId: String, onResult: (String?) -> Unit) -> Unit = { _, _ -> }
) {
    // ── Role-based tab definitions ─────────────────────────────────────────────
    // Each role sees different filter tabs that match their workflow.
    val tabs = when (currentUser.role) {
        UserRole.CUSTOMER  -> listOf("All", "Active", "Completed", "Cancelled")
        UserRole.COOK      -> listOf("All", "New", "In Kitchen", "Done")
        UserRole.DELIVERY  -> listOf("All", "Active", "Completed", "Cancelled")
        UserRole.ADMIN     -> listOf("All", "Active", "Completed", "Cancelled")
    }

    var selectedTab by remember { mutableStateOf(tabs.first()) }
    var orderToEdit by remember { mutableStateOf<Order?>(null) }
    var orderToDelete by remember { mutableStateOf<Order?>(null) }
    var orderToCancelDelivery by remember { mutableStateOf<Order?>(null) }

    // ── Role-based pre-filter: only show orders relevant to this user ───────────
    val roleFilteredOrders = remember(orders, currentUser) {
        when (currentUser.role) {
            // Customer: only their own placed orders
            UserRole.CUSTOMER -> orders.filter { it.customerId == currentUser.userId }
            // Cook: only orders sent to their kitchen
            UserRole.COOK     -> orders.filter { it.cookId == currentUser.userId }
            // Delivery: orders claimed by this driver OR orders this driver cancelled
            UserRole.DELIVERY -> orders.filter {
                it.deliveryId == currentUser.userId || it.cancelledDriverIds.contains(currentUser.userId)
            }
            // Admin: sees everything
            UserRole.ADMIN    -> orders
        }
    }

    // ── Tab sub-filter: further filter by selected tab ─────────────────────────
    val filteredOrders = remember(roleFilteredOrders, selectedTab, currentUser.role, currentUser.userId) {
        when (currentUser.role) {
            UserRole.CUSTOMER, UserRole.ADMIN -> when (selectedTab) {
                "Active"    -> roleFilteredOrders.filter {
                    it.status != OrderStatus.DELIVERED &&
                    it.status != OrderStatus.COMPLETED &&
                    it.status != OrderStatus.CANCELLED
                }
                "Completed" -> roleFilteredOrders.filter {
                    it.status == OrderStatus.DELIVERED || it.status == OrderStatus.COMPLETED
                }
                "Cancelled" -> roleFilteredOrders.filter { it.status == OrderStatus.CANCELLED }
                else        -> roleFilteredOrders
            }
            UserRole.COOK -> when (selectedTab) {
                "New"        -> roleFilteredOrders.filter { it.status == OrderStatus.PENDING || it.status == OrderStatus.ACCEPTED }
                "In Kitchen" -> roleFilteredOrders.filter { it.status == OrderStatus.PREPARING || it.status == OrderStatus.READY }
                "Done"       -> roleFilteredOrders.filter { it.status == OrderStatus.DELIVERED || it.status == OrderStatus.COMPLETED || it.status == OrderStatus.CANCELLED }
                else         -> roleFilteredOrders
            }
            UserRole.DELIVERY -> when (selectedTab) {
                "Active"    -> roleFilteredOrders.filter {
                    it.deliveryId == currentUser.userId &&
                    it.status != OrderStatus.DELIVERED &&
                    it.status != OrderStatus.COMPLETED &&
                    it.status != OrderStatus.CANCELLED
                }
                "Completed" -> roleFilteredOrders.filter {
                    it.deliveryId == currentUser.userId &&
                    (it.status == OrderStatus.DELIVERED || it.status == OrderStatus.COMPLETED)
                }
                "Cancelled" -> roleFilteredOrders.filter {
                    (it.cancelledDriverIds.contains(currentUser.userId) && it.deliveryId != currentUser.userId) ||
                    (it.deliveryId == currentUser.userId && it.status == OrderStatus.CANCELLED)
                }
                else        -> roleFilteredOrders
            }
        }
    }

    // ── Screen title per role ──────────────────────────────────────────────────
    val screenTitle = when (currentUser.role) {
        UserRole.CUSTOMER -> "My Orders"
        UserRole.COOK     -> "Kitchen Orders"
        UserRole.DELIVERY -> "My Deliveries"
        UserRole.ADMIN    -> "All Orders"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "$screenTitle (${filteredOrders.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = when (currentUser.role) {
                                UserRole.CUSTOMER -> "Track your placed orders"
                                UserRole.COOK     -> "Orders coming into your kitchen"
                                UserRole.DELIVERY -> "Your claimed delivery trips"
                                UserRole.ADMIN    -> "Platform-wide order overview"
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
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
            // Tab Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) GreenPrimary else Color(0xFFF1F5F9),
                        modifier = Modifier.clickable {
                            selectedTab = tab
                        }
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) Color.White else Color(0xFF64748B),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = "No Orders",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No \"$selectedTab\" Orders",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when (currentUser.role) {
                                UserRole.CUSTOMER -> "Orders you place will appear here"
                                UserRole.COOK     -> "New orders from customers will show up here"
                                UserRole.DELIVERY -> "Claim a delivery trip to see it here"
                                UserRole.ADMIN    -> "No orders match this filter"
                            },
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredOrders, key = { it.orderId }) { order ->
                        OrderItemCard(
                            order = order,
                            currentUserRole = currentUser.role,
                            currentUserId = currentUser.userId,
                            onClick = { onOrderClick(order) },
                            onEditClick = { orderToEdit = order },
                            onDeleteClick = { orderToDelete = order },
                            onCancelDeliveryClick = { orderToCancelDelivery = order }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    orderToEdit?.let { ord ->
        CustomerEditOrderDialog(
            order = ord,
            onDismiss = { orderToEdit = null },
            onSave = { orderId, addr, phone, notes, items, onResult ->
                onUpdateOrder(orderId, addr, phone, notes, items, onResult)
            }
        )
    }

    orderToDelete?.let { ord ->
        CustomerDeleteOrderDialog(
            order = ord,
            onDismiss = { orderToDelete = null },
            onConfirmDelete = { orderId, onResult ->
                onDeleteOrder(orderId, onResult)
            }
        )
    }

    orderToCancelDelivery?.let { ord ->
        CancelDeliveryDialog(
            order = ord,
            onDismiss = { orderToCancelDelivery = null },
            onConfirmCancel = { orderId, onResult ->
                onCancelDelivery(orderId, onResult)
            }
        )
    }
}

@Composable
fun OrderItemCard(
    order: Order,
    currentUserRole: UserRole = UserRole.CUSTOMER,
    currentUserId: String = "",
    onClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onCancelDeliveryClick: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(order.createdAt) { dateFormat.format(Date(order.createdAt)) }

    val isDeliveryCancelledByMe = currentUserRole == UserRole.DELIVERY &&
        order.cancelledDriverIds.contains(currentUserId) &&
        order.deliveryId != currentUserId

    val (statusColor, statusBg) = if (isDeliveryCancelledByMe) {
        Color(0xFFDC2626) to Color(0xFFFEE2E2)
    } else when (order.status) {
        OrderStatus.PENDING                                              -> Color(0xFFD97706) to Color(0xFFFEF3C7)
        OrderStatus.ACCEPTED, OrderStatus.PREPARING                     -> Color(0xFF2563EB) to Color(0xFFDBEAFE)
        OrderStatus.READY, OrderStatus.PICKED_UP,
        OrderStatus.OUT_FOR_DELIVERY                                     -> Color(0xFF059669) to Color(0xFFD1FAE5)
        OrderStatus.DELIVERED, OrderStatus.COMPLETED                    -> Color(0xFF16A34A) to Color(0xFFDCFCE7)
        OrderStatus.CANCELLED                                           -> Color(0xFFDC2626) to Color(0xFFFEE2E2)
    }

    val displayStatusLabel = if (isDeliveryCancelledByMe) "Delivery Cancelled" else order.status.label

    // Role-specific subtitle: who the "other party" is depends on perspective
    val subtitle = when (currentUserRole) {
        UserRole.CUSTOMER -> "From: ${order.cookName}"
        UserRole.COOK     -> "Customer: ${order.customerName}"
        UserRole.DELIVERY -> "Deliver to: ${order.deliveryAddress.take(30)}…"
        UserRole.ADMIN    -> "${order.customerName} → ${order.cookName}"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("order_item_${order.orderId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${order.orderId}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = subtitle,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBg
                ) {
                    Text(
                        text = displayStatusLabel,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Order items summary
            Text(
                text = order.items.joinToString(", ") { "${it.quantity}x ${it.title}" },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${String.format("%.2f", order.total)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = GreenPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Customer Actions before Cook Acceptance
            if (currentUserRole == UserRole.CUSTOMER && order.status == OrderStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = GreenContainer,
                            contentColor = GreenPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Update Order", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onDeleteClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDC2626)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Order", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Delivery Partner Actions before starting route (only if claimed by this driver)
            if (currentUserRole == UserRole.DELIVERY && order.deliveryId == currentUserId && (order.status == OrderStatus.PICKED_UP || order.status == OrderStatus.READY)) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onCancelDeliveryClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDC2626)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancel Delivery (Before Pickup)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
