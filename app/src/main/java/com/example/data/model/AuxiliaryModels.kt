package com.example.data.model

data class Review(
    val reviewId: String,
    val foodId: String,
    val customerId: String,
    val customerName: String,
    val rating: Double,
    val comment: String,
    val photoUrl: String? = null,
    val cookReply: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class NotificationItem(
    val notificationId: String,
    val userId: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = "ORDER"
)

data class CookProfile(
    val cookId: String,
    val storeName: String,
    val bio: String,
    val rating: Double = 4.9,
    val totalOrders: Int = 340,
    val isVerified: Boolean = true,
    val isAvailable: Boolean = true,
    val deliveryRadiusKm: Double = 8.0,
    val photoUrl: String = ""
)

data class Category(
    val id: String,
    val name: String,
    val iconName: String = "fastfood"
)
