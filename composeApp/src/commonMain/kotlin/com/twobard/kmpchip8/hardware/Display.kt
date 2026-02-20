package com.twobard.kmpchip8.hardware

import kotlin.collections.get


class Display {

    var matrix = Array(64) { BooleanArray(32) { false } }

    fun clear() {
        matrix = Array(64) { BooleanArray(32) { false } }
    }

    fun copy() : Array<BooleanArray> {
        return Array(matrix.size) { x -> matrix[x].copyOf() }
    }

    fun pixels() : Int {
        var sb = 0
        matrix.forEachIndexed { xIndex, column ->
            column.forEachIndexed { yIndex, value ->
                if(value){
                    sb++
                }
            }
        }
        return sb
    }

    fun display(frameBuffer: FrameBuffer) {
        //println("Display updating.")
        var i = 0
        frameBuffer.buffer.forEachIndexed { xIndexed, column ->
            column.forEachIndexed { yIndex, value ->
                matrix[xIndexed][yIndex] = value
                if(value == true){
                    i++
                }
            }
        }
        println("Display updating. Active pixels: " + i)
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