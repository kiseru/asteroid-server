package com.kiseru.asteroids.server.service.impl

import com.kiseru.asteroids.server.model.Room
import com.kiseru.asteroids.server.service.RoomService
import org.springframework.stereotype.Service
import java.util.concurrent.ExecutorService
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service
class RoomServiceImpl(
    private val mainExecutorService: ExecutorService,
) : RoomService {

    private val rooms = mutableListOf<Room>()

    private var notFullRoom = Room(mainExecutorService, this)

    private val lock = ReentrantLock()

    override fun getNotFullRoom(): Room {
        lock.withLock {
            if (!notFullRoom.isFull) {
                return notFullRoom
            }

            rooms.add(notFullRoom)
            notFullRoom = Room(mainExecutorService, this)
            return notFullRoom
        }
    }

    /**
     * Возвращает рейтинг пользователей комнаты.
     *
     * @return рейтинг пользователей комнаты
     */
    override fun getRoomRating(room: Room): String {
        return room.users.sortedBy { it.score }
            .joinToString("\n") { "${it.username} ${it.score}" }
    }

    /**
     * Рассылает сообщение пользователям комнаты.
     *
     * @param message сообщение
     */
    override fun sendMessageToUsers(room: Room, message: String) {
        for (user in room.users) {
            user.sendMessage(message)
        }
    }

    override fun showAllRatings() {
        for (room in rooms) {
            println(getRoomRating(room))
        }
    }

    override fun showAllGameFields() {
        for (room in rooms) {
            room.showGameField()
        }
    }
}