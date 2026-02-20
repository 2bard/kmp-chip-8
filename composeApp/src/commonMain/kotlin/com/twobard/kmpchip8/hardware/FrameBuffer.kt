package com.twobard.kmpchip8.hardware

class FrameBuffer(val width: Int = 64, val height: Int = 32) {
    private var buffer = Array(width) { BooleanArray(height) { false } }


    fun getFrameBuffer() = buffer

    override fun toString(): String {
        val sb = StringBuilder()
        buffer.forEachIndexed { xIndex, column ->
            column.forEachIndexed { yIndex, value ->
                sb.append("[$xIndex][$yIndex]=$value")
            }
        }
        return sb.toString()
    }

    fun setSpriteAt(x: Int, y: Int, item: Boolean) {
        buffer[x][y] = item
    }

    fun getSpriteAt(x: Int, y: Int): Boolean {
        return buffer[x][y]
    }
}