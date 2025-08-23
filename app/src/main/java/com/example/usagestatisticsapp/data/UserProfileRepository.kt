package com.example.usagestatisticsapp.data

import kotlinx.coroutines.flow.Flow

class UserProfileRepository(private val userProfileDao: UserProfileDao) {
    
    fun getUserProfile(): Flow<UserProfile?> {
        return userProfileDao.getUserProfile()
    }
    
    suspend fun insertUserProfile(userProfile: UserProfile) {
        userProfileDao.insertUserProfile(userProfile)
    }
    
    suspend fun updateUserProfile(userProfile: UserProfile) {
        userProfileDao.updateUserProfile(userProfile)
    }
    
    suspend fun deleteUserProfile() {
        userProfileDao.deleteUserProfile()
    }
    
    suspend fun hasUserProfile(): Boolean {
        return userProfileDao.hasUserProfile()
    }
    
    suspend fun getAllUserProfiles(): Flow<List<UserProfile>> {
        return userProfileDao.getAllUserProfiles()
    }
}
