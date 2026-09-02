package com.example.pooltracker

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel

class PoolViewModel(app: Application) : AndroidViewModel(app) {

    val matchups = mutableStateListOf<Matchup>()

    init {
        matchups.addAll(Storage.load(app))
    }

    private fun persist() {
        Storage.save(getApplication(), matchups)
    }

    fun addMatchup(teamA: List<String>, teamB: List<String>) {
        matchups.add(Matchup(teamA = teamA, teamB = teamB))
        persist()
    }

    fun removeMatchup(matchup: Matchup) {
        matchups.remove(matchup)
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
