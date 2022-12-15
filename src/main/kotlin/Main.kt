import com.example.snake.Thread.ThreadManager
import com.example.snake.client.GameSetings.SettingsProvider
import com.example.snake.server.SnakeServerUtils
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.stage.Stage
import mu.KotlinLogging

class Main:Application() {
    private val logger = KotlinLogging.logger {}
    override fun start(primaryStage: Stage) {
        logger.info { "Starting ui" }
        val loader = FXMLLoader(Main::class.java.getResource("mainWindow.fxml"))
        val tmp = Scene(loader.load())

        primaryStage.onCloseRequest = EventHandler {
            run {
                SnakeServerUtils.stopServer()
                ThreadManager.shutdown()
            }
        }

        primaryStage.title = SettingsProvider.getSettings().mainWindowTitle
        primaryStage.scene = tmp
        primaryStage.isResizable = false
        primaryStage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(Main::class.java)
}