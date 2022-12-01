package com.example.snake.client

object MessageId {
    private var nextMessageId: Long = 0L;

    @Synchronized
    fun setNextMessageId(id: Long) {
        nextMessageId = id
    }
    @Synchronized
    fun getNextMessageId(): Long {
        nextMessageId++
        return nextMessageId - 1L
    }

    @Synchronized
    fun nextMessageId(): Long {
        return nextMessageId
    }
}