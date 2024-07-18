package com.kiseru.asteroids.server.service

import com.kiseru.asteroids.server.model.Screen

interface ScreenService {

    /**
     * Отображает экран игры в консоли.
     *
     * @param screen экран игры
     */
    fun display(screen: Screen)
}
