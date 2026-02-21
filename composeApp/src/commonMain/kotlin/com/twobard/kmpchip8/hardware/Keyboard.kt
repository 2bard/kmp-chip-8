package com.twobard.kmpchip8.hardware

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type


interface KeyboardInterface {
    fun getKeyAt(pos: Int) : Boolean
    fun getPressedKey() : Int?
    fun pressKeyAt(pos: Int)
}

class Keyboard : KeyboardInterface{
    val keys = BooleanArray(16) { false }
    var pressed: Int? = null

    override fun getKeyAt(pos: Int) : Boolean{
        require(pos < keys.size)
        return keys[pos]
    }

    override fun getPressedKey() : Int ? {
       return pressed
    }

    override fun pressKeyAt(pos: Int) {
        if(this.pressed != null){
            println("Key pressed: null")
            this.pressed = null
        } else {
            println("Key pressed at: $pos")
            this.pressed = pos
        }
    }
}