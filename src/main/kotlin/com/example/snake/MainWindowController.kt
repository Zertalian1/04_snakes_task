package com.example.snake

import com.example.snake.Thread.ThreadManager
import com.example.snake.client.GameSetings.GameConfig
import com.example.snake.client.GameSetings.SettingsProvider
import com.example.snake.client.Message.Message
import com.example.snake.client.Message.MessageId
import com.example.snake.client.Painter.Bundle
import com.example.snake.client.Painter.JavaFxPainter
import com.example.snake.client.Painter.Painter
import com.example.snake.client.clientState.AnnounceItem
import com.example.snake.client.clientState.StateProvider
import com.example.snake.proto.SnakesProto.*
import com.example.snake.client.net.NetWorker
import com.example.snake.client.net.Subscriber
import com.example.snake.client.net.client.ClientThreadNetWorker
import com.example.snake.server.SnakeServerUtils
import com.example.snake.client.clientState.AnnounceHandler
import com.example.snake.proto.SnakesProto
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import mu.KotlinLogging
import java.net.InetAddress
import java.net.URL
import java.util.*
import kotlin.concurrent.fixedRateTimer

class MainWindowController : Subscriber, Initializable {
    private val logger = KotlinLogging.logger {}

    private val netWorker: NetWorker = ClientThreadNetWorker()
    private val netWorkerThread: Thread = Thread(netWorker, "Client net worker")

    private lateinit var painter: Painter

    private val announcer: AnnounceHandler = AnnounceHandler()
    private val announcerThread: Thread = Thread(announcer, "Announcer thread")

    private val availableServersBuffer = mutableSetOf<AnnounceItem>()
    private var gameConfig:GameConfig = GameConfig()

    @FXML
    lateinit var canvas: Canvas

    @FXML
    lateinit var hostNameLabel: Label

    @FXML
    lateinit var serverGameName: TextField

    @FXML
    lateinit var fieldSizeLabel: Label

    @FXML
    lateinit var foodRuleLabel: Label

    @FXML
    lateinit var errorLabel: Label

    @FXML
    lateinit var currentGameInfo: ListView<String>
    private val currentGameInfoList = FXCollections.observableArrayList<String>()

    @FXML
    lateinit var availableServers: ListView<AnnounceItem>
    private val availableServersList = FXCollections.observableArrayList<AnnounceItem>()

    fun handleAddFriend(){
        if(StateProvider.getState().role != NodeRole.MASTER){
            return
        }
        SnakeServerUtils.addFriend()
    }

    fun handleKeyboard(keyEvent: KeyEvent) {
        if (0 == StateProvider.getState().id) {
            logger.warn { "Client not connected, unable to handle steer" }
            return
        }
        val action: Direction = when (keyEvent.code) {
            KeyCode.W -> Direction.UP
            KeyCode.A -> Direction.LEFT
            KeyCode.S -> Direction.DOWN
            KeyCode.D -> Direction.RIGHT
            else -> return
        }

        when (StateProvider.getState().role) {
            NodeRole.VIEWER -> return
            else -> {
                val steer: GameMessage.SteerMsg = GameMessage.SteerMsg
                    .newBuilder()
                    .setDirection(action)
                    .build()
                val message: GameMessage = GameMessage.newBuilder()
                    .setMsgSeq(MessageId.getNextMessageId())
                    .setSenderId(StateProvider.getState().id)
                    .setSteer(steer)
                    .build()

                netWorker.putMessage(
                    message,
                    StateProvider.getState().serverAddress,
                    StateProvider.getState().serverPort
                )
            }
        }
    }

    fun handleExitGame() {
        if (0 == StateProvider.getState().id) {
            println("exit game Error")
            return
        }
        if (SnakeServerUtils.isRunning()){
            SnakeServerUtils.stopServer()
            println("stop server")
        }
        else {
            val message = GameMessage.newBuilder()
                .setRoleChange(GameMessage.RoleChangeMsg.newBuilder().setSenderRole(NodeRole.VIEWER).build())
                .setSenderId(StateProvider.getState().id)
                .setMsgSeq(MessageId.getNextMessageId())
                .build()
            netWorker.putMessage(message, StateProvider.getState().serverAddress, StateProvider.getState().serverPort)
        }
        StateProvider.getState().role = NodeRole.VIEWER
        StateProvider.getState().id = 0
        netWorker.subscribe(this, GameMessage.TypeCase.ACK)
        println(SnakeServerUtils.isRunning())
    }

