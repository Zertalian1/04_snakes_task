package com.example.snake.server

interface Server: Runnable{
    fun shutdown()
    fun getPort(): Int
}