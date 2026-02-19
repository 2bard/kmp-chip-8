package com.twobard.kmpchip8.hardware


import com.twobard.kmpchip8.Utils.Companion.toNibbles
import com.twobard.kmpchip8.Utils.Companion.toUnsignedInt
import com.twobard.kmpchip8.hardware.Config.Companion.`60HZ_TIMER`
import com.twobard.kmpchip8.hardware.Keyboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class System(val memory: Memory = Memory(),
             val frameBuffer: FrameBuffer = FrameBuffer(),
             var keyboard: KeyboardInterface = Keyboard(),
             val font: Config.Chip8Font = Config.DEFAULT_FONT,
             val display: Display = Display()) {

    var running = false
    private var delayTimer = 0
    private var soundTimer = 0

    var cpu: Cpu

    init {
        loadFont(memory, font)
        cpu = Cpu(
            systemInterface = object : SystemInterface {
                override fun clearDisplay() {
                   display.clear()
                }

                override fun getFrameBuffer(): FrameBuffer {
                    return frameBuffer
                }

                override fun getMemory(): Memory {
                    return memory
                }

                override fun getDisplay(): Display {
                    return display
                }

                override fun getKeyboard(): KeyboardInterface {
                    return keyboard
                }
            }
        )
    }

    fun setDelayTimer(timer: Int){
        this.delayTimer = timer
    }

    fun getDelayTimer() = this.delayTimer

    fun setSoundTimer(timer: Int){
        this.soundTimer = timer
    }

    fun getSoundTimer() = this.soundTimer

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

    fun startRunning(){
        running = true
    }

    fun stopRunning(){
        running = false
    }

    fun startTimers(scope: CoroutineScope) {
        scope.launch {
            while (running) {
                if (delayTimer > 0) delayTimer--
                if (soundTimer > 0) soundTimer--

                println("Timers. Delay: $delayTimer Sound:$soundTimer")
                delay(`60HZ_TIMER`)
            }
        }
    }

    suspend fun startCpu(cyclesPerSecond: Int = 500) {
        startRunning()
        val cycleDelay = 1000L / cyclesPerSecond

        while (running) {
            val opcode = fetchOpcode()
            cpu.incrementProgramCounter()
            cpu.execute(opcode)
            delay(cycleDelay)
        }
    }

    fun clearDisplay() {
        display.clear()
    }


    data class OpCode(val high: Byte, val low: Byte) {

        constructor(n1: Nibble, n2: Nibble, n3: Nibble, n4: Nibble) : this(((n1.value shl 4) or (n2.value and 0xF)).toByte(), ((n3.value shl 4) or (n4.value and 0xF)).toByte())

        fun toNibbles() : List<Nibble> {
            return high.toNibbles().toList().plus(low.toNibbles().toList())
        }
    }

}

fun combineNibbles(vararg nibbles: Nibble): Int {
    var result = 0
    for (n in nibbles) {
        result = (result shl 4) or (n.value and 0xF)
    }
    return result
}

data class Nibble(val value: Int) {

}