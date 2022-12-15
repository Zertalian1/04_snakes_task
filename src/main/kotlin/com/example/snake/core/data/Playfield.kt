package com.example.snake.core.data

data class Playfield(
    val width: Int,
    val height: Int
) {
    val size: Point = Point(width, height)
    /** Returning point to circle if it is out of bounds, but instead of normalizeDirty(), does not modify a point
     * On call creates new one
     * @param point target point
     * @return new point that bounds field size
     */

    fun normalize(point: Point): Point {
        var x = point.x % width
        var y = point.y % height
        if (x < 0) x += width
        if (y < 0) y += height

        return Point(x, y);
    }

    /** Returning point to circle if it is out of bounds, but instead of normalize(), does not recreate a point
     * @param point point ti modify
     */
    fun normalizeDirty(point: Point) {
        point.x = point.x % width
        point.y = point.y % height

        if (point.x < 0) point.x += width
        if (point.y < 0) point.y += height
    }

    /**
     * Checks if point bounds playfield
     * @param point target point
     */
    fun bounds(point: Point): Boolean {
        return (point.x in 0 until width) && (point.y in 0 until height);
    }
}
