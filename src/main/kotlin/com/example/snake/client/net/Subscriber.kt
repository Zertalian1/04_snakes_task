package com.example.snake.client.net

import com.example.snake.client.Message.Message

/*
метод которым обладают все клинеты, нужен для вызова его из Publiser
*/

interface Subscriber {
    fun update(message: Message)
}