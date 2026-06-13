package com.example.mazesolver

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Cell(val row: Int, val col: Int)

data class SolveResult(
    val path: List<Cell>,
    val visited: List<Cell>,
    val timeMs: Long
)

class PrologEngine(context: Context) {

    // INDIRIZZO DEL SERVER
    private val SERVER_URL = "http://172.20.10.4:5000/solve"

    fun solve(
        algorithm: String,
        walls: List<Cell>,
        start: Cell,
        goal: Cell
    ): SolveResult? {
        val t0 = System.currentTimeMillis()

        return try {
            val body = JSONObject().apply {
                put("algorithm", algorithm)
                put("start", JSONObject().apply {
                    put("row", start.row)
                    put("col", start.col)
                })
                put("goal", JSONObject().apply {
                    put("row", goal.row)
                    put("col", goal.col)
                })
                val wallsArray = JSONArray()
                walls.forEach { w ->
                    wallsArray.put(JSONObject().apply {
                        put("row", w.row)
                        put("col", w.col)
                    })
                }
                put("walls", wallsArray)
            }

            val url = URL(SERVER_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 30000
            conn.outputStream.write(body.toString().toByteArray())

            if (conn.responseCode != 200) return null

            val response = JSONObject(conn.inputStream.bufferedReader().readText())
            val elapsed  = System.currentTimeMillis() - t0

            val path    = parseJsonCells(response.getJSONArray("path"))
            val visited = parseJsonCells(response.getJSONArray("visited"))

            SolveResult(path, visited, elapsed)

        } catch (e: Exception) {
            android.util.Log.e("PrologEngine", "Errore HTTP: ${e.message}", e)
            null
        }
    }

    private fun parseJsonCells(array: JSONArray): List<Cell> {
        val cells = mutableListOf<Cell>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            cells.add(Cell(obj.getInt("row"), obj.getInt("col")))
        }
        return cells
    }
}