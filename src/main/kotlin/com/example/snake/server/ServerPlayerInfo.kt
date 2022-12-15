package com.example.snake.server

import com.example.snake.proto.SnakesProto
import java.net.InetAddress

class ServerPlayerInfo(
    val name: String = "",
    val id: Int,
    val address: String,
    val port: Int,
    var role: SnakesProto.NodeRole,
    var score: Int = 0,
    val playerType: SnakesProto.PlayerType = SnakesProto.PlayerType.HUMAN,
    var connected: Boolean = false
) {

    val addressInet: InetAddress = if (address.isNotEmpty()) {
        InetAddress.getByName(address)
    } else {
        InetAddress.getLocalHost()
    }

    fun toProto(): SnakesProto.GamePlayer {
        return SnakesProto.GamePlayer.newBuilder()
            .setName(name)
            .setId(id)
            .setIpAddress(address)
            .setPort(port)
            .setRole(role)
            .setScore(score)
            .setType(playerType).build()
    }

}