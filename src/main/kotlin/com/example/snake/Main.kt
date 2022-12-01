package com.example.snake

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class Main:Application() {
    override fun start(primaryStage: Stage) {
        val loader = FXMLLoader(Main::class.java.getResource("mainWindow.fxml"))
        val tmp = Scene(loader.load())

        primaryStage.title = "Snake"
        primaryStage.scene = tmp
        primaryStage.isResizable = false
        primaryStage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(Main::class.java)
}