package com.example.pooltracker

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel

class PoolViewModel(app: Application) : AndroidViewModel(app) {

    val matchups = mutableStateListOf<Matchup>()

    /** Every distinct player name seen across all matchups, for autocomplete suggestions. */
    val knownPlayers: List<String>
        get() = matchups
            .flatMap { m -> m.teams.flatMap { it.players } }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sorted()

    init {
        matchups.addAll(Storage.load(app))
    }

    private fun persist() {
        Storage.save(getApplication(), matchups)
    }

    fun addMatchup(teams: List<Team>) {
        if (teams.size < 2) return
        matchups.add(Matchup(teams = teams))
        persist()
    }

    fun removeMatchup(matchup: Matchup) {
        matchups.remove(matchup)
        persist()
    }

    /**
     * Update a matchup's teams (names, players, colors) and ball assignment.
     * History is kept, but results referring to teams that were removed are
     * dropped so win counts stay consistent with the new roster.
     */
    fun updateTeams(matchupId: String, teams: List<Team>, solidsTeam: Int?) {
        val idx = matchups.indexOfFirst { it.id == matchupId }
        if (idx == -1 || teams.size < 2) return
        val current = matchups[idx]
        val prunedHistory = current.history.filter { it.winnerTeam in 1..teams.size }.toMutableList()
        matchups[idx] = current.copy(
            teams = teams,
            history = prunedHistory,
            solidsTeam = solidsTeam?.takeIf { teams.size == 2 && it in 1..2 }
        )
        persist()
    }

    /** Record who won the most recent game. [team] is a 1-based team index. */
    fun recordResult(matchup: Matchup, team: Int) {
        val idx = matchups.indexOfFirst { it.id == matchup.id }
        if (idx == -1) return
        val current = matchups[idx]
        if (team !in 1..current.teams.size) return
        // Build a brand-new history list (not a mutated in-place reference) so the
        // new Matchup is structurally *different* from the old one. Compose (and
        // SnapshotStateList) can otherwise decide nothing changed and skip redrawing.
        val newHistory = (current.history + GameResult(System.currentTimeMillis(), team)).toMutableList()
        matchups[idx] = current.copy(history = newHistory)
        persist()
    }
}
