package com.example.usagestatisticsapp.network

import com.example.usagestatisticsapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Network configuration for GDP-B backend communication
 */
object NetworkConfig {
    
    // Backend URL - configure this for your deployment environment
    private val BASE_URL = if (BuildConfig.DEBUG) {
        // For development: Use emulator localhost (10.0.2.2) or your machine's IP
        // Change to your computer's IP address if testing on physical device
        "http://10.0.2.2:8080/api/"
    } else {
        // For production: Use your deployed backend URL
        "https://your-production-backend.com/api/"
    }
    // Note: 10.0.2.2 is the Android emulator's way to access host machine's localhost
    // For physical device testing, replace with your computer's IP (e.g., "http://192.168.1.100:8080/api/")
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
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
    suspend fun submitUsageDataAnonymous(usageDataList: List<UsageDataRequest>): Result<List<UsageDataRequest>> {
        return try {
            val response = apiService.submitUsageDataAnonymous(usageDataList)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Response body is null"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Submit usage data with Study ID (automatically creates participant record if needed)
     */
    suspend fun submitUsageDataWithStudyId(studyId: String, usageDataList: List<UsageDataRequest>): Result<List<UsageDataRequest>> {
        return try {
            val response = apiService.submitUsageDataWithStudyId(studyId, usageDataList)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Response body is null"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
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
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Login response body is null"))
                }
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
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Registration response body is null"))
                }
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
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Health check response body is null"))
                }
            } else {
                Result.failure(Exception("Health check failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
