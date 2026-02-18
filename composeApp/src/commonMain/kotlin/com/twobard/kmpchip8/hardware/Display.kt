package com.twobard.kmpchip8.hardware

class Display {

    var matrix = Array(64) { BooleanArray(32) { false } }

    fun clear() {
        matrix = Array(64) { BooleanArray(32) { false } }
    }
}