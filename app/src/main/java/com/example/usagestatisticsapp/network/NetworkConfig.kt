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
    val BASE_URL = if (BuildConfig.DEBUG) {
        // TEMPORARY: Using ngrok tunnel for network connectivity
        // This bypasses local network routing issues
        "https://d66c05fa0e82.ngrok-free.app/"
        
        // Previous attempts:
        // "http://172.20.4.49:8080/" // Failed due to EHOSTUNREACH
        // getLocalServerUrl() // Device detection approach
    } else {
        // For production: Use your deployed backend URL
        "https://your-production-backend.com/"
    }
    
    /**
     * Get the appropriate server URL based on device type
     * Returns emulator URL for emulators, physical device URL for real devices
     */
    private fun getLocalServerUrl(): String {
        // Check if running on emulator by looking for emulator-specific properties
        val isEmulator = android.os.Build.FINGERPRINT.startsWith("generic") ||
                android.os.Build.FINGERPRINT.startsWith("unknown") ||
                android.os.Build.MODEL.contains("google_sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.MODEL.contains("Android SDK built for x86") ||
                android.os.Build.MANUFACTURER.contains("Genymotion") ||
                (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
        
        val url = if (isEmulator) {
            // Emulator: Use special IP to access host machine
            "http://10.0.2.2:8080/"
        } else {
            // Physical device: Use computer's actual IP address on local network
            // Update this IP if your computer's IP changes
            "http://172.20.4.49:8080/"
        }
        
        // Debug logging
        android.util.Log.d("NetworkConfig", "Device detection - isEmulator: $isEmulator")
        android.util.Log.d("NetworkConfig", "Build.FINGERPRINT: ${android.os.Build.FINGERPRINT}")
        android.util.Log.d("NetworkConfig", "Build.MODEL: ${android.os.Build.MODEL}")
        android.util.Log.d("NetworkConfig", "Build.MANUFACTURER: ${android.os.Build.MANUFACTURER}")
        android.util.Log.d("NetworkConfig", "Build.BRAND: ${android.os.Build.BRAND}")
        android.util.Log.d("NetworkConfig", "Build.DEVICE: ${android.os.Build.DEVICE}")
        android.util.Log.d("NetworkConfig", "Selected base URL: $url")
        
        return url
    }
    
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
            android.util.Log.d("ApiRepository", "Starting health check to: ${NetworkConfig.BASE_URL}actuator/health")
            val response = apiService.healthCheck()
            android.util.Log.d("ApiRepository", "Health check response code: ${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    android.util.Log.d("ApiRepository", "Health check successful: ${body.status}")
                    Result.success(body)
                } else {
                    android.util.Log.e("ApiRepository", "Health check response body is null")
                    Result.failure(Exception("Health check response body is null"))
                }
            } else {
                android.util.Log.e("ApiRepository", "Health check failed with code: ${response.code()}")
                Result.failure(Exception("Health check failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ApiRepository", "Health check exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Submit participant demographics
     */
    suspend fun submitDemographics(request: DemographicsRequest): Result<DemographicsResponse> {
        return try {
            android.util.Log.d("ApiRepository", "Submitting demographics for Study ID: ${request.studyId}")
            val response = apiService.submitDemographics(request)
            android.util.Log.d("ApiRepository", "Demographics response code: ${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    android.util.Log.d("ApiRepository", "Demographics submitted successfully: ${body.message}")
                    Result.success(body)
                } else {
                    android.util.Log.e("ApiRepository", "Demographics response body is null")
                    Result.failure(Exception("Demographics response body is null"))
                }
            } else {
                android.util.Log.e("ApiRepository", "Demographics submission failed with code: ${response.code()}")
                Result.failure(Exception("Demographics submission failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ApiRepository", "Demographics submission exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
