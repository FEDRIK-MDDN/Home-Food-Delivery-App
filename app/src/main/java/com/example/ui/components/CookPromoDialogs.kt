package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PromoCode
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePromoDialog(
    onDismiss: () -> Unit,
    onCreate: (
        code: String,
        title: String,
        description: String,
        discountType: String,
        discountValue: Double,
        minOrderValue: Double,
        durationDays: Int,
        maxUsageLimit: Int,
        onResult: (String?) -> Unit
    ) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var discountType by remember { mutableStateOf("PERCENTAGE") } // "PERCENTAGE" or "FIXED"
    var discountValueText by remember { mutableStateOf("15") }
    var minOrderValueText by remember { mutableStateOf("0") }
    var durationDays by remember { mutableStateOf(7) }
    var maxUsageText by remember { mutableStateOf("50") }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "Create Promotion Deal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = Color(0xFF0F172A)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Offer special discounts or lunch combos to boost your kitchen's neighborhood orders.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )

                errorMessage?.let { error ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Promo Code Input
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(12) },
                    label = { Text("Coupon Code (e.g. WEEKEND15)") },
                    placeholder = { Text("WEEKEND15") },
                    leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = GreenPrimary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )

                // Campaign Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Campaign Title") },
                    placeholder = { Text("Weekend Family Feast") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Discount Type Selector (Percentage vs Fixed $)
                Text("Discount Type", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF334155))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isPerc = discountType == "PERCENTAGE"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPerc) GreenPrimary else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                discountType = "PERCENTAGE"
                                if (discountValueText.toDoubleOrNull() ?: 0.0 > 90) discountValueText = "20"
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Percent, contentDescription = null, tint = if (isPerc) Color.White else Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Percentage (%)", color = if (isPerc) Color.White else Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    val isFixed = discountType == "FIXED"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isFixed) GreenPrimary else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { discountType = "FIXED" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = if (isFixed) Color.White else Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fixed Amount ($)", color = if (isFixed) Color.White else Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // Discount Value & Min Order Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = discountValueText,
                        onValueChange = { discountValueText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(if (discountType == "PERCENTAGE") "Discount (%)" else "Discount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = minOrderValueText,
                        onValueChange = { minOrderValueText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Min Order ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Duration Selector
                Text("Validity Duration", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF334155))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(3, 7, 14, 30).forEach { days ->
                        val isSel = durationDays == days
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) GreenContainer else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) GreenPrimary else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { durationDays = days }
                        ) {
                            Text(
                                text = "$days Days",
                                color = if (isSel) GreenPrimary else Color(0xFF64748B),
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deal Description / Terms") },
                    placeholder = { Text("Get 15% off on all signature dishes when ordering $20 or more.") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Max usage limit
                OutlinedTextField(
                    value = maxUsageText,
                    onValueChange = { maxUsageText = it.filter { c -> c.isDigit() } },
                    label = { Text("Max Redemptions Limit") },
                    placeholder = { Text("50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanCode = code.trim().uppercase()
                    val dVal = discountValueText.toDoubleOrNull() ?: 0.0
                    val mVal = minOrderValueText.toDoubleOrNull() ?: 0.0
                    val limit = maxUsageText.toIntOrNull() ?: 50

                    if (cleanCode.length < 3) {
                        errorMessage = "Please enter a valid coupon code (min 3 chars)."
                        return@Button
                    }
                    if (dVal <= 0.0) {
                        errorMessage = "Discount value must be greater than 0."
                        return@Button
                    }
                    if (discountType == "PERCENTAGE" && dVal > 90.0) {
                        errorMessage = "Percentage discount cannot exceed 90%."
                        return@Button
                    }

                    errorMessage = null
                    isSubmitting = true
                    onCreate(cleanCode, title, description, discountType, dVal, mVal, durationDays, limit) { err ->
                        isSubmitting = false
                        if (err == null) {
                            onDismiss()
                        } else {
                            errorMessage = err
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Publish Deal", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )
}

@Composable
fun EditPromoDialog(
    promo: PromoCode,
    onDismiss: () -> Unit,
    onSave: (
        promoId: String,
        title: String,
        description: String,
        discountType: String,
        discountValue: Double,
        minOrderValue: Double,
        extendDays: Int,
        isActive: Boolean,
        onResult: (String?) -> Unit
    ) -> Unit
) {
    var title by remember { mutableStateOf(promo.title) }
    var description by remember { mutableStateOf(promo.description) }
    var discountType by remember { mutableStateOf(promo.discountType) }
    var discountValueText by remember {
        mutableStateOf(if (promo.discountValue % 1.0 == 0.0) promo.discountValue.toInt().toString() else promo.discountValue.toString())
    }
    var minOrderValueText by remember {
        mutableStateOf(if (promo.minOrderValue % 1.0 == 0.0) promo.minOrderValue.toInt().toString() else promo.minOrderValue.toString())
    }
    var extendDays by remember { mutableStateOf(0) }
    var isActive by remember { mutableStateOf(promo.isActive) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(24.dp))
                Text("Edit Promo: ${promo.code}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                errorMessage?.let { error ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = error, color = Color(0xFFDC2626), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                    }
                }

                // Status toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Promotion Status", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text(if (isActive) "Active & available to customers" else "Paused by cook", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GreenPrimary)
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Deal Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Discount Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isPerc = discountType == "PERCENTAGE"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPerc) GreenPrimary else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { discountType = "PERCENTAGE" }
                    ) {
                        Text("Percentage (%)", color = if (isPerc) Color.White else Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }

                    val isFixed = discountType == "FIXED"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isFixed) GreenPrimary else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { discountType = "FIXED" }
                    ) {
                        Text("Fixed ($)", color = if (isFixed) Color.White else Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = discountValueText,
                        onValueChange = { discountValueText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Discount Value") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = minOrderValueText,
                        onValueChange = { minOrderValueText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Min Order ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Terms") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Extend Expiry Option
                Text("Extend Expiration", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF334155))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0 to "Keep", 7 to "+7 Days", 14 to "+14 Days", 30 to "+30 Days").forEach { (days, label) ->
                        val isSel = extendDays == days
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) GreenContainer else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) GreenPrimary else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { extendDays = days }
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) GreenPrimary else Color(0xFF64748B),
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dVal = discountValueText.toDoubleOrNull() ?: 0.0
                    val mVal = minOrderValueText.toDoubleOrNull() ?: 0.0
                    if (dVal <= 0.0) {
                        errorMessage = "Discount value must be greater than 0."
                        return@Button
                    }
                    if (discountType == "PERCENTAGE" && dVal > 90.0) {
                        errorMessage = "Percentage discount cannot exceed 90%."
                        return@Button
                    }

                    errorMessage = null
                    isSubmitting = true
                    onSave(promo.promoId, title, description, discountType, dVal, mVal, extendDays, isActive) { err ->
                        isSubmitting = false
                        if (err == null) onDismiss() else errorMessage = err
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )
}

@Composable
fun DeletePromoDialog(
    promo: PromoCode,
    onDismiss: () -> Unit,
    onConfirmDelete: (promoId: String, onResult: (String?) -> Unit) -> Unit
) {
    var isDeleting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFFEE2E2), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(26.dp))
            }
        },
        title = {
            Text(
                text = "Delete Promotion Deal",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Are you sure you want to permanently delete the promotion coupon '${promo.code}' (${promo.title})?",
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                )
                Text(
                    text = "Customers will no longer be able to redeem this discount code at checkout.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                errorMessage?.let { error ->
                    Text(text = error, color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isDeleting = true
                    errorMessage = null
                    onConfirmDelete(promo.promoId) { err ->
                        isDeleting = false
                        if (err == null) onDismiss() else errorMessage = err
                    }
                },
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Delete Deal", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text("Keep Deal", color = Color(0xFF64748B))
            }
        }
    )
}
