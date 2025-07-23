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
    private lateinit var warPagerAdapter: WarPagerAdapter
    private var currentOverviewFragment: OverviewFragment? = null
    private var currentActivityFragment: ActivityFragment? = null
    private lateinit var warContentContainer: FrameLayout
    
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
        
        // Add a FrameLayout for war content if not present
        val contentContainer = FrameLayout(this)
        contentContainer.id = View.generateViewId()
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0, 1f
        )
        (binding.root as ViewGroup).addView(contentContainer, 1, params)
        warContentContainer = contentContainer
        
        // Get war data from intent
        val warDataJson = intent.getStringExtra("war_data")
        if (warDataJson != null) {
            warData = gson.fromJson(warDataJson, WarResponse::class.java)
            showWarContent(warData)
        } else {
            finish()
        }
    }

    private fun showWarContent(warData: WarResponse) {
        // Don't clear until new data is ready
        val newContent = FrameLayout(this)
        warDisplayHelper.displayWar(warData, newContent, com.google.android.material.tabs.TabLayout(this))
        warContentContainer.addView(newContent)
        // Remove the old view (keep only the last one)
        while (warContentContainer.childCount > 1) warContentContainer.removeViewAt(0)
    }

    private fun refreshWarData() {
        showLoadingOverlay()
        val clanTag = warData.clan.tag
        lifecycleScope.launch {
            try {
                val response = com.jkminidev.clashberry.network.NetworkModule.apiService.getWarData(clanTag)
                if (response.isSuccessful) {
                    response.body()?.let { newWarData ->
                        warData = newWarData
                        showWarContent(newWarData)
                    }
                } else {
                    android.widget.Toast.makeText(this@WarDetailActivity, "Failed to refresh war data", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@WarDetailActivity, "Error refreshing war data", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                hideLoadingOverlay()
            }
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
}