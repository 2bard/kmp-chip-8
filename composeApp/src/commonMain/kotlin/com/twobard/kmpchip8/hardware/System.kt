package com.twobard.kmpchip8.hardware


import com.twobard.kmpchip8.CustomHexFormat
import com.twobard.kmpchip8.Utils.Companion.toNibbles
import kmpchip8.composeapp.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.text.HexFormat

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
                    frameBuffer.clear()
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
            memory[index] = sprite
        }
    }

    fun fetchOpcode(): OpCode {
        val high = memory.get(cpu.getProgramCounter())
        val low  = memory.get(cpu.getProgramCounter() + 1)

        val result  = OpCode(high, low)
        return result
    }




    suspend fun startGame(title: String){

        coroutineScope {
            launch(Dispatchers.Default) {
                val rom = getRom(title)
                val intArray = rom.map { it.toInt() and 0xFF }.toIntArray()
                loadRom(intArray)
                startCpu(500, this@coroutineScope)
            }
        }
    }

    val _displayData = MutableStateFlow<Array<BooleanArray>?>(null)
    val displayData: StateFlow<Array<BooleanArray>?> = _displayData

    suspend fun startCpu(cyclesPerSecond: Int = 500, coroutineScope: CoroutineScope) {
        timer.startRunning(coroutineScope)
        val cycleDelay = 1000L / cyclesPerSecond

        while (timer.running) {
            val opcode = fetchOpcode()
            cpu.ensureValidState()
            val currentCounter = cpu.getProgramCounter()
            cpu.incrementProgramCounter()
            cpu.ensureValidState()
            cpu.execute(opcode, currentCounter.toHexString(CustomHexFormat()))
            cpu.ensureValidState()

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

    fun loadRom(rom: IntArray) {
        memory.addRom(rom)
    }

    data class OpCode(val high: Int, val low: Int) {

        constructor(n1: Nibble, n2: Nibble, n3: Nibble, n4: Nibble) : this(((n1.value shl 4) or (n2.value and 0xF)), ((n3.value shl 4) or (n4.value and 0xF)))

        init {
            require(high <= 255)
            require(low <= 255)
        }

        fun toNibbles() : List<Nibble> {
            return high.toNibbles().toList().plus(low.toNibbles().toList())
        }
    }

}

fun combineNibbles(vararg nibbles: Nibble): Int {
    require(nibbles.isNotEmpty()) { "combineNibbles requires at least 1 nibble" }

    var result = 0
    for (n in nibbles) {
        require(n.value in 0..0xF) { "Nibble out of range: ${n.value}" }
        result = (result shl 4) or (n.value and 0xF)
    }

    // A CHIP-8 address is 12-bit (0x000..0xFFF). For opcode pieces (kk), this is also safe.
    require(result in 0..0xFFF) { "Combined value out of 12-bit range: $result" }
    return result
}

data class Nibble(val value: Int) {
    init {
        require(value < 16)
    }
}