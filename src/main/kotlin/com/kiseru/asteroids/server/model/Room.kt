package com.kiseru.asteroids.server.model

import com.kiseru.asteroids.server.Server
import com.kiseru.asteroids.server.User
import com.kiseru.asteroids.server.logics.Game
import com.kiseru.asteroids.server.logics.Screen
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Room : Runnable {

    private val lock: Lock = ReentrantLock()

    private val endgameCondition = lock.newCondition()

    private val spaceShipCreatedCondition = lock.newCondition()

    private val users: MutableList<User> = CopyOnWriteArrayList()

    private var status = Status.WAITING_CONNECTIONS

    lateinit var game: Game
        private set

    override fun run() {
        sendMessageToUsers("start")
        status = Status.GAMING
        lock.withLock {
            game = Game(Screen(SCREEN_WIDTH, SCREEN_HEIGHT), NUMBER_OF_GARBAGE_CELLS, NUMBER_OF_ASTEROID_CELLS)
            for (user in users) {
                game.registerSpaceShipForUser(user)
            }
            spaceShipCreatedCondition.signalAll()
        }

        game.refresh()

        lock.withLock {
            while (status != Status.FINISHED) {
                endgameCondition.await()
            }
        }

        sendMessageToUsers("finish\n${getRating()}")
        log.info("Room released! Rating table:\n${getRating()}")
    }

    /**
     * Добавляет пользователя в комнату и рассылает уведомление об этом остальным пользователям.
     */
    fun addUser(user: User) {
        if (users.size >= MAX_USERS) {
            Server.getNotFullRoom().addUser(user)
        }
        sendMessageToUsers("User ${user.userName} has joined the room.")
        users.add(user)
    }

    /**
     * Возвращает рейтинг пользователей комнаты.
     *
     * @return рейтинг пользователей комнаты
     */
    fun getRating(): String {
        return users.sortedBy { it.score }
            .joinToString("\n") { "${it.userName} ${it.score}" }
    }

    /**
     * Возвращает `true` если комната заполнена, иначе `false`.
     *
     * @return `true` если комната заполнена, иначе `false`
     */
    fun isFull(): Boolean {
        return users.size >= MAX_USERS
    }

    /**
     * Возвращает `true` если комната играет, иначе `false`.
     *
     * @return `true` если комната играет, иначе `false`
     */
    fun isGameStarted(): Boolean {
        return status == Status.GAMING
    }

    /**
     * Возвращает `true` если комната закончила игру, иначе `false`.
     *
     * @return `true` если комната закончила игру, иначе `false`
     */
    fun isGameFinished(): Boolean {
        return status == Status.FINISHED
    }

    /**
     * Проверяет, собрали ли весь мусор
     *
     * @param collected количество собранного мусора
     */
    fun checkCollectedGarbage(collected: Int) {
        if (collected >= game.garbageNumber) {
            setGameFinished()
        }
    }

    /**
     * Переводит игру в статус завершен.
     */
    fun setGameFinished() = lock.withLock {
        if (status == Status.FINISHED) {
            return
        }

        status = Status.FINISHED
        log.info("Game finished")
        endgameCondition.signalAll()
    }

    fun addUserToRoom(user: User) = lock.withLock {
        addUser(user)
        if (isFull()) {
            Server.getNotFullRoom()
            EXECUTOR_SERVICE.execute(this)
        }

        while (user.spaceShip == null) {
            spaceShipCreatedCondition.await()
        }
    }

    /**
     * Рассылает сообщение пользователям комнаты.
     *
     * @param message сообщение
     */
    private fun sendMessageToUsers(message: String) {
        for (user in users) {
            user.sendMessage(message)
        }
    }

    companion object {

        private val EXECUTOR_SERVICE = Executors.newCachedThreadPool()

        private const val SCREEN_WIDTH = 30

        private const val SCREEN_HEIGHT = 30

        private const val NUMBER_OF_GARBAGE_CELLS = 150

        private const val NUMBER_OF_ASTEROID_CELLS = 50

        private const val MAX_USERS = 1

        private val log = LoggerFactory.getLogger(Room::class.java)
    }

    private enum class Status {
        WAITING_CONNECTIONS,
        GAMING,
        FINISHED,
    }
}