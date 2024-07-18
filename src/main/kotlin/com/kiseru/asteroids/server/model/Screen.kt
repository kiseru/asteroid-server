package com.kiseru.asteroids.server.model

import java.util.*

class Screen(val width: Int, val height: Int) {

    val mainMatrix: Array<Array<String>> = Array(height + 2) { Array(width + 2) { "." } }

    init {
        generateClearScreen()
    }

    fun draw(point: Point) {
        val symbol = point.symbolToShow
        if (mainMatrix[point.y][point.x] == ".") {
            mainMatrix[point.y][point.x] = symbol
        } else {
            mainMatrix[point.y][point.x] =
                String.format("%s|%s", mainMatrix[point.y][point.x], symbol)
        }
    }

    /**
     * Обновляет экран.
     */
    fun update() {
        generateClearScreen()
    }

    private fun generateClearScreen() {
        for (i in 0 until height + 2) {
            if (i == 0 || i == height + 1) {
                Arrays.fill(mainMatrix[i], "*")
            } else {
                Arrays.fill(mainMatrix[i], ".")
                mainMatrix[i][0] = "*"
                mainMatrix[i][width + 1] = "*"
            }
        }
    }
}
