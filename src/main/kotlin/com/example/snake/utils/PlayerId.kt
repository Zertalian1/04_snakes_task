package com.example.snake.utils

object PlayerId {
    private var nextPlayerId: Int = 1

    @Synchronized
    fun setNextPlayerId(id: Int) {
        nextPlayerId = id
    }

    @Synchronized
    fun getNextPlayerId(): Int {
        nextPlayerId++
        return nextPlayerId - 1
    }
}