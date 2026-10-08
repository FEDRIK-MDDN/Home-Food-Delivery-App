package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryIssue
import com.example.data.model.DeliveryIssueCategories
import com.example.data.model.Order
import com.example.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDeliveryIssueDialog(
    claimedOrders: List<Order>,
    preselectedOrderId: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (orderId: String?, category: String, description: String, priority: String, onResult: (String?) -> Unit) -> Unit
) {
    var selectedOrderId by remember { mutableStateOf(preselectedOrderId) }
    var selectedCategory by remember { mutableStateOf(DeliveryIssueCategories.CUSTOMER_NOT_ANSWERING) }
    var selectedPriority by remember { mutableStateOf("HIGH") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var orderExpanded by remember { mutableStateOf(false) }

    val priorities = listOf("LOW", "MEDIUM", "HIGH", "URGENT")

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEF2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReportProblem,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Report Delivery Issue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Submit a problem on your route",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Linked Order Selector
                Text(
                    text = "Related Order (Optional)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )

                ExposedDropdownMenuBox(
                    expanded = orderExpanded,
                    onExpandedChange = { orderExpanded = !orderExpanded }
                ) {
                    OutlinedTextField(
                        value = if (selectedOrderId != null) {
                            val ord = claimedOrders.find { it.orderId == selectedOrderId }
                            if (ord != null) "#${ord.orderId} (${ord.customerName})" else "#$selectedOrderId"
                        } else "General Route Issue (No Order)",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = orderExpanded,
                        onDismissRequest = { orderExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("General Route Issue (No Order)") },
                            onClick = {
                                selectedOrderId = null
                                orderExpanded = false
                            }
                        )
                        claimedOrders.forEach { ord ->
                            DropdownMenuItem(
                                text = { Text("#${ord.orderId} - ${ord.customerName} (${ord.deliveryAddress.take(24)}…)") },
                                onClick = {
                                    selectedOrderId = ord.orderId
                                    orderExpanded = false
                                }
                            )
                        }
                    }
                }

                // Issue Category Dropdown
                Text(
                    text = "Issue Category *",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DeliveryIssueCategories.ALL.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Priority Selection
                Text(
                    text = "Urgency / Priority",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    priorities.forEach { prio ->
                        val isSel = selectedPriority == prio
                        val prioColor = when (prio) {
                            "URGENT" -> Color(0xFFDC2626)
                            "HIGH"   -> Color(0xFFEA580C)
                            "MEDIUM" -> Color(0xFF2563EB)
                            else     -> Color(0xFF64748B)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) prioColor.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, prioColor) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPriority = prio }
                        ) {
                            Text(
                                text = prio,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) prioColor else Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // Description
                Text(
                    text = "Description *",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Describe the issue (e.g. Customer phone goes to voicemail, apt gate locked...)") },
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
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
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (description.isBlank()) {
                        errorMessage = "Please enter a description for the issue."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    onSubmit(selectedOrderId, selectedCategory, description, selectedPriority) { err ->
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
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Submit Report", fontWeight = FontWeight.Bold)
                }
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = Color.White
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeliveryIssueDialog(
    issue: DeliveryIssue,
    onDismiss: () -> Unit,
    onSave: (issueId: String, category: String, description: String, priority: String, onResult: (String?) -> Unit) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(issue.category) }
    var selectedPriority by remember { mutableStateOf(issue.priority) }
    var description by remember { mutableStateOf(issue.description) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val priorities = listOf("LOW", "MEDIUM", "HIGH", "URGENT")

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Edit Delivery Issue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Report #${issue.issueId}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!issue.orderId.isNullOrEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Linked Order: #${issue.orderId}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Category
                Text(
                    text = "Issue Category *",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DeliveryIssueCategories.ALL.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Priority Selection
                Text(
                    text = "Urgency / Priority",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    priorities.forEach { prio ->
                        val isSel = selectedPriority == prio
                        val prioColor = when (prio) {
                            "URGENT" -> Color(0xFFDC2626)
                            "HIGH"   -> Color(0xFFEA580C)
                            "MEDIUM" -> Color(0xFF2563EB)
                            else     -> Color(0xFF64748B)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) prioColor.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, prioColor) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPriority = prio }
                        ) {
                            Text(
                                text = prio,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) prioColor else Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // Description
                Text(
                    text = "Description *",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
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
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (description.isBlank()) {
                        errorMessage = "Description cannot be empty."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    onSave(issue.issueId, selectedCategory, description, selectedPriority) { err ->
                        isLoading = false
                        if (err != null) {
                            errorMessage = err
                        } else {
                            onDismiss()
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = Color.White
    )
}

@Composable
fun DeleteDeliveryIssueDialog(
    issue: DeliveryIssue,
    onDismiss: () -> Unit,
    onConfirmDelete: (issueId: String, onResult: (String?) -> Unit) -> Unit
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
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Withdraw Issue Report?",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Are you sure you want to withdraw and remove report #${issue.issueId} (${issue.category})?\nThis action cannot be undone.",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
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
                Text("Keep Report")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    onConfirmDelete(issue.issueId) { err ->
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
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Withdraw / Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = Color.White
    )
}
