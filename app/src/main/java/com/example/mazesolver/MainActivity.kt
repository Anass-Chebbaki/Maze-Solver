package com.example.mazesolver

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var mazeView: MazeView
    private lateinit var prologEngine: PrologEngine
    private var selectedAlgo = "astar"
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mazeView      = findViewById(R.id.mazeView)
        prologEngine  = PrologEngine(this)

        setupAlgoButtons()
        setupModeButtons()
        setupActionButtons()
    }
    private var selectedAlgoBtn: Button? = null
    private var selectedModeBtn: Button? = null

    private fun setupAlgoButtons() {
        mapOf(
            R.id.btnAstar  to "astar",
            R.id.btnBfs    to "bfs",
            R.id.btnDfs    to "dfs",
            R.id.btnGreedy to "greedy"
        ).forEach { (id, algo) ->
            val btn = findViewById<Button>(id)
            btn.setOnClickListener {
                selectedAlgo = algo
                selectedAlgoBtn?.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(0xFF2D2D4E.toInt())
                selectedAlgoBtn?.setTextColor(0xFFAAAACC.toInt())
                btn.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(0xFF4F46E5.toInt())
                btn.setTextColor(0xFFFFFFFF.toInt())
                selectedAlgoBtn = btn
                mazeView.clearPath()
                clearStats()
            }
        }
        // Seleziona A* di default
        findViewById<Button>(R.id.btnAstar).performClick()
    }

    private fun setupModeButtons() {
        mapOf(
            R.id.btnModeWall  to "wall",
            R.id.btnModeStart to "start",
            R.id.btnModeGoal  to "goal"
        ).forEach { (id, mode) ->
            val btn = findViewById<Button>(id)
            btn.setOnClickListener {
                mazeView.drawMode = mode
                selectedModeBtn?.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(0xFF2D2D4E.toInt())
                selectedModeBtn?.setTextColor(0xFFAAAACC.toInt())
                btn.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(0xFF1D9E75.toInt())
                btn.setTextColor(0xFFFFFFFF.toInt())
                selectedModeBtn = btn
            }
        }
        // Seleziona Muro di default
        findViewById<Button>(R.id.btnModeWall).performClick()
    }


    private fun setupActionButtons() {
        findViewById<Button>(R.id.btnReset).setOnClickListener {
            mazeView.reset()
            clearStats()
        }

        val btnSolve = findViewById<Button>(R.id.btnSolve)
        btnSolve.setOnClickListener {
            btnSolve.isEnabled = false
            btnSolve.text = "..."

            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    prologEngine.solve(
                        selectedAlgo,
                        mazeView.getWalls(),
                        mazeView.startCell,
                        mazeView.goalCell
                    )
                }

                if (result == null) {
                    Toast.makeText(
                        this@MainActivity,
                        "Nessun percorso trovato",
                        Toast.LENGTH_SHORT
                    ).show()
                    btnSolve.isEnabled = true
                    btnSolve.text = "Risolvi"
                } else {
                    updateStats(result)
                    mazeView.animateResult(result) {
                        btnSolve.isEnabled = true
                        btnSolve.text = "Risolvi"
                    }
                }
            }
        }
    }

    private fun updateStats(result: SolveResult) {
        findViewById<TextView>(R.id.statVisited).text = result.visited.size.toString()
        findViewById<TextView>(R.id.statLength).text  = result.path.size.toString()
        findViewById<TextView>(R.id.statTime).text    = result.timeMs.toString()
    }

    private fun clearStats() {
        findViewById<TextView>(R.id.statVisited).text = "—"
        findViewById<TextView>(R.id.statLength).text  = "—"
        findViewById<TextView>(R.id.statTime).text    = "—"
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}