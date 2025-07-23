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

class WarDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityWarDetailBinding
    private lateinit var warDisplayHelper: WarDisplayHelper
    private val gson = Gson()
    private lateinit var warData: WarResponse
    private lateinit var warPagerAdapter: WarPagerAdapter
    private var currentOverviewFragment: OverviewFragment? = null
    private var currentActivityFragment: ActivityFragment? = null
    
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
        
        // Get war data from intent
        val warDataJson = intent.getStringExtra("war_data")
        if (warDataJson != null) {
            warData = gson.fromJson(warDataJson, WarResponse::class.java)
            setupViewPagerAndBottomNav()
        } else {
            finish()
        }
    }

    private fun setupViewPagerAndBottomNav() {
        warPagerAdapter = WarPagerAdapter(this)
        binding.viewPager.adapter = warPagerAdapter
        binding.viewPager.offscreenPageLimit = 2
        binding.bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_overview -> binding.viewPager.currentItem = 0
                R.id.nav_activity -> binding.viewPager.currentItem = 1
            }
            true
        }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.bottomNavigationView.menu.getItem(position).isChecked = true
            }
        })
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

    private fun refreshWarData() {
        // Show loading indicator (add a simple ProgressBar overlay if not present)
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
        val clanTag = warData.clan.tag
        lifecycleScope.launch {
            try {
                val response = com.jkminidev.clashberry.network.NetworkModule.apiService.getWarData(clanTag)
                if (response.isSuccessful) {
                    response.body()?.let { newWarData ->
                        warData = newWarData
                        // Update fragments directly
                        updateFragmentsWithNewData(newWarData)
                    }
                } else {
                    android.widget.Toast.makeText(this@WarDetailActivity, "Failed to refresh war data", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@WarDetailActivity, "Error refreshing war data", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                val overlay = binding.root.findViewById<android.widget.FrameLayout>(R.id.loadingOverlay)
                overlay?.let { (binding.root as ViewGroup).removeView(it) }
            }
        }
    }

    private fun updateFragmentsWithNewData(newWarData: WarResponse) {
        val fm = supportFragmentManager
        val overviewTag = "f0"
        val activityTag = "f1"
        val overviewFragment = fm.findFragmentByTag("android:switcher:${binding.viewPager.id}:0") as? OverviewFragment
        val activityFragment = fm.findFragmentByTag("android:switcher:${binding.viewPager.id}:1") as? ActivityFragment
        overviewFragment?.updateWarData(newWarData)
        activityFragment?.updateWarData(newWarData)
    }

    inner class WarPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> OverviewFragment.newInstance(warData)
                1 -> ActivityFragment.newInstance(warData)
                else -> throw IllegalArgumentException()
            }
        }
    }

    class OverviewFragment : Fragment() {
        private var warData: WarResponse? = null
        private var rootView: View? = null
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
            warData = Gson().fromJson(requireArguments().getString(ARG_WAR_DATA), WarResponse::class.java)
            val helper = WarDisplayHelper(context)
            val frame = FrameLayout(context)
            warData?.let { helper.displayWar(it, frame, com.google.android.material.tabs.TabLayout(context)) }
            rootView = frame
            return frame
        }
        fun updateWarData(newWarData: WarResponse) {
            warData = newWarData
            rootView?.let {
                (it as? FrameLayout)?.removeAllViews()
                val helper = WarDisplayHelper(requireContext())
                helper.displayWar(newWarData, it as FrameLayout, com.google.android.material.tabs.TabLayout(requireContext()))
            }
        }
    }

    class ActivityFragment : Fragment() {
        private var warData: WarResponse? = null
        private var rootView: View? = null
        companion object {
            private const val ARG_WAR_DATA = "war_data"
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
            warData = Gson().fromJson(requireArguments().getString(ARG_WAR_DATA), WarResponse::class.java)
            val frame = FrameLayout(context)
            warData?.let {
                val helper = WarDisplayHelper(context)
                helper.showActivityTab(frame, it, true) { }
            }
            rootView = frame
            return frame
        }
        fun updateWarData(newWarData: WarResponse) {
            warData = newWarData
            rootView?.let {
                (it as? FrameLayout)?.removeAllViews()
                val helper = WarDisplayHelper(requireContext())
                helper.showActivityTab(it as FrameLayout, newWarData, true) { }
            }
        }
    }
}