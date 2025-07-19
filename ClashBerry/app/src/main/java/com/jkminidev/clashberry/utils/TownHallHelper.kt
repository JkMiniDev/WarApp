package com.jkminidev.clashberry.utils

object TownHallHelper {
    
    fun getTHImageUrl(level: Int): String {
        // Example URLs for townhall images - replace with your actual image URLs
        val baseUrl = "https://example.com/townhalls"
        // Ensure level is within valid range (1-17)
        val validLevel = when {
            level < 1 -> 1
            level > 17 -> 17
            else -> level
        }
        return "$baseUrl/th$validLevel.png"
    }
    
    fun getTHEmoji(level: Int): String {
        return when (level) {
            1 -> "🏠"
            2 -> "🏡"
            3 -> "🏘️"
            4 -> "🏢"
            5 -> "🏥"
            6 -> "🏰"
            7 -> "🕌"
            8 -> "🏯"
            9 -> "🛕"
            10 -> "🏛️"
            11 -> "🗼"
            12 -> "🏟️"
            13 -> "🗽"
            14 -> "🗿"
            15 -> "🏺"
            16 -> "🔧"
            17 -> "🔨"
            else -> "🏠"
        }
    }
    
    fun getStarsDisplay(stars: Int): String {
        val filledStars = "★".repeat(stars)
        val emptyStars = "☆".repeat(3 - stars)
        return filledStars + emptyStars
    }
}