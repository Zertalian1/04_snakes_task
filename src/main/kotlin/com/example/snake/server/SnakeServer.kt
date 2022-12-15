package com.example.snake.server

import com.example.snake.client.GameSetings.GameConfig
import com.example.snake.client.GameSetings.SettingsProvider
import com.example.snake.client.Message.Message
import com.example.snake.client.Message.MessageId
import com.example.snake.client.clientState.StateProvider
import com.example.snake.client.net.NetWorker
import com.example.snake.client.net.Publisher
import com.example.snake.client.net.Subscriber
import com.example.snake.client.net.errors.ErrorManager
import com.example.snake.core.GameCore
import com.example.snake.core.SnakeGameCore
import com.example.snake.core.data.CoreConfig
import com.example.snake.proto.SnakesProto
import com.example.snake.utils.AnnounceBuilder
import com.example.snake.utils.GameStateId
import com.example.snake.utils.PlayerId
import com.example.snake.utils.StateBuilder
import mu.KotlinLogging
import java.net.InetAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class SnakeServer(private val serverConfig: GameConfig) : Publisher(), Subscriber, Server {
    private var gameCore: GameCore

    init {
        gameCore = SnakeGameCore(
            CoreConfig(
                width = serverConfig.width,
                height = serverConfig.height
            )
        )
    }

    private val logger = KotlinLogging.logger {}

    private val netWorker: NetWorker = ServerThreadNetWorker()
    private val netWorkerThread = Thread(netWorker, "Server net worker")
    private var running: Boolean = true
    private val players: MutableMap<Int, ServerPlayerInfo> = ConcurrentHashMap()

    private var masterId: Int = 0
    private val timersPool = ScheduledThreadPoolExecutor(1)

    private val multicastAddress = InetAddress.getByName(SettingsProvider.getSettings().multicastAddress)
    private val multicastPort = SettingsProvider.getSettings().multicastPort

     fun fireAnnounce(address: InetAddress, port: Int) {
        val announce = AnnounceBuilder.getBuilder()
            .addPlayers(players.filter { players -> players.value.role != SnakesProto.NodeRole.VIEWER })
            .setJoin(true)
            .setServerConfig(serverConfig).build()

        val message = SnakesProto.GameMessage.newBuilder()
            .setAnnouncement(announce)
            .setMsgSeq(MessageId.getNextMessageId()).build()

        netWorker.putMessage(message, address, port)
    }

    private fun fireAnnounceMulti() {
        val announce = AnnounceBuilder.getBuilder()
            .addPlayers(players.filter { players -> players.value.role != SnakesProto.NodeRole.VIEWER })
            .setJoin(true)
            .setServerConfig(serverConfig).build()

        val message = SnakesProto.GameMessage.newBuilder()
            .setAnnouncement(announce)
            .setMsgSeq(MessageId.getNextMessageId()).build()

        netWorker.putMessage(message, multicastAddress, multicastPort)
    }

    private fun firePing() {
        players.forEach { (id, player) ->
            val message = SnakesProto.GameMessage.newBuilder()
                .setPing(SnakesProto.GameMessage.PingMsg.newBuilder().build())
                .setReceiverId(id)
                .setMsgSeq(MessageId.getNextMessageId()).build()
            netWorker.putMessage(message, player.addressInet, player.port)
        }
    }

    private fun fireTick() {
        gameCore.tick()
        if (players.isEmpty()) {
            return
        }
        gameCore.getPlayers().forEach { player ->
            players[player.key]!!.score = player.value.score
        }
        val state = StateBuilder.getBuilder()
            .setSnakes(gameCore.getSnakes())
            .setFoods(gameCore.getFoods())
            .setPlayers(players)
            .build()

        players.forEach{ (i, pl) ->
            println("player: $i role: ${pl.role}")
        }

        val gameMsgBuilder = SnakesProto.GameMessage.newBuilder()
            .setMsgSeq(MessageId.getNextMessageId())
            .setState(state)
        players.forEach { (_, player) ->
            netWorker.putMessage(gameMsgBuilder.setReceiverId(player.id).build(), player.addressInet, player.port)
        }

        players.forEach { (id, _) ->
            if (!gameCore.getPlayers().containsKey(id)) {
                setViewer(id)
            }
        }

    }

    private fun setViewer(id: Int) {
        if(players[id]!!.role == SnakesProto.NodeRole.MASTER){
            return
        }
        if (players[id]!!.role == SnakesProto.NodeRole.DEPUTY) {
            selectNewDeputy()
            players[id]!!.role = SnakesProto.NodeRole.NORMAL
        }


        val message = SnakesProto.GameMessage.newBuilder()
            .setRoleChange(
                SnakesProto.GameMessage.RoleChangeMsg.newBuilder().setReceiverRole(SnakesProto.NodeRole.VIEWER)
                    .build()
            )
            .setReceiverId(id)
            .setMsgSeq(MessageId.getNextMessageId()).build()

        netWorker.putMessage(message, players[id]!!.addressInet, players[id]!!.port)
        players[id]!!.role = SnakesProto.NodeRole.VIEWER
    }


    private fun handleJoin(message: Message) {
        logger.debug { "New client trying to join game!" }
        println("server add new player")
        var player: ServerPlayerInfo? = null
        players.forEach { (t, u) ->
            if(u.port == message.port && u.address == message.ip.toString().removePrefix("/")){
                player = u
            }
        }
        var newId = -1
        var setMaster = false
        if(player == null || gameCore.isZombie(newId)) {
            println("Player == null")
            newId = PlayerId.getNextPlayerId()
            setMaster = players.count { it -> it.value.role!==SnakesProto.NodeRole.VIEWER } == 0
            player = ServerPlayerInfo(
                name = message.msg.join.playerName,
                role = SnakesProto.NodeRole.NORMAL,
                id = newId,
                address = message.ip.toString().removePrefix("/"),
                port = message.port,
                connected = true
            )
        }else{
            newId = player!!.id
            player!!.role = SnakesProto.NodeRole.NORMAL
        }

        players[newId] = player!!
        gameCore.addPlayer(newId, SnakesProto.PlayerType.HUMAN)

        val response = SnakesProto.GameMessage.newBuilder()
            .setAck(SnakesProto.GameMessage.AckMsg.getDefaultInstance())
            .setReceiverId(newId)
            .setMsgSeq(MessageId.getNextMessageId())
            .build()

        netWorker.putMessage(response, message.ip, message.port)

        if (setMaster) {
            masterId = newId
            players[newId]!!.role = SnakesProto.NodeRole.MASTER
        }

        val roleChange = SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
            .setReceiverRole(if (setMaster) SnakesProto.NodeRole.MASTER else SnakesProto.NodeRole.NORMAL)
        val message0 = SnakesProto.GameMessage.newBuilder()
            .setRoleChange(roleChange)
            .setMsgSeq(MessageId.getNextMessageId())
            .build()
        netWorker.putMessage(message0, message.ip, message.port)

        logger.info { "New player accepted id: $newId" }

        if (players.size == 2) {
            selectNewDeputy()
        }
    }


    private fun handleError(message: Message) {
        val error = message.msg.error
        if (ErrorManager.isServiceError(error.errorMessage)) {
            val id = ErrorManager.fromString(error.errorMessage)
            disconnectPlayer(id)
            return
        } else {
            logger.error { "Received error message: ${error.errorMessage}" }
        }
    }

    private fun disconnectPlayer(id: Int) {
        when (players[id]!!.role) {
            SnakesProto.NodeRole.MASTER -> {
                logger.error { "I literally doesnt know what should happen if server trying to disconnect master, gl hf" }
            }
            SnakesProto.NodeRole.DEPUTY -> {
                selectNewDeputy()
            }
            else -> {}
        }
        players.remove(id)
        gameCore.removePlayer(id)
        logger.info { "Removed player with id $id" }
    }


    //TODO redo
    override fun update(message: Message) {
        when (message.msg.typeCase) {
            SnakesProto.GameMessage.TypeCase.STEER -> handleSteer(message)
            SnakesProto.GameMessage.TypeCase.JOIN -> handleJoin(message)
            SnakesProto.GameMessage.TypeCase.ERROR -> handleError(message)
            SnakesProto.GameMessage.TypeCase.ROLE_CHANGE -> handleRoleChange(message)
            else -> logger.error { "Snake server received message of invalid type: ${message.msg.typeCase}" }
        }
    }

    // TODO написано говно
    private fun handleRoleChange(message: Message) {
        val msg = message.msg
        /*when (msg.roleChange.senderRole) {
            SnakesProto.NodeRole.VIEWER -> {*/
                when (players[msg.senderId]!!.role) {
                    SnakesProto.NodeRole.NORMAL -> {
                        players[msg.senderId]?.role = msg.roleChange.senderRole
                        players.remove(msg.senderId)
                        gameCore.removePlayer(msg.senderId)
                    }
                    SnakesProto.NodeRole.DEPUTY -> {
                        selectNewDeputy()
                        players[msg.senderId]?.role = msg.roleChange.senderRole
                        players.remove(msg.senderId)
                        gameCore.removePlayer(msg.senderId)
                    }
                    SnakesProto.NodeRole.MASTER -> {
                        shutdown()
                    }
                    else -> {
                        logger.error {
                            "Unexpected state change, player: ${msg.senderId} trying to " +
                                    "change role from ${players[msg.senderId]!!.role} to ${msg.roleChange.senderRole}"
                        }
                        return
                    }
                }
           /* }
            else -> {
                logger.warn { "Client ${msg.senderId} trying to change role not to viewer" }
                return
            }
        }*/
        val changeAccept = SnakesProto.GameMessage.newBuilder()
            .setRoleChange(
                SnakesProto.GameMessage.RoleChangeMsg.newBuilder().setReceiverRole(message.msg.roleChange.senderRole)
            ).setMsgSeq(MessageId.getNextMessageId())
            .setReceiverId(message.msg.senderId)
            .build()
        netWorker.putMessage(changeAccept, message.ip, message.port)

    }

    private fun selectNewDeputy() {
        for (pair in players) {
            if (pair.value.role == SnakesProto.NodeRole.NORMAL) {
                val message = SnakesProto.GameMessage.newBuilder()
                    .setRoleChange(
                        SnakesProto.GameMessage.RoleChangeMsg.newBuilder().setSenderRole(SnakesProto.NodeRole.DEPUTY)
                            .build()
                    )
                    .setSenderId(masterId)
                    .setReceiverId(pair.value.id)
                    .setMsgSeq(MessageId.getNextMessageId())
                    .build()

                netWorker.putMessage(message, pair.value.addressInet, pair.value.port)
                pair.value.role = SnakesProto.NodeRole.DEPUTY
                logger.info { "Deputy changed to id: ${pair.value.id}" }
                break
            }
        }
    }

    private fun handleSteer(message: Message) {
        val dir = message.msg.steer!!.direction
        val id = message.msg.senderId

        gameCore.putTurn(id, dir)
    }


    override fun run() {
        try {
            initialize()
            while (running) {
                Thread.sleep(10000)
            }
        } catch (_: InterruptedException) {
            shutdown()
        }
    }

    @Synchronized
    override fun shutdown() {
        var deputy: ServerPlayerInfo? = null
        players.forEach { (_, player) ->
            if (player.role == SnakesProto.NodeRole.DEPUTY) deputy = player
        }

        if (null != deputy) {
            logger.info { "Master left, trying to change topology" }
            val changeAccept = SnakesProto.GameMessage.newBuilder()
                .setRoleChange(
                    SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                        .setReceiverRole(SnakesProto.NodeRole.MASTER)
                ).setMsgSeq(MessageId.getNextMessageId())
                .setReceiverId(deputy!!.id)
                .build()
            netWorker.putMessage(changeAccept, deputy!!.addressInet, deputy!!.port)
            Thread.sleep(1000)
        }

        netWorker.shutdown()
        netWorkerThread.join()

        running = false
        timersPool.shutdown()
    }

    override fun getPort(): Int {
        return netWorker.getPort()
    }

    private fun initialize() {
        timersPool.scheduleAtFixedRate(
            { fireTick() },
            0L,
            serverConfig.stateDelayMs.toLong(),
            TimeUnit.MILLISECONDS
        )
        timersPool.scheduleAtFixedRate(
            { firePing() },
            0L,
            serverConfig.pingDelayMs.toLong(),
            TimeUnit.MILLISECONDS
        )
        timersPool.scheduleAtFixedRate(
            { fireAnnounceMulti() },
            0L,
            SettingsProvider.getSettings().announceDelayMs.toLong(),
            TimeUnit.MILLISECONDS
        )

        netWorkerThread.start()
        netWorker.subscribe(this, SnakesProto.GameMessage.TypeCase.JOIN)
        netWorker.subscribe(this, SnakesProto.GameMessage.TypeCase.STEER)
        netWorker.subscribe(this, SnakesProto.GameMessage.TypeCase.ERROR)
        netWorker.subscribe(this, SnakesProto.GameMessage.TypeCase.ROLE_CHANGE)
        println("end init server")
    }

    private constructor(serverConfig: GameConfig, core: GameCore, masterId: Int) : this(serverConfig) {
        this.masterId = masterId
        this.gameCore = core
    }

    companion object {
        fun fromProto(state: SnakesProto.GameState, serverConfig: GameConfig, masterId: Int): SnakeServer {
            GameStateId.setNextStateId(state.stateOrder)
            val ids = state.players.playersList.map { gamePlayer -> gamePlayer.id }
            println(Collections.max(ids))
            PlayerId.setNextPlayerId(Collections.max(ids)+1)
            val newServer = SnakeServer(serverConfig, SnakeGameCore.fromProtoState(state, serverConfig), masterId)
            state.players.playersList.forEach { gamePlayer ->
                newServer.players[gamePlayer.id] = ServerPlayerInfo(
                    name = gamePlayer.name,
                    id = gamePlayer.id,
                    address = gamePlayer.ipAddress,
                    port = gamePlayer.port,
                    role = if (StateProvider.getState().id == gamePlayer.id) SnakesProto.NodeRole.MASTER else
                        if (gamePlayer.role == SnakesProto.NodeRole.MASTER) SnakesProto.NodeRole.VIEWER else gamePlayer.role,
                    score = gamePlayer.score,
                    playerType = gamePlayer.type,
                    connected = true
                )
            }
            /*for (i in newServer.players){
                println(i.value.role)
            }*/
            newServer.masterId = StateProvider.getState().id

            val notifyBuilder = SnakesProto.GameMessage.newBuilder()
                .setRoleChange(
                    SnakesProto.GameMessage.RoleChangeMsg.newBuilder().setSenderRole(SnakesProto.NodeRole.MASTER)
                        .build()
                )
                .setMsgSeq(MessageId.getNextMessageId())

            newServer.players.forEach { (id, player) ->
                if (player.role != SnakesProto.NodeRole.DEPUTY) {
                    newServer.netWorker.putMessage(
                        notifyBuilder.setReceiverId(id).build(),
                        player.addressInet,
                        player.port
                    )
                }
            }

            return newServer
        }
    }
}