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
    
    private lateinit var bookmarkedClansAdapter: BookmarkedClansAdapter
    private lateinit var warCardsAdapter: WarCardsAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferences = getSharedPreferences("clashberry_prefs", Context.MODE_PRIVATE)
        
        setupUI()
        loadBookmarkedClans()
        loadWarData()
        applyTheme()
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
        binding.homeTab.setOnClickListener {
            switchTab("home")
        }
        
        binding.bookmarksTab.setOnClickListener {
            switchTab("bookmarks")
        }
        
        // Set initial tab
        switchTab("home")
    }
    
    private fun setupTopBar() {
        binding.ivSearch.setOnClickListener {
            // Launch full screen search activity
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }
        
        binding.ivMenu.setOnClickListener { view ->
            showMenuPopup(view)
        }
    }
    
    private fun setupRecyclerViews() {
        // Bookmarked clans RecyclerView
        bookmarkedClansAdapter = BookmarkedClansAdapter(bookmarkedClans) { clan ->
            onClanClicked(clan.tag)
        }
        binding.bookmarkedClansRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = bookmarkedClansAdapter
            // Enable smooth scrolling
            isNestedScrollingEnabled = true
        }
        
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
                
                // Update top bar title
                binding.tvAppName.text = getString(R.string.current_war)
                
                // Update tab colors
                binding.homeIcon.setColorFilter(ContextCompat.getColor(this, R.color.accent_color))
                binding.homeText.setTextColor(ContextCompat.getColor(this, R.color.accent_color))
                binding.bookmarksIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_color_secondary))
                binding.bookmarksText.setTextColor(ContextCompat.getColor(this, R.color.text_color_secondary))
            }
            "bookmarks" -> {
                binding.homeContent.visibility = View.GONE
                binding.bookmarksContent.visibility = View.VISIBLE
                
                // Update top bar title
                binding.tvAppName.text = getString(R.string.bookmarks)
                
                // Update tab colors
                binding.homeIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_color_secondary))
                binding.homeText.setTextColor(ContextCompat.getColor(this, R.color.text_color_secondary))
                binding.bookmarksIcon.setColorFilter(ContextCompat.getColor(this, R.color.accent_color))
                binding.bookmarksText.setTextColor(ContextCompat.getColor(this, R.color.accent_color))
            }
        }
    }
    
    private fun showSearchDialog() {
        val dialog = Dialog(this)
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
        
        dialogBinding.btnSearch.setOnClickListener {
            val clanTag = dialogBinding.etSearchTag.text.toString().trim()
            if (clanTag.isNotEmpty()) {
                searchClan(clanTag, searchAdapter, dialogBinding)
            }
        }
        
        dialogBinding.btnCancel.setOnClickListener {
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
            bookmarkedClansAdapter.notifyDataSetChanged()
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
                R.id.menu_theme -> {
                    showThemeDialog()
                    true
                }
                R.id.menu_update -> {
                    checkForUpdates()
                    true
                }
                R.id.menu_notifications -> {
                    toggleNotifications()
                    true
                }
                R.id.menu_github -> {
                    openUrl("https://github.com") // Update with your GitHub link
                    true
                }
                R.id.menu_discord -> {
                    openUrl("https://discord.com") // Update with your Discord link
                    true
                }
                R.id.menu_patreon -> {
                    openUrl("https://patreon.com") // Update with your Patreon link
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
    
    private fun showThemeDialog() {
        val themes = arrayOf("Dark", "Light", "System Default")
        val currentTheme = preferences.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val selectedIndex = when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> 0
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            else -> 2
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Theme")
            .setSingleChoiceItems(themes, selectedIndex) { dialog, which ->
                val newTheme = when (which) {
                    0 -> AppCompatDelegate.MODE_NIGHT_YES
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                
                preferences.edit().putInt("theme", newTheme).apply()
                AppCompatDelegate.setDefaultNightMode(newTheme)
                dialog.dismiss()
            }
            .show()
    }
    
    private fun checkForUpdates() {
        // Implement update checking logic
        Toast.makeText(this, getString(R.string.no_update_available), Toast.LENGTH_SHORT).show()
    }
    
    private fun toggleNotifications() {
        val notificationsEnabled = preferences.getBoolean("notifications_enabled", false)
        val newState = !notificationsEnabled
        preferences.edit().putBoolean("notifications_enabled", newState).apply()
        
        val message = if (newState) {
            getString(R.string.notification_enabled)
        } else {
            getString(R.string.notification_disabled)
        }
        
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        
        if (newState) {
            // Start notification service
            // TODO: Implement notification worker
        }
    }
    
    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
    
    private fun loadBookmarkedClans() {
        val json = preferences.getString("bookmarked_clans", null)
        if (json != null) {
            val type = object : TypeToken<List<BookmarkedClan>>() {}.type
            val clans: List<BookmarkedClan> = gson.fromJson(json, type)
            bookmarkedClans.clear()
            bookmarkedClans.addAll(clans)
            bookmarkedClansAdapter.notifyDataSetChanged()
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
        
        warCardBinding.tvTimeRemaining.text = warData.timeRemaining ?: "Unknown"
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
    
    private fun applyTheme() {
        val theme = preferences.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(theme)
    }
    
    // Adapter classes
    inner class BookmarkedClansAdapter(
        private val clans: List<BookmarkedClan>,
        private val onClanClick: (BookmarkedClan) -> Unit
    ) : RecyclerView.Adapter<BookmarkedClansAdapter.ViewHolder>() {
        
        inner class ViewHolder(val binding: ItemClanSearchBinding) : RecyclerView.ViewHolder(binding.root)
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemClanSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val clan = clans[position]
            
            holder.binding.tvClanName.text = clan.name
            holder.binding.tvClanTag.text = clan.tag
            holder.binding.tvMembersCount.text = getString(R.string.members_count, clan.members)
            holder.binding.tvClanPoints.text = getString(R.string.clan_points, clan.clanPoints)
            
            Glide.with(holder.binding.ivClanBadge)
                .load(clan.badge)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .circleCrop()
                .into(holder.binding.ivClanBadge)
            
            holder.binding.ivBookmark.setImageResource(R.drawable.ic_bookmark)
            holder.binding.ivBookmark.setOnClickListener {
                // Remove from bookmarks
                bookmarkedClans.remove(clan)
                saveBookmarkedClans()
                notifyDataSetChanged()
                Toast.makeText(this@MainActivity, getString(R.string.clan_removed), Toast.LENGTH_SHORT).show()
            }
            
            holder.binding.root.setOnClickListener {
                onClanClick(clan)
            }
        }
        
        override fun getItemCount() = clans.size
    }
    
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
            
            holder.binding.tvTimeRemaining.text = war.timeRemaining ?: "Unknown"
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