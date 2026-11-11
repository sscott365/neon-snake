package com.sscott365.neonsnake

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        leaderboard = LocalLeaderboard(this)
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
            AlertDialog.Builder(this)
                .setTitle("Worldwide leaderboard")
                .setMessage("Online scores are not connected yet. A cloud leaderboard service and account setup are needed before scores can be shared worldwide.")
                .setPositiveButton("OK", null)
                .show()
        }

        gameView.onScoreChange = { score -> scoreText.text = "Score: $score" }
        gameView.onGameOver = {
            leaderboard.record(gameView.score)
            statusText.text = "Game over — score saved locally"
            updateLocalScores()
        }
        showHome()
    }

    private fun startGame() {
        homeScreen.visibility = android.view.View.GONE
        leaderboardScreen.visibility = android.view.View.GONE
        gameScreen.visibility = android.view.View.VISIBLE
        statusText.text = "Swipe to move"
        gameView.startGame()
    }

    private fun showHome() {
        gameView.stopGame()
        gameScreen.visibility = android.view.View.GONE
        leaderboardScreen.visibility = android.view.View.GONE
        homeScreen.visibility = android.view.View.VISIBLE
    }

    private fun showLeaderboard() {
        updateLocalScores()
        homeScreen.visibility = android.view.View.GONE
        gameScreen.visibility = android.view.View.GONE
        leaderboardScreen.visibility = android.view.View.VISIBLE
    }

    private fun updateLocalScores() {
        val scores = leaderboard.topScores()
        localScoresText.text = if (scores.isEmpty()) {
            "No scores yet. Play a game to set your first high score!"
        } else {
            scores.mapIndexed { index, score -> "${index + 1}.    $score" }.joinToString("\n")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            gameScreen.visibility == android.view.View.VISIBLE -> showHome()
            leaderboardScreen.visibility == android.view.View.VISIBLE -> showHome()
            else -> super.onBackPressed()
        }
    }
}
