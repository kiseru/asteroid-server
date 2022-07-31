package com.kiseru.asteroids.server.handler.impl

import com.kiseru.asteroids.server.User
import com.kiseru.asteroids.server.handler.CommandHandler

class UnknownCommandHandler : CommandHandler {

    override fun handle(user: User) {
        user.sendMessage("Unknown command")
    }
}