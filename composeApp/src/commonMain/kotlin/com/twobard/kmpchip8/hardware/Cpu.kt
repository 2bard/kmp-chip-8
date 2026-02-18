package com.twobard.kmpchip8.hardware

import com.twobard.kmpchip8.Utils.Companion.toUnsignedInt

class Cpu {

    //"a 64-byte stack with 8-bit stack pointer"
    //using an Int here to avoid autoboxing - better perfomance
    private val stack = ArrayDeque<Int>(16)
    private var stackPointer = 0x0
    private var programCounter = Config.PROGRAM_COUNTER_INIT

    val registers = IntArray(16)

    //Stack
    fun call(address: Int){
        stack.addFirst(programCounter)
        stackPointer++
        programCounter = address
    }

    fun ret(){
        val lastAddress = stack.removeLast()
        stackPointer--
        programCounter = lastAddress
    }

    fun getStackPointer() = stackPointer
    fun getProgramCounter() = programCounter
    fun getFromStack(pos: Int) = stack[pos]
    //End stack

    fun execute(opcode: System.OpCode) {
        val nibbles = opcode.toNibbles()

        when(nibbles[0].value) {
            0x0 -> {
                if(nibbles[2] + nibbles[3] == 0xE0.toByte()){
                    clearDisplay()
                }
            }
            0x6 -> load(nibbles[1], nibbles[2] + nibbles[3])
        }
    }

    fun clearDisplay(){
        println("clearing display")
    }

    fun load(dest: System.Nibble, value: Byte){
        registers[dest.value] = value.toUnsignedInt()
    }

    fun incrementProgramCounter() {
        programCounter += 2
    }


}