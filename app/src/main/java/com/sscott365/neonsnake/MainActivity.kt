package com.sscott365.neonsnake

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: SnakeGameView
    private lateinit var scoreText: TextView
    private lateinit var statusText: TextView
    private lateinit var actionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameView = findViewById(R.id.snakeGameView)
        scoreText = findViewById(R.id.scoreText)
        statusText = findViewById(R.id.statusText)
        actionButton = findViewById(R.id.actionButton)

        scoreText.text = "Score: 0"
        statusText.text = "Tap start to play"
        actionButton.text = "Start Game"

        gameView.onScoreChange = { score ->
            scoreText.text = "Score: $score"
        }

        gameView.onGameOver = {
            statusText.text = "Game Over"
            actionButton.text = "Play Again"
        }

        actionButton.setOnClickListener {
            gameView.startGame()
            statusText.text = "Swipe to move"
            actionButton.text = "Restart Game"
        }
    }
}
