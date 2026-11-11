package com.sscott365.neonsnake

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: SnakeGameView
    private lateinit var scoreText: TextView
    private lateinit var restartButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameView = findViewById(R.id.snakeGameView)
        scoreText = findViewById(R.id.scoreText)
        restartButton = findViewById(R.id.restartButton)

        gameView.onScoreChange = { score ->
            scoreText.text = "Score: $score"
        }

        gameView.onGameOver = {
            scoreText.text = "Game Over - Score: ${gameView.score}"
        }

        restartButton.setOnClickListener {
            gameView.startGame()
        }

        gameView.startGame()
    }
}
