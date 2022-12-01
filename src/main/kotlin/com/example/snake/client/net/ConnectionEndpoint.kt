package com.example.snake.client.net

import java.net.DatagramPacket

interface ConnectionEndpoint {
    var soTimeout: Int

    fun receive(buffer: DatagramPacket)

    fun send(datagramPacket: DatagramPacket)

    fun close()

    fun getPort(): Int
}