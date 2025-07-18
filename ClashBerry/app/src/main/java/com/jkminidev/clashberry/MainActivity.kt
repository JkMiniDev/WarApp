package com.jkminidev.clashberry

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.jkminidev.clashberry.data.ErrorResponse
import com.jkminidev.clashberry.data.WarResponse
import com.jkminidev.clashberry.databinding.ActivityMainBinding
import com.jkminidev.clashberry.network.NetworkModule
import com.jkminidev.clashberry.ui.WarDisplayHelper
import com.jkminidev.clashberry.utils.ErrorHandler
import kotlinx.coroutines.launch
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val apiService = NetworkModule.apiService
    private lateinit var warDisplayHelper: WarDisplayHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        warDisplayHelper = WarDisplayHelper(this)
        setupUI()
    }
    
    private fun setupUI() {
        // Search button click
        binding.btnSearch.setOnClickListener {
            searchWar()
        }
        
        // Enter key in edit text
        binding.etClanTag.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchWar()
                true
            } else {
                false
            }
        }
    }
    
    private fun searchWar() {
        val clanTag = binding.etClanTag.text.toString().trim()
        
        if (clanTag.isEmpty()) {
            Toast.makeText(this, "Please enter a clan tag", Toast.LENGTH_SHORT).show()
            return
        }
        
        showLoading(true)
        hideError()
        hideResults()
        
        lifecycleScope.launch {
            try {
                val response = apiService.getWarData(clanTag)
                handleWarResponse(response)
            } catch (e: Exception) {
                showError(ErrorResponse("network_error", "Failed to fetch war data. Please try again.", null))
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun handleWarResponse(response: Response<WarResponse>) {
        if (response.isSuccessful) {
            response.body()?.let { warData ->
                showWarResults(warData)
            } ?: run {
                showError(ErrorResponse("server_error", "Invalid response from server", null))
            }
        } else {
            val errorResponse = ErrorHandler.parseError(response)
            showError(errorResponse)
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSearch.isEnabled = !show
    }
    
    private fun showError(errorResponse: ErrorResponse) {
        binding.cardError.visibility = View.VISIBLE
        
        val (title, message) = ErrorHandler.getErrorDisplayText(this, errorResponse)
        binding.tvErrorTitle.text = title
        binding.tvErrorMessage.text = message
        
        // Show clan info if available
        errorResponse.clan?.let { clan ->
            binding.layoutClanInfo.visibility = View.VISIBLE
            binding.tvErrorClanName.text = clan.name
            binding.tvErrorClanTag.text = clan.tag
            
            Glide.with(this)
                .load(clan.badge)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .circleCrop()
                .into(binding.ivErrorClanBadge)
        } ?: run {
            binding.layoutClanInfo.visibility = View.GONE
        }
    }
    
    private fun hideError() {
        binding.cardError.visibility = View.GONE
    }
    
    private fun showWarResults(warData: WarResponse) {
        binding.layoutWarResults.visibility = View.VISIBLE
        warDisplayHelper.displayWar(warData, binding.layoutWarResults)
    }
    
    private fun hideResults() {
        binding.layoutWarResults.visibility = View.GONE
        binding.layoutWarResults.removeAllViews()
    }
}