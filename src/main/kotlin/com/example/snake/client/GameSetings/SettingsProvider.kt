package com.example.snake.client.GameSetings

import com.fasterxml.jackson.databind.ObjectMapper

object SettingsProvider {
    private var settings: Settings? = null


    private fun loadSettings(): Settings {
        val objectMapper = ObjectMapper()
        return objectMapper.readValue(SettingsProvider::class.java.getResource("/settings.json"), Settings::class.java)
    }

    fun getSettings(): Settings {
        if (null == settings) {
            settings = loadSettings()
        }
        return settings!!
    }
}