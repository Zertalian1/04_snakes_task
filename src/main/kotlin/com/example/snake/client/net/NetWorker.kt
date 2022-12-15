package com.example.snake.client.net

import com.example.snake.client.Message.Message
import com.example.snake.proto.SnakesProto
import mu.KotlinLogging
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.*
import kotlin.collections.ArrayDeque

abstract class NetWorker(protected var endpoint: ConnectionEndpoint) : Publisher(), Runnable {
    protected val logger = KotlinLogging.logger {}

    abstract fun putMessage(message: SnakesProto.GameMessage, ip: InetAddress, port: Int)
    abstract fun shutdown()
    abstract fun setEndpoint(endpoint: ConnectionEndpoint): NetWorker
    abstract fun clearQueue(): ArrayDeque<Message>

    fun getPort(): Int {
        return endpoint.getPort()
    }

    protected fun receiveMessage(): Message? {
        val packet = DatagramPacket(ByteArray(BUFFER_SIZE), BUFFER_SIZE)

        try {
            endpoint.soTimeout = TIMEOUT
            endpoint.receive(packet)
        } catch (e: SocketTimeoutException) {
            return null
        }
        println(packet.address)
        println(packet.port)
        val message = SnakesProto.GameMessage.parseFrom(Arrays.copyOf(packet.data, packet.length))
        return Message(message, packet.address, packet.port)
    }

    protected fun sendMessage(message: Message) {
        val arr = message.msg.toByteArray()
        /*println(arr.size)*/
        println(message.ip)
        println(message.port)
        val packet = DatagramPacket(arr, arr.size, message.ip, message.port)
        endpoint.send(packet)
        logger.trace { "Message seq of ${message.msg.msgSeq} sent" }
    }

    protected fun sendAck(message: Message) {
        if (message.msg.typeCase == SnakesProto.GameMessage.TypeCase.JOIN ||
            message.msg.typeCase == SnakesProto.GameMessage.TypeCase.ACK ||
            message.msg.typeCase == SnakesProto.GameMessage.TypeCase.ANNOUNCEMENT
        ) {
            return
        }

        val ack = SnakesProto.GameMessage.newBuilder()
            .setAck(SnakesProto.GameMessage.AckMsg.getDefaultInstance())
            .setReceiverId(message.msg.senderId)
            .setMsgSeq(message.msg.msgSeq)
            .build()

        sendMessage(Message(ack, message.ip, message.port))
    }


    companion object {
        internal const val TIMEOUT: Int = 20
        internal const val BUFFER_SIZE: Int = 8192
    }
}