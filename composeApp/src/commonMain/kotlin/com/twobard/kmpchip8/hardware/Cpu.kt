package com.twobard.kmpchip8.hardware

import com.twobard.kmpchip8.CustomHexFormat
import com.twobard.kmpchip8.Utils
import kotlinx.coroutines.delay

interface SystemInterface {
    fun clearDisplay()
    fun getFrameBuffer() : FrameBuffer
    fun getMemory() : Memory
    fun getDisplay() : Display
    fun getKeyboard() : KeyboardInterface
    fun getTimer() : Timer
}

class Cpu {

    val enableLogging = true
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
    //private var stackPointer = 0x0
    private var programCounter = Config.PROGRAM_COUNTER_INIT

    private val registers: IntArray
    private var indexRegister = 0
    val strictMode = false

    fun setRegisterValue(index: Int, value: Int){
        require(value in 0..255)
        require(index in 0..15)
        log("Setting register value at index $index to $value")
        this.registers[index] = value
    }

    fun getRegisterValue(index: Int) : Int{
        require(index in 0..15)
        return this.registers[index]
    }

    //Stack
    fun call(address: Int){
        if(strictMode){
            require(address % 2 == 0)
        }
        println("subroutine calling: " + address)
        // stackPointer is treated as current stack depth
        require(stack.size < 16) { "Stack overflow" }
        println("subroutine adding. new address=$programCounter")
        stack.addFirst(programCounter)
        //stackPointer++
        setProgramCounter(address)
        ensureValidState()
    }

    fun ret(){
        println("subroutine returning")
        require(stack.isNotEmpty()) { "Stack underflow" }
        val lastAddress = stack.removeFirst()
        println("subroutine returning. last address=$lastAddress")

       // stackPointer--
        setProgramCounter(lastAddress)
        ensureValidState()
    }

    fun ensureValidState(){

        if(strictMode){
            // Stack


            // Registers must always remain 8-bit
            require(registers.size == 16)
            require(registers.all { it in 0..0xFF }) { "Register out of 8-bit range" }

            // Address space safety
            val memSize = systemInterface.getMemory().ramSize
            require(indexRegister in 0 until memSize) { "Invalid I=$indexRegister" }

            // PC should always point to a valid 2-byte opcode fetch
            // Traditional program space starts at 0x200 in this project.
            require(programCounter in 0x200 until memSize) { "Invalid PC=$programCounter" }
            require(programCounter <= memSize - 2) { "PC=$programCounter cannot fetch 2 bytes" }
            require(programCounter % 2 == 0) { "PC must be even, was $programCounter" }

            // Return addresses on the stack should follow the same constraint
            stack.forEach {
                require(it in 0x200 until memSize) { "Invalid return address on stack: $it" }
                require(it <= memSize - 2) { "Return address cannot fetch 2 bytes: $it" }
                require(it % 2 == 0) { "Return address must be even: $it" }
            }

            // Timers are 8-bit in CHIP-8 and must never be negative
            val delay = systemInterface.getTimer().getDelayTimer()
            val sound = systemInterface.getTimer().getSoundTimer()
            require(delay in 0..0xFF) { "Invalid delay timer=$delay" }
            require(sound in 0..0xFF) { "Invalid sound timer=$sound" }
        }

    }

    fun getStackPointer() = stack.size
    fun getProgramCounter() = programCounter
    fun getFromStack(pos: Int) = stack[pos]
    fun getIndexRegister() = indexRegister

    fun log(str: String){
        if(enableLogging){
            println(str)
        }
    }
    //End stack

    var currentCounter: String? = null

