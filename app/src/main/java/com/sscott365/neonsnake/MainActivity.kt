package com.sscott365.neonsnake

import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: SnakeGameView
    private lateinit var scoreText: TextView
    private lateinit var statusText: TextView
    private lateinit var homeScreen: LinearLayout
    private lateinit var gameScreen: LinearLayout
    private lateinit var leaderboardScreen: LinearLayout
    private lateinit var settingsScreen: LinearLayout
    private lateinit var localScoresText: TextView
    private lateinit var leaderboard: LocalLeaderboard
    private lateinit var onlineLeaderboard: OnlineLeaderboard
    private val playerPreferences by lazy {
        getSharedPreferences("neon_snake_player", MODE_PRIVATE)
    }
    private val settingsPreferences by lazy {
        getSharedPreferences("neon_snake_settings", MODE_PRIVATE)
    }

    private val snakeColorNames = listOf("Neon Mint", "Electric Cyan", "Sunshine", "Hot Pink", "Purple")
    private val snakeColors = intArrayOf(
        Color.rgb(124, 255, 178),
        Color.rgb(77, 238, 234),
        Color.rgb(255, 230, 109),
        Color.rgb(255, 107, 157),
        Color.rgb(179, 136, 255)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        leaderboard = LocalLeaderboard(this)
        onlineLeaderboard = OnlineLeaderboard()
        homeScreen = findViewById(R.id.homeScreen)
        gameScreen = findViewById(R.id.gameScreen)
        leaderboardScreen = findViewById(R.id.leaderboardScreen)
        settingsScreen = findViewById(R.id.settingsScreen)
        localScoresText = findViewById(R.id.localScoresText)
        gameView = findViewById(R.id.snakeGameView)
        scoreText = findViewById(R.id.scoreText)
        statusText = findViewById(R.id.statusText)

        applySavedSettings()
        configureSettingsControls()

        findViewById<Button>(R.id.startButton).setOnClickListener { startGame() }
        findViewById<Button>(R.id.homeLeaderboardButton).setOnClickListener { showLeaderboard() }
        findViewById<Button>(R.id.homeSettingsButton).setOnClickListener { showSettings() }
        findViewById<Button>(R.id.gameHomeButton).setOnClickListener { showHome() }
        findViewById<Button>(R.id.restartButton).setOnClickListener { startGame() }
        findViewById<Button>(R.id.leaderboardHomeButton).setOnClickListener { showHome() }
        findViewById<Button>(R.id.settingsHomeButton).setOnClickListener { showHome() }
        findViewById<Button>(R.id.worldLeaderboardButton).setOnClickListener {
            showWorldwideLeaderboard()
        }

        gameView.onScoreChange = { score -> scoreText.text = "Score: $score" }
        gameView.onGameOver = {
            val finalScore = gameView.score
            leaderboard.record(finalScore)
            updateLocalScores()

            if (finalScore > 0) {
                promptForPlayerName(finalScore)
            } else {
                statusText.text = "Game over — score saved locally"
            }
        }
        showHome()
    }

    private fun applySavedSettings() {
        val colorIndex = settingsPreferences.getInt(KEY_SNAKE_COLOR, 0).coerceIn(snakeColors.indices)
        val soundEnabled = settingsPreferences.getBoolean(KEY_SOUND_ENABLED, true)
        gameView.applyGameSettings(snakeColors[colorIndex], soundEnabled)
    }

    private fun configureSettingsControls() {
        val colorSpinner = findViewById<Spinner>(R.id.snakeColorSpinner)
        colorSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, snakeColorNames)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        colorSpinner.setSelection(
            settingsPreferences.getInt(KEY_SNAKE_COLOR, 0).coerceIn(snakeColors.indices),
            false
        )
        colorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                settingsPreferences.edit().putInt(KEY_SNAKE_COLOR, position).apply()
                applySavedSettings()
            }
        }

        val soundSwitch = findViewById<SwitchCompat>(R.id.soundSwitch)
        soundSwitch.isChecked = settingsPreferences.getBoolean(KEY_SOUND_ENABLED, true)
        soundSwitch.setOnCheckedChangeListener { _, checked ->
            settingsPreferences.edit().putBoolean(KEY_SOUND_ENABLED, checked).apply()
            applySavedSettings()
        }
    }

    private fun startGame() {
        homeScreen.visibility = View.GONE
        leaderboardScreen.visibility = View.GONE
        settingsScreen.visibility = View.GONE
        gameScreen.visibility = View.VISIBLE
        statusText.text = "Swipe to move"
        gameView.startGame()
    }

    private fun showHome() {
        gameView.stopGame()
        gameScreen.visibility = View.GONE
        leaderboardScreen.visibility = View.GONE
        settingsScreen.visibility = View.GONE
        homeScreen.visibility = View.VISIBLE
    }

    private fun showLeaderboard() {
        updateLocalScores()
        homeScreen.visibility = View.GONE
        gameScreen.visibility = View.GONE
        settingsScreen.visibility = View.GONE
        leaderboardScreen.visibility = View.VISIBLE
    }

    private fun showSettings() {
        gameView.stopGame()
        homeScreen.visibility = View.GONE
        gameScreen.visibility = View.GONE
        leaderboardScreen.visibility = View.GONE
        settingsScreen.visibility = View.VISIBLE
    }

    private fun updateLocalScores() {
        val scores = leaderboard.topScores()
        localScoresText.text = if (scores.isEmpty()) {
            "No scores yet. Play a game to set your first high score!"
        } else {
            scores.mapIndexed { index, score -> "${index + 1}.    $score" }.joinToString("\n")
        }
    }

    private fun promptForPlayerName(score: Int) {
        val nameInput = EditText(this).apply {
            hint = "Your leaderboard name"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            filters = arrayOf(InputFilter.LengthFilter(MAX_PLAYER_NAME_LENGTH))
            setSingleLine(true)
            setText(playerPreferences.getString(KEY_PLAYER_NAME, ""))
            setSelection(text.length)
        }
        val inputContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(nameInput)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Game over — $score points")
            .setMessage("Choose the name shown on the worldwide leaderboard.")
            .setView(inputContainer)
            .setPositiveButton("Save score", null)
            .setNegativeButton("Not now", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val playerName = nameInput.text.toString().trim()
                if (playerName.isBlank()) {
                    nameInput.error = "Enter a name"
                    return@setOnClickListener
                }
                playerPreferences.edit().putString(KEY_PLAYER_NAME, playerName).apply()
                dialog.dismiss()
                uploadScore(score, playerName)
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                dialog.dismiss()
                val savedName = playerPreferences.getString(KEY_PLAYER_NAME, null)
                uploadScore(score, savedName ?: "Snake Player")
            }
        }
        dialog.show()
    }

    private fun uploadScore(score: Int, playerName: String) {
        statusText.text = "Game over — saving score online…"
        onlineLeaderboard.submitScore(score, playerName) { uploaded, newBest, error ->
            if (isFinishing || isDestroyed) return@submitScore
            statusText.text = when {
                !uploaded -> "Game over — saved locally; online upload failed"
                newBest -> "Game over — best score saved online"
                else -> "Game over — name and best score saved online"
            }
            if (!uploaded) {
                android.util.Log.w("NeonSnake", "Could not upload leaderboard score", error)
                Toast.makeText(
                    this,
                    "Couldn't upload your score. Check your connection and try again next game.",
                    Toast.LENGTH_LONG
                ).show()
            }
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            gameScreen.visibility == View.VISIBLE -> showHome()
            leaderboardScreen.visibility == View.VISIBLE -> showHome()
            settingsScreen.visibility == View.VISIBLE -> showHome()
            else -> super.onBackPressed()
        }
    }

    private companion object {
        const val MAX_PLAYER_NAME_LENGTH = 20
        const val KEY_PLAYER_NAME = "player_name"
        const val KEY_SNAKE_COLOR = "snake_color"
        const val KEY_SOUND_ENABLED = "sound_enabled"
    }
}
