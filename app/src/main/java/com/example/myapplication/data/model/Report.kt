package com.example.myapplication.data.model

import com.google.firebase.Timestamp

data class Report(
    val reportId: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val wasteType: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val description: String = "",
    val status: String = "Pending", // Pending or Cleaned
    val timestamp: Timestamp = Timestamp.now()
)
