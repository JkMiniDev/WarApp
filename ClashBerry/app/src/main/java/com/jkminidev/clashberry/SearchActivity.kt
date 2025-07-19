package com.jkminidev.clashberry

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jkminidev.clashberry.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySearchBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupSearchFunctionality()
    }
    
    private fun setupUI() {
        // Set up back button
        binding.backButton.setOnClickListener {
            finish()
        }
        
        // Set up RecyclerView
        binding.searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // Focus on search input
        binding.searchEditText.requestFocus()
        
        // Set up swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            performSearch()
        }
    }
    
    private fun setupSearchFunctionality() {
        // Search button click
        binding.searchButton.setOnClickListener {
            performSearch()
        }
        
        // Enter key search
        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                performSearch()
                true
            } else {
                false
            }
        }
        
        // Retry button
        binding.retryButton.setOnClickListener {
            performSearch()
        }
    }
    
    private fun performSearch() {
        val query = binding.searchEditText.text.toString().trim()
        
        if (query.isEmpty()) {
            return
        }
        
        // Show loading state
        showLoadingState()
        
        // TODO: Implement actual search logic
        // For now, simulate search with delay
        binding.searchEditText.postDelayed({
            // Simulate different scenarios based on input
            when {
                query.isEmpty() -> {
                    showErrorState("Please enter a clan tag")
                }
                !query.startsWith("#") -> {
                    showErrorState("Invalid Clan not found")
                }
                query.length < 4 -> {
                    showErrorState("Invalid Clan not found") 
                }
                query.contains("server") -> {
                    showErrorState("Server Down")
                }
                else -> {
                    showSearchResults()
                }
            }
        }, 1500)
    }
    
    private fun showLoadingState() {
        binding.loadingLayout.visibility = android.view.View.VISIBLE
        binding.errorLayout.visibility = android.view.View.GONE
        binding.searchResultsRecyclerView.visibility = android.view.View.GONE
        binding.swipeRefreshLayout.isRefreshing = false
    }
    
    private fun showErrorState(message: String) {
        binding.loadingLayout.visibility = android.view.View.GONE
        binding.errorLayout.visibility = android.view.View.VISIBLE
        binding.searchResultsRecyclerView.visibility = android.view.View.GONE
        binding.errorText.text = message
        binding.swipeRefreshLayout.isRefreshing = false
    }
    
    private fun showSearchResults() {
        binding.loadingLayout.visibility = android.view.View.GONE
        binding.errorLayout.visibility = android.view.View.GONE
        binding.searchResultsRecyclerView.visibility = android.view.View.VISIBLE
        binding.swipeRefreshLayout.isRefreshing = false
        
        // TODO: Set up adapter with actual search results
    }
}