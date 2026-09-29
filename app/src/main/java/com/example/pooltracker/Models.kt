package com.example.pooltracker

import androidx.compose.ui.graphics.Color
import java.util.UUID

/** A single recorded game result: which team won (1-based index) and when. */
data class GameResult(
    val timestamp: Long,
    val winnerTeam: Int // 1-based index into Matchup.teams
)

/**
 * A team competing in a matchup. Either (or both) of these can be provided:
 * an optional custom team name (e.g. "The Sharks"), and/or a list of player
 * names. If no custom name is set, the display name falls back to the
 * player names joined together. An optional custom color tints that team's
 * segment of the win indicator.
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

/**
 * A matchup between two or more teams. [teams] is ordered, and results refer to
 * teams by 1-based index so existing saved history stays valid.
 */
data class Matchup(
    val id: String = UUID.randomUUID().toString(),
    val teams: List<Team>,
    val history: MutableList<GameResult> = mutableListOf(),
    /**
     * Which team is playing solids, 1-based. Only meaningful in a two-team
     * matchup; ignored when [teams] has more than two entries.
     */
    val solidsTeam: Int? = null
) {
    val teamCount: Int get() = teams.size
    val isHeadToHead: Boolean get() = teams.size == 2

    val lastWinner: Int? get() = history.lastOrNull()?.winnerTeam

    /** Display name for a 1-based team index. */
    fun teamName(index: Int): String = teams.getOrNull(index - 1)?.displayName() ?: "Team $index"

    fun winsFor(team: Int): Int = history.count { it.winnerTeam == team }

    /** Which team is playing stripes, derived from [solidsTeam]. Two-team only. */
    val stripesTeam: Int? get() = when {
        !isHeadToHead || solidsTeam == null -> null
        solidsTeam == 1 -> 2
        else -> 1
    }
}
