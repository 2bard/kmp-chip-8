package com.twobard.kmpchip8.hardware

import androidx.compose.ui.text.font.Font

class System(memory: Memory, font: Font) {

    init {

    }

    fun loadFont(memory: Memory, font: Config.Chip8Font){
        require(font.sprites.size == 80)
        font.sprites.forEachIndexed{ index, sprite ->
            memory[index] = sprite.toByte()
        }
    }
}