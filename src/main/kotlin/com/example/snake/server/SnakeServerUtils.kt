package com.example.snake.server

import com.example.snake.client.GameSetings.GameConfig
import com.example.snake.client.clientState.StateProvider
import com.example.snake.proto.SnakesProto
import mu.KotlinLogging
import java.net.InetAddress

object SnakeServerUtils {
    private val logger = KotlinLogging.logger {}
    private var serverThread: Thread? = null
    private var isRunning  = false
    private var server: Server? = null

    @Synchronized
    fun addFriend() {
        /*server?.let {
            (it as SnakeServer).fireAnnounce(InetAddress.getByName("snakes.ippolitov.me"), 9192)
        }*/
    }

    @Synchronized
    fun isRunning(): Boolean  = isRunning


    @Synchronized
    fun getPort(): Int = server?.getPort() ?: -1

    @Synchronized
    fun startServer(serverConfig: GameConfig) {
        server = SnakeServer(serverConfig)
        serverThread = Thread(server, "Snake server")
        serverThread?.start()
        println("Server started")
        isRunning = true
    }

    @Synchronized
    fun restoreServer(message: SnakesProto.GameState, serverConfig: GameConfig) {
        server = SnakeServer.fromProto(message,serverConfig, StateProvider.getState().id)
        serverThread = Thread(server, "Snake server")
        serverThread?.start()
        println("Server Re started")
        isRunning = true
    }

    @Synchronized
    fun stopServer() {
        if (isRunning) {
            server?.shutdown()
            serverThread?.interrupt()
            logger.info { "Server stopped successfully" }
            serverThread?.join()
            println("Server stopped successfully")
            isRunning = false
        }
    }
}