    fun handleLoadServerGame() {
        //val discoverMsg = GameMessage.DiscoverMsg.newBuilder().build()
        //netWorker.putMessage( GameMessage.newBuilder().setDiscover(discoverMsg).build(), InetAddress.getByName("snakes.ippolitov.me"), 9192)
    }

    fun handleStartNewLGame() {
        if (SnakeServerUtils.isRunning()) {
            println("cant start new server")
            return
        }
        if(StateProvider.getState().id != 0){
            handleExitGame()
        }
        val settings = SettingsProvider.getSettings()
        gameConfig = GameConfig(
            pingDelayMs = settings.pingDelayMs,
            timeoutDelayMs = settings.timeoutDelayMs,
            width = settings.playfieldWidth,
            height = settings.playfieldHeight,
            foodStatic = settings.foodStatic,
            stateDelayMs = settings.stateTickDelayMs
        )
        if(null != serverGameName.text && serverGameName.text.isNotEmpty()) {
            gameConfig.gameName = serverGameName.text
        }
        gameConfig.gameName = findUniName()
        println(gameConfig.gameName)

        SnakeServerUtils.startServer(gameConfig)

        joinGame(
            AnnounceItem(
                playersCount = 0,
                canJoin = true,
                ip = InetAddress.getLocalHost(),
                port = SnakeServerUtils.getPort(),
                gameName = gameConfig.gameName,
                nodeRole = NodeRole.MASTER,
                gameConfig = gameConfig
            )
        )
    }

    fun handleStartNewSGame() {
        /*if(null != serverGameName.text) {
            val joinMessage = GameMessage.newBuilder()
                .setJoin(
                    GameMessage.JoinMsg.newBuilder().setGameName(serverGameName.text)
                        .build()
                )
                .setMsgSeq(MessageId.getNextMessageId())
                .build()
            netWorker.putMessage(joinMessage, InetAddress.getByName("snakes.ippolitov.me"), 9192)
        }*/
    }

    private fun findUniName(): String {
        var name: String = gameConfig.gameName
        val gameNameSize = gameConfig.gameName.length
        var isUni = true
        var i = 1
        do {
            isUni = true
            availableServersList.forEach {
                if(it.gameName == name){
                    isUni = false
                }
            }
            if(!isUni){
                name = name.substring(0, gameNameSize) + i.toString()
                i++
            }
        }while(!isUni)
        return name
    }

    private fun joinGame(announceItem: AnnounceItem) {
        println("joining game")
        netWorker.subscribe(this, GameMessage.TypeCase.ACK)

        this.gameConfig = announceItem.gameConfig
        painter.setConfig(gameConfig)

        val joinMessage = GameMessage.newBuilder()
            .setJoin(
                GameMessage.JoinMsg.newBuilder().
                setGameName(announceItem.gameName).
                setPlayerName(SettingsProvider.getSettings().playerName).
                setRequestedRole(NodeRole.NORMAL).build()
            )
            .setMsgSeq(MessageId.getNextMessageId())
            .build()

        netWorker.putMessage(joinMessage, announceItem.ip, announceItem.port)
    }

    fun handleJoinGame() {
        if (StateProvider.getState().role != NodeRole.VIEWER && StateProvider.getState().serverPort > 0) {
            handleExitGame()
            //return
            /*logger.warn { "Already joined game" }
            return*/
        }

        if (null != availableServers.selectionModel.selectedItem) {

            joinGame(availableServers.selectionModel.selectedItem)
            StateProvider.getState().role = NodeRole.NORMAL
        }
    }

    private fun handleAck(message: Message) {
        StateProvider.getState().serverAddress = message.ip
        StateProvider.getState().serverPort = message.port
        StateProvider.getState().id = message.msg.receiverId
        netWorker.unsubscribe(this, GameMessage.TypeCase.ACK)
    }

    private fun handleError(message: Message) {
        if(StateProvider.getState().role == NodeRole.DEPUTY){
            restoreServerLocal()
            if (message.msg.roleChange.senderRole == NodeRole.MASTER) {
                changeServer(message.ip, message.port)
            }
        }
    }

    private fun restoreServerLocal() {
        SnakeServerUtils.restoreServer(StateProvider.getState().lastGameState!!, gameConfig)
    }

    private fun changeServer(newAddress: InetAddress, newPort: Int) {
        StateProvider.getState().serverAddress = newAddress
        StateProvider.getState().serverPort = newPort

        netWorker.clearQueue().forEach { message ->
            netWorker.putMessage(message.msg, newAddress, newPort)
        }
    }

