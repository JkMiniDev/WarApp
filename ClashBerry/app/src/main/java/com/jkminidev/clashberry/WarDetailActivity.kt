package com.jkminidev.clashberry

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.jkminidev.clashberry.data.WarResponse
import com.jkminidev.clashberry.databinding.ActivityWarDetailBinding
import com.jkminidev.clashberry.ui.WarDisplayHelper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.os.Parcelable
import android.os.Parcel
import android.widget.FrameLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.LinearLayout

class WarDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityWarDetailBinding
    private lateinit var warDisplayHelper: WarDisplayHelper
    private val gson = Gson()
    private lateinit var warData: WarResponse
    private lateinit var warContentContainer: FrameLayout
    private lateinit var warPagerAdapter: WarPagerAdapter
    private var currentOverviewFragment: OverviewFragment? = null
    private var currentActivityFragment: ActivityFragment? = null
    
    companion object {
        private const val KEY_WAR_DATA = "war_data"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWarDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar) // Ensure toolbar acts as ActionBar
        warDisplayHelper = WarDisplayHelper(this)
        
        // Set up toolbar navigation
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Restore war data from saved state or intent
        val warDataJson = if (savedInstanceState != null) {
            savedInstanceState.getString(KEY_WAR_DATA)
        } else {
            intent.getStringExtra("war_data")
        }
        
        if (warDataJson != null) {
            warData = gson.fromJson(warDataJson, WarResponse::class.java)
            setupViewPagerAndBottomNav()
        } else {
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save war data to prevent loss on configuration changes
        if (::warData.isInitialized) {
            outState.putString(KEY_WAR_DATA, gson.toJson(warData))
        }
    }

    private fun setupViewPagerAndBottomNav() {
        if (!::warData.isInitialized) {
            android.util.Log.e("WarDetailActivity", "War data not initialized when setting up ViewPager")
            return
        }
        
        warPagerAdapter = WarPagerAdapter(this)
        binding.viewPager.adapter = warPagerAdapter
        binding.viewPager.offscreenPageLimit = 2
        
        // Disable swipe functionality
        binding.viewPager.isUserInputEnabled = false
        
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
        
        // Set initial selection
        binding.bottomNavigationView.selectedItemId = R.id.nav_overview
    }

    private fun refreshWarData() {
        showLoadingOverlay()
        val clanTag = warData.clan.tag
        val currentTabPosition = binding.viewPager.currentItem // Save current tab position
        lifecycleScope.launch {
            try {
                val response = com.jkminidev.clashberry.network.NetworkModule.apiService.getWarData(clanTag)
                if (response.isSuccessful) {
                    response.body()?.let { newWarData ->
                        warData = newWarData
                        updateCurrentFragmentWithNewData(newWarData)
                        // Restore the tab position after refresh
                        binding.viewPager.post {
                            binding.viewPager.currentItem = currentTabPosition
                        }
                        android.widget.Toast.makeText(this@WarDetailActivity, "Refreshed Successful", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorHandler = com.jkminidev.clashberry.utils.ErrorHandler
                    val errorResponse = errorHandler.parseError(response)
                    android.widget.Toast.makeText(this@WarDetailActivity, errorResponse.message, android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@WarDetailActivity, "Connection Failed", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                hideLoadingOverlay()
            }
        }
    }

    private fun updateCurrentFragmentWithNewData(newWarData: WarResponse) {
        try {
            // Update all fragments that might be instantiated without recreating adapter
            val fm = supportFragmentManager
            
            // Try to find fragments using different methods
            for (i in 0 until warPagerAdapter.itemCount) {
                val fragmentTag = "f$i"
                var fragment = fm.findFragmentByTag(fragmentTag)
                
                // Fallback to the old method if the new one doesn't work
                if (fragment == null) {
                    fragment = fm.findFragmentByTag("android:switcher:${binding.viewPager.id}:$i")
                }
                
                when (fragment) {
                    is OverviewFragment -> fragment.bindWarData(newWarData)
                    is ActivityFragment -> fragment.bindWarData(newWarData)
                }
            }
            
            // DON'T recreate the adapter - this was causing the tab reset
            // Just update the existing fragments with new data
            
        } catch (e: Exception) {
            android.util.Log.e("WarDetailActivity", "Error updating fragments", e)
            // Only recreate adapter as last resort, and preserve tab position
            val currentPosition = binding.viewPager.currentItem
            warPagerAdapter = WarPagerAdapter(this)
            binding.viewPager.adapter = warPagerAdapter
            binding.viewPager.post {
                binding.viewPager.currentItem = currentPosition
            }
        }
    }

    private fun showLoadingOverlay() {
        runOnUiThread {
            hideLoadingOverlay() // Remove any existing overlay first
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
        runOnUiThread {
            val overlay = binding.root.findViewById<android.widget.FrameLayout>(R.id.loadingOverlay)
            overlay?.let { (binding.root as ViewGroup).removeView(it) }
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_refresh -> {
                refreshWarData()
                true
            }
            R.id.menu_settings -> {
                startActivity(android.content.Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    inner class WarPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> OverviewFragment.newInstance(warData).also { currentOverviewFragment = it }
                1 -> ActivityFragment.newInstance(warData).also { currentActivityFragment = it }
                else -> throw IllegalArgumentException()
            }
        }
    }

    class OverviewFragment : Fragment() {
        private var warData: WarResponse? = null
        private var rootView: FrameLayout? = null
        companion object {
            private const val ARG_WAR_DATA = "war_data"
            fun newInstance(warData: WarResponse): OverviewFragment {
                val fragment = OverviewFragment()
                val args = Bundle()
                args.putString(ARG_WAR_DATA, Gson().toJson(warData))
                fragment.arguments = args
                return fragment
            }
        }
        
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val context = requireContext()
            
            // Restore war data from arguments or saved state
            val warDataJson = savedInstanceState?.getString(ARG_WAR_DATA) 
                ?: requireArguments().getString(ARG_WAR_DATA)
            
            warData = Gson().fromJson(warDataJson, WarResponse::class.java)
            val helper = WarDisplayHelper(context)
            val frame = FrameLayout(context)
            warData?.let { helper.displayWar(it, frame, com.google.android.material.tabs.TabLayout(context)) }
            rootView = frame
            return frame
        }
        
        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            warData?.let {
                outState.putString(ARG_WAR_DATA, Gson().toJson(it))
            }
        }
        
        fun bindWarData(newWarData: WarResponse) {
            warData = newWarData
            // Update only the data views, not the root view
            rootView?.let {
                it.removeAllViews()
                val helper = WarDisplayHelper(requireContext())
                helper.displayWar(newWarData, it, com.google.android.material.tabs.TabLayout(requireContext()))
            }
        }
    }

    class ActivityFragment : Fragment() {
        private var warData: WarResponse? = null
        private var rootView: FrameLayout? = null
        private var selectedSubTab: Int = 0 // 0: Attack, 1: Defence, 2: Remaining/Missed
        private var selectedClan: Int = 0 // 0: own clan, 1: opponent clan
        
        companion object {
            private const val ARG_WAR_DATA = "war_data"
            private const val ARG_SELECTED_SUB_TAB = "selected_sub_tab"
            private const val ARG_SELECTED_CLAN = "selected_clan"
            
            fun newInstance(warData: WarResponse): ActivityFragment {
                val fragment = ActivityFragment()
                val args = Bundle()
                args.putString(ARG_WAR_DATA, Gson().toJson(warData))
                fragment.arguments = args
                return fragment
            }
        }
        
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val context = requireContext()
            
            // Restore war data from arguments or saved state
            val warDataJson = savedInstanceState?.getString(ARG_WAR_DATA) 
                ?: requireArguments().getString(ARG_WAR_DATA)
            
            // Restore selected sub-tab and clan state
            selectedSubTab = savedInstanceState?.getInt(ARG_SELECTED_SUB_TAB, 0) ?: 0
            selectedClan = savedInstanceState?.getInt(ARG_SELECTED_CLAN, 0) ?: 0
            
            warData = Gson().fromJson(warDataJson, WarResponse::class.java)
            val frame = FrameLayout(context)
            warData?.let {
                val helper = WarDisplayHelper(context)
                helper.showActivityTab(frame, it, selectedSubTab, selectedClan) { newSelectedTab, newSelectedClan ->
                    selectedSubTab = newSelectedTab
                    selectedClan = newSelectedClan
                }
            }
            rootView = frame
            return frame
        }
        
        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            warData?.let {
                outState.putString(ARG_WAR_DATA, Gson().toJson(it))
            }
            outState.putInt(ARG_SELECTED_SUB_TAB, selectedSubTab)
            outState.putInt(ARG_SELECTED_CLAN, selectedClan)
        }
        
        fun bindWarData(newWarData: WarResponse) {
            warData = newWarData
            // Update only the data views, not the root view, preserve selected sub-tab
            rootView?.let {
                it.removeAllViews()
                val helper = WarDisplayHelper(requireContext())
                helper.showActivityTab(it, newWarData, selectedSubTab, selectedClan) { newSelectedTab, newSelectedClan ->
                    selectedSubTab = newSelectedTab
                    selectedClan = newSelectedClan
                }
            }
        }
    }
}