package com.example.usagestatisticsapp.network

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

/**
 * API model matching the backend UsageData entity structure
 * Used for sending usage data to the GDP-B web dashboard
 */
data class UsageDataRequest(
    @SerializedName("tenantId")
    val tenantId: String? = null, // Will be set by backend
    
    @SerializedName("userId")
    val userId: Long? = null, // Will be set by backend
    
    @SerializedName("deviceId")
    val deviceId: String,
    
    @SerializedName("appPackageName")
    val appPackageName: String,
    
    @SerializedName("appName")
    val appName: String? = null,
    
    @SerializedName("category")
    val category: String? = null,
    
    @SerializedName("usageTimeMs")
    val usageTimeMs: Long,
    
    @SerializedName("timestamp")
    val timestamp: String, // ISO 8601 format
    
    @SerializedName("lastTimeUsed")
    val lastTimeUsed: String? = null,
    
    @SerializedName("firstTimeStamp")
    val firstTimeStamp: String? = null,
    
    @SerializedName("launchCount")
    val launchCount: Int? = null,
    
    @SerializedName("totalTimeInForeground")
    val totalTimeInForeground: Long? = null,
    
    @SerializedName("sessionId")
    val sessionId: String? = null,
    
    @SerializedName("interactionType")
    val interactionType: String? = null,
    
    @SerializedName("screenTimeMinutes")
    val screenTimeMinutes: Double? = null,
    
    @SerializedName("productivityScore")
    val productivityScore: Double? = null,
    
    @SerializedName("economicValue")
    val economicValue: Double? = null
)

/**
 * Response models for API calls
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

data class MessageResponse(
    val message: String
)

/**
 * Authentication models
 */
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val type: String = "Bearer",
    val id: Long,
    val username: String,
    val email: String,
    val roles: List<String>,
    val tenantId: String
)
