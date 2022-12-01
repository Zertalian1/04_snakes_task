package com.example.snake.client

import com.example.snake.proto.SnakesProto.*
import com.example.snake.client.net.NetWorker
import com.example.snake.client.net.client.ClientThreadNetWorker
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import mu.KotlinLogging

class MainWindowController {
    private val logger = KotlinLogging.logger {}

    private val netWorker: NetWorker = ClientThreadNetWorker()
    private val netWorkerThread: Thread = Thread(netWorker, "Client net worker")


    @FXML
    lateinit var canvas: Canvas

    @FXML
    lateinit var hostNameLabel: Label

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

    fun handleExitGame() {}

    fun handleStartNewGame() {}

    fun handleJoinGame() {}
}