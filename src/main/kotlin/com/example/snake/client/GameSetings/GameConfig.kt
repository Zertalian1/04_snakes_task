package com.example.snake.client.GameSetings

class GameConfig(var width: Int = SettingsProvider.getSettings().playfieldWidth,
                 var height: Int = SettingsProvider.getSettings().playfieldHeight,
                 var foodStatic: Int =  SettingsProvider.getSettings().foodStatic,
                 var stateDelayMs: Int = SettingsProvider.getSettings().stateTickDelayMs,
                 val pingDelayMs: Int = SettingsProvider.getSettings().pingDelayMs,
                 val timeoutDelayMs: Int = SettingsProvider.getSettings().timeoutDelayMs,
                 var gameName: String = "unnamedGame"
                 ) {

}