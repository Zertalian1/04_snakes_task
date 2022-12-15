package com.example.snake.client.Painter

import com.example.snake.client.GameSetings.GameConfig
import com.example.snake.client.clientState.AnnounceItem
import com.example.snake.client.net.Subscriber
import com.example.snake.proto.SnakesProto

interface Painter: Subscriber {
    fun repaint(state: SnakesProto.GameMessage.StateMsg)
    fun repaintAvailableServers(aboba: List<AnnounceItem>)
    fun clearPlayfield()
    fun setConfig(gameConfig: GameConfig)
}