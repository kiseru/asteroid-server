package com.kiseru.asteroids.server.service

import com.kiseru.asteroids.server.model.Room
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object RoomService {

    val rooms = mutableListOf<Room>()

    private var notFullRoom = Room()

    private val lock = ReentrantLock()

    fun getNotFullRoom(): Room {
        lock.withLock {
            if (!notFullRoom.isFull()) {
                return notFullRoom
            }

            rooms.add(notFullRoom)
            notFullRoom = Room()
            return notFullRoom
        }
    }
}