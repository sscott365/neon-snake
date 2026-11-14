package com.sscott365.neonsnake

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: SnakeGameView
    private lateinit var scoreText: TextView
    private lateinit var statusText: TextView
    private lateinit var homeScreen: LinearLayout
    private lateinit var gameScreen: LinearLayout
    private lateinit var leaderboardScreen: LinearLayout
    private lateinit var localScoresText: TextView
    private lateinit var leaderboard: LocalLeaderboard
    private lateinit var onlineLeaderboard: OnlineLeaderboard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        leaderboard = LocalLeaderboard(this)
        onlineLeaderboard = OnlineLeaderboard()
        homeScreen = findViewById(R.id.homeScreen)
        gameScreen = findViewById(R.id.gameScreen)
        leaderboardScreen = findViewById(R.id.leaderboardScreen)
        localScoresText = findViewById(R.id.localScoresText)
        gameView = findViewById(R.id.snakeGameView)
        scoreText = findViewById(R.id.scoreText)
        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.startButton).setOnClickListener { startGame() }
        findViewById<Button>(R.id.homeLeaderboardButton).setOnClickListener { showLeaderboard() }
        findViewById<Button>(R.id.gameHomeButton).setOnClickListener { showHome() }
        findViewById<Button>(R.id.restartButton).setOnClickListener { startGame() }
        findViewById<Button>(R.id.leaderboardHomeButton).setOnClickListener { showHome() }
        findViewById<Button>(R.id.worldLeaderboardButton).setOnClickListener {
            showWorldwideLeaderboard()
        }

        gameView.onScoreChange = { score -> scoreText.text = "Score: $score" }
        gameView.onGameOver = {
            val finalScore = gameView.score
            leaderboard.record(finalScore)
            statusText.text = if (finalScore > 0) {
                "Game over — saving score online…"
            } else {
                "Game over — score saved locally"
            }
            updateLocalScores()

            if (finalScore > 0) {
                onlineLeaderboard.submitScore(finalScore) { uploaded, newBest, error ->
                    if (isFinishing || isDestroyed) return@submitScore
                    statusText.text = when {
                        !uploaded -> "Game over — saved locally; online upload failed"
                        newBest -> "Game over — best score saved online"
                        else -> "Game over — saved locally; online best kept"
                    }
                    if (!uploaded && error != null) {
                        android.util.Log.w("NeonSnake", "Could not upload leaderboard score", error)
                    }
                }
            }
        }
        showHome()
    }

    private fun startGame() {
        homeScreen.visibility = View.GONE
        leaderboardScreen.visibility = View.GONE
        gameScreen.visibility = View.VISIBLE
        statusText.text = "Swipe to move"
        gameView.startGame()
    }

    private fun showHome() {
        gameView.stopGame()
        gameScreen.visibility = View.GONE
        leaderboardScreen.visibility = View.GONE
        homeScreen.visibility = View.VISIBLE
    }

    private fun showLeaderboard() {
        updateLocalScores()
        homeScreen.visibility = View.GONE
        gameScreen.visibility = View.GONE
        leaderboardScreen.visibility = View.VISIBLE
    }

    private fun updateLocalScores() {
        val scores = leaderboard.topScores()
        localScoresText.text = if (scores.isEmpty()) {
            "No scores yet. Play a game to set your first high score!"
        } else {
            scores.mapIndexed { index, score -> "${index + 1}.    $score" }.joinToString("\n")
        }
    }

    private fun showWorldwideLeaderboard() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Worldwide leaderboard")
            .setMessage("Loading online scores…")
            .setPositiveButton("OK", null)
            .create()
        dialog.show()

        onlineLeaderboard.loadTopScores { entries, error ->
            if (isFinishing || isDestroyed || !dialog.isShowing) return@loadTopScores
            dialog.setMessage(
                when {
                    error != null -> "Couldn't load scores. Check your internet connection and Firebase setup, then try again."
                    entries.isNullOrEmpty() -> "No worldwide scores yet. Be the first to set one!"
                    else -> entries.mapIndexed { index, entry ->
                        "${index + 1}.  ${entry.name} — ${entry.score}"
                    }.joinToString("\n")
                }
            )
            if (error != null) {
                android.util.Log.w("NeonSnake", "Could not load worldwide leaderboard", error)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            gameScreen.visibility == View.VISIBLE -> showHome()
            leaderboardScreen.visibility == View.VISIBLE -> showHome()
            else -> super.onBackPressed()
        }
    }
}
