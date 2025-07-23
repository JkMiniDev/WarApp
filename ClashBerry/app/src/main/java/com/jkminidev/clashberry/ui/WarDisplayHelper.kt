package com.jkminidev.clashberry.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import androidx.cardview.widget.CardView
import com.google.android.material.tabs.TabLayout
import com.jkminidev.clashberry.R
import com.jkminidev.clashberry.data.WarResponse
import com.jkminidev.clashberry.adapters.MemberAdapter
import com.jkminidev.clashberry.utils.TownHallHelper
import android.widget.FrameLayout
import android.widget.RadioGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import android.os.Bundle

class WarDisplayHelper(private val context: Context) {
    
    // SubTab Fragment for ViewPager2
    class SubTabFragment : Fragment() {
        companion object {
            private const val ARG_TAB_TYPE = "tab_type"
            private const val ARG_WAR_DATA = "war_data"
            
            fun newInstance(tabType: Int, warData: WarResponse): SubTabFragment {
                val fragment = SubTabFragment()
                val args = Bundle()
                args.putInt(ARG_TAB_TYPE, tabType)
                args.putString(ARG_WAR_DATA, com.google.gson.Gson().toJson(warData))
                fragment.arguments = args
                return fragment
            }
        }
        
        override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): android.view.View? {
            val context = requireContext()
            val tabType = requireArguments().getInt(ARG_TAB_TYPE)
            val warData = com.google.gson.Gson().fromJson(requireArguments().getString(ARG_WAR_DATA), WarResponse::class.java)
            
            val recyclerView = RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                setPadding(8, 8, 8, 8)
            }
            
            val filteredMembers = when (tabType) {
                0 -> warData.clan.members.filter { it.attacks.isNotEmpty() }
                1 -> warData.clan.members // Defence logic unchanged
                2 -> {
                    val attacksExpected = if (warData.warType == "cwl") 1 else 2
                    warData.clan.members.filter { it.attacks.size < attacksExpected }
                }
                else -> warData.clan.members
            }
            
            val displayType = when (tabType) {
                0 -> MemberAdapter.DisplayType.ATTACKS
                1 -> MemberAdapter.DisplayType.DEFENSES
                2 -> MemberAdapter.DisplayType.REMAINING
                else -> MemberAdapter.DisplayType.ATTACKS
            }
            
