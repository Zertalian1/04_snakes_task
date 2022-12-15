package com.example.snake.server

import com.example.snake.client.GameSetings.SettingsProvider
import com.example.snake.client.Message.Message
import com.example.snake.client.Message.MessageId
import com.example.snake.client.net.ConnectionEndpoint
import com.example.snake.client.net.MessageWrapper
import com.example.snake.client.net.NetWorker
import com.example.snake.client.net.SocketEndpoint
import com.example.snake.client.net.errors.ErrorManager
import com.example.snake.proto.SnakesProto
import java.net.InetAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.collections.ArrayDeque

class ServerThreadNetWorker : NetWorker {
    private val messageQueue: MutableMap<InetAddress, Deque<Message>> = ConcurrentHashMap()
    private val pendingMessages: MutableMap<InetAddress, MessageWrapper> = HashMap()
    private val lastMsgSeq: MutableMap<InetAddress, Long> = ConcurrentHashMap()

    private var running: Boolean = true

    constructor() : super(SocketEndpoint(0))
    constructor(connectionEndpoint: ConnectionEndpoint) : super(connectionEndpoint)

    @Synchronized
    override fun putMessage(message: SnakesProto.GameMessage, ip: InetAddress, port: Int) {
        val wrapper = Message(message, ip, port)
        if (!messageQueue.containsKey(ip)) {
            messageQueue[ip] = ConcurrentLinkedDeque()
        }
        messageQueue[ip]!!.push(wrapper)

    }

    override fun shutdown() {
        running = false
        logger.info { "Shutting down server networker" }
    }


    override fun setEndpoint(endpoint: ConnectionEndpoint): NetWorker {
        this.endpoint = endpoint
        return this
    }

    override fun clearQueue(): ArrayDeque<Message> {
        return ArrayDeque()
    }


    override fun run() {
        while (running && !Thread.interrupted()) {
            for (queue in messageQueue) {
                if (queue.value.isNotEmpty() && pendingMessages[queue.key] == null) {
                    val message = queue.value.poll()
                    sendMessage(message)
                    if (message.msg.typeCase != SnakesProto.GameMessage.TypeCase.ACK &&
                        message.msg.typeCase != SnakesProto.GameMessage.TypeCase.ANNOUNCEMENT
                    ) {
                        pendingMessages[message.ip] =
                            MessageWrapper(message, System.currentTimeMillis(), System.currentTimeMillis())
                    }

                }
            }

            //receiving messages
            val incMsg = receiveMessage()

            if (incMsg != null) {
                sendAck(incMsg)
                logger.trace { "Received new message type of: ${incMsg.msg.typeCase}, from ${incMsg.ip}" }
                if (incMsg.msg.typeCase == SnakesProto.GameMessage.TypeCase.ACK) {
                    val pending = pendingMessages[incMsg.ip]
                    if (pending != null) {
                        if (pending.message.msg.msgSeq <= incMsg.msg.msgSeq) {
                            pendingMessages.remove(incMsg.ip)
                        }
                    }
                }
                if(incMsg.msg.typeCase == SnakesProto.GameMessage.TypeCase.JOIN){
                    println("Server Handle Join")
                }
                notifyMembers(incMsg, incMsg.msg.typeCase)
            }

            //check for timeouts
            val disconnected: MutableList<Message> = ArrayList()
            pendingMessages.forEach { (_, wrapper) ->
                if (System.currentTimeMillis() - wrapper.firstSendTime > SettingsProvider.getSettings().timeoutDelayMs) {
                    disconnected.add(wrapper.message)
                } else {
                    if (System.currentTimeMillis() - wrapper.resendTime > SettingsProvider.getSettings().pingDelayMs) {
                        sendMessage(wrapper.message)
                        wrapper.resendTime = System.currentTimeMillis()
                    }
                }
            }

            disconnected.forEach { player ->
                if (!player.msg.hasReceiverId()) {
                    logger.error { "Id not found for message: ${player.msg}" }
                    return
                }
                logger.info { "Disconnecting player: ${player.msg.receiverId}" }
                val error = SnakesProto.GameMessage.newBuilder().setError(
                    SnakesProto.GameMessage.ErrorMsg.newBuilder().setErrorMessage(
                        ErrorManager.wrap(player.msg.receiverId)
                    )
                )
                    .setMsgSeq(MessageId.getNextMessageId())
                    .build()
                notifyMembers(Message(error, InetAddress.getLoopbackAddress(), -1), error.typeCase)
                pendingMessages.remove(player.ip)
                messageQueue.remove(player.ip)
            }
        }
        shutdown()
    }
}