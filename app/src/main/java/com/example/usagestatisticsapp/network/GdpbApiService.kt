package com.example.usagestatisticsapp.network

import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service interface for GDP-B backend communication
 */
interface GdpbApiService {
    
    /**
     * Submit usage data to the backend (for authenticated scientists)
     */
    @POST("api/usage/submit")
    suspend fun submitUsageData(
        @Header("Authorization") token: String,
        @Body usageDataList: List<UsageDataRequest>
    ): Response<List<UsageDataRequest>>
    
    /**
     * Submit usage data anonymously (for study participants)
     */
    @POST("api/usage/submit-anonymous")
    suspend fun submitUsageDataAnonymous(@Body usageDataList: List<UsageDataRequest>): Response<List<UsageDataRequest>>
    
    /**
     * Submit usage data with Study ID (automatically creates participant record if needed)
     */
    @POST("api/usage/submit-with-study-id")
    suspend fun submitUsageDataWithStudyId(
        @Query("studyId") studyId: String,
        @Body usageDataList: List<UsageDataRequest>
    ): Response<List<UsageDataRequest>>
    
    /**
     * Get user's own usage data
     */
    @GET("api/usage/my-data")
    suspend fun getMyUsageData(
        @Header("Authorization") token: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<List<UsageDataRequest>>
    
    /**
     * Authentication endpoints
     */
    @POST("api/auth/signin")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>
    
    @POST("api/auth/signup")
    suspend fun register(@Body signupRequest: SignupRequest): Response<MessageResponse>
    
    /**
     * Health check endpoint (absolute path since it's not under /api)
     */
    @GET("actuator/health")
    suspend fun healthCheck(): Response<HealthResponse>
    
    /**
     * Submit participant demographics (anonymous - no authentication required)
     */
    @POST("api/usage/update-demographics-anonymous")
    suspend fun submitDemographics(@Body request: DemographicsRequest): Response<DemographicsResponse>
}

/**
 * Additional request/response models
 */
data class SignupRequest(
    val username: String,
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val roles: List<String> = listOf("ROLE_BASE")
)

data class HealthResponse(
    val status: String
)
