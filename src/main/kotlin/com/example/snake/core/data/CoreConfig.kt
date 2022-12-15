package com.example.snake.core.data

data class CoreConfig(
    val width: Int = 40,
    val height: Int = 30,
    val foodStatic: Int = 1,
    val foodPerPlayer: Float = 10f,
    val deadFoodProbe: Float = .1f
)
