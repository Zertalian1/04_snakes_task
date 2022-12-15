package com.example.snake.client.clientState

import com.example.snake.client.GameSetings.SettingsProvider
import com.example.snake.client.Message.Message
import com.example.snake.client.net.Publisher
import com.example.snake.proto.SnakesProto
import mu.KotlinLogging
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException
import java.util.*

class AnnounceHandler : Publisher(), Runnable {
    private val multicastAddress: InetAddress
    private val multicastPort: Int
    private val socket: MulticastSocket

    private var running: Boolean = true
    private val logger = KotlinLogging.logger {}


    init {
        val settings = SettingsProvider.getSettings()
        multicastAddress = InetAddress.getByName(settings.multicastAddress)
        multicastPort = settings.multicastPort
        socket = MulticastSocket(settings.multicastPort)
    }


    override fun run() {
        initialize()
        while (running && !Thread.interrupted()) {
            val packet = DatagramPacket(ByteArray(BUFFER_SIZE), BUFFER_SIZE)

            try {
                socket.soTimeout = SettingsProvider.getSettings().announceDelayMs
                socket.receive(packet)
            } catch (_: SocketTimeoutException) {
                continue
            }

            val message = SnakesProto.GameMessage.parseFrom(Arrays.copyOf(packet.data, packet.length))!!
            if (message.typeCase != SnakesProto.GameMessage.TypeCase.ANNOUNCEMENT) {
                logger.error { "Announcer received not an announce message" }
                continue
            }
            notifyMembers(
                Message(message, packet.address, packet.port),
                SnakesProto.GameMessage.TypeCase.ANNOUNCEMENT
            )
        }

        shutdown()
    }

    private fun initialize() {
        socket.joinGroup(multicastAddress)
    }

    @Synchronized
    fun shutdown() {
        logger.info { "Shutting down announcer thread" }
        socket.leaveGroup(multicastAddress)
        running = false
    }

    companion object {
        private const val BUFFER_SIZE: Int = 4096
    }
}