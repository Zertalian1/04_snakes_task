package com.example.snake.client.Message

import java.net.InetAddress
import com.example.snake.proto.SnakesProto

data class Message(
    val msg: SnakesProto.GameMessage,
    val ip: InetAddress,
    val port: Int
)
