package com.twobard.kmpchip8.hardware


interface KeyboardInterface {
    fun getKeyAt(pos: Int) : Boolean
}

class Keyboard : KeyboardInterface{
    val keys = BooleanArray(16) { false }

    override fun getKeyAt(pos: Int) : Boolean{
        require(pos < keys.size)
        return keys[pos]
    }
}