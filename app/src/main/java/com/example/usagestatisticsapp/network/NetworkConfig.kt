package com.example.usagestatisticsapp.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Network configuration for GDP-B backend communication
 */
object NetworkConfig {
    
    // Backend URL - change this to your deployed backend URL when ready
    private const val BASE_URL = "http://10.0.2.2:8080/" // Android emulator localhost
    // For physical device, use: "http://YOUR_COMPUTER_IP:8080/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: GdpbApiService = retrofit.create(GdpbApiService::class.java)
}

/**
 * Repository for handling API calls and data synchronization
 */
class ApiRepository {
    
    private val apiService = NetworkConfig.apiService
    
    /**
     * Submit usage data to backend without authentication (for study participants)
     */
    suspend fun submitUsageDataAnonymous(
        usageDataList: List<UsageDataRequest>
    ): Result<List<UsageDataRequest>> {
        return try {
            val response = apiService.submitUsageDataAnonymous(usageDataList)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Login to get JWT token
     */
    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(username, password))
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Login failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Register new user
     */
    suspend fun register(
        username: String,
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<MessageResponse> {
        return try {
            val signupRequest = SignupRequest(username, email, password, firstName, lastName)
            val response = apiService.register(signupRequest)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Registration failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check backend health
     */
    suspend fun healthCheck(): Result<HealthResponse> {
        return try {
            val response = apiService.healthCheck()
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Health check failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
