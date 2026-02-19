package com.twobard.kmpchip8.hardware

class FrameBuffer(val width: Int = 64, val height: Int = 32) {
    var buffer = Array(width) { BooleanArray(height) { false } }

    override fun toString(): String {
        val sb = StringBuilder()
        buffer.forEachIndexed { xIndex, column ->
            column.forEachIndexed { yIndex, value ->
                sb.append("[$xIndex][$yIndex]=$value")
            }
        }
        return sb.toString()
    }
}