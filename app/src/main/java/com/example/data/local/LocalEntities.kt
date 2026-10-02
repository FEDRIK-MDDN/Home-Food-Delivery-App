package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val foodId: String,
    val cookId: String,
    val cookName: String,
    val title: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val quantity: Int,
    val specialRequest: String
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val foodId: String
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    val photoUrl: String,
    val address: String
)

/** Full user record stored in DB — includes hashed password */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String,          // unique login key
    val passwordHash: String,   // SHA-256 hex
    val phone: String,
    val role: String,           // CUSTOMER / COOK / DELIVERY / ADMIN
    val photoUrl: String = "",
    val address: String = "",
    val isApprovedCook: Boolean = false,
    val isSuspended: Boolean = false
)
