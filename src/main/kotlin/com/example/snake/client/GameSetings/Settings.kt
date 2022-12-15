package com.example.snake.client.GameSetings

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class Settings {
    var screenSizeX = 1280
    var screenSizeY = 720
    var mainWindowTitle = "Snake FX"

    var multicastAddress: String = "239.192.0.4"
    var multicastPort: Int = 9192

    var announceDelayMs: Int = 1000
    var playerName = "Name"

    val stateTickDelayMs: Int = 1000
    val pingDelayMs: Int = 100
    val timeoutDelayMs: Int = 800
    val foodStatic  = 1


    val playfieldHeight: Int = 30
    val playfieldWidth: Int = 40
}