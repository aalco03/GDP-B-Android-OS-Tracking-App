package com.example.usagestatisticsapp.ui

import com.example.usagestatisticsapp.data.UserProfile

data class UserSetupUiState(
    val userId: String = "",
    val isUserIdValid: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSetupComplete: Boolean = false,
    val userProfile: UserProfile? = null
)
