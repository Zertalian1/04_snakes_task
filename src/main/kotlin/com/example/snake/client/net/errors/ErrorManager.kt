package com.example.snake.client.net.errors

object ErrorManager {
    fun fromString(target: String): Int = target.substringAfter("%%", "-1").toInt()

    fun isServiceError(target: String): Boolean = target.startsWith("%%")

    fun wrap(id: Int): String = "%%$id"
}