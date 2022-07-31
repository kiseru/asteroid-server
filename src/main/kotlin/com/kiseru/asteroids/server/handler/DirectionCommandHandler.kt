package com.kiseru.asteroids.server.handler

import com.kiseru.asteroids.server.User
import com.kiseru.asteroids.server.model.Direction

interface DirectionCommandHandler : CommandHandler {

    fun handleDirection(user: User, direction: Direction) {
        user.setSpaceshipDirection(direction)
        user.refreshRoom()
        user.sendMessage("success")
    }
}