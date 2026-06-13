package com.example.mazesolver

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class MazeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val ROWS = 14
        const val COLS = 14
        const val EMPTY   = 0
        const val WALL    = 1
        const val START   = 2
        const val GOAL    = 3
        const val PATH    = 4
        const val VISITED = 5
    }

    private val grid = Array(ROWS) { IntArray(COLS) { EMPTY } }

    var startCell = Cell(2, 2)
        private set
    var goalCell  = Cell(ROWS - 3, COLS - 3)
        private set

    var drawMode = "wall"

    private val paintEmpty   = Paint().apply { color = 0xFF1E1E3A.toInt() }
    private val paintWall    = Paint().apply { color = 0xFFFFFFFF.toInt() }
    private val paintStart   = Paint().apply { color = 0xFF1D9E75.toInt() }
    private val paintGoal    = Paint().apply { color = 0xFFE05A2B.toInt() }
    private val paintPath    = Paint().apply { color = 0xFF4F46E5.toInt() }
    private val paintVisited = Paint().apply { color = 0xFF2D2B6E.toInt() }
    private val paintGrid    = Paint().apply {
        color = 0xFF2D2D4E.toInt()
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private var cellSize = 0f

    init {
        loadDefaultMaze()
    }

    private fun loadDefaultMaze() {
        // Pulisce la griglia
        for (r in 0 until ROWS) for (c in 0 until COLS) grid[r][c] = EMPTY

        // Labirinto predefinito — start (0,0), goal (13,13)
        startCell = Cell(0, 0)
        goalCell  = Cell(13, 13)
        grid[0][0]   = START
        grid[13][13] = GOAL

        // Muri — labirinto a corridoi con più percorsi possibili
        val walls = listOf(
            Pair(0,1),
            Pair(0,13),
            Pair(1,1), Pair(1,3), Pair(1,5), Pair(1,6), Pair(1,7),
            Pair(1,8), Pair(1,9), Pair(1,10), Pair(1,11), Pair(1,13),
            Pair(2,1), Pair(2,3), Pair(2,5), Pair(2,11), Pair(2,13),
            Pair(3,1), Pair(3,3), Pair(3,5), Pair(3,7), Pair(3,8),
            Pair(3,9), Pair(3,10), Pair(3,11), Pair(3,13),
            Pair(4,1), Pair(4,3), Pair(4,5), Pair(4,9), Pair(4,13),
            Pair(5,1), Pair(5,2), Pair(5,3), Pair(5,5), Pair(5,6),
            Pair(5,7), Pair(5,9), Pair(5,11), Pair(5,12), Pair(5,13),
            Pair(6,5), Pair(6,9), Pair(6,11), Pair(6,13),
            Pair(7,0), Pair(7,1), Pair(7,2), Pair(7,3), Pair(7,4),
            Pair(7,5), Pair(7,7), Pair(7,8), Pair(7,9), Pair(7,11), Pair(7,13),
            Pair(8,11), Pair(8,13),
            Pair(9,1), Pair(9,2), Pair(9,3), Pair(9,4), Pair(9,5),
            Pair(9,6), Pair(9,7), Pair(9,8), Pair(9,9), Pair(9,10),
            Pair(9,11), Pair(9,13),
            Pair(10,5), Pair(10,11), Pair(10,13),
            Pair(11,0), Pair(11,1), Pair(11,2), Pair(11,3), Pair(11,5),
            Pair(11,7), Pair(11,8), Pair(11,9), Pair(11,11), Pair(11,13),
            Pair(12,7),
            Pair(13,0), Pair(13,1), Pair(13,2), Pair(13,3), Pair(13,4),
            Pair(13,5), Pair(13,6), Pair(13,7), Pair(13,8), Pair(13,9),
            Pair(13,10), Pair(13,11)
        )

        walls.forEach { (r, c) ->
            if (grid[r][c] == EMPTY) grid[r][c] = WALL
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cellSize = minOf(w, h).toFloat() / COLS
    }

    override fun onDraw(canvas: Canvas) {

        canvas.drawColor(0xFF1A1A2E.toInt())

        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val left   = c * cellSize + 1f
                val top    = r * cellSize + 1f
                val right  = left + cellSize - 2f
                val bottom = top  + cellSize - 2f

                val paint = when (grid[r][c]) {
                    WALL    -> paintWall
                    START   -> paintStart
                    GOAL    -> paintGoal
                    PATH    -> paintPath
                    VISITED -> paintVisited
                    else    -> paintEmpty
                }
                canvas.drawRect(left, top, right, bottom, paint)
                // Bordo cella
                canvas.drawRect(left, top, right, bottom, paintGrid)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN &&
            event.action != MotionEvent.ACTION_MOVE) return true

        val c = (event.x / cellSize).toInt().coerceIn(0, COLS - 1)
        val r = (event.y / cellSize).toInt().coerceIn(0, ROWS - 1)

        when (drawMode) {
            "start" -> {
                grid[startCell.row][startCell.col] = EMPTY
                startCell = Cell(r, c)
                grid[r][c] = START
            }
            "goal" -> {
                grid[goalCell.row][goalCell.col] = EMPTY
                goalCell = Cell(r, c)
                grid[r][c] = GOAL
            }
            "wall" -> {
                if (grid[r][c] != START && grid[r][c] != GOAL)
                    grid[r][c] = if (grid[r][c] == WALL) EMPTY else WALL
            }
        }
        clearPath()
        invalidate()
        return true
    }

    fun getWalls(): List<Cell> {
        val walls = mutableListOf<Cell>()
        for (r in 0 until ROWS)
            for (c in 0 until COLS)
                if (grid[r][c] == WALL) walls.add(Cell(r, c))
        return walls
    }

    fun showResult(result: SolveResult) {
        clearPath()
        result.visited.forEach { (r, c) ->
            if (grid[r][c] == EMPTY) grid[r][c] = VISITED
        }
        result.path.forEach { (r, c) ->
            if (grid[r][c] == EMPTY || grid[r][c] == VISITED) grid[r][c] = PATH
        }
        invalidate()
    }

    fun reset() {
        for (r in 0 until ROWS)
            for (c in 0 until COLS)
                grid[r][c] = EMPTY
        startCell = Cell(2, 2)
        goalCell  = Cell(ROWS - 3, COLS - 3)
        grid[startCell.row][startCell.col] = START
        grid[goalCell.row][goalCell.col]   = GOAL
        invalidate()
    }

    fun clearPath() {
        for (r in 0 until ROWS)
            for (c in 0 until COLS)
                if (grid[r][c] == PATH || grid[r][c] == VISITED)
                    grid[r][c] = EMPTY
    }

    fun animateResult(result: SolveResult, onDone: () -> Unit) {
        clearPath()
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var delay = 0L
        val visitedDelay = 30L  // ms
        val pathDelay = 60L

        // Fase 1: anima le celle visitate
        result.visited.forEach { (r, c) ->
            handler.postDelayed({
                if (grid[r][c] == EMPTY) {
                    grid[r][c] = VISITED
                    invalidate()
                }
            }, delay)
            delay += visitedDelay
        }

        // Fase 2: anima il percorso finale dopo che l'esplorazione è finita
        val pathStartDelay = delay + 200L
        delay = pathStartDelay
        result.path.forEach { (r, c) ->
            handler.postDelayed({
                if (grid[r][c] == EMPTY || grid[r][c] == VISITED) {
                    grid[r][c] = PATH
                    invalidate()
                }
                // Quando è l'ultima cella del percorso, notifica che l'animazione è finitaaaaa
                if (r == result.path.last().row && c == result.path.last().col) {
                    onDone()
                }
            }, delay)
            delay += pathDelay
        }
    }
}