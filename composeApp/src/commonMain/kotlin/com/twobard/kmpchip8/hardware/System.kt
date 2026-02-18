package com.twobard.kmpchip8.hardware

import androidx.compose.ui.text.font.Font
import com.twobard.kmpchip8.Utils.Companion.toUnsignedInt

class System(val memory: Memory = Memory(),
             val font: Config.Chip8Font = Config.DEFAULT_FONT,
             val cpu: Cpu = Cpu()) {

    init {
        loadFont(memory, font)
    }

    fun loadFont(memory: Memory, font: Config.Chip8Font){
        require(font.sprites.size == 80)
        font.sprites.forEachIndexed{ index, sprite ->
            memory[index] = sprite.toByte()
        }
    }

    fun fetchOpcode(): OpCode {
        val high = memory[cpu.getProgramCounter()]
        val low  = memory[cpu.getProgramCounter() + 1]
        return OpCode(high, low)
    }

    data class OpCode(val high: Byte, val low: Byte)
}