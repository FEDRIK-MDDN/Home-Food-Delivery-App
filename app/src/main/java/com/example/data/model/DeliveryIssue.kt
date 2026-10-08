package com.example.data.model

data class DeliveryIssue(
    val issueId: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val orderId: String? = null,
    val category: String = DeliveryIssueCategories.CUSTOMER_NOT_ANSWERING,
    val description: String = "",
    val priority: String = "MEDIUM", // "LOW", "MEDIUM", "HIGH", "URGENT"
    val status: String = "OPEN",     // "OPEN", "RESOLVED", "WITHDRAWN"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

object DeliveryIssueCategories {
    const val CUSTOMER_NOT_ANSWERING = "Customer not answering"
    const val WRONG_ADDRESS = "Incorrect / Unreachable address"
    const val RESTAURANT_DELAY = "Restaurant / Cook preparation delay"
    const val VEHICLE_BREAKDOWN = "Vehicle breakdown / Flat tire"
    const val DAMAGED_PACKAGE = "Damaged / Spilled packaging"
    const val TRAFFIC_WEATHER = "Severe traffic / Weather issue"
    const val SAFETY_CONCERN = "Safety concern"
    const val OTHER = "Other delivery issue"

    val ALL = listOf(
        CUSTOMER_NOT_ANSWERING,
        WRONG_ADDRESS,
        RESTAURANT_DELAY,
        VEHICLE_BREAKDOWN,
        DAMAGED_PACKAGE,
        TRAFFIC_WEATHER,
        SAFETY_CONCERN,
        OTHER
    )
}
