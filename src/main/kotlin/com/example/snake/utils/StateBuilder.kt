package com.example.snake.utils

import com.example.snake.core.Snake
import com.example.snake.core.data.Point
import com.example.snake.proto.SnakesProto
import com.example.snake.server.ServerPlayerInfo

class StateBuilder private constructor() {
    private var players: Map<Int, ServerPlayerInfo> = HashMap()
    private var snakes: Map<Int, Snake> = HashMap()
    private var foods: List<Point> = ArrayList()

    fun setFoods(foods: List<Point>): StateBuilder {
        this.foods = foods
        return this
    }

    fun setSnakes(snakes: Map<Int, Snake>): StateBuilder {
        this.snakes = snakes
        return this
    }

    fun setPlayers(players: Map<Int, ServerPlayerInfo>): StateBuilder {
        this.players = players
        return this
    }

    fun build(): SnakesProto.GameMessage.StateMsg {

        val state = SnakesProto.GameState.newBuilder()
            .setStateOrder(GameStateId.getNextStateId())


        snakes.forEach { (id, snake) ->
            val keyPoints = snake.serialize()
            val snakeMsg = SnakesProto.GameState.Snake.newBuilder()
                .setPlayerId(id)
                .setHeadDirection(snake.direction)
                .setState(snake.getState())


            keyPoints.forEach { point ->
                snakeMsg.addPoints(pointToCoord(point))
            }

            state.addSnakes(snakeMsg.build())
        }
        val playersMsg = SnakesProto.GamePlayers.newBuilder()
        players.forEach { (id, player) ->
            playersMsg.addPlayers(
                SnakesProto.GamePlayer.newBuilder()
                    .setId(id)
                    .setRole(player.role)
                    .setIpAddress(player.address)
                    .setPort(player.port)
                    .setName(player.name)
                    .setScore(player.score).build()
            )
        }
        state.setPlayers(playersMsg.build())
        foods.forEach { point ->
            state.addFoods(pointToCoord(point))
        }

        return SnakesProto.GameMessage.StateMsg.newBuilder().setState(state.build()).build()
    }

    companion object {
        fun getBuilder(): StateBuilder {
            return StateBuilder()
        }
    }
}