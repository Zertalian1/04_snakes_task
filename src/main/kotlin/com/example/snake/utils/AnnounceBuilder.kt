package com.example.snake.utils

import com.example.snake.client.GameSetings.GameConfig
import com.example.snake.proto.SnakesProto
import com.example.snake.server.ServerPlayerInfo

class AnnounceBuilder private constructor() {
    private var serverConfig: GameConfig? = null
    private var players: Map<Int, ServerPlayerInfo> = HashMap()
    private var canJoin = true

    fun addPlayers(players: Map<Int, ServerPlayerInfo>): AnnounceBuilder {
        this.players = players
        return this
    }

    fun setJoin(value: Boolean): AnnounceBuilder {
        this.canJoin = value
        return this
    }

    fun setServerConfig(serverConfig: GameConfig): AnnounceBuilder {
        this.serverConfig = serverConfig
        return this
    }

    fun build(): SnakesProto.GameMessage.AnnouncementMsg {
        assert(serverConfig != null) { "Set server config before building message" }

        val config = SnakesProto.GameConfig.newBuilder()
            .setWidth(serverConfig!!.width)
            .setHeight(serverConfig!!.height)
            .setStateDelayMs(serverConfig!!.stateDelayMs)
            .build()

        val playersTmp = SnakesProto.GamePlayers.newBuilder()
        this.players.forEach { (_, player) ->
            playersTmp.addPlayers(player.toProto())
        }

        val game = SnakesProto.GameAnnouncement.newBuilder().
        setPlayers(playersTmp.build()).
        setConfig(config).
        setCanJoin(canJoin).
        setGameName(serverConfig!!.gameName).
        build()

        return SnakesProto.GameMessage.AnnouncementMsg.newBuilder()
            .addGames(game).build()
    }

    companion object {
        fun getBuilder(): AnnounceBuilder {
            return AnnounceBuilder()
        }
    }
}