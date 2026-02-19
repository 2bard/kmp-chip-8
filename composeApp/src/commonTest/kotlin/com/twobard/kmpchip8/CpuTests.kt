package com.twobard.kmpchip8

import com.twobard.kmpchip8.hardware.Config
import com.twobard.kmpchip8.hardware.Cpu
import com.twobard.kmpchip8.hardware.Display
import com.twobard.kmpchip8.hardware.FrameBuffer
import com.twobard.kmpchip8.hardware.KeyboardInterface
import com.twobard.kmpchip8.hardware.Memory
import com.twobard.kmpchip8.hardware.System
import com.twobard.kmpchip8.hardware.SystemInterface
import com.twobard.kmpchip8.ui.Keyboard
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CpuTests {

    lateinit var cpu: Cpu

    @BeforeTest
    fun setUp() {
        cpu = Cpu(systemInterface = object : SystemInterface {
            override fun clearDisplay() {

            }

            override fun getFrameBuffer(): FrameBuffer {
                return FrameBuffer()
            }

            override fun getMemory(): Memory {
                return Memory()
            }

            override fun getDisplay(): Display {
                return Display()
            }

            override fun getKeyboard(): KeyboardInterface {
                return com.twobard.kmpchip8.hardware.Keyboard()
            }
        })
    }

    @Test
    fun `given an empty stack when call then increment stackpointer`() {
        cpu.call(100)
        assertEquals(1, cpu.getStackPointer())
    }


    @Test
    fun `given an empty stack when call then set programCounter`() {
        cpu.call(100)
        assertEquals(100, cpu.getProgramCounter())
    }

    @Test
    fun `given an empty stack when call then set stack0 to return address`() {
        cpu.call(100)
        assertEquals(Config.PROGRAM_COUNTER_INIT, cpu.getFromStack(0))
    }

    @Test
    fun `given a stack with one address when ret then stackPointer should be decremented`() {
        cpu.call(100)
        cpu.ret()
        assertEquals(0, cpu.getStackPointer())
    }

    @Test
    fun `given a stack with one address when ret then programCounter should equal last address`() {
        cpu.call(100)
        cpu.ret()
        assertEquals(Config.PROGRAM_COUNTER_INIT, cpu.getProgramCounter())
    }
}