package com.example.snake.core

import com.example.snake.core.data.Point


interface Collidable {
    fun ifCollide(point: Point): Boolean
}