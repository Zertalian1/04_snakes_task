package com.example.snake.core

import com.example.snake.core.data.PlayerWrapper
import com.example.snake.core.data.Point
import com.example.snake.proto.SnakesProto

interface GameCore {
    fun tick()
    fun addPlayer(id: Int, playerType: SnakesProto.PlayerType): Boolean
    fun removePlayer(id: Int)
    fun putTurn(id: Int, dir: SnakesProto.Direction)
    fun isZombie(id: Int): Boolean
    fun getPlayers(): Map<Int, PlayerWrapper>
    fun getSnakes(): Map<Int, Snake>
    fun getFoods(): List<Point>

}