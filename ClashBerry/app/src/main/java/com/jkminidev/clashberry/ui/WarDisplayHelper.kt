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

class WarDisplayHelper(private val context: Context) {
    
    fun displayWar(warData: WarResponse, container: LinearLayout) {
        container.removeAllViews()
        
        val warCard = createWarCard(warData)
        container.addView(warCard)
    }
    
    private fun createWarCard(warData: WarResponse): View {
        val inflater = LayoutInflater.from(context)
        val cardView = inflater.inflate(R.layout.war_card, null) as CardView
        
        setupWarHeader(cardView, warData)
        setupWarStats(cardView, warData)
        setupTabLayout(cardView, warData)
        
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
    
    private fun setupTabLayout(cardView: CardView, warData: WarResponse) {
        val tabLayout = cardView.findViewById<TabLayout>(R.id.tabLayout)
        val contentContainer = cardView.findViewById<LinearLayout>(R.id.contentContainer)
        
        // Add tabs
        tabLayout.addTab(tabLayout.newTab().setText(context.getString(R.string.overview)))
        tabLayout.addTab(tabLayout.newTab().setText(context.getString(R.string.attacks)))
        tabLayout.addTab(tabLayout.newTab().setText(context.getString(R.string.defenses)))
        tabLayout.addTab(tabLayout.newTab().setText(context.getString(R.string.roster)))
        
        // Show initial content (overview)
        showOverviewTab(contentContainer, warData)
        
        // Tab selection listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showOverviewTab(contentContainer, warData)
                    1 -> showAttacksTab(contentContainer, warData)
                    2 -> showDefensesTab(contentContainer, warData)
                    3 -> showRosterTab(contentContainer, warData)
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun showOverviewTab(container: LinearLayout, warData: WarResponse) {
        container.removeAllViews()
        
        val textView = TextView(context).apply {
            text = context.getString(R.string.war_overview)
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.text_color))
            setPadding(16, 16, 16, 32)
        }
        container.addView(textView)
        
        // Add team composition here if needed
    }
    
    private fun showAttacksTab(container: LinearLayout, warData: WarResponse) {
        container.removeAllViews()
        
        val recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MemberAdapter(warData.clan.members, MemberAdapter.DisplayType.ATTACKS)
            setPadding(8, 8, 8, 8)
        }
        container.addView(recyclerView)
    }
    
    private fun showDefensesTab(container: LinearLayout, warData: WarResponse) {
        container.removeAllViews()
        
        val recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MemberAdapter(warData.clan.members, MemberAdapter.DisplayType.DEFENSES)
            setPadding(8, 8, 8, 8)
        }
        container.addView(recyclerView)
    }
    
    private fun showRosterTab(container: LinearLayout, warData: WarResponse) {
        container.removeAllViews()
        
        val recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MemberAdapter(warData.clan.members, MemberAdapter.DisplayType.ROSTER)
            setPadding(8, 8, 8, 8)
        }
        container.addView(recyclerView)
    }
}