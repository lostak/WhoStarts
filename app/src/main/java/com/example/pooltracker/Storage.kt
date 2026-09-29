package com.example.pooltracker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Simple local persistence using SharedPreferences + JSON.
 * Good enough for a lightweight tracker with no backend.
 */
object Storage {
    private const val PREFS = "pool_tracker_prefs"
    private const val KEY_MATCHUPS = "matchups_json"

    fun save(context: Context, matchups: List<Matchup>) {
        val arr = JSONArray()
        for (m in matchups) {
            val obj = JSONObject()
            obj.put("id", m.id)
            val teamsArr = JSONArray()
            for (t in m.teams) teamsArr.put(teamToJson(t))
            obj.put("teams", teamsArr)
            val historyArr = JSONArray()
            for (h in m.history) {
                val hObj = JSONObject()
                hObj.put("timestamp", h.timestamp)
                hObj.put("winnerTeam", h.winnerTeam)
                historyArr.put(hObj)
            }
            obj.put("history", historyArr)
            if (m.solidsTeam != null) obj.put("solidsTeam", m.solidsTeam)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MATCHUPS, arr.toString())
            .apply()
    }

    fun load(context: Context): MutableList<Matchup> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MATCHUPS, null) ?: return mutableListOf()

        val result = mutableListOf<Matchup>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.getString("id")

            // Current format stores an ordered "teams" array. Older saves stored
            // exactly two teams under "teamA"/"teamB"; fall back to those so
            // existing data keeps working.
            val teams = mutableListOf<Team>()
            val teamsArr = obj.optJSONArray("teams")
            if (teamsArr != null) {
                for (j in 0 until teamsArr.length()) teams.add(parseTeam(teamsArr.get(j)))
            } else {
                if (obj.has("teamA")) teams.add(parseTeam(obj.get("teamA")))
                if (obj.has("teamB")) teams.add(parseTeam(obj.get("teamB")))
            }
            if (teams.size < 2) continue

            val history = mutableListOf<GameResult>()
            val historyArr = obj.optJSONArray("history") ?: JSONArray()
            for (j in 0 until historyArr.length()) {
                val hObj = historyArr.getJSONObject(j)
                val winner = hObj.getInt("winnerTeam")
                // Drop results pointing at a team that no longer exists.
                if (winner in 1..teams.size) {
                    history.add(GameResult(hObj.getLong("timestamp"), winner))
                }
            }

            val solidsTeam = if (obj.has("solidsTeam") && !obj.isNull("solidsTeam")) {
                obj.getInt("solidsTeam").takeIf { it in 1..teams.size }
            } else null

            result.add(Matchup(id = id, teams = teams, history = history, solidsTeam = solidsTeam))
        }
        return result
    }

    private fun teamToJson(team: Team): JSONObject {
        val obj = JSONObject()
        if (team.name != null) obj.put("name", team.name)
        obj.put("players", JSONArray(team.players))
        if (team.colorArgb != null) obj.put("colorArgb", team.colorArgb)
        return obj
    }

    private fun parseTeam(raw: Any): Team {
        return when (raw) {
            is JSONObject -> {
                val name = if (raw.has("name") && !raw.isNull("name")) raw.getString("name") else null
                val players = raw.optJSONArray("players")?.toStringList() ?: emptyList()
                val colorArgb = if (raw.has("colorArgb") && !raw.isNull("colorArgb")) raw.getInt("colorArgb") else null
                Team(name = name, players = players, colorArgb = colorArgb)
            }
            is JSONArray -> Team(name = null, players = raw.toStringList())
            else -> Team()
        }
    }

    private fun JSONArray.toStringList(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until length()) list.add(getString(i))
        return list
    }
}
