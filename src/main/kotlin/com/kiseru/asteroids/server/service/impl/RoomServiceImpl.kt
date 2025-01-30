package com.kiseru.asteroids.server.service.impl

import com.kiseru.asteroids.server.room.Room
import com.kiseru.asteroids.server.service.RoomService
import java.io.IOException
import java.io.OutputStream

class RoomServiceImpl : RoomService {

    private val rooms = mutableListOf<Room>()

    private var notFullRoom = Room(this)

    override fun writeRatings(outputStream: OutputStream) =
        synchronized(this) {
            for (room in rooms) {
                writeRating(room, outputStream)
            }
        }

    private fun writeRating(room: Room, outputStream: OutputStream) =
        try {
            val rating = room.rating
            outputStream.write("$rating\n".toByteArray())
        } catch (_: IOException) {
            println("Failed to write the room's rating")
        }

    override fun writeGameFields(outputStream: OutputStream) =
        synchronized(this) {
            for (room in rooms) {
                writeGameField(room, outputStream)
            }
        }

    private fun writeGameField(room: Room, outputStream: OutputStream) =
        try {
            val screen = room.game.screen
            val gameField = screen.display()
            outputStream.write("$gameField\n".toByteArray())
        } catch (_: IOException) {
            println("Failed to write the room's game field")
        }

    override fun getNotFullRoom(): Room =
        synchronized(this) {
            if (notFullRoom.isFull) {
                rooms.add(notFullRoom)
                notFullRoom = Room(this)
            }

            notFullRoom
        }
}
