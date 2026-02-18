package com.twobard.kmpchip8

import kotlin.experimental.and

class Utils {

    companion object {
        fun Byte.toUnsignedInt() : Int {
            return this.toInt() and 0xFF
        }
    }
}