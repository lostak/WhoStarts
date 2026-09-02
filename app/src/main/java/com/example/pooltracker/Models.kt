package com.example.pooltracker

import java.util.UUID

/** A single recorded game result: who won and when. */
data class GameResult(
    val timestamp: Long,
    val winnerTeam: Int // 1 = Team A, 2 = Team B
)

/** A matchup between Team A and Team B, each a list of one or more player names. */
data class Matchup(
    val id: String = UUID.randomUUID().toString(),
    val teamA: List<String>,
    val teamB: List<String>,
    val history: MutableList<GameResult> = mutableListOf()
) {
    val lastWinner: Int? get() = history.lastOrNull()?.winnerTeam
    fun teamAName(): String = teamA.joinToString(" & ")
    fun teamBName(): String = teamB.joinToString(" & ")
    fun winsFor(team: Int): Int = history.count { it.winnerTeam == team }
}
