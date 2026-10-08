package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenPrimary

@Composable
fun CustomerEditOrderDialog(
    order: Order,
    onDismiss: () -> Unit,
    onSave: (orderId: String, deliveryAddress: String, customerPhone: String, notesForCook: String, updatedItems: List<CartItem>, onResult: (String?) -> Unit) -> Unit
) {
    var deliveryAddress by remember { mutableStateOf(order.deliveryAddress) }
    var customerPhone by remember { mutableStateOf(order.customerPhone) }
    var notesForCook by remember { mutableStateOf(order.notesForCook) }
    val items = remember { mutableStateListOf<CartItem>().apply { addAll(order.items) } }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val subtotal = items.sumOf { it.price * it.quantity }
    val deliveryFee = order.deliveryFee
    val tax = subtotal * 0.05
    val total = subtotal + deliveryFee + tax

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Update Order #${order.orderId}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Awaiting chef acceptance • Edit anytime now",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Error Banner
                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(errorMessage!!, color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Delivery Address
                OutlinedTextField(
                    value = deliveryAddress,
                    onValueChange = { deliveryAddress = it; errorMessage = null },
                    label = { Text("Delivery Address") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = GreenPrimary) },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // Contact Phone
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it; errorMessage = null },
                    label = { Text("Contact Phone") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = GreenPrimary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // Notes for Cook
                OutlinedTextField(
                    value = notesForCook,
                    onValueChange = { notesForCook = it },
                    label = { Text("Notes for Cook / Special Request") },
                    placeholder = { Text("e.g. Less spicy, contact on arrival...") },
                    leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = GreenPrimary) },
                    singleLine = false,
                    maxLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // Order Items Section
                Text(
                    text = "Items in Order",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )

                items.forEachIndexed { index, item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAF8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "$${String.format("%.2f", item.price)} each",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Minus button
                                IconButton(
                                    onClick = {
                                        if (item.quantity > 1) {
                                            items[index] = item.copy(quantity = item.quantity - 1)
                                        } else if (items.size > 1) {
                                            items.removeAt(index)
                                        }
                                    },
                                    enabled = items.size > 1 || item.quantity > 1,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (item.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = if (item.quantity == 1) Color(0xFFDC2626) else Color(0xFF334155),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Text(
                                    text = "${item.quantity}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                // Plus button
                                IconButton(
                                    onClick = {
                                        if (item.quantity < 20) {
                                            items[index] = item.copy(quantity = item.quantity + 1)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(GreenPrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Price Summary
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("$${String.format("%.2f", subtotal)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Delivery Fee", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("$${String.format("%.2f", deliveryFee)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tax", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("$${String.format("%.2f", tax)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Updated Total", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            Text("$${String.format("%.2f", total)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = GreenPrimary)
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = {
                            if (deliveryAddress.isBlank()) {
                                errorMessage = "Delivery address cannot be empty."
                                return@Button
                            }
                            if (customerPhone.isBlank()) {
                                errorMessage = "Contact phone cannot be empty."
                                return@Button
                            }
                            if (items.isEmpty()) {
                                errorMessage = "Order must contain at least one item."
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            onSave(order.orderId, deliveryAddress, customerPhone, notesForCook, items.toList()) { err ->
                                isLoading = false
                                if (err != null) {
                                    errorMessage = err
                                } else {
                                    onDismiss()
                                }
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Updates")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerDeleteOrderDialog(
    order: Order,
    onDismiss: () -> Unit,
    onConfirmDelete: (orderId: String, onResult: (String?) -> Unit) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Delete Order #${order.orderId}?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Since the cook hasn't accepted this order yet, it will be cancelled and permanently deleted.",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Keep Order", color = Color(0xFF64748B))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    onConfirmDelete(order.orderId) { err ->
                        isLoading = false
                        if (err != null) {
                            errorMessage = err
                        } else {
                            onDismiss()
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Delete Order", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun CancelDeliveryDialog(
    order: Order,
    onDismiss: () -> Unit,
    onConfirmCancel: (orderId: String, onResult: (String?) -> Unit) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PriorityHigh,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Cancel Delivery?",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Are you sure you want to cancel this delivery?\nThis action cannot be undone.",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        },
        dismissButton = {
            FilledTonalButton(
                onClick = onDismiss,
                enabled = !isLoading,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Keep Delivery", fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    onConfirmCancel(order.orderId) { err ->
                        isLoading = false
                        if (err != null) {
                            errorMessage = err
                        } else {
                            onDismiss()
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Cancel Delivery", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = Color.White
    )
}
