package com.example.data.model

data class CartItem(
    val foodId: String,
    val cookId: String,
    val cookName: String,
    val title: String,
    val description: String = "",
    val price: Double,
    val imageUrl: String,
    var quantity: Int = 1,
    var specialRequest: String = ""
)
