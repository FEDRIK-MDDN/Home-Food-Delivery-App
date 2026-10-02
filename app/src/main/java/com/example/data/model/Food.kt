package com.example.data.model

data class Food(
    val foodId: String,
    val cookId: String,
    val cookName: String,
    val isCookVerified: Boolean = true,
    val title: String,
    val description: String,
    val price: Double,
    val discountPrice: Double? = null,
    val category: String,
    val imageUrl: String,
    val ingredients: List<String> = emptyList(),
    val allergens: List<String> = emptyList(),
    val calories: Int = 350,
    val preparationTimeMins: Int = 20,
    val rating: Double = 4.8,
    val reviewCount: Int = 124,
    val availableQuantity: Int = 25,
    val isAvailable: Boolean = true,
    val deliveryFee: Double = 0.0,
    val isFeatured: Boolean = false,
    val isReorder: Boolean = false
)
