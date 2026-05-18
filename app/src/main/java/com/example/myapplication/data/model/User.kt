package com.example.myapplication.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val ecoPoints: Int = 0,
    val reportsSubmitted: Int = 0,
    val verifiedCleanups: Int = 0,
    val badges: List<String> = emptyList()
) {
    val rank: EcoRank
        get() = EcoRank.fromPoints(ecoPoints)
}

enum class EcoRank(val displayName: String, val minPoints: Int, val nextRankPoints: Int?) {
    BEGINNER("Eco Beginner", 0, 100),
    WATCHER("Waste Watcher", 100, 300),
    GUARDIAN("Green Guardian", 300, 700),
    PROTECTOR("Earth Protector", 700, 1500),
    WARRIOR("Eco Warrior", 1500, 3000),
    SAVIOR("City Savior", 3000, null);

    companion object {
        fun fromPoints(points: Int): EcoRank {
            return entries.findLast { points >= it.minPoints } ?: BEGINNER
        }
    }
}
