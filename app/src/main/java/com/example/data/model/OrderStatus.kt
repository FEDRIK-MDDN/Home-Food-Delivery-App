package com.example.data.model

enum class OrderStatus(val label: String) {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    PREPARING("Preparing"),
    READY("Ready"),
    PICKED_UP("Picked Up"),
    OUT_FOR_DELIVERY("Out For Delivery"),
    DELIVERED("Delivered"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}
