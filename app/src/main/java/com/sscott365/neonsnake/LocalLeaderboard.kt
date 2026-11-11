package com.sscott365.neonsnake

import android.content.Context
import org.json.JSONArray

/** Stores this device's ten best game scores locally. */
class LocalLeaderboard(context: Context) {
    private val preferences = context.getSharedPreferences("neon_snake_scores", Context.MODE_PRIVATE)

    fun record(score: Int) {
        if (score <= 0) return
        val scores = (preferences.getString("scores", "[]") ?: "[]").let { saved ->
            runCatching {
                val json = JSONArray(saved)
                (0 until json.length()).map { json.optInt(it) }
            }.getOrDefault(emptyList())
        }
        val updated = (scores + score).sortedDescending().take(MAX_SCORES)
        val json = JSONArray().apply { updated.forEach(::put) }
        preferences.edit().putString("scores", json.toString()).apply()
    }

    fun topScores(): List<Int> = runCatching {
        val json = JSONArray(preferences.getString("scores", "[]") ?: "[]")
        (0 until json.length()).map { json.optInt(it) }.sortedDescending().take(MAX_SCORES)
    }.getOrDefault(emptyList())

    private companion object {
        const val MAX_SCORES = 10
    }
}