    suspend fun execute(opcode: System.OpCode, currentCounter: String?) {
        val nibbles = opcode.toNibbles()
        var didExecute = false
        this.currentCounter = currentCounter
        when(nibbles[0].value) {
            0x0 -> {
                nibbles[3].value.let {
                    when(it) {
                        0x0 -> {
                            systemInterface.clearDisplay()
                            didExecute = true
                        }
                        0xE -> {
                            ret()
                            didExecute = true
                        }
                    }
                }
            }
            0x1 -> {
                jump(nibbles[1], nibbles[2], nibbles[3])
                didExecute = true
            }
            0x2 -> {
                call(combineNibbles(nibbles[1], nibbles[2], nibbles[3]))
                didExecute = true
            }
            0x3 -> {
                se(nibbles[1], combineNibbles(nibbles[2], nibbles[3]))
                didExecute = true
            }
            0x4 -> {
                sne(nibbles[1], combineNibbles(nibbles[2], nibbles[3]))
                didExecute = true
            }
            0x5 -> {
                seVxVy(nibbles[1], nibbles[2])
                didExecute = true
            }
            0x6 -> {
                load(nibbles[1], combineNibbles(nibbles[2], nibbles[3]))
                didExecute = true
            }
            0x7 -> {
                add(nibbles[1], combineNibbles(nibbles[2], nibbles[3]))
                didExecute = true
            }
            0x8 -> {
                when(nibbles[3].value) {
                    0x0 -> {
                        ldVxVy(nibbles[1], nibbles[2])
                        didExecute = true
                    }
                    0x1 -> {
                        orVxVy(nibbles[1], nibbles[2])
                        didExecute = true
                    }
                    0x2 -> {
                        andVxVy(nibbles[1], nibbles[2])
                        didExecute = true
                    }
                    0x3 -> {
                        xorVxVy(nibbles[1], nibbles[2])
                        didExecute = true
                    }
                    0x4 -> {
                        addVxVy(nibbles[1], nibbles[2])
                        didExecute = true
                    }
                    0x5 -> {
                        subVxVy(nibbles[1], nibbles[2])
                        didExecute = true
                    }
                    0x6 -> {
                        shrVxVy(nibbles[1], nibbles[2])
                        didExecute = true
                    }
                    0x7 -> {
                        subnVxVy(nibbles[1], nibbles[2])
                        didExecute = true
                    }
                    0xE -> {
                        shlVxVy(nibbles[1], nibbles[2])
                        didExecute = true
                    }
                }
            }
            0x9 -> {
                sneVxVy(nibbles[1], nibbles[2])
                didExecute = true
            }
            0xA -> {
                annn(nibbles[1], nibbles[2], nibbles[3])
                didExecute = true
            }
            0xB -> {
                bnnn(nibbles[1], nibbles[2], nibbles[3])
                didExecute = true
            }
            0xC -> {
                cxkk(nibbles[1], nibbles[2], nibbles[3])
                didExecute = true
            }
            0xD  -> {
                dxyn(nibbles[1], nibbles[2], nibbles[3])
                didExecute = true
            }
            0xE  -> {
                when (nibbles[3].value) {
                    0xE -> {
                        skp(nibbles[1])
                        didExecute = true
                    }

                    0x1 -> {
                        sknp(nibbles[1])
                        didExecute = true
                    }
                }
            }
            0xF -> {
                when (nibbles[2].value) {
                    0x0 -> {
                        when(nibbles[3].value){
                            0x7 -> {
                                stdt(nibbles[2])
                                didExecute = true
                            }
                            0xA -> {
                                wait4Keypress(nibbles[2])
                                didExecute = true
                            }
                        }
                    }
                    0x1 -> {
                        when(nibbles[3].value){
                            0x5 -> {
                                lddt(nibbles[1])
                                didExecute = true
                            }
                            0x8 -> {
                                ldst(nibbles[1])
                                didExecute = true
                            }
                            0xE -> {
                                addI(nibbles[1])
                                didExecute = true
                            }
                        }
                    }
                    0x2 -> {
                        ldf(nibbles[1])
                        didExecute = true
                    }
                    0x3 -> {
                        ldb(nibbles[1])
                        didExecute = true
                    }
                    0x5 -> {
                        ldivx(nibbles[1])
                        didExecute = true
                    }
                    0x6 -> {
                        ldvxi(nibbles[1])
                        didExecute = true
                    }
                    0xf -> {
                        throw IllegalStateException("Sdfsdfsdfsdf")
                    }
                }
            }
        }

        if(didExecute == false){
            throw IllegalStateException("Invalid opcode:" + opcode.toString())
        }
        ensureValidState()
    }

    suspend fun wait4Keypress(x: Nibble){
        while(systemInterface.getKeyboard().getPressedKey() == null){
            println("Waiting for key press")
            delay(10)
        }

        systemInterface.getKeyboard().getPressedKey()?.let {
            println("Waiting over")
            setRegisterValue(x.value, it)
        }
    }

    //Fills V0 to VX with values from memory starting at address I. I is then set to I + x + 1.
    fun ldvxi(x: Nibble){
        log("Opcode->ldxvi")
        var currentIndexRegister = indexRegister
        for (i in 0..x.value) {
            setRegisterValue(i, unsigned(systemInterface.getMemory().get(currentIndexRegister)))
            currentIndexRegister++
        }

        setIndexRegister(indexRegister + x.value + 1)
    }

    //Stores V0 to VX in memory starting at address I. I is then set to I + x + 1.
    fun ldivx(x: Nibble){
        log("Opcode->ldivx")

        for (i in 0..x.value) {
            systemInterface.getMemory().set(indexRegister + i, getRegisterValue(i))
        }
        indexRegister += x.value + 1
    }

