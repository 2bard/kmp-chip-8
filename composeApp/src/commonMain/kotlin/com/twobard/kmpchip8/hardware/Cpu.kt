package com.twobard.kmpchip8.hardware

import com.twobard.kmpchip8.Utils.Companion.toUnsignedInt

interface SystemInterface {
    fun clearDisplay()
}

class Cpu(val systemInterface: SystemInterface) {

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
                (nibbles[2] + nibbles[3]).toUnsignedInt().let {
                    when(it) {
                        0xE0 -> systemInterface.clearDisplay()
                        0xEE -> ret()
                    }
                }
            }
            0x1 -> {
                jump(nibbles[1], nibbles[2], nibbles[3])
            }
            0x6 -> load(nibbles[1], nibbles[2] + nibbles[3])
        }
    }

    fun jump(address1: Nibble, address2: Nibble, address3: Nibble){
        val address = address1.value + address2.value + address3.value
        programCounter = address
    }


    fun load(dest: Nibble, value: Byte){
        registers[dest.value] = value.toUnsignedInt()
    }

    fun incrementProgramCounter() {
        programCounter += 2
    }


}