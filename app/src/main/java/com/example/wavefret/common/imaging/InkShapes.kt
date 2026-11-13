package com.example.wavefret.common.imaging

private val NEIGHBOURS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, -1 to -1, 1 to -1, -1 to 1)

/**
 * Finds the separate ink shapes in part of an image (pixels touching,
 * diagonals included, belong to the same shape).
 *
 * @param region Part of the image to search; shapes are cut at its edges. Type: PixelRegion
 * @return Bounding box of each shape. Type: List<PixelRegion>
 */
fun BinaryImage.connectedInkShapes(region: PixelRegion): List<PixelRegion> {
    val regionWidth = region.width
    val visited = BooleanArray(regionWidth * region.height)
    val shapes = mutableListOf<PixelRegion>()
    val stack = ArrayDeque<Int>()
    for (startY in region.top..region.bottom) for (startX in region.left..region.right) {
        val startIndex = (startY - region.top) * regionWidth + (startX - region.left)
        if (visited[startIndex] || !isInk(startX, startY)) continue
        var left = startX
        var top = startY
        var right = startX
        var bottom = startY
        visited[startIndex] = true
        stack.addLast(startIndex)
        while (stack.isNotEmpty()) {
            val index = stack.removeLast()
            val x = index % regionWidth + region.left
            val y = index / regionWidth + region.top
            left = minOf(left, x)
            top = minOf(top, y)
            right = maxOf(right, x)
            bottom = maxOf(bottom, y)
            for ((dx, dy) in NEIGHBOURS) {
                val nextX = x + dx
                val nextY = y + dy
                if (nextX !in region.left..region.right || nextY !in region.top..region.bottom) continue
                val nextIndex = (nextY - region.top) * regionWidth + (nextX - region.left)
                if (!visited[nextIndex] && isInk(nextX, nextY)) {
                    visited[nextIndex] = true
                    stack.addLast(nextIndex)
                }
            }
        }
        shapes += PixelRegion(left, top, right, bottom)
    }
    return shapes
}
