package com.kiseru.asteroids.server.service.impl

import com.kiseru.asteroids.server.model.Screen
import com.kiseru.asteroids.server.service.ScreenService
import org.springframework.stereotype.Service

@Service
class ScreenServiceImpl : ScreenService {

    override fun display(screen: Screen) {
        val result = StringBuilder("")
        for (i in 1 until screen.height + 1) {
            for (j in 1 until screen.width + 1) {
                result.append(screen.mainMatrix[i][j])
                result.append("\t")
            }
            result.append("\n")
        }

        println(result.toString())
    }
}
