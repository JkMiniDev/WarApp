package com.jkminidev.clashberry.utils

import android.content.Context
import com.jkminidev.clashberry.R

object TownHallHelper {
    
    fun getTHImageResource(level: Int, context: Context? = null): Int {
        // Ensure level is within valid range (1-17)
        val validLevel = when {
            level < 1 -> 1
            level > 17 -> 17
            else -> level
        }
        
        return if (context != null) {
            // Try to get the resource ID for the townhall image from res/drawable/th directory
            val resourceName = "th$validLevel"
            val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
            
            if (resourceId != 0) {
                resourceId
            } else {
                // Fallback to placeholder if th image not found
                R.drawable.ic_placeholder
            }
        } else {
            // Fallback to placeholder if no context provided
            R.drawable.ic_placeholder
        }
    }
    
    fun getStarsDisplay(stars: Int): String {
        val filledStars = "★".repeat(stars)
        val emptyStars = "☆".repeat(3 - stars)
        return filledStars + emptyStars
    }
}