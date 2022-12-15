package com.example.snake.client.clientState


import com.example.snake.client.GameSetings.GameConfig
import com.example.snake.client.Message.Message
import com.example.snake.proto.SnakesProto
import com.example.snake.proto.SnakesProto.NodeRole
import java.net.InetAddress

data class AnnounceItem(
    val playersCount: Int,
    val canJoin: Boolean,
    val gameName: String,
    val gameConfig: GameConfig,
    val nodeRole: NodeRole,
    val ip: InetAddress,
    val port: Int
) {
    override fun toString(): String {
        return "$gameName, ${if (canJoin) "available" else "no places"}, players: $playersCount"
    }

    companion object {

        @Synchronized
        fun fromProto(message: Message): List<AnnounceItem> {
            assert(message.msg.typeCase != SnakesProto.GameMessage.TypeCase.ANNOUNCEMENT)
            { "Unable to build AnnounceItem from not an announcement message" }
            val announceItem = mutableListOf<AnnounceItem>()
            message.msg.announcement.gamesList.forEach{
                announceItem += AnnounceItem(
                    playersCount = it.players.playersCount,
                    canJoin = it.canJoin,
                    gameName = it.gameName,
                    gameConfig = GameConfig(
                        width = it.config.width,
                        height = it.config.height,
                        foodStatic = it.config.foodStatic,
                        stateDelayMs = it.config.stateDelayMs
                    ),
                    nodeRole = NodeRole.NORMAL,
                    ip = message.ip,
                    port = message.port

                )
            }
            return announceItem
        }
    }
}
