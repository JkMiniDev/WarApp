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
import com.jkminidev.clashberry.ui.WarDisplayHelper
import kotlinx.coroutines.launch
import retrofit2.Response
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val apiService = NetworkModule.apiService
    private lateinit var preferences: SharedPreferences
    private val gson = Gson()
    private lateinit var warDisplayHelper: WarDisplayHelper
    
    private val bookmarkedClans = mutableListOf<BookmarkedClan>()
    private var currentWarData: WarResponse? = null
    private var selectedClan: BookmarkedClan? = null
    
    private lateinit var warPagerAdapter: WarPagerAdapter
    private var currentOverviewFragment: OverviewFragment? = null
    private var currentActivityFragment: ActivityFragment? = null
    
    // Search related variables
    private var isSearchMode = false
    private lateinit var searchAdapter: ClanSearchAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferences = getSharedPreferences("clashberry_prefs", Context.MODE_PRIVATE)
        warDisplayHelper = WarDisplayHelper(this)
        
        setupUI()
        loadBookmarkedClans()
        setupWarViewPager()
    }
    
    override fun onBackPressed() {
        if (isSearchMode) {
            hideInlineSearch()
        } else {
            super.onBackPressed()
        }
    }
    
    private fun setupUI() {
        setupBottomNavigation()
        setupTopBar()
        setupPullToRefresh()
        setupInlineSearch()
        updateSelectedClanDisplay()
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_overview -> {
                    binding.viewPager.currentItem = 0
                    true
                }
                R.id.nav_activity -> {
                    binding.viewPager.currentItem = 1
                    true
                }
                else -> false
            }
        }
        // Set initial tab
        binding.bottomNavigationView.selectedItemId = R.id.nav_overview
    }
    
    private fun setupTopBar() {
        binding.clanSelectorLayout.setOnClickListener {
            showClanSelectorDialog()
        }
        
        binding.ivSearch.setOnClickListener {
            showInlineSearch()
        }
        
        binding.ivMenu.setOnClickListener { view ->
            showMenuPopup(view)
        }
    }
    
        private fun setupPullToRefresh() {
        // Add custom touch handling to block horizontal swipes
        var startX = 0f
        var startY = 0f
        var isHorizontalSwipe = false
        
        binding.swipeRefreshLayout.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    isHorizontalSwipe = false
                    false // Don't consume the event
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val deltaX = kotlin.math.abs(event.x - startX)
                    val deltaY = kotlin.math.abs(event.y - startY)
                    
                    // If horizontal movement is detected, mark as horizontal swipe
                    if (deltaX > 30 && deltaX > deltaY) {
                        isHorizontalSwipe = true
                        true // Consume the event to block SwipeRefreshLayout
                    } else {
                        false // Allow vertical movement for pull-to-refresh
                    }
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    if (isHorizontalSwipe) {
                        isHorizontalSwipe = false
                        true // Consume to prevent any refresh trigger
                    } else {
                        false // Allow normal touch handling
                    }
                }
                else -> false
            }
        }
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshDataFromPullToRefresh()
        }
        
        // Set refresh indicator colors to match app theme
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.accent_color,
            R.color.green_primary,
            R.color.accent_color_dark
        )
    }
    
    private fun setupWarViewPager() {
        warPagerAdapter = WarPagerAdapter(this)
        binding.viewPager.adapter = warPagerAdapter
        binding.viewPager.offscreenPageLimit = 2
        binding.viewPager.isUserInputEnabled = false // Disable swipe, use bottom nav only
    }
    
    private fun updateSelectedClanDisplay() {
        if (bookmarkedClans.isEmpty()) {
            // No bookmarks - show "No Bookmarks" without arrow, and show no bookmarks message in main screen
            binding.tvSelectedClanName.text = "No Bookmarks"
            binding.ivSelectedClanBadge.setImageResource(R.drawable.ic_placeholder)
            binding.clanSelectorLayout.findViewById<android.widget.ImageView>(R.id.ivDropdownArrow)?.visibility = View.GONE
            binding.noWarLayout.visibility = View.VISIBLE
            binding.viewPager.visibility = View.GONE
            
            // Update the no war message for no bookmarks case
            updateNoWarLayoutForNoBookmarks()
        } else if (selectedClan != null) {
            // Has bookmarks and clan is selected
            binding.tvSelectedClanName.text = selectedClan!!.name
            Glide.with(this)
                .load(selectedClan!!.badge)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .circleCrop()
                .into(binding.ivSelectedClanBadge)
            binding.clanSelectorLayout.findViewById<android.widget.ImageView>(R.id.ivDropdownArrow)?.visibility = View.VISIBLE
            binding.noWarLayout.visibility = View.GONE
            binding.viewPager.visibility = View.VISIBLE
        } else {
            // Has bookmarks but no clan selected - auto-select first clan
            selectedClan = bookmarkedClans.first()
            loadWarDataForClan(selectedClan!!)
            updateSelectedClanDisplay()
        }
    }
    
    private fun updateNoWarLayoutForNoBookmarks() {
        // Update the no war layout to show no bookmarks message
        val noWarLayout = binding.noWarLayout
        
        // Access child views directly by index since they don't have IDs
        val imageView = noWarLayout.getChildAt(0) as? android.widget.ImageView
        val titleTextView = noWarLayout.getChildAt(1) as? android.widget.TextView
        val messageTextView = noWarLayout.getChildAt(2) as? android.widget.TextView
        
        // Update the views
        imageView?.setImageResource(R.drawable.ic_bookmark)
        titleTextView?.text = "No Bookmarked Clans"
        messageTextView?.text = "Search and bookmark clans to view their war details here"
    }
    
    private fun showClanSelectorDialog() {
        // Don't show dialog if no bookmarks
        if (bookmarkedClans.isEmpty()) {
            return
        }
        
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogBinding = DialogClanSelectorBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        
        val clanSelectorAdapter = ClanSelectorAdapter(bookmarkedClans) { clan ->
            selectedClan = clan
            updateSelectedClanDisplay()
            loadWarDataForClan(clan)
            dialog.dismiss()
        }
        
        dialogBinding.clanSelectorRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = clanSelectorAdapter
        }
        
        // Show/hide no bookmarks message
        if (bookmarkedClans.isEmpty()) {
            dialogBinding.noBookmarksLayout.visibility = View.VISIBLE
            dialogBinding.clanSelectorRecyclerView.visibility = View.GONE
        } else {
            dialogBinding.noBookmarksLayout.visibility = View.GONE
            dialogBinding.clanSelectorRecyclerView.visibility = View.VISIBLE
        }
        
        dialogBinding.ivBack.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun showRemoveBookmarkConfirmation(clan: BookmarkedClan) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Remove Bookmark")
            .setMessage("Are you sure you want to remove '${clan.name}' from your bookmarks?")
            .setPositiveButton("Remove") { _, _ ->
                removeBookmarkedClan(clan)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun removeBookmarkedClan(clan: BookmarkedClan) {
        bookmarkedClans.remove(clan)
        saveBookmarkedClans()
        Toast.makeText(this, "${clan.name} removed from bookmarks", Toast.LENGTH_SHORT).show()
        
        // If the removed clan was currently selected or no bookmarks left
        if (selectedClan?.tag == clan.tag || bookmarkedClans.isEmpty()) {
            selectedClan = null
            currentWarData = null
            updateSelectedClanDisplay()
        }
    }
    
    private fun setupInlineSearch() {
        // Initialize search adapter
        searchAdapter = ClanSearchAdapter(mutableListOf()) { clan ->
            onSearchResultClicked(clan)
            hideInlineSearch()
        }
        
        // Setup search results RecyclerView
        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = searchAdapter
            isNestedScrollingEnabled = true
        }
        
        // Setup search back button
        binding.ivSearchBack.setOnClickListener {
            hideInlineSearch()
        }
        
        // Setup search EditText
        binding.etSearchTag.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val clanTag = binding.etSearchTag.text.toString().trim()
                if (clanTag.isNotEmpty()) {
                    searchClan(clanTag, searchAdapter)
                }
                true
            } else {
                false
            }
        }
    }
    
    private fun showInlineSearch() {
        isSearchMode = true
        
        // Hide normal top bar and show search bar
        binding.topAppBar.visibility = View.GONE
        binding.searchAppBar.visibility = View.VISIBLE
        
        // Hide main content and show search results
        binding.viewPager.visibility = View.GONE
        binding.noWarLayout.visibility = View.GONE
        binding.searchResultsRecyclerView.visibility = View.VISIBLE
        
        // Clear previous search results
        searchAdapter.updateResults(emptyList())
        binding.etSearchTag.text?.clear()
        
        // Focus search EditText and show keyboard
        binding.etSearchTag.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(binding.etSearchTag, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    
    private fun hideInlineSearch() {
        isSearchMode = false
        
        // Show normal top bar and hide search bar
        binding.topAppBar.visibility = View.VISIBLE
        binding.searchAppBar.visibility = View.GONE
        
        // Hide search results and show main content
        binding.searchResultsRecyclerView.visibility = View.GONE
        binding.viewPager.visibility = View.VISIBLE
        
        // Restore proper content visibility
        if (currentWarData != null) {
            binding.viewPager.visibility = View.VISIBLE
            binding.noWarLayout.visibility = View.GONE
        } else {
            binding.viewPager.visibility = View.GONE
            binding.noWarLayout.visibility = View.VISIBLE
        }
        
        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearchTag.windowToken, 0)
    }
    
    private fun searchClan(clanTag: String, adapter: ClanSearchAdapter) {
        lifecycleScope.launch {
            try {
                val response = apiService.getClanInfo(clanTag)
                if (response.isSuccessful) {
                    response.body()?.let { clanInfo ->
                        val searchResults = listOf(clanInfo)
                        adapter.updateResults(searchResults)
                    }
                } else {
                    adapter.updateResults(emptyList())
                    Toast.makeText(this@MainActivity, "Clan not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                adapter.updateResults(emptyList())
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
            val wasEmpty = bookmarkedClans.isEmpty()
            bookmarkedClans.add(bookmarkedClan)
            saveBookmarkedClans()
            Toast.makeText(this, getString(R.string.clan_bookmarked), Toast.LENGTH_SHORT).show()
            
            // If this was the first bookmark, auto-select it and load war data
            if (wasEmpty) {
                selectedClan = bookmarkedClan
                updateSelectedClanDisplay()
                loadWarDataForClan(bookmarkedClan)
            }
        } else {
            Toast.makeText(this, "Clan already bookmarked", Toast.LENGTH_SHORT).show()
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
                    refreshDataFromMenu()
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
    
    private fun refreshDataFromPullToRefresh() {
        loadBookmarkedClans()
        selectedClan?.let { clan ->
            loadWarDataForClan(clan, false) // false = don't show center loading
        } ?: run {
            // No selected clan, just stop the refresh animation
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }
    
    private fun refreshDataFromMenu() {
        loadBookmarkedClans()
        selectedClan?.let { clan ->
            loadWarDataForClan(clan, true) // true = show center loading
        }
    }

    private fun showLoadingOverlay() {
        if (binding.root.findViewById<android.widget.FrameLayout>(R.id.loadingOverlay) == null) {
            val overlay = android.widget.FrameLayout(this)
            overlay.id = R.id.loadingOverlay
            overlay.setBackgroundColor(android.graphics.Color.parseColor("#80000000"))
            val progress = android.widget.ProgressBar(this)
            val params = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            )
            params.gravity = android.view.Gravity.CENTER
            overlay.addView(progress, params)
            (binding.root as ViewGroup).addView(overlay, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
    }

    private fun hideLoadingOverlay() {
        val overlay = binding.root.findViewById<android.widget.FrameLayout>(R.id.loadingOverlay)
        overlay?.let { (binding.root as ViewGroup).removeView(it) }
    }
    
    private fun loadWarDataForClan(clan: BookmarkedClan, showCenterLoading: Boolean = true) {
        if (showCenterLoading) {
            binding.loadingLayout.visibility = View.VISIBLE
        }
        lifecycleScope.launch {
            try {
                val response = apiService.getWarData(clan.tag)
                if (response.isSuccessful) {
                    response.body()?.let { warData ->
                        currentWarData = warData
                        warPagerAdapter.updateWarData(warData)
                        if (showCenterLoading) {
                            binding.loadingLayout.visibility = View.GONE
                        }
                        // Stop pull-to-refresh animation
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                } else {
                    if (showCenterLoading) {
                        binding.loadingLayout.visibility = View.GONE
                    }
                    // Stop pull-to-refresh animation
                    binding.swipeRefreshLayout.isRefreshing = false
                    currentWarData = null
                    Toast.makeText(this@MainActivity, "No ongoing war for this clan", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (showCenterLoading) {
                    binding.loadingLayout.visibility = View.GONE
                }
                // Stop pull-to-refresh animation
                binding.swipeRefreshLayout.isRefreshing = false
                currentWarData = null
                Toast.makeText(this@MainActivity, "Connection Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }


    

    

    
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun loadBookmarkedClans() {
        val json = preferences.getString("bookmarked_clans", null)
        if (json != null) {
            val type = object : com.google.gson.reflect.TypeToken<List<BookmarkedClan>>() {}.type
            val clans: List<BookmarkedClan> = gson.fromJson(json, type)
            bookmarkedClans.clear()
            bookmarkedClans.addAll(clans)
            
            // Auto-select first clan if available and no clan currently selected
            if (bookmarkedClans.isNotEmpty() && selectedClan == null) {
                selectedClan = bookmarkedClans.first()
                loadWarDataForClan(selectedClan!!)
            }
        }
        updateSelectedClanDisplay()
    }

    private fun saveBookmarkedClans() {
        val json = gson.toJson(bookmarkedClans)
        preferences.edit().putString("bookmarked_clans", json).apply()
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
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
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
        
        fun hasResults(): Boolean {
            return results.isNotEmpty()
        }
    }
    
    inner class ClanSelectorAdapter(
        private val clans: List<BookmarkedClan>,
        private val onClanClick: (BookmarkedClan) -> Unit
    ) : RecyclerView.Adapter<ClanSelectorAdapter.ViewHolder>() {
        
        inner class ViewHolder(val binding: ItemBookmarkCardBinding) : RecyclerView.ViewHolder(binding.root)
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemBookmarkCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val clan = clans[position]
            
            holder.binding.tvClanName.text = clan.name
            holder.binding.tvClanTag.text = clan.tag
            holder.binding.tvClanLevel.text = "Level: ${clan.level}"
            holder.binding.tvMembersCount.text = getString(R.string.members_count, clan.members)
            
            Glide.with(holder.binding.ivClanBadge)
                .load(clan.badge)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .circleCrop()
                .into(holder.binding.ivClanBadge)
            
            holder.binding.root.setOnClickListener {
                onClanClick(clan)
            }
            
            // Handle bookmark removal with confirmation
            holder.binding.ivBookmark.setOnClickListener {
                showRemoveBookmarkConfirmation(clan)
            }
        }
        
        override fun getItemCount() = clans.size
    }
    
    inner class WarPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        private var warData: WarResponse? = null
        
        override fun getItemCount(): Int = 2
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> {
                    currentOverviewFragment = OverviewFragment()
                    warData?.let { currentOverviewFragment?.updateWarData(it) }
                    currentOverviewFragment!!
                }
                1 -> {
                    currentActivityFragment = ActivityFragment()
                    warData?.let { currentActivityFragment?.updateWarData(it) }
                    currentActivityFragment!!
                }
                else -> throw IllegalArgumentException("Invalid position")
            }
        }
        
        fun updateWarData(data: WarResponse) {
            warData = data
            currentOverviewFragment?.updateWarData(data)
            currentActivityFragment?.updateWarData(data)
        }
    }
}