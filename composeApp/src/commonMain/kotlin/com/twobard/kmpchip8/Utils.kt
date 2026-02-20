package com.twobard.kmpchip8


import com.twobard.kmpchip8.hardware.Nibble
import kotlin.random.Random

class Utils {

    companion object {


        fun Int.toNibbles() : Pair<Nibble, Nibble> {
            val unsigned = this and 0xFF
            val high = (unsigned shr 4) and 0x0F
            val low = unsigned and 0x0F
            return Nibble(high) to Nibble(low)
        }



    }

    interface RandomNumberGeneratorInterface {
        fun getRandom(): Int
    }

    class RandomNumberGenerator() : RandomNumberGeneratorInterface {
        override fun getRandom() = Random.nextInt(255)
    }
}

//fun Pair<Nibble, Nibble>.asByte(): Byte {
//    return ((first.value shl 4) or (second.value and 0xF)).toByte()
//}

//operator fun Int.plus(other: Nibble): Int {
//    return Pair(Nibble(this), other).asByte().toUnsignedInt()
//}