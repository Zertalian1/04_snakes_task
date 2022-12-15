package com.example.snake.client.Painter

import com.example.snake.client.GameSetings.GameConfig
import com.example.snake.client.Message.Message
import com.example.snake.client.clientState.AnnounceItem
import com.example.snake.client.clientState.StateProvider
import com.example.snake.client.net.Subscriber
import com.example.snake.core.Snake
import com.example.snake.core.data.Playfield
import com.example.snake.proto.SnakesProto
import com.example.snake.utils.coordToPoint

import javafx.application.Platform
import javafx.scene.paint.Color
import mu.KotlinLogging

class JavaFxPainter(bundle: Any) : Painter, Subscriber {
    private val logger = KotlinLogging.logger {}
    private val bundle: Bundle
    private var gameConfig: GameConfig? = null

    init {
        this.bundle = bundle as Bundle
    }

    private fun paintFood(food: SnakesProto.GameState.Coord) {
        val context = bundle.canvas.graphicsContext2D
        context.fill = Color.GREEN
        context.fillOval((CELL_SIZE * food.x).toDouble() + 4, (CELL_SIZE * food.y).toDouble() + 4, 12.0, 12.0)
    }

    private fun paintSnake(snake: Snake, color: Color) {
        val context = bundle.canvas.graphicsContext2D

        context.fill = color

        snake.getBody().forEach { point ->
            context.fillRect(
                (point.x * CELL_SIZE).toDouble(),
                (point.y * CELL_SIZE).toDouble(),
                CELL_SIZE.toDouble(),
                CELL_SIZE.toDouble()
            )
        }
    }

    private fun paintGrid(width: Int, height: Int) {
        val context = bundle.canvas.graphicsContext2D

        context.clearRect(0.0, 0.0, bundle.canvas.width, bundle.canvas.height)
        context.fill = Color.GRAY
        context.lineWidth = 0.5

        for (i in 0 until (width + 1)) {
            context.strokeLine(
                (CELL_SIZE * i).toDouble(),
                0.0,
                (CELL_SIZE * i).toDouble(),
                (height * CELL_SIZE).toDouble()
            )
        }
        for (i in 0 until (height + 1)) {
            context.strokeLine(
                0.0,
                (CELL_SIZE * i).toDouble(),
                (width * CELL_SIZE).toDouble(),
                (CELL_SIZE * i).toDouble()
            )
        }
    }

    override fun setConfig(gameConfig: GameConfig){
        this.gameConfig = gameConfig
    }

    override fun repaint(state: SnakesProto.GameMessage.StateMsg) {
        gameConfig?.let{
            // 1. Paint grid
            paintGrid(gameConfig!!.width, gameConfig!!.height)

            // 2. Paint foods
            state.state.foodsList.forEach { food ->
                paintFood(food)
            }
            // 3. Paint snakes
            state.state.snakesList.forEach { snake ->
                val tmp = snake.pointsList.map { coord -> coordToPoint(coord) }
                val snek = Snake(tmp, Playfield(gameConfig!!.width, gameConfig!!.height))
                val color = if (snake.playerId == StateProvider.getState().id) Color.RED else Color.BLACK
                paintSnake(snek, color)
            }

            // 4. Paint players
            bundle.currentGameInfoList.clear()
            state.state.players.playersList.forEach { player ->
                if (player.id == StateProvider.getState().id) {
                    bundle.hostNameLabel.text = "${player.role}"
                    bundle.fieldSizeLabel.text = "${player.id}"
                }
                paintPlayer(player)
                //println("Painter player: ${player.id} role: ${player.role} state provider id ${StateProvider.getState().id}")
            }
        } ?: return

    }

    override fun repaintAvailableServers(servers: List<AnnounceItem>) {
        bundle.availableServersList.clear()
        bundle.availableServersList.addAll(servers)
    }

    override fun clearPlayfield() {
        val context = bundle.canvas.graphicsContext2D
        context.clearRect(0.0, 0.0, bundle.canvas.width, bundle.canvas.height)
    }

    private fun paintPlayer(player: SnakesProto.GamePlayer) {
        bundle.currentGameInfoList.add("${player.score} ${player.name}")
    }

    @Synchronized
    override fun update(message: Message) {
        val msg = message.msg
        when (msg.typeCase) {
            SnakesProto.GameMessage.TypeCase.STATE -> {
                if ((StateProvider.getState().lastGameState?.stateOrder ?: 0) <= msg.state.state.stateOrder) {
                    StateProvider.getState().id = msg.receiverId
                    Platform.runLater { repaint(msg.state) }
                }
            }

            SnakesProto.GameMessage.TypeCase.ERROR -> {
                Platform.runLater { addErrorMessage(msg.error) }
            }
            else -> logger.error {
                "Painter received unacceptable message type: ${msg.typeCase}"
            }
        }
    }

    private fun addErrorMessage(error: SnakesProto.GameMessage.ErrorMsg) {
        bundle.errorLabel.text = error.errorMessage
    }

    companion object {
        private const val CELL_SIZE = 20
    }

}