            recyclerView.adapter = MemberAdapter(filteredMembers, displayType)
            return recyclerView
        }
    }
    
    // Adapter for sub-tab ViewPager2
    inner class SubTabPagerAdapter(
        private val options: List<String>,
        private val warData: WarResponse
    ) : FragmentStateAdapter(context as FragmentActivity) {
        
        override fun getItemCount(): Int = options.size
        
        override fun createFragment(position: Int): Fragment {
            return SubTabFragment.newInstance(position, warData)
        }
    }
    
    fun displayWar(warData: WarResponse, container: FrameLayout, tabLayout: com.google.android.material.tabs.TabLayout) {
        // Set up tabs
        tabLayout.removeAllTabs()
        tabLayout.addTab(tabLayout.newTab().setText(context.getString(R.string.overview)))
        tabLayout.addTab(tabLayout.newTab().setText("Activity"))
        tabLayout.addTab(tabLayout.newTab().setText(context.getString(R.string.roster)))

        // Show initial content (overview)
        showOverviewTab(container, warData)

        // Track which activity view is selected (0: Attack, 1: Defence, 2: Remaining/Missed)
        var selectedActivityTab = 0

        fun showActivityTab() {
            showActivityTab(container, warData, selectedActivityTab) { selectedTab ->
                selectedActivityTab = selectedTab
            }
        }

        // Tab selection listener
        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showOverviewTab(container, warData)
                    1 -> showActivityTab()
                    2 -> showRosterTab(container, warData)
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        // Optional: Add ViewPager2 for swipe navigation (if available in the project)
        // This would require refactoring to use a FragmentStateAdapter or similar.
    }
    
    private fun createWarCard(warData: WarResponse): View {
        val inflater = LayoutInflater.from(context)
        val cardView = inflater.inflate(R.layout.war_card, null) as CardView
        
        setupWarHeader(cardView, warData)
        setupWarStats(cardView, warData)
        
        return cardView
    }
    
    private fun setupWarHeader(cardView: CardView, warData: WarResponse) {
        // Clan 1 info
        val ivClan1Badge = cardView.findViewById<android.widget.ImageView>(R.id.ivClan1Badge)
        val tvClan1Name = cardView.findViewById<TextView>(R.id.tvClan1Name)
        val tvClan1Tag = cardView.findViewById<TextView>(R.id.tvClan1Tag)
        
        Glide.with(context)
            .load(warData.clan.badge)
            .placeholder(R.mipmap.ic_launcher)
            .error(R.mipmap.ic_launcher)
            .circleCrop()
            .into(ivClan1Badge)
        
        tvClan1Name.text = warData.clan.name
        tvClan1Tag.text = warData.clan.tag
        
        // Clan 2 info
        val ivClan2Badge = cardView.findViewById<android.widget.ImageView>(R.id.ivClan2Badge)
        val tvClan2Name = cardView.findViewById<TextView>(R.id.tvClan2Name)
        val tvClan2Tag = cardView.findViewById<TextView>(R.id.tvClan2Tag)
        
        Glide.with(context)
            .load(warData.opponent.badge)
            .placeholder(R.mipmap.ic_launcher)
            .error(R.mipmap.ic_launcher)
            .circleCrop()
            .into(ivClan2Badge)
        
        tvClan2Name.text = warData.opponent.name
        tvClan2Tag.text = warData.opponent.tag
        
        // War status
        val tvWarStatus = cardView.findViewById<TextView>(R.id.tvWarStatus)
        val statusText = when (warData.state) {
            "preparation" -> context.getString(R.string.preparation)
            "inWar" -> context.getString(R.string.battle_day)
            "warEnded" -> context.getString(R.string.war_ended)
            else -> context.getString(R.string.unknown)
        }
        
        var statusDisplay = statusText
        if (warData.timeRemaining != null && warData.timeLabel != null) {
            statusDisplay += " • ${warData.timeRemaining} ${warData.timeLabel}"
        } else if (warData.state == "warEnded") {
            statusDisplay += " • 0h 00m"
        }
        if (warData.warType == "cwl" && warData.cwlRound != null) {
            statusDisplay += " • ${context.getString(R.string.cwl_round, warData.cwlRound)}"
        }
        
        tvWarStatus.text = statusDisplay
        
        // Set status color
        val statusColor = when (warData.state) {
            "preparation" -> R.color.preparation_color
            "inWar" -> R.color.in_war_color
            "warEnded" -> R.color.war_ended_color
            else -> R.color.text_color_secondary
        }
        tvWarStatus.setTextColor(ContextCompat.getColor(context, statusColor))
    }
    
    private fun setupWarStats(cardView: CardView, warData: WarResponse) {
        val attacksExpected = if (warData.warType == "cwl") 1 else 2
        val totalAttacks = warData.teamSize * attacksExpected
        
        // Clan 1 stats
        cardView.findViewById<TextView>(R.id.tvClan1Stars).text = 
            "${warData.clan.stars}/${warData.teamSize * 3}"
        cardView.findViewById<TextView>(R.id.tvClan1Destruction).text = 
            "${String.format("%.2f", warData.clan.destructionPercentage)}%"
        cardView.findViewById<TextView>(R.id.tvClan1Attacks).text = 
            "${warData.clan.attacks}/$totalAttacks"
        
        // Clan 2 stats
        cardView.findViewById<TextView>(R.id.tvClan2Stars).text = 
            "${warData.opponent.stars}/${warData.teamSize * 3}"
        cardView.findViewById<TextView>(R.id.tvClan2Destruction).text = 
            "${String.format("%.2f", warData.opponent.destructionPercentage)}%"
        cardView.findViewById<TextView>(R.id.tvClan2Attacks).text = 
            "${warData.opponent.attacks}/$totalAttacks"
    }
    
    private fun showOverviewTab(container: FrameLayout, warData: WarResponse) {
        container.removeAllViews()
        val warCard = createWarCard(warData)
        container.addView(warCard)
    }
    
    fun showActivityTab(container: FrameLayout, warData: WarResponse, initialSelectedTab: Int, onToggle: (Int) -> Unit) {
        container.removeAllViews()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        val isWarEnded = warData.state == "warEnded"
        val thirdLabel = if (isWarEnded) context.getString(R.string.missed) else context.getString(R.string.remaining)
        val options = listOf(
            context.getString(R.string.attacks),
            context.getString(R.string.defenses),
            thirdLabel
        )
        var selected = initialSelectedTab // 0: Attack, 1: Defence, 2: Remaining/Missed
        
        // Create ViewPager2 for swipe functionality between sub-tabs
        val viewPager = androidx.viewpager2.widget.ViewPager2(context)
        val subTabAdapter = SubTabPagerAdapter(options, warData)
        viewPager.adapter = subTabAdapter
        
        // Set current item after a delay to avoid initialization issues
        viewPager.post {
            viewPager.currentItem = selected
        }
        
        // Custom toggle bar
        val toggleBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }
        val toggleViews = mutableListOf<TextView>()
        fun updateToggle() {
            toggleViews.forEachIndexed { idx, tv ->
                if (idx == selected) {
                    tv.setTextColor(ContextCompat.getColor(context, R.color.accent_color))
                    tv.setTypeface(tv.typeface, android.graphics.Typeface.BOLD)
                    // Only set underline, no background color
                    tv.background = null
                    tv.setBackgroundResource(R.drawable.toggle_underline)
                } else {
                    tv.setTextColor(ContextCompat.getColor(context, R.color.text_color))
                    tv.setTypeface(tv.typeface, android.graphics.Typeface.BOLD)
                    tv.background = null
                }
            }
        }
        
        options.forEachIndexed { idx, label ->
            val tv = TextView(context).apply {
                text = label
                setPadding(32, 16, 32, 16)
                setTextColor(ContextCompat.getColor(context, R.color.text_color))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setOnClickListener {
                    if (selected != idx) {
                        viewPager.currentItem = idx
                    }
                }
            }
            toggleViews.add(tv)
            toggleBar.addView(tv)
        }
        
        // Add ViewPager2 page change callback
        viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                selected = position
                updateToggle()
                onToggle(selected)
            }
        })
        
        // Add toggle bar and ViewPager2
        layout.addView(toggleBar)
        layout.addView(viewPager, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ))
        updateToggle()
        container.addView(layout)
    }
    
    fun showRosterTab(container: FrameLayout, warData: WarResponse) {
        container.removeAllViews()
        val recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MemberAdapter(warData.clan.members, MemberAdapter.DisplayType.ROSTER)
            setPadding(8, 8, 8, 8)
        }
        container.addView(recyclerView)
    }
}