package com.example.snake.client


import com.example.snake.proto.SnakesProto
import java.net.InetAddress

data class AnnounceItem(
    val playersCount: Int,
    val canJoin: Boolean,
    val gameName: String,
    val ip: InetAddress,
    val port: Int
) {
    override fun toString(): String {
        return "${ip.toString().removePrefix("/")} ${if (canJoin) "available" else "no places"}, players: $playersCount"
    }

    companion object {

        @Synchronized
        fun fromProto(message: Message): List<AnnounceItem> {
            assert(message.msg.typeCase != SnakesProto.GameMessage.TypeCase.ANNOUNCEMENT)
            { "Unable to build AnnounceItem from not an announcement message" }
            val game = message.msg.announcement.gamesList
            val announceItem = mutableListOf<AnnounceItem>()
            for(gameI in game){
                announceItem += AnnounceItem(
                    playersCount = gameI.players.playersCount,
                    canJoin = gameI.canJoin,
                    gameName = gameI.gameName,
                    ip = message.ip,
                    port = message.port

                )
            }
            return announceItem
        }
    }
}
