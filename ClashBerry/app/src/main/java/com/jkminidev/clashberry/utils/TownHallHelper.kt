package com.jkminidev.clashberry.utils

object TownHallHelper {
    
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