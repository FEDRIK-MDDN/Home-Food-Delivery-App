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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryIssue
import com.example.data.model.DeliveryIssueCategories
import com.example.data.model.Order
import com.example.data.model.User
import com.example.ui.components.CreateDeliveryIssueDialog
import com.example.ui.components.DeleteDeliveryIssueDialog
import com.example.ui.components.EditDeliveryIssueDialog
import com.example.ui.theme.GreenContainer
import com.example.ui.theme.GreenPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryIssuesScreen(
    currentUser: User,
    issues: List<DeliveryIssue>,
    claimedOrders: List<Order>,
    preselectedOrderId: String? = null,
    onCreateIssue: (orderId: String?, category: String, description: String, priority: String, onResult: (String?) -> Unit) -> Unit,
    onUpdateIssue: (issueId: String, category: String, description: String, priority: String, onResult: (String?) -> Unit) -> Unit,
    onDeleteIssue: (issueId: String, onResult: (String?) -> Unit) -> Unit,
    onBackClick: () -> Unit
) {
    val tabs = listOf("All", "Open", "Resolved")
    var selectedTab by remember { mutableStateOf("All") }

    var showCreateDialog by remember { mutableStateOf(preselectedOrderId != null) }
    var issueToEdit by remember { mutableStateOf<DeliveryIssue?>(null) }
    var issueToDelete by remember { mutableStateOf<DeliveryIssue?>(null) }

    val filteredIssues = remember(issues, selectedTab) {
        when (selectedTab) {
            "Open"     -> issues.filter { it.status == "OPEN" }
            "Resolved" -> issues.filter { it.status == "RESOLVED" }
            else       -> issues
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "My Delivery Issues (${filteredIssues.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Report & track issues during your deliveries",
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
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Report Issue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color(0xFFDC2626),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AddAlert, contentDescription = null) },
                text = { Text("Report Issue", fontWeight = FontWeight.Bold) }
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
                        modifier = Modifier.clickable { selectedTab = tab }
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

            if (filteredIssues.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(GreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Text(
                            text = if (selectedTab == "All") "No Delivery Issues Reported" else "No $selectedTab Issues",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Everything on your delivery routes is running smoothly! If you face any issues with a customer or route, tap below to report.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit an Issue Report")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredIssues, key = { it.issueId }) { issue ->
                        DeliveryIssueCard(
                            issue = issue,
                            onEditClick = { issueToEdit = issue },
                            onDeleteClick = { issueToDelete = issue }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateDeliveryIssueDialog(
            claimedOrders = claimedOrders,
            preselectedOrderId = preselectedOrderId,
            onDismiss = { showCreateDialog = false },
            onSubmit = { orderId, category, description, priority, onResult ->
                onCreateIssue(orderId, category, description, priority, onResult)
            }
        )
    }

    issueToEdit?.let { issue ->
        EditDeliveryIssueDialog(
            issue = issue,
            onDismiss = { issueToEdit = null },
            onSave = { issueId, category, description, priority, onResult ->
                onUpdateIssue(issueId, category, description, priority, onResult)
            }
        )
    }

    issueToDelete?.let { issue ->
        DeleteDeliveryIssueDialog(
            issue = issue,
            onDismiss = { issueToDelete = null },
            onConfirmDelete = { issueId, onResult ->
                onDeleteIssue(issueId, onResult)
            }
        )
    }
}



@Composable
fun DeliveryIssueCard(
    issue: DeliveryIssue,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(issue.createdAt) { dateFormat.format(Date(issue.createdAt)) }

    val categoryIcon: ImageVector = when (issue.category) {
        DeliveryIssueCategories.CUSTOMER_NOT_ANSWERING -> Icons.Default.PhoneMissed
        DeliveryIssueCategories.WRONG_ADDRESS          -> Icons.Default.LocationOff
        DeliveryIssueCategories.RESTAURANT_DELAY       -> Icons.Default.Schedule
        DeliveryIssueCategories.VEHICLE_BREAKDOWN      -> Icons.Default.TwoWheeler
        DeliveryIssueCategories.DAMAGED_PACKAGE        -> Icons.Default.ShoppingBag
        DeliveryIssueCategories.TRAFFIC_WEATHER        -> Icons.Default.Thunderstorm
        DeliveryIssueCategories.SAFETY_CONCERN         -> Icons.Default.Security
        else                                           -> Icons.Default.ReportProblem
    }

    val (priorityColor, priorityBg) = when (issue.priority) {
        "URGENT" -> Color(0xFFDC2626) to Color(0xFFFEE2E2)
        "HIGH"   -> Color(0xFFEA580C) to Color(0xFFFFEDD5)
        "MEDIUM" -> Color(0xFF2563EB) to Color(0xFFDBEAFE)
        else     -> Color(0xFF64748B) to Color(0xFFF1F5F9)
    }

    val (statusColor, statusBg) = when (issue.status) {
        "OPEN"      -> Color(0xFFD97706) to Color(0xFFFEF3C7)
        "RESOLVED"  -> Color(0xFF059669) to Color(0xFFD1FAE5)
        "WITHDRAWN" -> Color(0xFF64748B) to Color(0xFFF1F5F9)
        else        -> Color(0xFF2563EB) to Color(0xFFDBEAFE)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("issue_item_${issue.issueId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Category with icon + Status pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF2F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = issue.category,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "#${issue.issueId} • $formattedDate",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBg
                ) {
                    Text(
                        text = if (issue.status == "OPEN") "Under Review" else issue.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges Row: Linked Order (if any) + Priority
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!issue.orderId.isNullOrEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(13.dp))
                            Text(
                                text = "Order #${issue.orderId}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = priorityBg
                ) {
                    Text(
                        text = "${issue.priority} PRIORITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Description Body
            Text(
                text = issue.description,
                fontSize = 13.sp,
                color = Color(0xFF334155),
                lineHeight = 18.sp
            )

            // Actions for OPEN issues: Edit and Withdraw/Delete
            if (issue.status == "OPEN") {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Update / Edit button (matches user graphic orange pencil)
                    OutlinedButton(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEA580C)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Issue", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Delete / Withdraw button (matches user graphic red trash)
                    OutlinedButton(
                        onClick = onDeleteClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Withdraw", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
