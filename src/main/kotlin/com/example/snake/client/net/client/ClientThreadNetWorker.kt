package com.example.snake.client.net.client

import com.example.snake.client.Message
import com.example.snake.client.net.ConnectionEndpoint
import com.example.snake.client.net.MessageWrapper
import com.example.snake.client.net.NetWorker
import com.example.snake.proto.SnakesProto
import java.net.InetAddress
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.collections.ArrayDeque

class ClientThreadNetWorker : NetWorker {


    private val outgoingQueue: Deque<Message> = ConcurrentLinkedDeque()

    private var pendingMessage: MessageWrapper? = null
    private var running = true

    constructor() : super(SocketEndpoint(0))

    constructor(connectionEndpoint: ConnectionEndpoint) : super(connectionEndpoint)

    override fun setEndpoint(endpoint: ConnectionEndpoint): NetWorker {
        this.endpoint = endpoint
        return this
    }

    override fun clearQueue(): Deque<Message> {
        val tmp = ArrayDeque(outgoingQueue)
        outgoingQueue.clear()
        return tmp
    }

    override fun run() {

        while (running && !Thread.interrupted()) {

            //пуляем сообщение
            if (outgoingQueue.isNotEmpty() && pendingMessage == null) {
                val message = outgoingQueue.poll()
                sendMessage(message)

                if (message.msg.typeCase != SnakesProto.GameMessage.TypeCase.ACK &&
                    message.msg.typeCase != SnakesProto.GameMessage.TypeCase.ANNOUNCEMENT
                ) {
                    pendingMessage = MessageWrapper(message, System.currentTimeMillis(), System.currentTimeMillis())
                }
            }

            //принимаем сообщение
            val incMessage = receiveMessage()

            if (incMessage != null) {
                sendAck(incMessage)
                logger.trace { "Received new message type of: ${incMessage.msg.typeCase}, from ${incMessage.ip}" }
                notifyMembers(incMessage, incMessage.msg.typeCase)

                if (incMessage.msg.typeCase == SnakesProto.GameMessage.TypeCase.ACK) {
                    if (null != pendingMessage) {
                        if (pendingMessage!!.message.msg.msgSeq <= incMessage.msg.msgSeq) {
                            pendingMessage = null
                        }
                    }
                }
            }


            //таймауты и переотправка сообщений
            var serverProblems = false
            if (pendingMessage != null) {
                if (System.currentTimeMillis() - pendingMessage!!.firstSendTime > SettingsProvider.getSettings().timeoutDelayMs) {
                    serverProblems = true
                } else {
                    if (System.currentTimeMillis() - pendingMessage!!.resendTime > SettingsProvider.getSettings().pingDelayMs) {
                        sendMessage(pendingMessage!!.message)
                        pendingMessage!!.resendTime = System.currentTimeMillis()
                    }
                }
            }

            if (serverProblems) {
                val error = SnakesProto.GameMessage.newBuilder().setError(
                    SnakesProto.GameMessage.ErrorMsg.newBuilder().setErrorMessage(
                        ErrorManager.wrap(-1)
                    )
                )
                    .setMsgSeq(MessageIdProvider.getNextMessageId())
                    .build()
                notifyMembers(Message(error, InetAddress.getLoopbackAddress(), -1), error.typeCase)
                pendingMessage = null
            }

        }
        shutdown()
    }

    @Synchronized
    override fun putMessage(message: SnakesProto.GameMessage, ip: InetAddress, port: Int) {
        outgoingQueue.push(Message(message, ip, port))
        logger.trace { "New message of type: ${message.typeCase} in queue, seq: ${message.msgSeq} " }
    }

    override fun shutdown() {
        running = false
        logger.info { "Shutting down client net worker" }
    }
}