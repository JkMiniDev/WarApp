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

class WarDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityWarDetailBinding
    private lateinit var warDisplayHelper: WarDisplayHelper
    private val gson = Gson()
    private lateinit var warData: WarResponse
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWarDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        warDisplayHelper = WarDisplayHelper(this)
        
        // Set up toolbar
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Get war data from intent
        val warDataJson = intent.getStringExtra("war_data")
        if (warDataJson != null) {
            warData = gson.fromJson(warDataJson, WarResponse::class.java)
            setupViewPagerAndTabs()
        } else {
            finish()
        }
    }

    private fun setupViewPagerAndTabs() {
        val adapter = WarPagerAdapter(this, warData)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 3
        // TabLayout and ViewPager2 sync
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.overview)))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Activity"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.roster)))
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                tab?.let { binding.viewPager.currentItem = it.position }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position))
            }
        })
    }

    class WarPagerAdapter(fa: FragmentActivity, private val warData: WarResponse) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> OverviewFragment.newInstance(warData)
                1 -> ActivityFragment.newInstance(warData)
                2 -> RoasterFragment.newInstance(warData)
                else -> throw IllegalArgumentException()
            }
        }
    }

    class OverviewFragment : Fragment() {
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
            val warData = Gson().fromJson(requireArguments().getString(ARG_WAR_DATA), WarResponse::class.java)
            val helper = WarDisplayHelper(context)
            val frame = FrameLayout(context)
            helper.displayWar(warData, frame, com.google.android.material.tabs.TabLayout(context)) // Only overview logic will be used
            return frame
        }
    }

    class ActivityFragment : Fragment() {
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
            val warData = Gson().fromJson(requireArguments().getString(ARG_WAR_DATA), WarResponse::class.java)
            val frame = FrameLayout(context)
            // Use the helper's showActivityTab logic
            val helper = WarDisplayHelper(context)
            helper.showActivityTab(frame, warData, true) { }
            return frame
        }
    }

    class RoasterFragment : Fragment() {
        companion object {
            private const val ARG_WAR_DATA = "war_data"
            fun newInstance(warData: WarResponse): RoasterFragment {
                val fragment = RoasterFragment()
                val args = Bundle()
                args.putString(ARG_WAR_DATA, Gson().toJson(warData))
                fragment.arguments = args
                return fragment
            }
        }
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val context = requireContext()
            val warData = Gson().fromJson(requireArguments().getString(ARG_WAR_DATA), WarResponse::class.java)
            val frame = FrameLayout(context)
            val helper = WarDisplayHelper(context)
            helper.showRosterTab(frame, warData)
            return frame
        }
    }
}