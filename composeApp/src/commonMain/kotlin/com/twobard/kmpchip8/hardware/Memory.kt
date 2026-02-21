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

    fun addRom(rom: IntArray) {
        val startAddress = Config.PROGRAM_COUNTER_INIT
        require(startAddress in 0 until ramSize) {
            "Invalid ROM startAddress=$startAddress for ramSize=$ramSize"
        }
        require(rom.size <= ramSize - startAddress) {
            "ROM too large: size=${rom.size}, capacity=${ramSize - startAddress} (start=$startAddress)"
        }
        rom.forEachIndexed { index, value ->
            require(value in 0..0xFF) { "ROM byte at index=$index out of range: $value" }
            set(startAddress + index, value)
        }
    }

}