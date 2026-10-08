package com.example.data.model

data class PromoCode(
    val promoId: String,
    val cookId: String,
    val cookName: String,
    val code: String,
    val title: String,
    val description: String,
    val discountType: String = "PERCENTAGE", // "PERCENTAGE" or "FIXED"
    val discountValue: Double = 10.0,
    val minOrderValue: Double = 0.0,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000, // default 7 days
    val isActive: Boolean = true,
    val usageCount: Int = 0,
    val maxUsageLimit: Int = 50,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isExpired: Boolean
        get() = if (endDate > 0L) System.currentTimeMillis() > endDate else false

    val discountLabel: String
        get() = if (discountType == "PERCENTAGE") {
            if (discountValue % 1.0 == 0.0) "${discountValue.toInt()}% OFF" else "${String.format("%.1f", discountValue)}% OFF"
        } else {
            "$${String.format("%.2f", discountValue)} OFF"
        }

    val isUsable: Boolean
        get() = isActive && !isExpired && (maxUsageLimit == 0 || usageCount < maxUsageLimit)
}