    //Store BCD representation of Vx in memory locations I, I+1, and I+2. The interpreter takes the decimal
    //value of Vx, and places the hundreds digit in memory at location in I, the tens digit at location I+1, and
    //the ones digit at location I+2.
    fun ldb(x: Nibble){
        log("Opcode->ldb")

        val value = getRegisterValue(x.value) // Get Vx value

        systemInterface.getMemory().set(indexRegister, (value / 100))
        systemInterface.getMemory().set(indexRegister + 1, ((value / 10) % 10))
        systemInterface.getMemory().set(indexRegister + 2,(value % 10))
    }

    //Set I = location of sprite for digit Vx. The value of I is set to the location for the hexadecimal sprite
    //corresponding to the value of Vx. See section 2.4, Display, for more information on the Chip-8 hexadecimal
    //font. To obtain this value, multiply VX by 5 (all font data stored in first 80 bytes of memory).
    fun ldf(x: Nibble){
        log("Opcode->ldf")
        val digit = getRegisterValue(x.value)
        setIndexRegister(digit * 5)
    }

    //Set I = I + Vx. The values of I and Vx are added, and the results are stored in I.
    fun addI(x: Nibble){
        log("DEBUGGAH " + currentCounter + " - ADD \t I, "+ regValueString(x))
        setIndexRegister(getIndexRegister() + getRegisterValue(x.value))
    }

    //Set delay timer = Vx. Delay Timer is set equal to the value of Vx.
    fun lddt(x: Nibble){
        log("Opcode->lddt")
        val dtValue = getRegisterValue(x.value)
        log("Opcode->lddt new delayTimer value is $dtValue")
        systemInterface.getTimer().setDelayTimer(dtValue)
    }

    //Set Vx = Delay Timer. Vx is set equal to the value of Delay timber.
    fun stdt(x: Nibble){
        log("Opcode->stdt")
        val delayTimer = systemInterface.getTimer().getDelayTimer()
        log("Opcode->stdt. Delay timer is " + delayTimer)
        setRegisterValue(x.value, delayTimer)
    }

    //Set sound timer = Vx. Sound Timer is set equal to the value of Vx.
    fun ldst(x: Nibble){
        log("Opcode->ldst")
        systemInterface.getTimer().setSoundTimer(getRegisterValue(x.value))
    }

    fun sknp(n1: Nibble) {
        log("Opcode->sknp")
        systemInterface.getKeyboard().getKeyAt(n1.value).let {
            if(!it) {
                incrementProgramCounter()
            }
        }
    }

    fun skp(n1: Nibble) {
        log("Opcode->skp")
        systemInterface.getKeyboard().getKeyAt(n1.value).let {
            if(it) {
                incrementProgramCounter()
            }
        }
    }

    fun regValueString(n: Nibble) : String {
        return "V${n.value.toHexString(CustomHexFormat())}"
    }

    fun argValueString(n: Nibble) : String {
        return "#${n.value.toHexString(CustomHexFormat())}"
    }

    fun argValueString(n: Int) : String {
        return "#${n.toHexString(CustomHexFormat())}"
    }

