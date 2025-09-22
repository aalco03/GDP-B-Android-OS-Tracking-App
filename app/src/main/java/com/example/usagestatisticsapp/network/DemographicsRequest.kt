package com.example.usagestatisticsapp.network

/**
 * Data class for demographics submission request
 * Matches the backend ParticipantDemographicsRequest DTO format
 */
data class DemographicsRequest(
    val studyId: String,
    val location: String?,
    val ageRange: String?,
    val gender: String?,
    val incomeLevel: String?,
    val selfReportedUsage: String?
)

/**
 * Response for demographics submission
 */
data class DemographicsResponse(
    val message: String
)

/**
 * Enum for age ranges - exactly matching backend validation
 */
enum class AgeRange(val value: String, val displayName: String) {
    RANGE_18_24("18-24", "18-24"),
    RANGE_25_29("25-29", "25-29"),
    RANGE_30_34("30-34", "30-34"),
    RANGE_35_39("35-39", "35-39"),
    RANGE_40_44("40-44", "40-44"),
    RANGE_45_49("45-49", "45-49"),
    RANGE_50_54("50-54", "50-54"),
    RANGE_55_59("55-59", "55-59"),
    RANGE_60_64("60-64", "60-64"),
    RANGE_65_PLUS("65+", "65+");
    
    companion object {
        fun getAllDisplayNames(): List<String> = values().map { it.displayName }
        fun fromDisplayName(displayName: String): AgeRange? = values().find { it.displayName == displayName }
    }
}

/**
 * Enum for gender options - exactly matching backend validation
 */
enum class Gender(val value: String, val displayName: String) {
    MALE("male", "Male"),
    FEMALE("female", "Female"),
    OTHER("other", "Other");
    
    companion object {
        fun getAllDisplayNames(): List<String> = values().map { it.displayName }
        fun fromDisplayName(displayName: String): Gender? = values().find { it.displayName == displayName }
    }
}

/**
 * Enum for income levels - exactly matching backend validation
 */
enum class IncomeLevel(val value: String, val displayName: String) {
    LOW("low", "Low"),
    MEDIUM("medium", "Medium"),
    HIGH("high", "High");
    
    companion object {
        fun getAllDisplayNames(): List<String> = values().map { it.displayName }
        fun fromDisplayName(displayName: String): IncomeLevel? = values().find { it.displayName == displayName }
    }
}

/**
 * Enum for self-reported usage levels - exactly matching backend validation
 */
enum class UsageLevel(val value: String, val displayName: String) {
    LOW("low", "Low"),
    MEDIUM("medium", "Medium"),
    HIGH("high", "High");
    
    companion object {
        fun getAllDisplayNames(): List<String> = values().map { it.displayName }
        fun fromDisplayName(displayName: String): UsageLevel? = values().find { it.displayName == displayName }
    }
}
