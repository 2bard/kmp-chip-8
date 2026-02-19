package com.twobard.kmpchip8.hardware

import com.twobard.kmpchip8.Utils.Companion.asByte
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
    private var indexRegister = 0

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
    fun getIndexRegister() = indexRegister

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
            0x2 -> {
                call(nibbles[1].value + nibbles[2].value + nibbles[3].value)
            }
            0x3 -> {
                se(nibbles[1], nibbles[2] + nibbles[3])
            }
            0x4 -> {
                sne(nibbles[1], nibbles[2] + nibbles[3])
            }
            0x5 -> {
                seVxVy(nibbles[1], nibbles[2])
            }
            0x6 -> {
                load(nibbles[1], nibbles[2] + nibbles[3])
            }
            0x7 -> {
                add(nibbles[1], nibbles[2] + nibbles[3])
            }
            0x8 -> {
                when(nibbles[3].value) {
                    0x0 -> {
                        ldVxVy(nibbles[1], nibbles[2])
                    }
                    0x1 -> {
                        orVxVy(nibbles[1], nibbles[2])
                    }
                    0x2 -> {
                        andVxVy(nibbles[1], nibbles[2])
                    }
                    0x3 -> {
                        xorVxVy(nibbles[1], nibbles[2])
                    }
                    0x4 -> {
                        addVxVy(nibbles[1], nibbles[2])
                    }
                    0x5 -> {
                        subVxVy(nibbles[1], nibbles[2])
                    }
                    0x6 -> {
                        shrVxVy(nibbles[1], nibbles[2])
                    }
                    0x7 -> {
                        subnVxVy(nibbles[1], nibbles[2])
                    }
                    0x8 -> {
                        shlVxVy(nibbles[1], nibbles[2])
                    }
                }
            }
            0x9 -> {
                sneVxVy(nibbles[1], nibbles[2])
            }
            0xA -> {
                annn(nibbles[1], nibbles[2], nibbles[3])
            }
        }
    }

    //Set I = nnn. The value of register I is set to nnn.
    fun annn(n1: Nibble, n2: Nibble, n3: Nibble){
        indexRegister = n1.value + n2.value + n3.value
    }

    //Skip next instruction if Vx != Vy. The values of Vx and Vy are compared, and if they are not equal, the
    //program counter is increased by 2.
    fun sneVxVy(x: Nibble, y: Nibble){
        if(registers[x.value] != registers[y.value]) {
            incrementProgramCounter()
        }
    }

    //Set Vx = Vx SHL 1. If the most-significant bit of Vx is 1, then VF is set to 1, otherwise to 0. Then Vx is
    //multiplied by 2.
    fun shlVxVy(x: Nibble, y: Nibble){
        val vx = registers[x.value]
        registers[0xF] = (vx shr 7) and 0x1
        registers[x.value] = (vx shl 1) and 0xFF
    }

    //Set Vx = Vy - Vx, set VF = NOT borrow. If Vy ¿ Vx, then VF is set to 1, otherwise 0. Then Vx is
    //subtracted from Vy, and the results stored in Vx.
    fun subnVxVy(x: Nibble, y: Nibble){
        val vx = registers[x.value]
        val vy = registers[y.value]
        registers[0xF] = if (vy >= vx) 1 else 0
        registers[x.value] = (vy - vx) and 0xFF
    }

    //Set Vx = Vx SHR 1. If the least-significant bit of Vx is 1, then VF is set to 1, otherwise 0. Then Vx is
    //divided by 2.
    fun shrVxVy(x: Nibble, y: Nibble){
        val vx = registers[x.value]
        registers[0xF] = vx and 1
        registers[x.value] = vx shr 1
    }

    //Set Vx = Vx - Vy, set VF = NOT borrow. If Vx ¿ Vy, then VF is set to 1, otherwise 0. Then Vy is
    //subtracted from Vx, and the results stored in Vx.
    fun subVxVy(x: Nibble, y: Nibble){
        val vx = registers[x.value]
        val vy = registers[y.value]

        //Set VF = 1 if Vx >= Vy (no borrow), else 0
        registers[0xF] = if (vx >= vy) 1 else 0

        //use mod to ensure value is always positive
        registers[x.value] = (vx - vy + 256) % 256
    }

    //Set Vx = Vx + Vy, set VF = carry. The values of Vx and Vy are added together. If the result is greater
    //than 8 bits (i.e., ¿ 255,) VF is set to 1, otherwise 0. Only the lowest 8 bits of the result are kept, and stored
    //in Vx.
    fun addVxVy(x: Nibble, y: Nibble){
        val newValue = registers[x.value] + registers[y.value]
        registers[x.value] = newValue.toUByte().toInt()
        registers[0xF] = if(newValue > 255) 1 else 0
    }

    //Set Vx = Vx XOR Vy. Performs a bitwise exclusive OR on the values of Vx and Vy, then stores the result
    //in Vx. An exclusive OR compares the corresponding bits from two values, and if the bits are not both the
    //same, then the corresponding bit in the result is set to 1. Otherwise, it is 0.
    fun xorVxVy(x: Nibble, y: Nibble){
        registers[x.value] = registers[x.value] xor registers[y.value]
    }

    //Set Vx = Vx AND Vy. Performs a bitwise AND on the values of Vx and Vy, then stores the result in Vx.
    //A bitwise AND compares the corresponding bits from two values, and if both bits are 1, then the same bit
    //in the result is also 1. Otherwise, it is 0.
    fun andVxVy(x: Nibble, y: Nibble){
        registers[x.value] = registers[x.value] and registers[y.value]
    }

    //Set Vx = Vx OR Vy. Performs a bitwise OR on the values of Vx and Vy, then stores the result in Vx. A
    //bitwise OR compares the corresponding bits from two values, and if either bit is 1, then the same bit in the
    //result is also 1. Otherwise, it is 0.
    fun orVxVy(x: Nibble, y: Nibble){
        registers[x.value] = registers[x.value] or registers[y.value]
    }

    //Set Vx = Vy. Stores the value of register Vy in register Vx.
    fun ldVxVy(x: Nibble, y: Nibble){
        registers[x.value] = registers[y.value]
    }

    //Set Vx = Vx + kk. Adds the value kk to the value of register Vx, then stores the result in Vx.
    fun add(x: Nibble, kk: Byte) {
        registers[x.value] = registers[x.value] + kk
    }

    //Jump to location nnn. The interpreter sets the program counter to nnn.
    fun jump(address1: Nibble, address2: Nibble, address3: Nibble){
        //Jump to location nnn. The interpreter sets the program counter to nnn.
        val address = address1.value + address2.value + address3.value
        programCounter = address
    }

    //Set Vx = kk. The interpreter puts the value kk into register Vx.
    fun load(dest: Nibble, value: Byte){
        registers[dest.value] = value.toUnsignedInt()
    }

    fun seVxVy(vX: Nibble, vY: Nibble){
       se(vX, registers[vY.value].toByte())
    }

    fun se(dest: Nibble, value: Byte){
        if(registers[dest.value] == value.toUnsignedInt()){
            incrementProgramCounter()
        }
    }

    fun sne(dest: Nibble, value: Byte){
        if(registers[dest.value] != value.toUnsignedInt()){
            incrementProgramCounter()
        }
    }

    fun incrementProgramCounter() {
        programCounter += 2
    }

    fun setRegisterData(index: Int, kk: Pair<Nibble, Nibble>) {
        registers[index] = kk.asByte().toUnsignedInt()
    }


}