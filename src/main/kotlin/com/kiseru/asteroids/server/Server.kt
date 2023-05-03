package com.kiseru.asteroids.server

import com.kiseru.asteroids.server.exception.GameFinishedException
import com.kiseru.asteroids.server.factory.MessageReceiverServiceFactory
import com.kiseru.asteroids.server.factory.MessageSenderServiceFactory
import com.kiseru.asteroids.server.command.factory.CommandHandlerFactory
import com.kiseru.asteroids.server.dto.TokenDto
import com.kiseru.asteroids.server.model.Room
import com.kiseru.asteroids.server.model.Spaceship
import com.kiseru.asteroids.server.model.User
import com.kiseru.asteroids.server.service.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.*

@Component
class Server(
    private val messageReceiverServiceFactory: MessageReceiverServiceFactory,
    private val messageSenderServiceFactory: MessageSenderServiceFactory,
    private val commandHandlerFactory: CommandHandlerFactory,
    private val roomService: RoomService,
    private val serverSocket: ServerSocket,
    private val userService: UserService,
    private val tokenService: TokenService,
) {

    suspend fun startServer() = coroutineScope {
        launch {
            startAcceptingConnections()
        }

        val scanner = Scanner(System.`in`)
        while (true) {
            val command = withContext(Dispatchers.IO) {
                scanner.nextLine()
            }
            when (command) {
                "rating" -> roomService.showAllRatings()
                "gamefield" -> roomService.showAllGameFields()
                "exit" -> break
            }
        }
    }

    private suspend fun startAcceptingConnections() = coroutineScope {
        newConnections()
            .onStart { log.info("Started accepting new connections") }
            .collect {
                launch { handleNewConnection(it) }
            }
    }

    private suspend fun newConnections(): Flow<Socket> = channelFlow {
        while (true) {
            send(serverSocket.awaitAccept())
        }
    }

    private suspend fun handleNewConnection(newConnection: Socket) = coroutineScope {
        log.info("Started handling new connection")
        val room = roomService.getNotFullRoom()
        val messageReceiverService = messageReceiverServiceFactory.create(newConnection)
        val messageSenderService = messageSenderServiceFactory.create(newConnection)
        val user = try {
            messageSenderService.sendWelcomeMessage()
            val username = messageReceiverService.receive()
            val user = userService.createUser(username)
            val tokenDto = TokenDto(tokenService.generateToken(user))
            messageSenderService.send(Json.encodeToString(tokenDto))
            messageSenderService.sendInstructions(user)
            user
        } catch (e: IOException) {
            log.error("Failed to authorize user", e)
            throw e
        }
        val spaceship = addUser(room, user, messageSenderService)
        if (room.users.size == Room.MAX_USERS) {
            launch {
                startRoom(room)
            }
        }
        roomService.sendMessageToUsers(room, "User ${user.username} has joined the room.")
        launch {
            runUser(user, room, messageSenderService, messageReceiverService, spaceship) {
                newConnection.awaitClose()
            }
        }
    }

    private suspend fun addUser(room: Room, user: User, messageSenderService: MessageSenderService): Spaceship {
        check(room.users.size < Room.MAX_USERS)
        room.status = Room.Status.WAITING_CONNECTIONS
        room.users = room.users + user
        room.messageSenderServices += messageSenderService
        return room.game.registerSpaceshipForUser(user, room)
    }

    private suspend fun runUser(
        user: User,
        room: Room,
        messageSenderService: MessageSenderService,
        messageReceiverService: MessageReceiverService,
        spaceship: Spaceship,
        closeSocket: suspend () -> Unit,
    ) {
        messageReceiverService.receivingFlow()
            .onCompletion {
                user.isAlive = false
                room.setGameFinished()
            }
            .takeWhile { !room.isGameFinished && user.isAlive }
            .collect { command ->
                handleCommand(user, room, messageSenderService, command, spaceship, closeSocket)
                incrementSteps(user)
                checkIsAlive(user, messageSenderService)
            }
    }

    private suspend fun handleCommand(
        user: User,
        room: Room,
        messageSenderService: MessageSenderService,
        command: String,
        spaceship: Spaceship,
        closeSocket: suspend () -> Unit,
    ) {
        val commandHandler = commandHandlerFactory.create(command)
        commandHandler.handle(user, room, messageSenderService, spaceship, closeSocket)
    }

    private fun incrementSteps(user: User) {
        if (!user.isAlive) {
            throw GameFinishedException()
        }

        user.steps++
    }

    private suspend fun checkIsAlive(user: User, messageSenderService: MessageSenderService) {
        if (user.steps >= 1500 || user.score < 0) {
            died(user, messageSenderService)
        }
    }

    private suspend fun died(user: User, messageSenderService: MessageSenderService) {
        user.isAlive = false
        messageSenderService.sendGameOver(user.score)
    }

    private suspend fun startRoom(room: Room) {
        roomService.sendMessageToUsers(room, "start")
        room.status = Room.Status.GAMING
        room.game.refresh()
        roomService.awaitEndgame(room)
        val rating = room.rating
        roomService.sendMessageToUsers(room, "finish\n$rating")
        log.info("Room $room released! Rating table:\n$rating")
    }

    companion object {

        private val log = LoggerFactory.getLogger(Server::class.java)
    }
}
