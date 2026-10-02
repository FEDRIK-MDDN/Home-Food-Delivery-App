package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    currentUser: User,
    users: List<User>,
    orders: List<Order>,
    onApproveCook: (userId: String) -> Unit,
    onToggleSuspendUser: (userId: String) -> Unit
) {
    var selectedTab by remember { mutableStateOf("Pending Cooks") }

    val pendingCooks = remember(users) {
        users.filter { it.role == UserRole.COOK && !it.isApprovedCook }
    }

    val totalRevenue = orders.sumOf { it.total }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Admin Portal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "System Controller",
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
            // High Level Metrics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "$${String.format("%.2f", totalRevenue)}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GreenPrimary)
                        Text(text = "Volume", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "${users.size}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                        Text(text = "Users", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "${pendingCooks.size}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD97706))
                        Text(text = "Pending", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
            }

            TabRow(
                selectedTabIndex = if (selectedTab == "Pending Cooks") 0 else 1,
                containerColor = Color.White,
                contentColor = GreenPrimary
            ) {
                Tab(
                    selected = selectedTab == "Pending Cooks",
                    onClick = { selectedTab = "Pending Cooks" },
                    text = { Text("Cook Approvals (${pendingCooks.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == "User Management",
                    onClick = { selectedTab = "User Management" },
                    text = { Text("All Users (${users.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == "Pending Cooks") {
                if (pendingCooks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No pending cook applications!", fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(pendingCooks, key = { it.userId }) { user ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                        Text(text = user.email, fontSize = 13.sp, color = Color(0xFF64748B))
                                        Text(text = "Status: Pending Kitchen Inspection", fontSize = 11.sp, color = Color(0xFFD97706))
                                    }
                                    Button(
                                        onClick = { onApproveCook(user.userId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Approve Cook")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(users, key = { it.userId }) { user ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                    Text(text = "${user.email} • Role: ${user.role.name}", fontSize = 12.sp, color = Color(0xFF64748B))
                                }

                                OutlinedButton(
                                    onClick = { onToggleSuspendUser(user.userId) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (user.isSuspended) "Unsuspend" else "Suspend",
                                        color = if (user.isSuspended) GreenPrimary else Color(0xFFDC2626),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
