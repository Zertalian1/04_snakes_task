package com.example.snake.utils

object GameStateId {
    private var nextStateId: Int = 0

    @Synchronized
    fun setNextStateId(id: Int) {
        nextStateId = id
    }

    @Synchronized
    fun getNextStateId(): Int {
        nextStateId++
        return nextStateId - 1
    }
}