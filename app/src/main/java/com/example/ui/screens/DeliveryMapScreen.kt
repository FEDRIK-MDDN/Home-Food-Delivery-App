package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.ui.components.CancelDeliveryDialog
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryMapScreen(
    order: Order,
    onBackClick: () -> Unit,
    onUpdateOrderStatus: (orderId: String, newStatus: OrderStatus) -> Unit,
    onCancelDelivery: (orderId: String, onResult: (String?) -> Unit) -> Unit = { _, _ -> },
    onReportIssue: () -> Unit = {}
) {
    val context = LocalContext.current
    var isSimulatingDrive by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    // Animated progress along route (0.0 to 1.0)
    val driveProgress by animateFloatAsState(
        targetValue = if (isSimulatingDrive) 1.0f else 0.15f,
        animationSpec = tween(
            durationMillis = if (isSimulatingDrive) 8000 else 600,
            easing = LinearEasing
        ),
        label = "driveProgress"
    )

    val isPickupPhase = order.status == OrderStatus.READY || order.status == OrderStatus.ACCEPTED || order.status == OrderStatus.PREPARING || order.status == OrderStatus.PENDING
    val isDeliveringPhase = order.status == OrderStatus.PICKED_UP || order.status == OrderStatus.OUT_FOR_DELIVERY
    val isCompleted = order.status == OrderStatus.DELIVERED || order.status == OrderStatus.COMPLETED

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Live Route Navigation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Order #${order.orderId} • ${order.cookName}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onReportIssue) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = "Report Delivery Issue",
                            tint = Color(0xFFDC2626)
                        )
                    }
                    IconButton(
                        onClick = {
                            val address = if (isPickupPhase) order.cookName else order.deliveryAddress
                            val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(mapIntent)
                            } else {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(address)}"))
                                context.startActivity(browserIntent)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open Google Maps",
                            tint = GreenPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAF8)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Interactive Canvas Map View
            InAppMapView(
                driveProgress = driveProgress,
                isPickupPhase = isPickupPhase,
                cookName = order.cookName,
                customerAddress = order.deliveryAddress,
                modifier = Modifier.fillMaxSize()
            )

            // Top Floating Route Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isPickupPhase) Color(0xFFFEF3C7) else GreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPickupPhase) Icons.Default.Storefront else Icons.Default.TwoWheeler,
                            contentDescription = "Step",
                            tint = if (isPickupPhase) Color(0xFFD97706) else GreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isPickupPhase -> "Step 1: Go to Cook's Home Kitchen"
                                isDeliveringPhase -> "Step 2: Deliver Food to Customer"
                                else -> "Order Delivered Successfully!"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = when {
                                isPickupPhase -> "Pickup from ${order.cookName}"
                                isDeliveringPhase -> "Dropoff at ${order.deliveryAddress}"
                                else -> "Completed • Earnings added to account"
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Text(
                        text = if (isCompleted) "0 min" else "${((1f - driveProgress) * 12).toInt() + 2} min",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = GreenPrimary
                    )
                }
            }

            // Bottom Floating Action Panel
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Contact Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isPickupPhase) "Home Cook Kitchen" else "Customer Contact",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isPickupPhase) order.cookName else order.customerName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (isPickupPhase) "Verified Residential Kitchen" else order.deliveryAddress,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Phone Call Action
                        Surface(
                            shape = CircleShape,
                            color = GreenContainer,
                            modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    val phone = if (isPickupPhase) "555-0192" else order.customerPhone
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(intent)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Drive Simulation Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Simulate GPS Movement",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                        Switch(
                            checked = isSimulatingDrive,
                            onCheckedChange = { isSimulatingDrive = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimary)
                        )
                    }

                    // Main Phase Progression Action Buttons
                    when {
                        isPickupPhase -> {
                            Button(
                                onClick = {
                                    onUpdateOrderStatus(order.orderId, OrderStatus.PICKED_UP)
                                    isSimulatingDrive = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("confirm_pickup_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("I've Picked Up Food from Kitchen", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                        order.status == OrderStatus.PICKED_UP || order.status == OrderStatus.READY -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showCancelDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                Button(
                                    onClick = {
                                        onUpdateOrderStatus(order.orderId, OrderStatus.OUT_FOR_DELIVERY)
                                        isSimulatingDrive = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    modifier = Modifier
                                        .weight(1.8f)
                                        .height(52.dp)
                                        .testTag("start_route_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.TwoWheeler, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Start Route", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                        order.status == OrderStatus.OUT_FOR_DELIVERY -> {
                            Button(
                                onClick = {
                                    onUpdateOrderStatus(order.orderId, OrderStatus.DELIVERED)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("mark_delivered_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mark Order Delivered to Customer", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GreenContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Order Fully Delivered! Great Job!",
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPrimary,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        CancelDeliveryDialog(
            order = order,
            onDismiss = { showCancelDialog = false },
            onConfirmCancel = { orderId, onResult ->
                onCancelDelivery(orderId) { err ->
                    onResult(err)
                    if (err == null) {
                        onBackClick()
                    }
                }
            }
        )
    }
}

@Composable
fun InAppMapView(
    driveProgress: Float,
    isPickupPhase: Boolean,
    cookName: String,
    customerAddress: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.background(Color(0xFFE2E8F0))) {
        val width = size.width
        val height = size.height

        // Draw Map Background Grid Streets
        val roadColor = Color.White
        val roadStroke = 24f
        val secondaryRoadColor = Color(0xFFCBD5E1)

        // Horizontal roads
        drawLine(secondaryRoadColor, Offset(0f, height * 0.25f), Offset(width, height * 0.25f), strokeWidth = 16f)
        drawLine(roadColor, Offset(0f, height * 0.5f), Offset(width, height * 0.5f), strokeWidth = roadStroke)
        drawLine(secondaryRoadColor, Offset(0f, height * 0.75f), Offset(width, height * 0.75f), strokeWidth = 16f)

        // Vertical roads
        drawLine(secondaryRoadColor, Offset(width * 0.25f, 0f), Offset(width * 0.25f, height), strokeWidth = 16f)
        drawLine(roadColor, Offset(width * 0.5f, 0f), Offset(width * 0.5f, height), strokeWidth = roadStroke)
        drawLine(secondaryRoadColor, Offset(width * 0.75f, 0f), Offset(width * 0.75f, height), strokeWidth = 16f)

        // Draw Green Park Area
        drawRect(
            color = Color(0xFFDCFCE7),
            topLeft = Offset(width * 0.05f, height * 0.1f),
            size = Size(width * 0.35f, height * 0.25f)
        )

        // Driver Start Point, Waypoints & Target Point
        val driverStart = Offset(width * 0.2f, height * 0.7f)
        val cookLocation = Offset(width * 0.5f, height * 0.5f)
        val customerLocation = Offset(width * 0.8f, height * 0.3f)

        val destination = if (isPickupPhase) cookLocation else customerLocation

        // Draw Route Polyline
        val routePath = Path().apply {
            moveTo(driverStart.x, driverStart.y)
            lineTo(driverStart.x, cookLocation.y)
            lineTo(cookLocation.x, cookLocation.y)
            if (!isPickupPhase) {
                lineTo(customerLocation.x, cookLocation.y)
                lineTo(customerLocation.x, customerLocation.y)
            }
        }

        // Base route path
        drawPath(
            path = routePath,
            color = Color(0xFF94A3B8),
            style = Stroke(width = 12f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f))
        )

        // Active highlighted route path
        drawPath(
            path = routePath,
            color = GreenPrimary,
            style = Stroke(width = 12f)
        )

        // Calculate current simulated driver position along path
        val activeSegmentStartX = driverStart.x + (destination.x - driverStart.x) * driveProgress
        val activeSegmentStartY = driverStart.y + (destination.y - driverStart.y) * driveProgress
        val currentDriverPos = Offset(activeSegmentStartX, activeSegmentStartY)

        // Draw Pin Marker 1: Cook Kitchen Pin
        drawCircle(
            color = Color(0xFFD97706),
            radius = 22f,
            center = cookLocation
        )
        drawCircle(
            color = Color.White,
            radius = 10f,
            center = cookLocation
        )

        // Draw Pin Marker 2: Customer House Pin
        drawCircle(
            color = Color(0xFFEF4444),
            radius = 22f,
            center = customerLocation
        )
        drawCircle(
            color = Color.White,
            radius = 10f,
            center = customerLocation
        )

        // Draw Driver Live Scooter Circle Pulse
        drawCircle(
            color = GreenPrimary.copy(alpha = 0.3f),
            radius = 38f,
            center = currentDriverPos
        )
        drawCircle(
            color = GreenPrimary,
            radius = 22f,
            center = currentDriverPos
        )
        drawCircle(
            color = Color.White,
            radius = 8f,
            center = currentDriverPos
        )
    }
}