    private fun handleRoleChange(message: Message) {
        StateProvider.getState().role = message.msg.roleChange.receiverRole
        if (message.msg.roleChange.receiverRole == NodeRole.MASTER && !SnakeServerUtils.isRunning()) {
            logger.info { "Master left from game, trying to restore topology" }
            restoreServerLocal()
            return
        }

        if (message.msg.roleChange.senderRole == NodeRole.MASTER) {
            changeServer(message.ip, message.port)
        }
    }

    private fun handleAnnounce(msg: Message) {
        availableServersBuffer.addAll(AnnounceItem.fromProto(msg))
        println(availableServersBuffer)
    }


    override fun update(message: Message) {
        when (message.msg.typeCase) {
            GameMessage.TypeCase.STATE -> {
                if ((StateProvider.getState().lastGameState?.stateOrder ?: -1) < message.msg.state.state.stateOrder) {
                    StateProvider.getState().lastGameState = message.msg.state.state
                }
            }
            GameMessage.TypeCase.ACK -> handleAck(message)
            GameMessage.TypeCase.ROLE_CHANGE -> handleRoleChange(message)
            GameMessage.TypeCase.ANNOUNCEMENT -> handleAnnounce(message)
            GameMessage.TypeCase.ERROR -> handleError(message)
            else -> return
        }
    }

    private fun fireAnnounceUpdate() {
        Platform.runLater {
            var selectedItem: AnnounceItem? = null
            if (null != availableServers.selectionModel.selectedItem) {
                selectedItem = availableServers.selectionModel.selectedItem
            }
            availableServersList.clear()
            availableServersList.addAll(availableServersBuffer)
            availableServersBuffer.clear()

            if (selectedItem != null) {
                val tmp =
                    availableServersList.find { announceItem ->
                        announceItem.ip == selectedItem.ip && announceItem.port == selectedItem.port
                    }
                if (null != tmp) {
                    if (tmp.canJoin) {
                        availableServers.selectionModel.select(tmp)
                    }
                }
            }
        }
    }

    private fun pingServer() {
        if (StateProvider.getState().role != NodeRole.VIEWER && StateProvider.getState().serverPort > 0) {
            val message = GameMessage.newBuilder().setPing(GameMessage.PingMsg.getDefaultInstance())
                .setMsgSeq(MessageId.getNextMessageId())
                .build()
            netWorker.putMessage(message, StateProvider.getState().serverAddress, StateProvider.getState().serverPort)
        }
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        painter = JavaFxPainter(
            Bundle(
                canvas = this.canvas,
                hostNameLabel = this.hostNameLabel,
                fieldSizeLabel = this.fieldSizeLabel,
                foodRuleLabel = this.foodRuleLabel,
                errorLabel = this.errorLabel,
                serverGameName = this.serverGameName,

                currentGameInfo = this.currentGameInfo,
                currentGameInfoList = this.currentGameInfoList,

                availableServers = this.availableServers,
                availableServersList = this.availableServersList
            )
        )

        currentGameInfo.items = currentGameInfoList
        currentGameInfo.isEditable = false
        availableServers.items = availableServersList
        availableServers.isEditable = false

        netWorker.subscribe(this, GameMessage.TypeCase.ROLE_CHANGE)
        netWorker.subscribe(this, GameMessage.TypeCase.STATE)
        netWorker.subscribe(this, GameMessage.TypeCase.ERROR)

        netWorker.subscribe(painter, GameMessage.TypeCase.STATE)
        netWorker.subscribe(painter, GameMessage.TypeCase.ERROR)

        announcer.subscribe(this, GameMessage.TypeCase.ANNOUNCEMENT)

        ThreadManager.addThread(netWorkerThread)
        ThreadManager.addThread(announcerThread)

        netWorkerThread.start()
        announcerThread.start()

        fixedRateTimer(
            name = "Core tick timer",
            daemon = true,
            initialDelay = 0L,
            period = SettingsProvider.getSettings().announceDelayMs.toLong()
        ) { fireAnnounceUpdate() }
        fixedRateTimer(
            name = "Client ping timer",
            daemon = true,
            initialDelay = 0L,
            period = SettingsProvider.getSettings().announceDelayMs.toLong()
        ) { pingServer() }
    }
}

 /*TODO косячит прорисовка: 1) при старте нового сервера, игра не отображается - пофикшено
                            2) при отключении игрока после смены мастера игра виснит - ЗАКОСТЫЛЕНО
                            3)  при join присоединяется 20 человек ИМЕННО ПОСЛЕ ПЕРЕДАЧИ - сделать проверку, кто подключается - пофикшено
                            4) handleRoleChange в SnakeServer не работает, что то не так с конфигом после отключения от игры
                            5) при отключении от игры, пользователь должен получает роль Viever и при повторном подключении создаёт свою копию пофикшено*/