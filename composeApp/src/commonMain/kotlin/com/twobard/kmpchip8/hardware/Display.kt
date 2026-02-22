package com.twobard.kmpchip8.hardware

import kotlin.collections.get


class Display {

    var matrix = Array(64) { BooleanArray(32) { false } }

    fun clear() {
        matrix = Array(64) { BooleanArray(32) { false } }
    }

    fun copy() : Array<BooleanArray> {
        val result = Array(matrix.size) { x -> matrix[x].copyOf() }
        requiresDraw = false
        return result
    }

    private var requiresDraw = false

    fun requiresDraw() = requiresDraw

    fun display(frameBuffer: FrameBuffer) {
        frameBuffer.getFrameBuffer().forEachIndexed { xIndexed, column ->
            column.forEachIndexed { yIndex, value ->
                val current = matrix[xIndexed][yIndex]
                if(current != value){
                    matrix[xIndexed][yIndex] = value
                    requiresDraw = true
                }
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