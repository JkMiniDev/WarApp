package com.jkminidev.clashberry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jkminidev.clashberry.R
import com.jkminidev.clashberry.data.MemberData
import com.jkminidev.clashberry.utils.TownHallHelper

class MemberAdapter(
    private val members: List<MemberData>,
    private val displayType: DisplayType
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    enum class DisplayType {
        ATTACKS, DEFENSES, ROSTER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(members[position], displayType)
    }

    override fun getItemCount(): Int = members.size

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvThEmoji: TextView = itemView.findViewById(R.id.tvThEmoji)
        private val tvMemberName: TextView = itemView.findViewById(R.id.tvMemberName)
        private val tvMemberTag: TextView = itemView.findViewById(R.id.tvMemberTag)
        private val tvMapPosition: TextView = itemView.findViewById(R.id.tvMapPosition)
        private val layoutAttackInfo: LinearLayout = itemView.findViewById(R.id.layoutAttackInfo)
        private val layoutAttacks: LinearLayout = itemView.findViewById(R.id.layoutAttacks)
        private val layoutDefenseInfo: LinearLayout = itemView.findViewById(R.id.layoutDefenseInfo)
        private val layoutDefenses: LinearLayout = itemView.findViewById(R.id.layoutDefenses)

        fun bind(member: MemberData, displayType: DisplayType) {
            // Basic member info
            tvThEmoji.text = member.thEmoji
            tvMemberName.text = member.name
            tvMemberTag.text = member.tag
            tvMapPosition.text = "#${member.mapPosition}"

            // Clear previous content
            layoutAttacks.removeAllViews()
            layoutDefenses.removeAllViews()
            
            // Hide all sections first
            layoutAttackInfo.visibility = View.GONE
            layoutDefenseInfo.visibility = View.GONE

            when (displayType) {
                DisplayType.ATTACKS -> showAttacks(member)
                DisplayType.DEFENSES -> showDefenses(member)
                DisplayType.ROSTER -> {
                    // Just show basic info for roster
                }
            }
        }

        private fun showAttacks(member: MemberData) {
            if (member.attacks.isNotEmpty()) {
                layoutAttackInfo.visibility = View.VISIBLE
                
                member.attacks.forEach { attack ->
                    val attackView = createAttackView(attack.stars, attack.destructionPercentage)
                    layoutAttacks.addView(attackView)
                }
            } else {
                layoutAttackInfo.visibility = View.VISIBLE
                val noAttacksView = TextView(itemView.context).apply {
                    text = itemView.context.getString(R.string.no_attacks_yet)
                    setTextColor(itemView.context.getColor(R.color.text_color_secondary))
                    textSize = 14f
                    setPadding(8, 8, 8, 8)
                }
                layoutAttacks.addView(noAttacksView)
            }
        }

        private fun showDefenses(member: MemberData) {
            if (member.opponentAttacks > 0) {
                layoutDefenseInfo.visibility = View.VISIBLE
                
                val defenseView = TextView(itemView.context).apply {
                    text = "${member.opponentAttacks} defense(s) received"
                    setTextColor(itemView.context.getColor(R.color.text_color))
                    textSize = 14f
                    setPadding(8, 8, 8, 8)
                }
                layoutDefenses.addView(defenseView)
            }
        }

        private fun createAttackView(stars: Int, destruction: Double): View {
            val inflater = LayoutInflater.from(itemView.context)
            val attackView = inflater.inflate(R.layout.item_attack, null)
            
            attackView.findViewById<TextView>(R.id.tvStars).text = 
                TownHallHelper.getStarsDisplay(stars)
            attackView.findViewById<TextView>(R.id.tvDestruction).text = 
                "${String.format("%.1f", destruction)}%"
            
            return attackView
        }
    }
}