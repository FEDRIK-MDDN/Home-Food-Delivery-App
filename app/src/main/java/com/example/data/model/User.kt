package com.example.data.model

data class User(
    val userId: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole = UserRole.CUSTOMER,
    val photoUrl: String = "",
    val address: String = "123 Green Street, Tech City",
    val isApprovedCook: Boolean = true,
    val isSuspended: Boolean = false
)
