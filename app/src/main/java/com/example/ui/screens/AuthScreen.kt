package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (name: String, email: String, phone: String, password: String, role: UserRole) -> Unit,
    authError: String? = null,
    onClearError: () -> Unit = {}
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var selectedRole   by remember { mutableStateOf(UserRole.CUSTOMER) }

    var name           by remember { mutableStateOf("") }
    var email          by remember { mutableStateOf("") }
    var phone          by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var confirmPass    by remember { mutableStateOf("") }
    var showPassword   by remember { mutableStateOf(false) }
    var showConfirmPass by remember { mutableStateOf(false) }

    // local validation error (client-side only)
    var localError by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor       = Color(0xFF1E1E1E),
        unfocusedTextColor     = Color(0xFF1E1E1E),
        focusedPlaceholderColor   = Color(0xFF737880),
        unfocusedPlaceholderColor = Color(0xFF737880),
        focusedLabelColor      = GreenPrimary,
        unfocusedLabelColor    = Color(0xFF555555),
        focusedLeadingIconColor  = GreenPrimary,
        unfocusedLeadingIconColor = Color(0xFF737880),
        focusedTrailingIconColor  = GreenPrimary,
        unfocusedTrailingIconColor = Color(0xFF737880),
        focusedBorderColor     = GreenPrimary,
        unfocusedBorderColor   = Color(0xFFD0D5DD),
        unfocusedContainerColor = Color.White,
        focusedContainerColor  = Color.White,
        cursorColor            = GreenPrimary
    )

    // Roles available for registration (not ADMIN)
    val registerableRoles = listOf(UserRole.CUSTOMER, UserRole.COOK, UserRole.DELIVERY)
    val roleLabel: (UserRole) -> String = { role ->
        when (role) {
            UserRole.CUSTOMER -> "Customer"
            UserRole.COOK     -> "Cook"
            UserRole.DELIVERY -> "Delivery"
            UserRole.ADMIN    -> "Admin"
        }
    }

    fun validate(): Boolean {
        localError = null
        if (isRegisterMode) {
            if (name.isBlank())     { localError = "Full name is required."; return false }
            if (email.isBlank())    { localError = "Email is required."; return false }
            if (phone.isBlank())    { localError = "Phone number is required."; return false }
            if (password.length < 6) { localError = "Password must be at least 6 characters."; return false }
            if (password != confirmPass) { localError = "Passwords do not match."; return false }
        } else {
            if (email.isBlank())    { localError = "Email is required."; return false }
            if (password.isBlank()) { localError = "Password is required."; return false }
        }
        return true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF0FDF4), Color(0xFFE8F5E9), Color(0xFFF8F9FA))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Logo ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF10B981), Color(0xFF047857))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.RestaurantMenu,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isRegisterMode) "Create Account" else "Welcome Back",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isRegisterMode)
                    "Join the HomeChef community today"
                else
                    "Sign in to order delicious homemade meals",
                fontSize = 13.5.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Form Card ─────────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // ── Register-only fields ──────────────────────────────────
                    AnimatedVisibility(visible = isRegisterMode, enter = fadeIn(), exit = fadeOut()) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            OutlinedTextField(
                                value       = name,
                                onValueChange = { name = it; onClearError(); localError = null },
                                label       = { Text("Full Name") },
                                placeholder = { Text("Enter your full name") },
                                leadingIcon = { Icon(Icons.Filled.Person, null) },
                                modifier    = Modifier.fillMaxWidth(),
                                shape       = RoundedCornerShape(14.dp),
                                colors      = fieldColors,
                                singleLine  = true
                            )

                            OutlinedTextField(
                                value       = phone,
                                onValueChange = { phone = it; onClearError(); localError = null },
                                label       = { Text("Phone Number") },
                                placeholder = { Text("Enter your phone number") },
                                leadingIcon = { Icon(Icons.Filled.Phone, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier    = Modifier.fillMaxWidth(),
                                shape       = RoundedCornerShape(14.dp),
                                colors      = fieldColors,
                                singleLine  = true
                            )

                            // Role selector (no ADMIN option)
                            Text(
                                text = "I am signing up as:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF374151)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                registerableRoles.forEach { role ->
                                    val isSelected = selectedRole == role
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) GreenPrimary else Color(0xFFF1F5F9)
                                            )
                                            .clickable { selectedRole = role }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = when (role) {
                                                    UserRole.CUSTOMER -> Icons.Default.Person
                                                    UserRole.COOK     -> Icons.Default.Restaurant
                                                    UserRole.DELIVERY -> Icons.Default.TwoWheeler
                                                    else              -> Icons.Default.Person
                                                },
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else Color(0xFF64748B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = roleLabel(role),
                                                color = if (isSelected) Color.White else Color(0xFF64748B),
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Email ─────────────────────────────────────────────────
                    OutlinedTextField(
                        value       = email,
                        onValueChange = { email = it; onClearError(); localError = null },
                        label       = { Text("Email Address") },
                        placeholder = { Text("Enter your email") },
                        leadingIcon = { Icon(Icons.Filled.Email, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier    = Modifier.fillMaxWidth(),
                        shape       = RoundedCornerShape(14.dp),
                        colors      = fieldColors,
                        singleLine  = true
                    )

                    // ── Password ──────────────────────────────────────────────
                    OutlinedTextField(
                        value       = password,
                        onValueChange = { password = it; onClearError(); localError = null },
                        label       = { Text("Password") },
                        placeholder = { Text("Enter your password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier    = Modifier.fillMaxWidth(),
                        shape       = RoundedCornerShape(14.dp),
                        colors      = fieldColors,
                        singleLine  = true
                    )

                    // ── Confirm Password (register only) ──────────────────────
                    AnimatedVisibility(visible = isRegisterMode, enter = fadeIn(), exit = fadeOut()) {
                        OutlinedTextField(
                            value       = confirmPass,
                            onValueChange = { confirmPass = it; localError = null },
                            label       = { Text("Confirm Password") },
                            placeholder = { Text("Re-enter your password") },
                            leadingIcon = { Icon(Icons.Filled.LockOpen, null) },
                            trailingIcon = {
                                IconButton(onClick = { showConfirmPass = !showConfirmPass }) {
                                    Icon(
                                        imageVector = if (showConfirmPass) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (showConfirmPass) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier    = Modifier.fillMaxWidth(),
                            shape       = RoundedCornerShape(14.dp),
                            colors      = fieldColors,
                            singleLine  = true
                        )
                    }

                    // ── Error Banner ──────────────────────────────────────────
                    val displayError = localError ?: authError
                    AnimatedVisibility(visible = displayError != null, enter = fadeIn(), exit = fadeOut()) {
                        displayError?.let { err ->
                            Card(
                                shape  = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = err,
                                        fontSize = 13.sp,
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // ── Submit Button ─────────────────────────────────────────
                    Button(
                        onClick = {
                            if (validate()) {
                                if (isRegisterMode) {
                                    onRegister(name.trim(), email.trim(), phone.trim(), password, selectedRole)
                                } else {
                                    onLogin(email.trim(), password)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape  = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary
                        )
                    ) {
                        Text(
                            text = if (isRegisterMode) "Create Account" else "Sign In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Toggle login / register ────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRegisterMode) "Already have an account? " else "Don't have an account? ",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = if (isRegisterMode) "Sign In" else "Sign Up",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary,
                    modifier = Modifier.clickable {
                        isRegisterMode = !isRegisterMode
                        localError = null
                        onClearError()
                    }
                )
            }
        }
    }
}
