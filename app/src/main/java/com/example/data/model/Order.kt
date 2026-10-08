package com.example.data.model

data class Order(
    val orderId: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val cookId: String,
    val cookName: String,
    val deliveryId: String? = null,
    val deliveryPartnerName: String? = null,
    val items: List<CartItem>,
    val subtotal: Double,
    val deliveryFee: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double,
    val status: OrderStatus = OrderStatus.PENDING,
    val paymentMethod: String = "Card",
    val notesForCook: String = "",
    val rejectReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val estimatedMinutes: Int = 25,
    val cancelledDriverIds: List<String> = emptyList()
)
