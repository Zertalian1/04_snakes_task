package com.example.snake.core.data

import com.example.snake.proto.SnakesProto

data class PlayerWrapper(
    val id: Int,
    var playerType: SnakesProto.PlayerType = SnakesProto.PlayerType.HUMAN,
    var lastTurn: SnakesProto.Direction,
    var score: Int = 0
)