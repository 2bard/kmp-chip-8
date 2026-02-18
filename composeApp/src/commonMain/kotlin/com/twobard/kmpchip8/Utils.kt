package com.twobard.kmpchip8

import com.twobard.kmpchip8.hardware.Nibble
import kotlin.experimental.and

class Utils {

    companion object {
        fun Byte.toUnsignedInt() : Int {
            return this.toInt() and 0xFF
        }

        fun Byte.toNibbles() : Pair<Nibble,Nibble> {
            val unsigned = this.toUnsignedInt()

            val high = (unsigned shr 4) and 0x0F
            val low  = unsigned and 0x0F

            return Nibble(high) to Nibble(low)
        }

        fun Pair<Nibble, Nibble>.asByte(): Byte {
            return ((first.value shl 4) or (second.value and 0xF)).toByte()
        }
    }
}