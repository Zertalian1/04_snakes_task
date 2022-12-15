package com.example.snake.client.clientState

import com.example.snake.proto.SnakesProto
import java.net.InetAddress

data class ClientState(
    var id: Int = 0,
    var role: SnakesProto.NodeRole = SnakesProto.NodeRole.VIEWER,
    var serverAddress: InetAddress = InetAddress.getLocalHost(),
    var serverPort: Int = 0,
    var lastGameState: SnakesProto.GameState? = null
)
