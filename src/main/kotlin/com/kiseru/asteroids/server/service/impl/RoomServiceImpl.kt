package com.kiseru.asteroids.server.service.impl

import com.kiseru.asteroids.server.game.GameService
import com.kiseru.asteroids.server.model.Room
import com.kiseru.asteroids.server.service.RoomService
import com.kiseru.asteroids.server.service.ScreenService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class RoomServiceImpl(
    private val gameService: GameService,
    private val screenService: ScreenService,
) : RoomService {

    private val roomStorage = mutableMapOf<UUID, Room>()

    private val roomStorageMutex = Mutex()

    override suspend fun getNotFullRoom(): Room {
        roomStorageMutex.withLock {
            val room = roomStorage.values.firstOrNull { !it.isFull }
            if (room != null) {
                return room
            }

            val newRoom = createRoom()
            roomStorage[newRoom.id] = newRoom
            return newRoom
        }
    }

    /**
     * Рассылает сообщение пользователям комнаты.
     *
     * @param message сообщение
     */
    override suspend fun sendMessageToUsers(room: Room, message: String) {
        for (messageSenderService in room.messageSenderServices) {
            messageSenderService.send(message)
        }
    }

    override fun showAllRatings() {
        for (room in roomStorage.values) {
            println(room.rating)
        }
    }

    override fun showAllGameFields() {
        for (room in roomStorage.values) {
            screenService.display(room.game.screen)
        }
    }

    override suspend fun startRoom(room: Room) {
        sendMessageToUsers(room, "start")
        room.status = Room.Status.GAMING
        room.refresh()
        awaitEndgame(room)
        val rating = room.rating
        sendMessageToUsers(room, "finish\n$rating")
        log.info("Room released! Rating table:\n$rating")
    }

    override suspend fun awaitEndgame(room: Room) {
        while (room.status != Room.Status.FINISHED) {
            yield()
        }
    }

    private suspend fun createRoom(): Room {
        val game = gameService.createGame()
        val roomId = generateUniqueRoomId()
        return Room(roomId, game)
    }

    private fun generateUniqueRoomId() = generateSequence { UUID.randomUUID() }
        .dropWhile { roomStorage.containsKey(it) }
        .first()

    override fun findRoomById(id: UUID): Room? {
        return roomStorage[id]
    }

    companion object {

        private val log = LoggerFactory.getLogger(RoomServiceImpl::class.java)
    }
}
