package com.example.snake.Thread

object ThreadManager {

    private val threads: MutableList<Thread> = ArrayList()

    @Synchronized
    fun addThread(thread: Thread) {
        threads.add(thread)
    }

    @Synchronized
    fun shutdown() {
        threads.forEach { it.interrupt() }
        threads.forEach { it.join() }
    }
}