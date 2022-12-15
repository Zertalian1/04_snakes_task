package com.example.snake.core

import com.example.snake.client.GameSetings.GameConfig
import com.example.snake.core.data.CoreConfig
import com.example.snake.core.data.PlayerWrapper
import com.example.snake.core.data.Playfield
import com.example.snake.core.data.Point
import com.example.snake.proto.SnakesProto
import com.example.snake.utils.coordToPoint
import com.example.snake.utils.invertDir
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class SnakeGameCore(private val config: CoreConfig) : GameCore {
    private val logger = KotlinLogging.logger {}

    //entities
    private val snakes: MutableMap<Int, Snake> = ConcurrentHashMap()
    private var players: MutableMap<Int, PlayerWrapper> = ConcurrentHashMap()
    private val foods: MutableList<Point> = ArrayList()
    private val playfield: Playfield = Playfield(config.width, config.height)

    private var targetFoodCount: Int = config.foodStatic /*+ players.size * config.foodPerPlayer).toInt()*/

    private var randomNumberGenerator: Random = Random(System.nanoTime())

    /**
     *  этапы игры':
     *  1. двигать питонов
     *  2. проверять столкновения
     *  3. создавать еду
     *  4. отсылать результат
     */
    override fun tick() {
        logger.debug { "Updating game state" }
        targetFoodCount = (config.foodStatic + players.size * config.foodPerPlayer).toInt()

        players.forEach { (id, player) -> snakes[id]?.direction = player.lastTurn }
        snakes.forEach { (_, snake) -> snake.tick() }

        val removeIds: MutableSet<Int> = HashSet()
        snakes.forEach { (id0, snake0) ->
            snakes.forEach { (id1, snake1) ->
                if (id0 != id1 && snake0.ifCollide(snake1.getHead())) {
                    removeIds.add(id1)
                }
            }
            if (snake0.selfCollide()) {
                removeIds.add(id0)
            }
        }

        removeIds.forEach { id ->
            // если вставлять генерацию еды после смерти, то сюда
            snakes.remove(id)
            players.remove(id)
        }

        val eatenFoods: MutableSet<Point> = HashSet()
        snakes.forEach { (id, snake) ->
            foods.forEach { food ->
                if (snake.ifCollide(food)) {
                    if (eatenFoods.add(food)) {
                        snake.grow()
                        players[id]!!.score++
                    }
                }
            }
        }
        eatenFoods.forEach { food -> foods.remove(food) }

        if (foods.size < targetFoodCount) {
            for (i in 0 until targetFoodCount - foods.size) {
                foods.add(generateFood())
            }
        }

        players = players.filter { (id, _) -> snakes.containsKey(id)}.toMutableMap()
    }

    private fun generateFood(): Point {
        while (true) {
            val x = randomNumberGenerator.nextInt()
            val y = randomNumberGenerator.nextInt()
            val point = Point(x, y)
            playfield.normalizeDirty(point)

            if (!checkCollisions(point)) return point
        }
    }

    private fun checkCollisions(point: Point): Boolean {
        for (snake in snakes) {
            if (snake.value.ifCollide(point)) {
                return true
            }
        }
        if (point in foods) return true
        return false
    }

    private fun checkFree(point: Point): Boolean {
        return true
    }

    override fun putTurn(id: Int, dir: SnakesProto.Direction) {
        println("sender id $id")
        players.forEach{
            println("players id: ${it.value}")
        }
        players[id]?.let { wrapper -> wrapper.lastTurn = if (dir != invertDir(wrapper.lastTurn)) dir else return }
            ?: logger.error { "Player with id: $id not found, unable put next turn" }
    }

    //TODO доделать адекватное добавление игрока
    override fun addPlayer(id: Int, playerType: SnakesProto.PlayerType): Boolean {
        val player =
            PlayerWrapper(id = id, playerType = playerType, lastTurn = SnakesProto.Direction.DOWN)
        if (players[player.id] != null) {
            logger.warn { "Trying to add player with existing id: ${player.id}" }
            return false
        }

        //TODO поиск места на поле, для спавна
        val head = findPlace()
        if(head == null){
            logger.warn { "Can't find plaice to player : ${player.id}" }
            println("Can't find plaice to player : ${player.id}")
            return false
        }
        players[player.id] = player
        snakes[player.id] = Snake(head, Point(0, -1), playfield)
        return true
    }

    private fun findPlace(): Point? {
        val points = mutableListOf<Point>()
        points.addAll(foods)
        snakes.forEach { (t, u) ->
            points.addAll(u.getBody().toList())
        }
        for(i in 0..config.height){ //y
            for (j in 0 .. config.width){   // x
                val a = points.find {
                    it.x == i && it.y == j
                }
                val b = points.find {
                    it.x == i && it.y == j-1
                }
                if(a== null && b==null)
                    return Point(i,j)
            }
        }
        return null
    }

    /** Removes player from players list, but not his snake, snake state changes to ZOMBIE and removes only when snake
     * dies
     * @param id player to remove
     */
    override fun isZombie(id: Int) = snakes[id]?.getState() == SnakesProto.GameState.Snake.SnakeState.ZOMBIE

    override fun removePlayer(id: Int) {
        players.remove(id) ?: logger.error { "Player with id: $id not found, unable to remove player" }
        snakes[id]?.setStateZombie()
            ?: logger.error { "Snake for player with id: $id not found!, unable to remove snake" }
    }

    override fun getPlayers(): Map<Int, PlayerWrapper> {
        return players
    }

    override fun getFoods(): List<Point> {
        return foods
    }

    override fun getSnakes(): Map<Int, Snake> {
        return snakes
    }


    companion object {
        fun fromProtoState(state: SnakesProto.GameState, serverConfig: GameConfig): SnakeGameCore {

            val tmp = SnakeGameCore(
                CoreConfig(
                    width = serverConfig.width,
                    height = serverConfig.height,
                    foodStatic = serverConfig.foodStatic,
                )
            )

            //restoring foods
            state.foodsList.forEach { food ->
                tmp.foods.add(coordToPoint(food))
            }

            //restoring snakes
            val playfield = Playfield(serverConfig.width, serverConfig.height)
            state.snakesList.forEach { snake ->
                val mySnake = Snake(snake.pointsList.map { coord -> coordToPoint(coord) }, playfield)
                mySnake.direction = snake.headDirection
                if (SnakesProto.GameState.Snake.SnakeState.ZOMBIE == snake.state) {
                    mySnake.setStateZombie()
                }
                tmp.snakes[snake.playerId] = mySnake
            }

            //restoring players
            state.players.playersList.forEach { player ->
                tmp.players[player.id] = PlayerWrapper(
                    id = player.id,
                    playerType = player.type,
                    lastTurn = tmp.snakes[player.id]?.direction ?:SnakesProto.Direction.DOWN,
                    score = player.score
                )
            }


            return tmp
        }
    }
}