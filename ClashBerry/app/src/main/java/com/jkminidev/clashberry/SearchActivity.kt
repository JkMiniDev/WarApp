package com.jkminidev.clashberry

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jkminidev.clashberry.data.BookmarkedClan
import com.jkminidev.clashberry.data.ClanBasicInfo
import com.jkminidev.clashberry.databinding.ActivitySearchBinding
import com.jkminidev.clashberry.databinding.ItemClanSearchBinding
import com.jkminidev.clashberry.network.NetworkModule
import com.jkminidev.clashberry.utils.ErrorHandler
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySearchBinding
    private val apiService = NetworkModule.apiService
    private val gson = Gson()
    private lateinit var searchAdapter: ClanSearchAdapter
    private val bookmarkedClans = mutableListOf<BookmarkedClan>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupSearchFunctionality()
        loadBookmarkedClans()
    }
    
    private fun setupUI() {
        // Set up back button
        binding.backButton.setOnClickListener {
            finish()
        }
        
        // Set up RecyclerView with real adapter
        searchAdapter = ClanSearchAdapter(mutableListOf()) { clan ->
            onSearchResultClicked(clan)
        }
        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = searchAdapter
        }
        
        // Focus on search input
        binding.etSearchTag.requestFocus()
        
        // Set up swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            performSearch()
        }
        
        // Set up end icon click (search icon in TextInputLayout)
        binding.tilSearchTag.setEndIconOnClickListener {
            performSearch()
        }
    }
    
    private fun setupSearchFunctionality() {
        // Search button click
        binding.btnSearch.setOnClickListener {
            performSearch()
        }
        
        // Enter key search
        binding.etSearchTag.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                performSearch()
                true
            } else {
                false
            }
        }
        
        // Retry button
        binding.btnRetry.setOnClickListener {
            performSearch()
        }
    }
    
    private fun performSearch() {
        val clanTag = binding.etSearchTag.text.toString().trim()
        
        if (clanTag.isEmpty()) {
            return
        }
        
        // Show loading state
        showLoadingState()
        
        // Use the original working search logic
        lifecycleScope.launch {
            try {
                val response = apiService.getClanInfo(clanTag)
                if (response.isSuccessful) {
                    response.body()?.let { clanInfo ->
                        val searchResults = listOf(clanInfo)
                        searchAdapter.updateResults(searchResults)
                        showSearchResults()
                    }
                } else {
                    showErrorState("Clan not found")
                }
            } catch (e: Exception) {
                showErrorState("Server Down")
            }
        }
    }
    
    private fun showLoadingState() {
        binding.searchLoadingLayout.visibility = android.view.View.VISIBLE
        binding.searchErrorLayout.visibility = android.view.View.GONE
        binding.searchResultsRecyclerView.visibility = android.view.View.GONE
        binding.swipeRefreshLayout.isRefreshing = false
    }
    
    private fun showErrorState(message: String) {
        binding.searchLoadingLayout.visibility = android.view.View.GONE
        binding.searchErrorLayout.visibility = android.view.View.VISIBLE
        binding.searchResultsRecyclerView.visibility = android.view.View.GONE
        binding.errorText.text = message
        binding.swipeRefreshLayout.isRefreshing = false
    }
    
    private fun showSearchResults() {
        binding.searchLoadingLayout.visibility = android.view.View.GONE
        binding.searchErrorLayout.visibility = android.view.View.GONE
        binding.searchResultsRecyclerView.visibility = android.view.View.VISIBLE
        binding.swipeRefreshLayout.isRefreshing = false
        
        // Results are now showing via adapter
    }
    
    private fun onSearchResultClicked(clan: ClanBasicInfo) {
        // Add to bookmarks exactly like the original
        val bookmarkedClan = BookmarkedClan(
            tag = clan.tag,
            name = clan.name,
            badge = clan.badge,
            level = clan.level,
            members = clan.members,
            clanPoints = 0 // We'll update this when we get war data
        )
        
        if (!bookmarkedClans.any { it.tag == clan.tag }) {
            bookmarkedClans.add(bookmarkedClan)
            saveBookmarkedClans()
            Toast.makeText(this, getString(R.string.clan_bookmarked), Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Clan already bookmarked", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadBookmarkedClans() {
        val prefs = getSharedPreferences("clashberry_prefs", Context.MODE_PRIVATE)
        val bookmarkedJson = prefs.getString("bookmarked_clans", "[]")
        val type = object : TypeToken<List<BookmarkedClan>>() {}.type
        val savedClans: List<BookmarkedClan> = gson.fromJson(bookmarkedJson, type)
        bookmarkedClans.clear()
        bookmarkedClans.addAll(savedClans)
    }
    
    private fun saveBookmarkedClans() {
        val prefs = getSharedPreferences("clashberry_prefs", Context.MODE_PRIVATE)
        val bookmarkedJson = gson.toJson(bookmarkedClans)
        prefs.edit().putString("bookmarked_clans", bookmarkedJson).apply()
    }
    
    // Standalone adapter for search results
    class ClanSearchAdapter(
        private val results: MutableList<ClanBasicInfo>,
        private val onClanClick: (ClanBasicInfo) -> Unit
    ) : RecyclerView.Adapter<ClanSearchAdapter.ViewHolder>() {
        
        class ViewHolder(val binding: ItemClanSearchBinding) : RecyclerView.ViewHolder(binding.root)
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemClanSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val clan = results[position]
            
            holder.binding.tvClanName.text = clan.name
            holder.binding.tvClanTag.text = clan.tag
            holder.binding.tvMembersCount.text = "Members: ${clan.members}"
            holder.binding.tvClanPoints.text = "Level: ${clan.level}"
            
            holder.binding.root.setOnClickListener {
                onClanClick(clan)
            }
        }
        
        override fun getItemCount(): Int = results.size
        
        fun updateResults(newResults: List<ClanBasicInfo>) {
            results.clear()
            results.addAll(newResults)
            notifyDataSetChanged()
        }
    }
}