package com.twobard.kmpchip8.hardware


import com.twobard.kmpchip8.Utils.Companion.toNibbles
import com.twobard.kmpchip8.Utils.Companion.toUnsignedInt
import com.twobard.kmpchip8.hardware.Config.Companion.`60HZ_TIMER`
import com.twobard.kmpchip8.hardware.Keyboard
import kmpchip8.composeapp.generated.resources.Res
import kmpchip8.composeapp.generated.resources.compose_multiplatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class System(val memory: Memory = Memory(),
             val frameBuffer: FrameBuffer = FrameBuffer(),
             var keyboard: KeyboardInterface = Keyboard(),
             var timer : Timer = Timer(),
             val font: Config.Chip8Font = Config.DEFAULT_FONT,
             val display: Display = Display()) {


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

                override fun getTimer(): Timer {
                    return timer
                }
            }
        )
    }

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

    suspend fun startGame(title: String){

        coroutineScope {
            launch {
                val rom = getRom(title)
                loadRom(rom)
                startCpu()
            }
        }
    }

    val _displayData = MutableStateFlow<Array<BooleanArray>?>(null)
    val displayData: StateFlow<Array<BooleanArray>?> = _displayData

    suspend fun startCpu(cyclesPerSecond: Int = 500) {
        timer.startRunning()
        val cycleDelay = 1000L / cyclesPerSecond

        while (timer.running) {
            val opcode = fetchOpcode()
            cpu.incrementProgramCounter()
            cpu.execute(opcode)
            delay(cycleDelay)

                println("recomposing update. Active pixels: " + display.pixels())
                _displayData.value = display.copy()

        }
    }

    fun clearDisplay() {
        display.clear()
    }

    suspend fun getRom(title: String) : ByteArray {
        return Res.readBytes("files/$title")
    }

    fun loadRom(rom: ByteArray) {
        memory.addRom(rom)
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