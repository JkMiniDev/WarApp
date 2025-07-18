package com.jkminidev.clashberry.data

import com.google.gson.annotations.SerializedName

data class WarResponse(
    val state: String,
    val teamSize: Int,
    val warType: String,
    val cwlRound: Int?,
    val timeRemaining: String?,
    val timeLabel: String?,
    val clan: ClanData,
    val opponent: ClanData
)

data class ClanData(
    val tag: String,
    val name: String,
    val badge: String,
    val stars: Int,
    val attacks: Int,
    val destructionPercentage: Double,
    val members: List<MemberData>
)

data class MemberData(
    val tag: String,
    val name: String,
    val townhallLevel: Int,
    val thEmoji: String,
    val mapPosition: Int,
    val attacks: List<AttackData>,
    val attacksUsed: Int,
    val opponentAttacks: Int
)

data class AttackData(
    val defenderTag: String,
    val stars: Int,
    val destructionPercentage: Double
)

data class ErrorResponse(
    val error: String,
    val message: String,
    val clan: ClanInfo?
)

data class ClanInfo(
    val name: String,
    val tag: String,
    val badge: String
)

data class ClanBasicInfo(
    val tag: String,
    val name: String,
    val badge: String,
    val level: Int,
    val members: Int,
    val isWarLogPublic: Boolean
)