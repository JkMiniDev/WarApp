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

class WarDisplayHelper(private val context: Context) {
    
    fun displayWar(warData: WarResponse, container: FrameLayout, tabLayout: com.google.android.material.tabs.TabLayout) {
        // Set up tabs
        tabLayout.removeAllTabs()
        tabLayout.addTab(tabLayout.newTab().setText(context.getString(R.string.overview)))
        tabLayout.addTab(tabLayout.newTab().setText("Activity"))
        tabLayout.addTab(tabLayout.newTab().setText(context.getString(R.string.roster)))

        // Show initial content (overview)
        showOverviewTab(container, warData)

        // Track which activity view is selected (attack or defence)
        var showAttacks = true

        fun showActivityTab() {
            showActivityTab(container, warData, showAttacks) { isAttack ->
                showAttacks = isAttack
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
            statusDisplay += " • 00:00"
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
    
    fun showActivityTab(container: FrameLayout, warData: WarResponse, showAttacks: Boolean, onToggle: (Boolean) -> Unit) {
        container.removeAllViews()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        // Toggle button (Switch or SegmentedButton)
        val toggle = android.widget.Switch(context).apply {
            text = if (showAttacks) context.getString(R.string.attacks) else context.getString(R.string.defenses)
            isChecked = showAttacks
            setOnCheckedChangeListener { _, isChecked ->
                text = if (isChecked) context.getString(R.string.attacks) else context.getString(R.string.defenses)
                layout.removeViewAt(1)
                val recyclerView = RecyclerView(context).apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = MemberAdapter(warData.clan.members, if (isChecked) MemberAdapter.DisplayType.ATTACKS else MemberAdapter.DisplayType.DEFENSES)
                    setPadding(8, 8, 8, 8)
                }
                layout.addView(recyclerView)
                onToggle(isChecked)
            }
        }
        // Align toggle to right
        val toggleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
            addView(toggle)
        }
        layout.addView(toggleContainer)
        val recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MemberAdapter(warData.clan.members, if (showAttacks) MemberAdapter.DisplayType.ATTACKS else MemberAdapter.DisplayType.DEFENSES)
            setPadding(8, 8, 8, 8)
        }
        layout.addView(recyclerView)
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