package com.example.snake.client.net

import com.example.snake.client.Message

data class MessageWrapper(
    val message: Message,
    val firstSendTime: Long,
    var resendTime: Long
) {
}