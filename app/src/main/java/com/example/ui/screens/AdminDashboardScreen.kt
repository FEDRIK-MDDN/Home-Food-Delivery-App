package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    onToggleSuspendUser: (userId: String) -> Unit,
    onAddUser: (
        name: String,
        email: String,
        phone: String,
        password: String,
        role: UserRole,
        isApprovedCook: Boolean,
        onResult: (errorMessage: String?) -> Unit
    ) -> Unit,
    onSendPasswordReset: (email: String, onResult: (String?) -> Unit) -> Unit = { _, _ -> },
    onDeleteUser: (userId: String, onResult: (String?) -> Unit) -> Unit = { _, _ -> },
    onUpdateUser: (
        userId: String,
        name: String,
        email: String,
        phone: String,
        role: UserRole,
        isApprovedCook: Boolean,
        isSuspended: Boolean,
        onResult: (errorMessage: String?) -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("Pending Cooks") }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<User?>(null) }
    var userToEdit by remember { mutableStateOf<User?>(null) }

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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddUserDialog = true },
                containerColor = GreenPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.PersonAdd, contentDescription = "Add User") },
                text = { Text("Add User", fontWeight = FontWeight.Bold) }
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
                        Text(
                            text = "$${String.format("%.2f", totalRevenue)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GreenPrimary
                        )
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
                        Text(
                            text = "${users.size}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
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
                        Text(
                            text = "${pendingCooks.size}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD97706)
                        )
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
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(users, key = { it.userId }) { user ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = user.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                            UserRoleBadge(role = user.role)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = user.email, fontSize = 12.sp, color = Color(0xFF64748B))
                                        if (user.phone.isNotBlank()) {
                                            Text(text = "Phone: ${user.phone}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                        }
                                    }

                                    if (user.role != UserRole.ADMIN) {
                                        OutlinedButton(
                                            onClick = { onToggleSuspendUser(user.userId) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (user.isSuspended) "Unsuspend" else "Suspend",
                                                color = if (user.isSuspended) GreenPrimary else Color(0xFFDC2626),
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }
                                }

                                if (user.role != UserRole.ADMIN) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilledTonalButton(
                                            onClick = { userToEdit = user },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = GreenContainer,
                                                contentColor = GreenPrimary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Filled.Edit, null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Update", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        TextButton(
                                            onClick = {
                                                onSendPasswordReset(user.email) { err ->
                                                    if (err == null) {
                                                        Toast.makeText(context, "Password reset link sent to ${user.email}", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Filled.LockReset, null, modifier = Modifier.size(14.dp), tint = GreenPrimary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reset Pass", fontSize = 11.5.sp, color = GreenPrimary)
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        TextButton(
                                            onClick = { userToDelete = user },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Filled.DeleteOutline, null, modifier = Modifier.size(14.dp), tint = Color(0xFFEF4444))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Delete", fontSize = 11.5.sp, color = Color(0xFFEF4444))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddUserDialog) {
        AdminAddUserDialog(
            onDismiss = { showAddUserDialog = false },
            onCreateUser = onAddUser
        )
    }

    // Delete Confirmation Dialog
    userToDelete?.let { targetUser ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete User?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove ${targetUser.name} (${targetUser.email}) from the system?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(targetUser.userId) { err ->
                            userToDelete = null
                            if (err == null) {
                                Toast.makeText(context, "User removed successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit User Dialog
    userToEdit?.let { targetUser ->
        AdminEditUserDialog(
            user = targetUser,
            onDismiss = { userToEdit = null },
            onSave = onUpdateUser
        )
    }
}

@Composable
private fun UserRoleBadge(role: UserRole) {
    val (bgColor, textColor, label) = when (role) {
        UserRole.CUSTOMER -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), "Customer")
        UserRole.COOK     -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Cook")
        UserRole.DELIVERY -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Delivery")
        UserRole.ADMIN    -> Triple(Color(0xFFF3E8FF), Color(0xFF7E22CE), "Admin")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

data class CreatedAccountDetails(
    val name: String,
    val email: String,
    val pass: String,
    val role: UserRole
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddUserDialog(
    onDismiss: () -> Unit,
    onCreateUser: (
        name: String,
        email: String,
        phone: String,
        password: String,
        role: UserRole,
        isApprovedCook: Boolean,
        onResult: (errorMessage: String?) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.CUSTOMER) }
    var autoApproveCook by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successDetails by remember { mutableStateOf<CreatedAccountDetails?>(null) }

    // If successfully created, show confirmation card with copy option
    if (successDetails != null) {
        val details = successDetails!!
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                    }
                    Text("Account Created!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "The user can now sign in immediately using these credentials:",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row {
                                Text("Email: ", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF64748B))
                                Text(details.email, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Password: ", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF64748B))
                                Text(details.pass, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = GreenPrimary)
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(details.pass))
                                        Toast.makeText(context, "Password copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Password", modifier = Modifier.size(16.dp), tint = GreenPrimary)
                                }
                            }
                            Row {
                                Text("Role: ", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF64748B))
                                Text(details.role.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(GreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PersonAdd,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Add New User",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Assign credentials & system role",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Error Banner
                errorMessage?.let { err ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = err,
                                color = Color(0xFF991B1B),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Full Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Full Name") },
                    placeholder = { Text("e.g. John Doe") },
                    leadingIcon = { Icon(Icons.Filled.Person, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Email Address *") },
                    placeholder = { Text("user@example.com") },
                    leadingIcon = { Icon(Icons.Filled.Email, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Password *") },
                    placeholder = { Text("Min 6 characters") },
                    leadingIcon = { Icon(Icons.Filled.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                // Confirm Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text("Confirm Password *") },
                    placeholder = { Text("Re-enter password") },
                    leadingIcon = { Icon(Icons.Filled.LockOpen, null) },
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                imageVector = if (showConfirmPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (Optional)") },
                    placeholder = { Text("+1 234 567 890") },
                    leadingIcon = { Icon(Icons.Filled.Phone, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Role Selection Header
                Text(
                    text = "Select Role *",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A)
                )

                // Role Cards (2x2 grid)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoleSelectionCard(
                            modifier = Modifier.weight(1f),
                            role = UserRole.CUSTOMER,
                            title = "Customer",
                            icon = Icons.Filled.Person,
                            isSelected = selectedRole == UserRole.CUSTOMER,
                            onSelect = { selectedRole = UserRole.CUSTOMER }
                        )
                        RoleSelectionCard(
                            modifier = Modifier.weight(1f),
                            role = UserRole.COOK,
                            title = "Cook",
                            icon = Icons.Filled.RestaurantMenu,
                            isSelected = selectedRole == UserRole.COOK,
                            onSelect = { selectedRole = UserRole.COOK }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoleSelectionCard(
                            modifier = Modifier.weight(1f),
                            role = UserRole.DELIVERY,
                            title = "Delivery",
                            icon = Icons.Filled.TwoWheeler,
                            isSelected = selectedRole == UserRole.DELIVERY,
                            onSelect = { selectedRole = UserRole.DELIVERY }
                        )
                        RoleSelectionCard(
                            modifier = Modifier.weight(1f),
                            role = UserRole.ADMIN,
                            title = "Admin",
                            icon = Icons.Filled.AdminPanelSettings,
                            isSelected = selectedRole == UserRole.ADMIN,
                            onSelect = { selectedRole = UserRole.ADMIN }
                        )
                    }
                }

                // If Cook is selected: auto-approve checkbox
                if (selectedRole == UserRole.COOK) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable { autoApproveCook = !autoApproveCook }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = autoApproveCook,
                            onCheckedChange = { autoApproveCook = it },
                            colors = CheckboxDefaults.colors(checkedColor = GreenPrimary)
                        )
                        Column {
                            Text(
                                text = "Pre-approve Cook Immediately",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Allows cook to publish dishes without review",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanEmail = email.trim()
                    val cleanPass = password.trim()
                    val cleanConfirm = confirmPassword.trim()

                    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                        errorMessage = "Please enter a valid email address."
                        return@Button
                    }
                    if (cleanPass.length < 6) {
                        errorMessage = "Password must be at least 6 characters."
                        return@Button
                    }
                    if (cleanPass != cleanConfirm) {
                        errorMessage = "Passwords do not match."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    onCreateUser(
                        name,
                        cleanEmail,
                        phone,
                        cleanPass,
                        selectedRole,
                        autoApproveCook
                    ) { err ->
                        isLoading = false
                        if (err != null) {
                            errorMessage = err
                        } else {
                            successDetails = CreatedAccountDetails(
                                name = name.ifBlank { "User" },
                                email = cleanEmail.lowercase(),
                                pass = cleanPass,
                                role = selectedRole
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creating...")
                } else {
                    Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Account", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )
}

@Composable
private fun RoleSelectionCard(
    modifier: Modifier = Modifier,
    role: UserRole,
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) GreenContainer else Color(0xFFF8FAFC)
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) GreenPrimary else Color(0xFFE2E8F0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) GreenPrimary else Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.5.sp,
                color = if (isSelected) GreenPrimary else Color(0xFF334155)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (
        userId: String,
        name: String,
        email: String,
        phone: String,
        role: UserRole,
        isApprovedCook: Boolean,
        isSuspended: Boolean,
        onResult: (errorMessage: String?) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(user.name) }
    var email by remember { mutableStateOf(user.email) }
    var phone by remember { mutableStateOf(user.phone) }
    var selectedRole by remember { mutableStateOf(user.role) }
    var isApprovedCook by remember { mutableStateOf(user.isApprovedCook) }
    var isSuspended by remember { mutableStateOf(user.isSuspended) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(GreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Update User",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Modify details, role & permissions",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Error Banner
                errorMessage?.let { err ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = err,
                                color = Color(0xFF991B1B),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Full Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g. John Doe") },
                    leadingIcon = { Icon(Icons.Filled.Person, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                // Email Address
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Email Address *") },
                    placeholder = { Text("user@example.com") },
                    leadingIcon = { Icon(Icons.Filled.Email, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+1 234 567 890") },
                    leadingIcon = { Icon(Icons.Filled.Phone, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Role Selection Header
                Text(
                    text = "User Role *",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A)
                )

                // Role Cards (2x2 grid)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoleSelectionCard(
                            modifier = Modifier.weight(1f),
                            role = UserRole.CUSTOMER,
                            title = "Customer",
                            icon = Icons.Filled.Person,
                            isSelected = selectedRole == UserRole.CUSTOMER,
                            onSelect = { selectedRole = UserRole.CUSTOMER }
                        )
                        RoleSelectionCard(
                            modifier = Modifier.weight(1f),
                            role = UserRole.COOK,
                            title = "Cook",
                            icon = Icons.Filled.RestaurantMenu,
                            isSelected = selectedRole == UserRole.COOK,
                            onSelect = { selectedRole = UserRole.COOK }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoleSelectionCard(
                            modifier = Modifier.weight(1f),
                            role = UserRole.DELIVERY,
                            title = "Delivery",
                            icon = Icons.Filled.TwoWheeler,
                            isSelected = selectedRole == UserRole.DELIVERY,
                            onSelect = { selectedRole = UserRole.DELIVERY }
                        )
                        RoleSelectionCard(
                            modifier = Modifier.weight(1f),
                            role = UserRole.ADMIN,
                            title = "Admin",
                            icon = Icons.Filled.AdminPanelSettings,
                            isSelected = selectedRole == UserRole.ADMIN,
                            onSelect = { selectedRole = UserRole.ADMIN }
                        )
                    }
                }

                // If Cook: Approved status
                if (selectedRole == UserRole.COOK) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable { isApprovedCook = !isApprovedCook }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isApprovedCook,
                            onCheckedChange = { isApprovedCook = it },
                            colors = CheckboxDefaults.colors(checkedColor = GreenPrimary)
                        )
                        Column {
                            Text(
                                text = "Approved Cook Status",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Allow cook to publish and manage menu items",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Suspended toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSuspended) Color(0xFFFEE2E2) else Color(0xFFF1F5F9))
                        .clickable { isSuspended = !isSuspended }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isSuspended,
                        onCheckedChange = { isSuspended = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFDC2626))
                    )
                    Column {
                        Text(
                            text = if (isSuspended) "Account Suspended" else "Active Account",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (isSuspended) Color(0xFF991B1B) else Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isSuspended) "User is blocked from placing orders or actions" else "User has full access",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Name cannot be empty."
                        return@Button
                    }
                    if (email.isBlank() || !email.contains("@")) {
                        errorMessage = "Please enter a valid email."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    onSave(
                        user.userId,
                        name.trim(),
                        email.trim(),
                        phone.trim(),
                        selectedRole,
                        isApprovedCook,
                        isSuspended
                    ) { err ->
                        isLoading = false
                        if (err != null) {
                            errorMessage = err
                        } else {
                            Toast.makeText(context, "User updated successfully!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )
}
