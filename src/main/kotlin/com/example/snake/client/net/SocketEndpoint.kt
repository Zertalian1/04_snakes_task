package com.example.snake.client.net

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketAddress

class SocketEndpoint : ConnectionEndpoint {

    override var soTimeout: Int = DEFAULT_TIMEOUT
        set(value) {
            socket.soTimeout = value
            field = value
        }

    private val socket: DatagramSocket

    constructor(address: InetAddress, port: Int) {
        socket = DatagramSocket(port, address)
    }

    constructor(port: Int) {
        socket = DatagramSocket()
    }

    constructor(socketAddress: SocketAddress) {
        socket = DatagramSocket(socketAddress)
    }

    override fun receive(buffer: DatagramPacket) {
        socket.receive(buffer)
    }

    override fun send(datagramPacket: DatagramPacket) {
        socket.send(datagramPacket)
    }

    override fun close() {
        socket.close()
    }

    override fun getPort(): Int {
        return socket.localPort
    }

    companion object {
        const val DEFAULT_TIMEOUT = Int.MAX_VALUE
    }

}