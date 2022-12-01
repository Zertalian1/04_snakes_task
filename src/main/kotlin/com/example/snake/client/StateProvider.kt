package com.example.snake.client

object StateProvider {
    private val state = ClientState()

    fun getState(): ClientState {
        return state
    }
}