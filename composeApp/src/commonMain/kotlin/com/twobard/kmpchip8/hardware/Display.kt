package com.twobard.kmpchip8.hardware

class Display {

    var matrix = Array(64) { BooleanArray(32) { false } }

    fun clear() {
        matrix = Array(64) { BooleanArray(32) { false } }
    }

    fun display(frameBuffer: FrameBuffer) {
        frameBuffer.buffer.forEachIndexed { xIndexed, column ->
            column.forEachIndexed { yIndex, value ->
                matrix[xIndexed][yIndex] = value
            }
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        matrix.forEachIndexed { xIndex, column ->
            column.forEachIndexed { yIndex, value ->
                sb.append("[$xIndex][$yIndex]=$value")
            }
        }
        return sb.toString()
    }
}