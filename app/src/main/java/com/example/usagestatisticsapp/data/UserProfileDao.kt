package com.example.usagestatisticsapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfile)
    
    @Update
    suspend fun updateUserProfile(userProfile: UserProfile)
    
    @Query("DELETE FROM user_profile")
    suspend fun deleteUserProfile()
    
    @Query("SELECT EXISTS(SELECT 1 FROM user_profile LIMIT 1)")
    suspend fun hasUserProfile(): Boolean
    
    @Query("SELECT * FROM user_profile")
    fun getAllUserProfiles(): Flow<List<UserProfile>>
}
