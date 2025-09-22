package com.example.usagestatisticsapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.usagestatisticsapp.network.*
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Activity for collecting participant demographics information
 * Shown after Study ID entry to gather required demographic data
 */
class DemographicsActivity : AppCompatActivity() {
    
    private lateinit var studyIdText: TextView
    private lateinit var cityEditText: TextInputEditText
    private lateinit var ageRangeSpinner: Spinner
    private lateinit var genderSpinner: Spinner
    private lateinit var incomeLevelSpinner: Spinner
    private lateinit var usageLevelSpinner: Spinner
    private lateinit var submitButton: Button
    private lateinit var skipButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    
    private lateinit var apiRepository: ApiRepository
    private var studyId: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demographics)
        
        // Get Study ID from intent
        studyId = intent.getStringExtra("STUDY_ID") ?: ""
        if (studyId.isEmpty()) {
            finish()
            return
        }
        
        apiRepository = ApiRepository()
        initializeViews()
        setupSpinners()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        studyIdText = findViewById(R.id.studyIdText)
        cityEditText = findViewById(R.id.cityEditText)
        ageRangeSpinner = findViewById(R.id.ageRangeSpinner)
        genderSpinner = findViewById(R.id.genderSpinner)
        incomeLevelSpinner = findViewById(R.id.incomeLevelSpinner)
        usageLevelSpinner = findViewById(R.id.usageLevelSpinner)
        submitButton = findViewById(R.id.submitButton)
        skipButton = findViewById(R.id.skipButton)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        
        studyIdText.text = "Study ID: $studyId"
    }
    
    private fun setupSpinners() {
        // Age Range Spinner
        val ageRangeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("Select Age Range") + AgeRange.getAllDisplayNames()
        )
        ageRangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ageRangeSpinner.adapter = ageRangeAdapter
        
        // Gender Spinner
        val genderAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("Select Gender") + Gender.getAllDisplayNames()
        )
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        genderSpinner.adapter = genderAdapter
        
        // Income Level Spinner
        val incomeLevelAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("Select Income Level") + IncomeLevel.getAllDisplayNames()
        )
        incomeLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        incomeLevelSpinner.adapter = incomeLevelAdapter
        
        // Usage Level Spinner
        val usageLevelAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("Select Usage Level") + UsageLevel.getAllDisplayNames()
        )
        usageLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        usageLevelSpinner.adapter = usageLevelAdapter
    }
    
    private fun setupClickListeners() {
        submitButton.setOnClickListener {
            submitDemographics()
        }
        
        skipButton.setOnClickListener {
            // Mark demographics as collected (even if skipped) to avoid showing again
            markDemographicsCollected()
            // Skip demographics and go to main activity
            proceedToMainActivity()
        }
    }
    
    private fun submitDemographics() {
        // Validate required fields
        val city = cityEditText.text.toString().trim()
        if (city.isEmpty()) {
            statusText.text = "Please enter your city"
            statusText.setTextColor(getColor(android.R.color.holo_red_dark))
            return
        }
        
        // Get selected values
        val selectedAgeRange = if (ageRangeSpinner.selectedItemPosition > 0) {
            AgeRange.fromDisplayName(ageRangeSpinner.selectedItem.toString())?.value
        } else null
        
        val selectedGender = if (genderSpinner.selectedItemPosition > 0) {
            Gender.fromDisplayName(genderSpinner.selectedItem.toString())?.value
        } else null
        
        val selectedIncomeLevel = if (incomeLevelSpinner.selectedItemPosition > 0) {
            IncomeLevel.fromDisplayName(incomeLevelSpinner.selectedItem.toString())?.value
        } else null
        
        val selectedUsageLevel = if (usageLevelSpinner.selectedItemPosition > 0) {
            UsageLevel.fromDisplayName(usageLevelSpinner.selectedItem.toString())?.value
        } else null
        
        // Check if at least some demographics are provided
        if (selectedAgeRange == null && selectedGender == null && 
            selectedIncomeLevel == null && selectedUsageLevel == null) {
            statusText.text = "Please select at least one demographic option"
            statusText.setTextColor(getColor(android.R.color.holo_red_dark))
            return
        }
        
        // Show loading
        setLoadingState(true)
        statusText.text = "Submitting demographics..."
        statusText.setTextColor(getColor(android.R.color.darker_gray))
        
        // Create request
        val request = DemographicsRequest(
            studyId = studyId,
            location = city,
            ageRange = selectedAgeRange,
            gender = selectedGender,
            incomeLevel = selectedIncomeLevel,
            selfReportedUsage = selectedUsageLevel
        )
        
        // Submit demographics
        lifecycleScope.launch {
            val result = apiRepository.submitDemographics(request)
            
            setLoadingState(false)
            
            result.fold(
                onSuccess = { response ->
                    statusText.text = "✅ Demographics submitted successfully!"
                    statusText.setTextColor(getColor(android.R.color.holo_green_dark))
                    statusText.visibility = android.view.View.VISIBLE
                    
                    // Mark demographics as collected for this Study ID
                    markDemographicsCollected()
                    
                    // Show confirmation toast
                    android.widget.Toast.makeText(
                        this@DemographicsActivity, 
                        "Demographics saved! Returning to main app...", 
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    
                    // Wait a moment then proceed to main activity
                    submitButton.postDelayed({
                        proceedToMainActivity()
                    }, 1000)
                },
                onFailure = { error ->
                    statusText.text = "❌ Failed to submit demographics: ${error.message}"
                    statusText.setTextColor(getColor(android.R.color.holo_red_dark))
                    statusText.visibility = android.view.View.VISIBLE
                    
                    android.widget.Toast.makeText(
                        this@DemographicsActivity, 
                        "Submission failed. Please try again.", 
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
    
    private fun setLoadingState(loading: Boolean) {
        progressBar.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        submitButton.isEnabled = !loading
        skipButton.isEnabled = !loading
        
        // Disable spinners and edit text during loading
        cityEditText.isEnabled = !loading
        ageRangeSpinner.isEnabled = !loading
        genderSpinner.isEnabled = !loading
        incomeLevelSpinner.isEnabled = !loading
        usageLevelSpinner.isEnabled = !loading
    }
    
    private fun markDemographicsCollected() {
        val sharedPrefs = getSharedPreferences("demographics_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean("demographics_collected_$studyId", true)
            .apply()
    }
    
    private fun proceedToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("STUDY_ID", studyId)
        startActivity(intent)
        finish()
    }
}
