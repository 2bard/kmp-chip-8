package com.twobard.kmpchip8

import com.twobard.kmpchip8.Utils.Companion.toNibbles
import com.twobard.kmpchip8.hardware.Config
import com.twobard.kmpchip8.hardware.Nibble
import com.twobard.kmpchip8.hardware.System
import com.twobard.kmpchip8.hardware.combineNibbles
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemTests {

    val defaultFont = Config.DEFAULT_FONT
    lateinit var system : System

    @BeforeTest
    fun before(){
        system = System()
    }

    @Test
    fun `runGame`() = runTest {
        system.startGame("octojam1title.ch8")
    }

    @Test
    fun `given a rom when loaded then return bytearray`() = runTest {
        val rom = system.getRom("octojam1title.ch8")
        assertTrue (rom.isNotEmpty())
    }

    @Test
    fun `load rom into system`() = runTest {
        val rom = system.getRom("octojam1title.ch8")
        system.loadRom(rom)
        rom.forEachIndexed { index, byte ->
            //assertEquals(byte, system.memory[Config.PROGRAM_COUNTER_INIT + index])
        }
    }

    @Test
    fun `given 3 nibbles 0x1 0x2 0x3 when combined then result is 0x123`() {
        val n1 = Nibble(0x1)
        val n2 = Nibble(0x2)
        val n3 = Nibble(0x3)

        val result = combineNibbles(n1, n2, n3)

        assertEquals(0x123, result)
    }

    @Test
    fun `given intial system state when init then font should be loaded`() {
        system.memory.getAll().subList(0, defaultFont.component1().size).forEachIndexed{ index, sprite ->
            assertEquals(defaultFont.component1()[index], sprite)
        }
    }

    @Test
    fun `given a loaded OpCode when fetch first OpCode correct OpCode should be returned`() {
        val byte1 =  0xFF
        system.memory[Config.PROGRAM_COUNTER_INIT] = byte1
        system.memory[Config.PROGRAM_COUNTER_INIT + 1] = byte1

        val opcode = system.fetchOpcode()
        assertEquals(System.OpCode(byte1, byte1), opcode)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given a new delay of 60 when 1 second passes then delay should reduce to 0`() = runTest {
        system.timer.setDelayTimer(60)
        system.timer.startTimers(this)
        system.timer.startRunning()

        advanceTimeBy(1000)

        system.timer.stopRunning()

        assertEquals(0, system.timer.getDelayTimer())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given a new sound timer value of 60 when 1 second passes then sound timer should reduce to 0`() = runTest {
        system.timer.setSoundTimer(60)
        system.timer.startTimers(this)
        system.timer.startRunning()

        advanceTimeBy(1000)

        system.timer.stopRunning()

        assertEquals(0, system.timer.getSoundTimer())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given a byte when it is split into nibbles then nibbles should be correct`() {
        val someByte = 128
        val nibbles = someByte.toNibbles()
        assertEquals(Nibble(0x8), nibbles.first)
        assertEquals(Nibble(0x0), nibbles.second)

        val someOtherByte = 0
        val zeroNibbles = someOtherByte.toNibbles()
        assertEquals(Nibble(0x0), zeroNibbles.first)
        assertEquals(Nibble(0x0), zeroNibbles.second)

        val topByte = 255
        val topNibbles = topByte.toNibbles()
        assertEquals(Nibble(0xF), topNibbles.first)
        assertEquals(Nibble(0xF), topNibbles.second)

    }
}