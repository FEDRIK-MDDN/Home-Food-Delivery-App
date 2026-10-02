package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderStatus
import com.example.ui.theme.CoralPrice

@Composable
fun StatusProgressTracker(status: OrderStatus) {
    val steps = listOf("Order received", "Headed to pickup", "Food's on the way", "Arriving soon")

    val activeStepIndex = when (status) {
        OrderStatus.PENDING, OrderStatus.ACCEPTED -> 0
        OrderStatus.PREPARING, OrderStatus.READY -> 1
        OrderStatus.PICKED_UP, OrderStatus.OUT_FOR_DELIVERY -> 2
        OrderStatus.DELIVERED, OrderStatus.COMPLETED -> 3
        else -> 0
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Step bar container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFEFEFEF)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, _ ->
                val isCompleted = index <= activeStepIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCompleted) CoralPrice else Color(0xFFE0E0E0))
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, label ->
                val isActive = index == activeStepIndex
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) Color(0xFF1E1E1E) else Color(0xFF9EA3AE),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
