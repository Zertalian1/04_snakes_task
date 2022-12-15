package com.example.snake.utils

import com.example.snake.core.data.Point
import com.example.snake.proto.SnakesProto


fun coordToPoint(coord: SnakesProto.GameState.Coord): Point {
    return Point(coord.x, coord.y)
}

fun pointToCoord(point: Point): SnakesProto.GameState.Coord {
    return SnakesProto.GameState.Coord.newBuilder()
        .setX(point.x)
        .setY(point.y)
        .build()
}

fun pointToDir(point: Point): SnakesProto.Direction {
    assert(
        (point.x == 0) xor (point.y == 0)
    ) { "Unable to create dir for point x: ${point.x}, y: ${point.y}" }


    return if (point.x > 0) {
        SnakesProto.Direction.RIGHT
    } else if (point.x < 0) {
        SnakesProto.Direction.LEFT
    } else if (point.y > 0) {
        SnakesProto.Direction.DOWN
    } else {
        SnakesProto.Direction.UP
    }
}

fun dirToPoint(direction: SnakesProto.Direction): Point {
    return when (direction) {
        SnakesProto.Direction.UP -> Point(0, -1)
        SnakesProto.Direction.DOWN -> Point(0, 1)
        SnakesProto.Direction.LEFT -> Point(-1, 0)
        SnakesProto.Direction.RIGHT -> Point(1, 0)
    }
}

fun invertDir(direction: SnakesProto.Direction): SnakesProto.Direction {
    return when (direction) {
        SnakesProto.Direction.UP -> SnakesProto.Direction.DOWN
        SnakesProto.Direction.DOWN -> SnakesProto.Direction.UP
        SnakesProto.Direction.LEFT -> SnakesProto.Direction.RIGHT
        SnakesProto.Direction.RIGHT -> SnakesProto.Direction.LEFT
    }
}