package com.twobard.kmpchip8.hardware

import com.twobard.kmpchip8.Utils.Companion.toUnsignedInt

class Memory(val ramSize: Int = DEFAULT_RAM_SIZE) {

    companion object {
        val DEFAULT_RAM_SIZE = 4096
    }

    private val ram = ByteArray(ramSize)

    fun fill(newVal: Byte){
        ram.fill(newVal)
    }

    operator fun set(address: Int, value: Byte) {
        require(address in 0 until ramSize)
        ram[address] = value
    }

    operator fun get(address: Int): Byte {
        require(address in 0 until ramSize)
        return ram[address]
    }

    fun getAll() = ram.map {
        it.toUnsignedInt()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        getAll().forEachIndexed { index, value ->
            sb.append("Val at $index is $value \n")
        }

        return sb.toString()
    }

}