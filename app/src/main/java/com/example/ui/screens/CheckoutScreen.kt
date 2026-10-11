package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.Dialog
import com.example.data.model.CartItem
import com.example.ui.theme.DarkPill
import com.example.ui.theme.GreenPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartItems: List<CartItem>,
    defaultAddress: String,
    promoDiscount: Double = 0.0,
    onBackClick: () -> Unit,
    onConfirmOrder: (String, String, String) -> Unit
) {
    var deliveryAddress by remember { mutableStateOf(defaultAddress) }
    var selectedPaymentMethod by remember { mutableStateOf("Card") }
    var notesForCook by remember { mutableStateOf("Please knock gently upon arrival.") }
    var scheduleOption by remember { mutableStateOf("Deliver Now (25-35 mins)") }
    var showCardPaymentDialog by remember { mutableStateOf(false) }

    val subtotal = cartItems.sumOf { it.price * it.quantity }
    val deliveryFee = 2.00
    // Bug 5 fix: Match the repository formula exactly:
    // total = subtotal + deliveryFee + 5% tax - promoDiscount
    val tax = subtotal * 0.05
    val total = (subtotal + deliveryFee + tax - promoDiscount).coerceAtLeast(1.0)

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
                        text = "Checkout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E1E1E)
                    )

                    Spacer(modifier = Modifier.size(38.dp))
                }
            }
        },
        bottomBar = {
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
                        onClick = {
                            if (selectedPaymentMethod == "Card") {
                                showCardPaymentDialog = true
                            } else {
                                onConfirmOrder(deliveryAddress, selectedPaymentMethod, notesForCook)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        Text(
                            text = "Confirm Order ($${String.format("%.2f", total)})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Delivery Address Box with Map Preview Simulator
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = GreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Delivery Address", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = deliveryAddress,
                        onValueChange = { deliveryAddress = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedContainerColor = Color(0xFFF8F9FA)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Map Simulator Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFE8ECEF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Map, contentDescription = null, tint = GreenPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GPS Location Pinned on Map",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1E1E)
                            )
                        }
                    }
                }
            }

            // Schedule Delivery
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Delivery Time", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    listOf("Deliver Now (25-35 mins)", "Schedule for Later Today (6:00 PM)").forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { scheduleOption = opt }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = scheduleOption == opt,
                                onClick = { scheduleOption = opt },
                                colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = opt, fontSize = 14.sp, color = Color(0xFF1E1E1E))
                        }
                    }
                }
            }

            // Payment Method Selection
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Payment Method", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    listOf(
                        Triple("Card", "Credit / Debit Card", Icons.Filled.CreditCard),
                        Triple("Wallet", "HomeChef Wallet / QR Pay", Icons.Filled.QrCode),
                        Triple("Cash", "Cash on Delivery", Icons.Filled.Payments)
                    ).forEach { (key, label, icon) ->
                        val isSelected = selectedPaymentMethod == key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color(0xFFE6F6EF) else Color(0xFFF8F9FA))
                                .clickable { selectedPaymentMethod = key }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null, tint = if (isSelected) GreenPrimary else Color(0xFF737880))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = Color(0xFF1E1E1E),
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GreenPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Notes for Cook
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Notes for Home Cook", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notesForCook,
                        onValueChange = { notesForCook = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedContainerColor = Color(0xFFF8F9FA)
                        )
                    )
                }
            }
        }
    }

    if (showCardPaymentDialog) {
        DummyCardPaymentDialog(
            totalAmount = total,
            onDismiss = { showCardPaymentDialog = false },
            onPaymentSuccess = { cardNumber ->
                showCardPaymentDialog = false
                val digits = cardNumber.filter { it.isDigit() }
                val masked = if (digits.length >= 4) "Card (•••• ${digits.takeLast(4)})" else "Credit/Debit Card"
                onConfirmOrder(deliveryAddress, masked, notesForCook)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DummyCardPaymentDialog(
    totalAmount: Double,
    onDismiss: () -> Unit,
    onPaymentSuccess: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var cardNumber by remember { mutableStateOf("") }
    var cardholderName by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    var isProcessing by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with SSL secure badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Payment Gateway",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "256-Bit SSL Encrypted",
                                fontSize = 11.sp,
                                color = GreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5)
                    ) {
                        Text(
                            text = "$${String.format("%.2f", totalAmount)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = GreenPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Realistic Credit/Debit Card Visual Preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(165.dp),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0F172A),
                                        Color(0xFF1E293B),
                                        Color(0xFF047857)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "HomeChef Pay",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            val formattedPreview = remember(cardNumber) {
                                val digits = cardNumber.filter { it.isDigit() }
                                if (digits.isEmpty()) {
                                    "•••• •••• •••• ••••"
                                } else {
                                    val padded = digits.padEnd(16, '•')
                                    padded.chunked(4).joinToString(" ")
                                }
                            }
                            Text(
                                text = formattedPreview,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 2.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "CARDHOLDER",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = cardholderName.ifBlank { "YOUR NAME" }.uppercase(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "EXPIRES",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = expiryDate.ifBlank { "MM/YY" },
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // One-tap Autofill Demo Card button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isProcessing && !isSuccess) {
                            cardNumber = "4532892011458892"
                            cardholderName = "Jane Doe"
                            expiryDate = "12/28"
                            cvv = "882"
                            errorMessage = null
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Fill Demo Test Card",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card Number field
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(16)
                        cardNumber = digits
                        errorMessage = null
                    },
                    label = { Text("Card Number", fontSize = 12.sp) },
                    placeholder = { Text("16-digit card number", fontSize = 12.sp) },
                    singleLine = true,
                    enabled = !isProcessing && !isSuccess,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Cardholder Name field
                OutlinedTextField(
                    value = cardholderName,
                    onValueChange = {
                        cardholderName = it
                        errorMessage = null
                    },
                    label = { Text("Cardholder Name", fontSize = 12.sp) },
                    placeholder = { Text("Name on card", fontSize = 12.sp) },
                    singleLine = true,
                    enabled = !isProcessing && !isSuccess,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Expiry and CVV row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() || it == '/' }.take(5)
                            expiryDate = clean
                            errorMessage = null
                        },
                        label = { Text("Expiry", fontSize = 12.sp) },
                        placeholder = { Text("MM/YY", fontSize = 12.sp) },
                        singleLine = true,
                        enabled = !isProcessing && !isSuccess,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = cvv,
                        onValueChange = { input ->
                            cvv = input.filter { it.isDigit() }.take(3)
                            errorMessage = null
                        },
                        label = { Text("CVV", fontSize = 12.sp) },
                        placeholder = { Text("3 digits", fontSize = 12.sp) },
                        singleLine = true,
                        enabled = !isProcessing && !isSuccess,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color.White
                        )
                    )
                }

                // Error message
                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action buttons
                if (isSuccess) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Payment Approved!",
                                color = GreenPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (cardNumber.filter { it.isDigit() }.length < 16) {
                                errorMessage = "Please enter a valid 16-digit card number"
                                return@Button
                            }
                            if (cardholderName.isBlank()) {
                                errorMessage = "Please enter the cardholder name"
                                return@Button
                            }
                            if (expiryDate.length < 4) {
                                errorMessage = "Please enter valid expiry date (MM/YY)"
                                return@Button
                            }
                            if (cvv.length < 3) {
                                errorMessage = "Please enter 3-digit CVV"
                                return@Button
                            }

                            isProcessing = true
                            errorMessage = null
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1600)
                                isProcessing = false
                                isSuccess = true
                                kotlinx.coroutines.delay(650)
                                onPaymentSuccess(cardNumber)
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Authorizing with Bank...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        } else {
                            Text(
                                text = "Pay $${String.format("%.2f", totalAmount)}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onDismiss,
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

