package com.example.snake.client.clientState

object StateProvider {
    private val state = ClientState()

    fun getState(): ClientState {
        return state
    }
}