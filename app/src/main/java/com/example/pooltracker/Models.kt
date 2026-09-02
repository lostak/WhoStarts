package com.example.pooltracker

import androidx.compose.ui.graphics.Color
import java.util.UUID

/** A single recorded game result: who won and when. */
data class GameResult(
    val timestamp: Long,
    val winnerTeam: Int // 1 = Team A, 2 = Team B
)

/**
 * A team competing in a matchup. Either (or both) of these can be provided:
 * an optional custom team name (e.g. "The Sharks"), and/or a list of player
 * names. If no custom name is set, the display name falls back to the
 * player names joined together. An optional custom color tints that team's
 * side of the win indicator.
 */
data class Team(
    val name: String? = null,
    val players: List<String> = emptyList(),
    val colorArgb: Int? = null
) {
    fun displayName(): String {
        if (!name.isNullOrBlank()) return name
        if (players.isNotEmpty()) return players.joinToString(" & ")
        return "Unnamed"
    }

    fun color(default: Color): Color = colorArgb?.let { Color(it) } ?: default
}

/** A matchup between Team A and Team B. */
data class Matchup(
    val id: String = UUID.randomUUID().toString(),
    val teamA: Team,
    val teamB: Team,
    val history: MutableList<GameResult> = mutableListOf(),
    /** Which team is playing solids: 1 = Team A, 2 = Team B, null = not assigned. */
    val solidsTeam: Int? = null
) {
    val lastWinner: Int? get() = history.lastOrNull()?.winnerTeam
    fun teamAName(): String = teamA.displayName()
    fun teamBName(): String = teamB.displayName()
    fun winsFor(team: Int): Int = history.count { it.winnerTeam == team }

    /** Which team is playing stripes, derived from [solidsTeam]. */
    val stripesTeam: Int? get() = when (solidsTeam) {
        1 -> 2
        2 -> 1
        else -> null
    }
}
