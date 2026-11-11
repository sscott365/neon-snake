package com.sscott365.neonsnake

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }
private data class Cell(val x: Int, val y: Int)

class SnakeGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var score: Int = 0
        private set
    var onScoreChange: ((Int) -> Unit)? = null
    var onGameOver: (() -> Unit)? = null

    private val gridSize = 18
    private var snake = mutableListOf<Cell>()
    private var food = Cell(10, 10)
    private var direction = Direction.RIGHT
    private var nextDirection = Direction.RIGHT
    private var running = false
    private var lastTick = 0L
    private val tickMs = 170L
    private val handler = Handler(Looper.getMainLooper())

    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            val now = SystemClock.uptimeMillis()
            if (now - lastTick >= tickMs) {
                updateGame()
                lastTick = now
                invalidate()
            }
            if (running) handler.postDelayed(this, 16L)
        }
    }

    private val snakePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(124, 255, 178)
    }
    private val foodPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 95, 109)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(42, 53, 82)
        strokeWidth = 1f
    }
    private val messagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 48f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    init {
        isFocusable = true
        setBackgroundColor(Color.rgb(11, 16, 32))
    }

    fun startGame() {
        handler.removeCallbacks(loop)
        snake = mutableListOf(Cell(5, 8), Cell(4, 8), Cell(3, 8))
        direction = Direction.RIGHT
        nextDirection = Direction.RIGHT
        score = 0
        running = true
        lastTick = SystemClock.uptimeMillis()
        spawnFood()
        onScoreChange?.invoke(score)
        handler.post(loop)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(loop)
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!running) return true
        when (event.action) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                val dx = event.x - width / 2f
                val dy = event.y - height / 2f
                nextDirection = if (abs(dx) > abs(dy)) {
                    if (dx >= 0 && direction != Direction.LEFT) Direction.RIGHT
                    else if (dx < 0 && direction != Direction.RIGHT) Direction.LEFT
                    else nextDirection
                } else {
                    if (dy >= 0 && direction != Direction.UP) Direction.DOWN
                    else if (dy < 0 && direction != Direction.DOWN) Direction.UP
                    else nextDirection
                }
                return true
            }
        }
        return true
    }

    private fun updateGame() {
        direction = nextDirection
        val head = snake.first()
        val next = when (direction) {
            Direction.UP -> Cell(head.x, head.y - 1)
            Direction.DOWN -> Cell(head.x, head.y + 1)
            Direction.LEFT -> Cell(head.x - 1, head.y)
            Direction.RIGHT -> Cell(head.x + 1, head.y)
        }
        val newHead = Cell((next.x + gridSize) % gridSize, (next.y + gridSize) % gridSize)
        val grows = newHead == food
        val bodyToCheck = if (grows) snake else snake.dropLast(1)

        if (newHead in bodyToCheck) {
            running = false
            onGameOver?.invoke()
            invalidate()
            return
        }

        snake.add(0, newHead)
        if (grows) {
            score++
            onScoreChange?.invoke(score)
            spawnFood()
        } else {
            snake.removeAt(snake.lastIndex)
        }
    }

    private fun spawnFood() {
        val freeCells = (0 until gridSize).flatMap { y ->
            (0 until gridSize).map { x -> Cell(x, y) }
        }.filterNot { it in snake }

        if (freeCells.isNotEmpty()) {
            food = freeCells[Random.nextInt(freeCells.size)]
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(11, 16, 32))

        val cell = minOf(width.toFloat(), height.toFloat()) / gridSize
        val boardWidth = cell * gridSize
        val leftOffset = (width - boardWidth) / 2f
        val topOffset = (height - boardWidth) / 2f

        for (i in 0..gridSize) {
            val x = leftOffset + i * cell
            val y = topOffset + i * cell
            canvas.drawLine(x, topOffset, x, topOffset + boardWidth, gridPaint)
            canvas.drawLine(leftOffset, y, leftOffset + boardWidth, y, gridPaint)
        }

        fun drawCell(position: Cell, paint: Paint) {
            val inset = cell * 0.08f
            val left = leftOffset + position.x * cell + inset
            val top = topOffset + position.y * cell + inset
            canvas.drawRoundRect(
                left, top,
                left + cell - inset * 2,
                top + cell - inset * 2,
                cell * 0.18f, cell * 0.18f,
                paint
            )
        }

        snake.forEach { drawCell(it, snakePaint) }
        drawCell(food, foodPaint)

        if (!running) {
            canvas.drawText("Game Over", width / 2f, height / 2f, messagePaint)
        }
    }
}
