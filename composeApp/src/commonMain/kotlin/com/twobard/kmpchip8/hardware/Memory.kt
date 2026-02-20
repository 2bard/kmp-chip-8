package com.twobard.kmpchip8.hardware

class Memory(val ramSize: Int = DEFAULT_RAM_SIZE) {

    companion object {
        val DEFAULT_RAM_SIZE = 4096
    }

    private val ram = IntArray(ramSize)

    fun fill(newVal: Int){
        ram.fill(newVal)
    }

    operator fun set(address: Int, value: Int) {
        require(address in 0 until ramSize)
        ram[address] = value
    }

    operator fun get(address: Int): Int {
        require(address in 0 until ramSize)
        return ram[address]
    }

    fun getAll() = ram.map {
        it
    }

    override fun toString(): String {
        val sb = StringBuilder()
        getAll().forEachIndexed { index, value ->
            sb.append("Val at $index is $value \n")
        }

        return sb.toString()
    }

    fun addRom(rom: ByteArray) {
        var currentAddress = Config.PROGRAM_COUNTER_INIT
        rom.forEach {
            set(currentAddress, it.toInt())
            currentAddress++
        }
    }

}