package com.example.snake.client.Painter

import com.example.snake.client.clientState.AnnounceItem
import javafx.collections.ObservableList
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextArea
import javafx.scene.control.TextField

data class Bundle(
    val canvas: Canvas,
    val hostNameLabel: Label,
    val fieldSizeLabel: Label,
    val foodRuleLabel: Label,
    val errorLabel: Label,
    val serverGameName: TextField,

    val currentGameInfo: ListView<String>,
    val currentGameInfoList: ObservableList<String>,

    val availableServers: ListView<AnnounceItem>,
    val availableServersList: ObservableList<AnnounceItem>
)
