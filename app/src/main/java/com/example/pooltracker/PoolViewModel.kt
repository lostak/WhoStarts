package com.example.pooltracker

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel

class PoolViewModel(app: Application) : AndroidViewModel(app) {

    val matchups = mutableStateListOf<Matchup>()

    /** Every distinct player name seen across all matchups, for autocomplete suggestions. */
    val knownPlayers: List<String>
        get() = matchups
            .flatMap { it.teamA.players + it.teamB.players }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sorted()

    init {
        matchups.addAll(Storage.load(app))
    }

    private fun persist() {
        Storage.save(getApplication(), matchups)
    }

    fun addMatchup(teamA: Team, teamB: Team) {
        matchups.add(Matchup(teamA = teamA, teamB = teamB))
        persist()
    }

    fun removeMatchup(matchup: Matchup) {
        matchups.remove(matchup)
        persist()
    }

    /** Update a matchup's team info (name, players, color) and ball assignment, keeping its history. */
    fun updateTeams(matchupId: String, teamA: Team, teamB: Team, solidsTeam: Int?) {
        val idx = matchups.indexOfFirst { it.id == matchupId }
        if (idx == -1) return
        matchups[idx] = matchups[idx].copy(teamA = teamA, teamB = teamB, solidsTeam = solidsTeam)
        persist()
    }

    /** Record who won the most recent game for this matchup. team = 1 or 2 */
    fun recordResult(matchup: Matchup, team: Int) {
        val idx = matchups.indexOfFirst { it.id == matchup.id }
        if (idx == -1) return
        val current = matchups[idx]
        // Build a brand-new history list (not a mutated in-place reference) so the
        // new Matchup is structurally *different* from the old one. Compose (and
        // SnapshotStateList) can otherwise decide nothing changed and skip redrawing.
        val newHistory = (current.history + GameResult(System.currentTimeMillis(), team)).toMutableList()
        matchups[idx] = current.copy(history = newHistory)
        persist()
    }
}
