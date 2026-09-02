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
            obj.put("teamA", teamToJson(m.teamA))
            obj.put("teamB", teamToJson(m.teamB))
            val historyArr = JSONArray()
            for (h in m.history) {
                val hObj = JSONObject()
                hObj.put("timestamp", h.timestamp)
                hObj.put("winnerTeam", h.winnerTeam)
                historyArr.put(hObj)
            }
            obj.put("history", historyArr)
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
            val teamA = parseTeam(obj.get("teamA"))
            val teamB = parseTeam(obj.get("teamB"))
            val history = mutableListOf<GameResult>()
            val historyArr = obj.optJSONArray("history") ?: JSONArray()
            for (j in 0 until historyArr.length()) {
                val hObj = historyArr.getJSONObject(j)
                history.add(GameResult(hObj.getLong("timestamp"), hObj.getInt("winnerTeam")))
            }
            result.add(Matchup(id = id, teamA = teamA, teamB = teamB, history = history))
        }
        return result
    }

    private fun teamToJson(team: Team): JSONObject {
        val obj = JSONObject()
        if (team.name != null) obj.put("name", team.name)
        obj.put("players", JSONArray(team.players))
        return obj
    }

    /**
     * Parses a team from either the current format (a JSON object with
     * "name"/"players") or the older format (a plain JSON array of player
     * name strings), so existing saved data doesn't break on update.
     */
    private fun parseTeam(raw: Any): Team {
        return when (raw) {
            is JSONObject -> {
                val name = if (raw.has("name") && !raw.isNull("name")) raw.getString("name") else null
                val players = raw.optJSONArray("players")?.toStringList() ?: emptyList()
                Team(name = name, players = players)
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