    //Display n-byte sprite starting at memory location I at (Vx, Vy), set VF = collision. The interpreter reads nibbles from memory, starting at the address stored in I. These bytes are then displayed as sprites on screen
    //at coordinates (Vx, Vy). Sprites are XOR’d onto the existing screen. If this causes any pixels to be erased,
    //VF is set to 1, otherwise it is set to 0. If the sprite is positioned so part of it is outside the coordinates of
    //the display, it wraps around to the opposite side of the screen.
    fun dxyn(n1: Nibble, n2: Nibble, n3: Nibble){

        log("DEBUGGAH " + currentCounter + " - DRW \t ${regValueString(n1)} ${regValueString(n2)} ${argValueString(n3)}")

        val displayWidth = systemInterface.getFrameBuffer().width
        val displayHeight = systemInterface.getFrameBuffer().height
        val xStart = getRegisterValue(n1.value) % displayWidth
        val yStart = getRegisterValue(n2.value) % displayHeight

        val height = n3.value
        registers[0xF] = 0  // Reset collision flag

        fun getSpriteAt(x: Int, y: Int) : Boolean {
            val wrappedX = ((x % displayWidth) + displayWidth) % displayWidth
            val wrappedY = ((y % displayHeight) + displayHeight) % displayHeight
            return systemInterface.getFrameBuffer().getSpriteAt(wrappedX, wrappedY)
        }

        fun setSpriteAt(item: Boolean, x: Int, y: Int) {
            val wrappedX = ((x % displayWidth) + displayWidth) % displayWidth
            val wrappedY = ((y % displayHeight) + displayHeight) % displayHeight
            systemInterface.getFrameBuffer().setSpriteAt(wrappedX, wrappedY, item)
        }

        for (row in 0 until height) {
            val spriteByte = systemInterface.getMemory().get(indexRegister + row) and 0xFF

            for (bit in 0 until 8) {
                val spritePixel = (spriteByte shr (7 - bit)) and 1
                if (spritePixel == 0) continue

                val x = xStart + bit
                val y = yStart + row

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
        log("Opcode->cxkk")
        // Chip-8 requires an 8-bit random value.
        val rnd = randomNumberGenerator.getRandom() and 0xFF
        setRegisterValue(n1.value, rnd and combineNibbles(n2, n3))
    }

    //Jump to location nnn + V0. The program counter is set to nnn plus the value of V0.
    fun bnnn(n1: Nibble, n2: Nibble, n3: Nibble){
        log("Opcode->bnnn")
       setProgramCounter (combineNibbles(n1, n2, n3) + getRegisterValue(0) )
    }

    //Set I = nnn. The value of register I is set to nnn.
    fun annn(n1: Nibble, n2: Nibble, n3: Nibble){
        val dest = combineNibbles(n1, n2, n3)
        log("DEBUGGAH " + currentCounter + " - LD \t I, "+dest.toHexString(CustomHexFormat()))
        setIndexRegister(dest)
    }

    //Skip next instruction if Vx != Vy. The values of Vx and Vy are compared, and if they are not equal, the
    //program counter is increased by 2.
    fun sneVxVy(x: Nibble, y: Nibble){
        log("Opcode->sneVxVy ")
        if(getRegisterValue(x.value) != getRegisterValue(y.value)) {
            incrementProgramCounter()
        }
    }

    //Set Vx = Vx SHR 1. If the least-significant bit of Vx is 1, then VF is set to 1, otherwise 0. Then Vx is
    //divided by 2.
    fun shrVxVy(x: Nibble, y: Nibble){
        log("Opcode->shrVxVy (8xy6) x=${x.value}")
        val vx = getRegisterValue(x.value)
        setRegisterValue(x.value, vx shr 1)
        setRegisterValue(0xF,vx % 2)
    }

    //Set Vx = Vx SHL 1. If the most-significant bit of Vx is 1, then VF is set to 1, otherwise to 0. Then Vx is
    //multiplied by 2.
    fun shlVxVy(x: Nibble, y: Nibble){
        log("Opcode->shlVxVy (8xyE) x=${x.value}")
        val vx = getRegisterValue(x.value)

        setRegisterValue(x.value, (vx shl 1) and 0xFF)
        setRegisterValue(0xF, (vx shr 7) and 0x1)
    }

    //Set Vx = Vy - Vx, set VF = NOT borrow. If Vy >= Vx, then VF is set to 1, otherwise 0. Then Vx is
    //subtracted from Vy, and the results stored in Vx.
    fun subnVxVy(x: Nibble, y: Nibble){
        log("Opcode->subnVxVy x=${x.value} y=${y.value}")

        val vx = getRegisterValue(x.value)
        val vy = getRegisterValue(y.value)

        val result = vy - vx

        // VF = 1 if Vy >= Vx (no borrow), else 0
        // Keep only lowest 8 bits
        setRegisterValue(x.value, unsigned(result))
        setRegisterValue(0xF, if (vy >= vx) 1 else 0)

    }

    protected fun unsigned(n: Int): Int = (n + 0x100) and 0xff


    //Set Vx = Vx - Vy, set VF = NOT borrow. If Vx ¿ Vy, then VF is set to 1, otherwise 0. Then Vy is
    //subtracted from Vx, and the results stored in Vx.
    fun subVxVy(x: Nibble, y: Nibble){
        log("Opcode->subVxVy")

        val vx = getRegisterValue(x.value)
        val vy = getRegisterValue(y.value)
        val newValue = vx - vy

        setRegisterValue(x.value, unsigned(newValue))
        setRegisterValue(0xF, if (newValue < 0) 0 else 1)

    }


    //Set Vx = Vx + Vy, set VF = carry. The values of Vx and Vy are added together. If the result is greater
    //than 8 bits (i.e., ¿ 255,) VF is set to 1, otherwise 0. Only the lowest 8 bits of the result are kept, and stored
    //in Vx.
    fun addVxVy(x: Nibble, y: Nibble){
        log("Opcode->addVxVy x=${x.value} y=${y.value}")

        val sum = getRegisterValue(x.value) + getRegisterValue(y.value)
        setRegisterValue(x.value, unsigned(sum))
        setRegisterValue(0xF, if (sum > 0xFF) 1 else 0)
    }

    //Set Vx = Vx XOR Vy. Performs a bitwise exclusive OR on the values of Vx and Vy, then stores the result
    //in Vx. An exclusive OR compares the corresponding bits from two values, and if the bits are not both the
    //same, then the corresponding bit in the result is set to 1. Otherwise, it is 0.
    fun xorVxVy(x: Nibble, y: Nibble){
        log("Opcode->xorVxVy x=${x.value} y=${y.value}")
        setRegisterValue(x.value, getRegisterValue(x.value) xor getRegisterValue(y.value))
    }

    //Set Vx = Vx AND Vy. Performs a bitwise AND on the values of Vx and Vy, then stores the result in Vx.
    //A bitwise AND compares the corresponding bits from two values, and if both bits are 1, then the same bit
    //in the result is also 1. Otherwise, it is 0.
    fun andVxVy(x: Nibble, y: Nibble){
        log("Opcode->andVxVy x=${x.value} y=${y.value}")
        setRegisterValue(x.value, getRegisterValue(x.value) and getRegisterValue(y.value))
    }

    //Set Vx = Vx OR Vy. Performs a bitwise OR on the values of Vx and Vy, then stores the result in Vx. A
    //bitwise OR compares the corresponding bits from two values, and if either bit is 1, then the same bit in the
    //result is also 1. Otherwise, it is 0.
    fun orVxVy(x: Nibble, y: Nibble){
        log("Opcode->orVxVy x=${x.value} y=${y.value}")
        setRegisterValue(x.value, getRegisterValue(x.value) or getRegisterValue(y.value))
    }

    //Set Vx = Vy. Stores the value of register Vy in register Vx.
    fun ldVxVy(x: Nibble, y: Nibble){
        log("Opcode->ldVxVy x=${x.value} y=${y.value}")
        setRegisterValue(x.value,getRegisterValue(y.value))
    }

    //Set Vx = Vx + kk. Adds the value kk to the value of register Vx, then stores the result in Vx.
    fun add(x: Nibble, kk: Int) {
        log("DEBUGGAH " + currentCounter + " - ADD \t ${regValueString(x)}, ${kk.toHexString(CustomHexFormat())}")
        // 7xkk wraps at 8-bit and does not affect VF.
        setRegisterValue(x.value,(getRegisterValue(x.value) + kk) and 0xFF)
    }

    //Jump to location nnn. The interpreter sets the program counter to nnn.
    fun jump(address1: Nibble, address2: Nibble, address3: Nibble){
        log("Opcode->jump address1=${address1.value} address2=${address2.value} address3=${address3.value}")
        val address = combineNibbles(address1, address2, address3)
        setProgramCounter(address)
    }

    //Set Vx = kk. The interpreter puts the value kk into register Vx.
    fun load(dest: Nibble, value: Int){
        log("DEBUGGAH " + currentCounter + " - LD \t V"+dest.value.toHexString(CustomHexFormat()) + ", " + "#${value.toHexString(CustomHexFormat())}")
        setRegisterValue(dest.value, value)
    }

    fun seVxVy(vX: Nibble, vY: Nibble){
        log("Opcode->seVxVy vX=${vX.value} vY=${vY.value}")
        if(getRegisterValue(vX.value) == getRegisterValue(vY.value)){
            incrementProgramCounter()
        }
    }

    fun se(dest: Nibble, value: Int){
        log("Opcode->se dest=${dest.value} value=$value")
        if(getRegisterValue(dest.value) == value){
            incrementProgramCounter()
        }
    }

    fun sne(dest: Nibble, value: Int){
        log("DEBUGGAH " + currentCounter + " - SNE \t ${regValueString(dest)}, ${argValueString(value)}")
        if(getRegisterValue(dest.value) != value){
            incrementProgramCounter()
        }
    }

    fun setIndexRegister(newValue: Int) {
        require(newValue in 0 .. 4095)
        log("Setting index register to  $newValue")
        indexRegister = newValue
    }

    fun incrementProgramCounter() {
        setProgramCounter(programCounter + 2)
    }

    fun setProgramCounter(newValue: Int){
        log("Setting program counter to $newValue")
        programCounter = newValue
        ensureValidState()
    }

    fun setRegisterData(index: Int, kk: Pair<Nibble, Nibble>) {
        setRegisterValue(index, combineNibbles(kk.first, kk.second))
    }


}