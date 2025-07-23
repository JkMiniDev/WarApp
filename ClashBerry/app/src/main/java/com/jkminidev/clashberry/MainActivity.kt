package com.jkminidev.clashberry

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jkminidev.clashberry.data.*
import com.jkminidev.clashberry.databinding.*
import com.jkminidev.clashberry.network.NetworkModule
import com.jkminidev.clashberry.utils.ErrorHandler
import kotlinx.coroutines.launch
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val apiService = NetworkModule.apiService
    private lateinit var preferences: SharedPreferences
    private val gson = Gson()
    
    private var currentTab = "home"
    private val bookmarkedClans = mutableListOf<BookmarkedClan>()
    private val warCards = mutableListOf<WarResponse>()
    
    private lateinit var warCardsAdapter: WarCardsAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferences = getSharedPreferences("clashberry_prefs", Context.MODE_PRIVATE)
        
        setupUI()
        loadBookmarkedClans()
        loadWarData()
        displayBookmarkedClans()
    }
    
    private fun setupUI() {
        setupBottomNavigation()
        setupTopBar()
        setupRecyclerViews()
        
        // Enable smooth scrolling
        binding.homeContent.isSmoothScrollingEnabled = true
        binding.bookmarksContent.isSmoothScrollingEnabled = true
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchTab("home")
                R.id.nav_bookmarks -> switchTab("bookmarks")
            }
            true
        }
        // Set initial tab
        binding.bottomNavigationView.selectedItemId = R.id.nav_home
        switchTab("home")
    }
    
    private fun setupTopBar() {
        binding.ivSearch.setOnClickListener {
            showSearchDialog()
        }
        
        binding.ivMenu.setOnClickListener { view ->
            showMenuPopup(view)
        }
    }
    
    private fun setupRecyclerViews() {
        // War cards setup
        warCardsAdapter = WarCardsAdapter(warCards) { war ->
            openWarDetail(war)
        }
    }
    
    private fun switchTab(tab: String) {
        currentTab = tab
        when (tab) {
            "home" -> {
                binding.homeContent.visibility = View.VISIBLE
                binding.bookmarksContent.visibility = View.GONE
                binding.tvAppName.text = getString(R.string.current_war)
            }
            "bookmarks" -> {
                binding.homeContent.visibility = View.GONE
                binding.bookmarksContent.visibility = View.VISIBLE
                binding.tvAppName.text = getString(R.string.bookmarks)
            }
        }
    }
    
    private fun showSearchDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogBinding = DialogSearchBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        
        val searchAdapter = ClanSearchAdapter(mutableListOf()) { clan ->
            onSearchResultClicked(clan)
            dialog.dismiss()
        }
        
        dialogBinding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = searchAdapter
            // Enable smooth scrolling
            isNestedScrollingEnabled = true
        }
        
        // Remove btnSearch and btnCancel listeners
        // dialogBinding.btnSearch.setOnClickListener {
        //     val clanTag = dialogBinding.etSearchTag.text.toString().trim()
        //     if (clanTag.isNotEmpty()) {
        //         searchClan(clanTag, searchAdapter, dialogBinding)
        //     }
        // }
        
        // dialogBinding.btnCancel.setOnClickListener {
        //     dialog.dismiss()
        // }
        
        dialogBinding.ivBack.setOnClickListener {
            dialog.dismiss()
        }
        
        dialogBinding.etSearchTag.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val clanTag = dialogBinding.etSearchTag.text.toString().trim()
                if (clanTag.isNotEmpty()) {
                    searchClan(clanTag, searchAdapter, dialogBinding)
                }
                true
            } else {
                false
            }
        }
        
        dialog.show()
        // Automatically focus the search EditText and show the keyboard
        dialogBinding.etSearchTag.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(dialogBinding.etSearchTag, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    
    private fun searchClan(clanTag: String, adapter: ClanSearchAdapter, dialogBinding: DialogSearchBinding) {
        dialogBinding.searchLoadingLayout.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val response = apiService.getClanInfo(clanTag)
                if (response.isSuccessful) {
                    response.body()?.let { clanInfo ->
                        val searchResults = listOf(clanInfo)
                        adapter.updateResults(searchResults)
                        dialogBinding.searchLoadingLayout.visibility = View.GONE
                    }
                } else {
                    dialogBinding.searchLoadingLayout.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Clan not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                dialogBinding.searchLoadingLayout.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Search failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun onSearchResultClicked(clan: ClanBasicInfo) {
        // Add to bookmarks
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
            displayBookmarkedClans()
            Toast.makeText(this, getString(R.string.clan_bookmarked), Toast.LENGTH_SHORT).show()
            
            // Refresh war data
            loadWarData()
        } else {
            Toast.makeText(this, "Clan already bookmarked", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun onClanClicked(clanTag: String) {
        // Load and show war data for this clan
        lifecycleScope.launch {
            try {
                val response = apiService.getWarData(clanTag)
                if (response.isSuccessful) {
                    response.body()?.let { warData ->
                        openWarDetail(warData)
                    }
                } else {
                    val errorResponse = ErrorHandler.parseError(response)
                    Toast.makeText(this@MainActivity, errorResponse.message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to load war data", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun openWarDetail(warData: WarResponse) {
        val intent = Intent(this, WarDetailActivity::class.java)
        intent.putExtra("war_data", gson.toJson(warData))
        startActivity(intent)
    }
    
    private fun showMenuPopup(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_refresh -> {
                    refreshData()
                    true
                }
                R.id.menu_settings -> {
                    openSettings()
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
    
    private fun refreshData() {
        // Show loading indicator
        binding.loadingLayout.visibility = View.VISIBLE
        
        // Refresh the data by reloading war data and bookmarked clans
        loadBookmarkedClans()
        loadWarData()
        
        // Hide loading indicator after a short delay to show the animation
        binding.loadingLayout.postDelayed({
            binding.loadingLayout.visibility = View.GONE
        }, 1000)
    }
    
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
    
    private fun loadBookmarkedClans() {
        val json = preferences.getString("bookmarked_clans", null)
        if (json != null) {
            val type = object : TypeToken<List<BookmarkedClan>>() {}.type
            val clans: List<BookmarkedClan> = gson.fromJson(json, type)
            bookmarkedClans.clear()
            bookmarkedClans.addAll(clans)
            displayBookmarkedClans()
        }
    }
    
    private fun saveBookmarkedClans() {
        val json = gson.toJson(bookmarkedClans)
        preferences.edit().putString("bookmarked_clans", json).apply()
    }
    
    private fun loadWarData() {
        if (bookmarkedClans.isEmpty()) {
            binding.noWarsLayout.visibility = View.VISIBLE
            binding.warCardsContainer.removeAllViews()
            return
        }
        
        binding.noWarsLayout.visibility = View.GONE
        binding.warCardsContainer.removeAllViews()
        
        // Load war data for each bookmarked clan
        bookmarkedClans.forEach { clan ->
            lifecycleScope.launch {
                try {
                    val response = apiService.getWarData(clan.tag)
                    if (response.isSuccessful) {
                        response.body()?.let { warData ->
                            addWarCard(warData)
                        }
                    }
                } catch (e: Exception) {
                    // Silently handle errors for individual clans
                }
            }
        }
    }
    
    private fun addWarCard(warData: WarResponse) {
        val warCardBinding = ItemWarCardBinding.inflate(layoutInflater)
        
        // Populate war card data
        warCardBinding.tvWarStatus.text = when (warData.state) {
            "preparation" -> getString(R.string.preparation)
            "inWar" -> getString(R.string.battle_day)
            else -> getString(R.string.war_ended)
        }
        
        warCardBinding.tvTimeRemaining.text = when {
            warData.state == "warEnded" -> "00:00"
            else -> warData.timeRemaining ?: "Unknown"
        }
        warCardBinding.tvLeftStars.text = warData.clan.stars.toString()
        warCardBinding.tvRightStars.text = warData.opponent.stars.toString()
        warCardBinding.tvLeftClanName.text = warData.clan.name
        warCardBinding.tvRightClanName.text = warData.opponent.name
        
        // Load clan badges
        Glide.with(this)
            .load(warData.clan.badge)
            .placeholder(R.mipmap.ic_launcher)
            .error(R.mipmap.ic_launcher)
            .circleCrop()
            .into(warCardBinding.ivLeftClanBadge)
            
        Glide.with(this)
            .load(warData.opponent.badge)
            .placeholder(R.mipmap.ic_launcher)
            .error(R.mipmap.ic_launcher)
            .circleCrop()
            .into(warCardBinding.ivRightClanBadge)
        
        // Set click listener
        warCardBinding.root.setOnClickListener {
            openWarDetail(warData)
        }
        
        binding.warCardsContainer.addView(warCardBinding.root)
    }
    
    private fun displayBookmarkedClans() {
        binding.bookmarkedClansContainer.removeAllViews()
        
        if (bookmarkedClans.isEmpty()) {
            binding.noBookmarksLayout.visibility = View.VISIBLE
            return
        }
        
        binding.noBookmarksLayout.visibility = View.GONE
        
        bookmarkedClans.forEach { clan ->
            addBookmarkCard(clan)
        }
    }
    
    private fun addBookmarkCard(clan: BookmarkedClan) {
        val bookmarkCardBinding = ItemBookmarkCardBinding.inflate(layoutInflater)
        
        // Populate bookmark card data
        bookmarkCardBinding.tvClanName.text = clan.name
        bookmarkCardBinding.tvClanTag.text = clan.tag
        bookmarkCardBinding.tvMembersCount.text = getString(R.string.members_count, clan.members)
        bookmarkCardBinding.tvClanLevel.text = "Level: ${clan.level}"
        
        // Load clan badge
        Glide.with(this)
            .load(clan.badge)
            .placeholder(R.mipmap.ic_launcher)
            .error(R.mipmap.ic_launcher)
            .circleCrop()
            .into(bookmarkCardBinding.ivClanBadge)
        
        // Set click listener for the card
        bookmarkCardBinding.root.setOnClickListener {
            onClanClicked(clan.tag)
        }
        
        // Set bookmark icon click listener
        bookmarkCardBinding.ivBookmark.setOnClickListener {
            // Remove from bookmarks
            bookmarkedClans.remove(clan)
            saveBookmarkedClans()
            displayBookmarkedClans()
            Toast.makeText(this, getString(R.string.clan_removed), Toast.LENGTH_SHORT).show()
            
            // Refresh war data
            loadWarData()
        }
        
        binding.bookmarkedClansContainer.addView(bookmarkCardBinding.root)
    }
    

    
    // Adapter classes
    
    inner class ClanSearchAdapter(
        private val results: MutableList<ClanBasicInfo>,
        private val onClanClick: (ClanBasicInfo) -> Unit
    ) : RecyclerView.Adapter<ClanSearchAdapter.ViewHolder>() {
        
        inner class ViewHolder(val binding: ItemClanSearchBinding) : RecyclerView.ViewHolder(binding.root)
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemClanSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val clan = results[position]
            
            holder.binding.tvClanName.text = clan.name
            holder.binding.tvClanTag.text = clan.tag
            holder.binding.tvMembersCount.text = getString(R.string.members_count, clan.members)
            holder.binding.tvClanPoints.text = "Level: ${clan.level}"
            
            Glide.with(holder.binding.ivClanBadge)
                .load(clan.badge)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .circleCrop()
                .into(holder.binding.ivClanBadge)
            
            val isBookmarked = bookmarkedClans.any { it.tag == clan.tag }
            holder.binding.ivBookmark.setImageResource(
                if (isBookmarked) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border
            )
            
            holder.binding.root.setOnClickListener {
                onClanClick(clan)
            }
        }
        
        override fun getItemCount() = results.size
        
        fun updateResults(newResults: List<ClanBasicInfo>) {
            results.clear()
            results.addAll(newResults)
            notifyDataSetChanged()
        }
    }
    
    inner class WarCardsAdapter(
        private val wars: List<WarResponse>,
        private val onWarClick: (WarResponse) -> Unit
    ) : RecyclerView.Adapter<WarCardsAdapter.ViewHolder>() {
        
        inner class ViewHolder(val binding: ItemWarCardBinding) : RecyclerView.ViewHolder(binding.root)
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemWarCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val war = wars[position]
            
            holder.binding.tvWarStatus.text = when (war.state) {
                "preparation" -> getString(R.string.preparation)
                "inWar" -> getString(R.string.battle_day)
                else -> getString(R.string.war_ended)
            }
            
            holder.binding.tvTimeRemaining.text = when {
                war.state == "warEnded" -> "00:00"
                else -> war.timeRemaining ?: "Unknown"
            }
            holder.binding.tvLeftStars.text = war.clan.stars.toString()
            holder.binding.tvRightStars.text = war.opponent.stars.toString()
            holder.binding.tvLeftClanName.text = war.clan.name
            holder.binding.tvRightClanName.text = war.opponent.name
            
            Glide.with(holder.binding.ivLeftClanBadge)
                .load(war.clan.badge)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .circleCrop()
                .into(holder.binding.ivLeftClanBadge)
                
            Glide.with(holder.binding.ivRightClanBadge)
                .load(war.opponent.badge)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .circleCrop()
                .into(holder.binding.ivRightClanBadge)
            
            holder.binding.root.setOnClickListener {
                onWarClick(war)
            }
        }
        
        override fun getItemCount() = wars.size
    }
}