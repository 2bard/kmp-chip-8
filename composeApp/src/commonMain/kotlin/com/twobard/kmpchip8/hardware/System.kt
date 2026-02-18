package com.twobard.kmpchip8.hardware

import androidx.compose.ui.text.font.Font

class System(val memory: Memory = Memory(), val font: Config.Chip8Font = Config.DEFAULT_FONT) {

    init {
        loadFont(memory, font)
    }

    fun loadFont(memory: Memory, font: Config.Chip8Font){
        require(font.sprites.size == 80)
        font.sprites.forEachIndexed{ index, sprite ->
            memory[index] = sprite.toByte()
        }
    }
}