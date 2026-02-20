package com.twobard.kmpchip8.hardware

import com.twobard.kmpchip8.Utils

interface SystemInterface {
    fun clearDisplay()
    fun getFrameBuffer() : FrameBuffer
    fun getMemory() : Memory
    fun getDisplay() : Display
    fun getKeyboard() : KeyboardInterface
    fun getTimer() : Timer
}

class Cpu {

    val systemInterface: SystemInterface
    var randomNumberGenerator: Utils.RandomNumberGeneratorInterface

    constructor(
        systemInterface: SystemInterface,
        randomNumberGenerator: Utils.RandomNumberGeneratorInterface = Utils.RandomNumberGenerator()
    ) {
        this.systemInterface = systemInterface
        this.randomNumberGenerator = randomNumberGenerator
        this.stack = ArrayDeque<Int>(16)
        this.registers = IntArray(16)
    }

    //"a 64-byte stack with 8-bit stack pointer"
    //using an Int here to avoid autoboxing - better perfomance
    private val stack: ArrayDeque<Int>
    private var stackPointer = 0x0
    private var programCounter = Config.PROGRAM_COUNTER_INIT


    val registers: IntArray
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
                (combineNibbles(nibbles[2], nibbles[3])).let {
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
                se(nibbles[1], combineNibbles(nibbles[2], nibbles[3]))
            }
            0x4 -> {
                sne(nibbles[1], combineNibbles(nibbles[2], nibbles[3]))
            }
            0x5 -> {
                seVxVy(nibbles[1], nibbles[2])
            }
            0x6 -> {
                load(nibbles[1], combineNibbles(nibbles[2], nibbles[3]))
            }
            0x7 -> {
                add(nibbles[1], combineNibbles(nibbles[2], nibbles[3]))
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
            0xB -> {
                bnnn(nibbles[1], nibbles[2], nibbles[3])
            }
            0xC -> {
                cxkk(nibbles[1], nibbles[2], nibbles[3])
            }
            0xD  -> {
                dxyn(nibbles[1], nibbles[2], nibbles[3])
            }
            0xE  -> {
                when (nibbles[3].value) {
                    0xE -> {
                        skp(nibbles[1])
                    }

                    0x1 -> {
                        sknp(nibbles[1])
                    }
                }
            }
            0xF -> {
                when (nibbles[2].value) {
                    0x1 -> {
                        when(nibbles[3].value){
                            0x5 -> lddt(nibbles[1])
                            0x8 -> ldst(nibbles[1])
                            0xE -> addI(nibbles[1])
                        }
                    }

                }
            }
        }
    }

    //Set I = I + Vx. The values of I and Vx are added, and the results are stored in I.
    fun addI(x: Nibble){
        indexRegister += registers[x.value]
    }

    //Set delay timer = Vx. Delay Timer is set equal to the value of Vx.
    fun lddt(x: Nibble){
        systemInterface.getTimer().setDelayTimer(registers[x.value])
    }

    //Set sound timer = Vx. Sound Timer is set equal to the value of Vx.
    fun ldst(x: Nibble){
        systemInterface.getTimer().setSoundTimer(registers[x.value])
    }

    fun sknp(n1: Nibble) {
        systemInterface.getKeyboard().getKeyAt(n1.value).let {
            if(!it) {
                incrementProgramCounter()
            }
        }
    }

    fun skp(n1: Nibble) {
        systemInterface.getKeyboard().getKeyAt(n1.value).let {
            if(it) {
                incrementProgramCounter()
            }
        }
    }

    //Display n-byte sprite starting at memory location I at (Vx, Vy), set VF = collision. The interpreter reads n
    //bytes from memory, starting at the address stored in I. These bytes are then displayed as sprites on screen
    //at coordinates (Vx, Vy). Sprites are XOR’d onto the existing screen. If this causes any pixels to be erased,
    //VF is set to 1, otherwise it is set to 0. If the sprite is positioned so part of it is outside the coordinates of
    //the display, it wraps around to the opposite side of the screen.
    fun dxyn(n1: Nibble, n2: Nibble, n3: Nibble){
        val displayWidth = systemInterface.getFrameBuffer().width
        val displayHeight = systemInterface.getFrameBuffer().height
        val xStart = registers[n1.value] % displayWidth
        val yStart = registers[n2.value] % displayHeight
        val height = n3.value
        registers[0xF] = 0  // Reset collision flag

        fun getSpriteAt(x: Int, y: Int) : Boolean {
            return systemInterface.getFrameBuffer().buffer[x][y]
        }

        fun setSpriteAt(item: Boolean, x: Int, y: Int) {
            println("Setting sprite at [$x][$y] to $item")
            systemInterface.getFrameBuffer().buffer[x][y] = item
        }

        for (row in 0 until height) {

            val spriteByte = systemInterface.getMemory()[indexRegister + row].toInt() and 0xFF

            for (bit in 0 until 8) {

                val spritePixel = (spriteByte shr (7 - bit)) and 1
                if (spritePixel == 0) continue

                val x = (xStart + bit) % displayWidth
                val y = (yStart + row) % displayHeight

                val oldPixel = getSpriteAt(x, y)
                val newPixel = oldPixel xor true

                if (oldPixel && !newPixel) {
                    registers[0xF] = 1
                }

                setSpriteAt(newPixel, x, y)
            }
        }

        systemInterface.getDisplay().display(systemInterface.getFrameBuffer())
    }

    //Set Vx = random byte AND kk. The interpreter generates a random number from 0 to 255, which is then
    //ANDed with the value kk. The results are stored in Vx. See instruction 8xy2 for more information on AND.
    fun cxkk(n1: Nibble, n2: Nibble, n3: Nibble){
        registers[n1.value] = randomNumberGenerator.getRandom() and combineNibbles(n2, n3)
    }

    //Jump to location nnn + V0. The program counter is set to nnn plus the value of V0.
    fun bnnn(n1: Nibble, n2: Nibble, n3: Nibble){
        programCounter = (combineNibbles(n1, n2, n3)) + registers[0]
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
    fun add(x: Nibble, kk: Int) {
        registers[x.value] = registers[x.value] + kk
    }

    //Jump to location nnn. The interpreter sets the program counter to nnn.
    fun jump(address1: Nibble, address2: Nibble, address3: Nibble){
        //Jump to location nnn. The interpreter sets the program counter to nnn.
        val address = address1.value + address2.value + address3.value
        programCounter = address
    }

    //Set Vx = kk. The interpreter puts the value kk into register Vx.
    fun load(dest: Nibble, value: Int){
        registers[dest.value] = value
    }

    fun seVxVy(vX: Nibble, vY: Nibble){
       se(vX, registers[vY.value])
    }

    fun se(dest: Nibble, value: Int){
        if(registers[dest.value] == value){
            incrementProgramCounter()
        }
    }

    fun sne(dest: Nibble, value: Int){
        if(registers[dest.value] != value){
            incrementProgramCounter()
        }
    }

    fun setIndexRegister(newValue: Int) {
        indexRegister = newValue
    }

    fun incrementProgramCounter() {
        programCounter += 2
    }

    fun setRegisterData(index: Int, kk: Pair<Nibble, Nibble>) {
        registers[index] = combineNibbles(kk.first, kk.second)
    }


}