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
            obj.put("teamA", JSONArray(m.teamA))
            obj.put("teamB", JSONArray(m.teamB))
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
            val teamA = obj.getJSONArray("teamA").toStringList()
            val teamB = obj.getJSONArray("teamB").toStringList()
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

    private fun JSONArray.toStringList(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until length()) list.add(getString(i))
        return list
    }
}
