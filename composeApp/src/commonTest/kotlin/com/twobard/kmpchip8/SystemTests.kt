package com.twobard.kmpchip8

import com.twobard.kmpchip8.Utils.Companion.toNibbles
import com.twobard.kmpchip8.hardware.Config
import com.twobard.kmpchip8.hardware.Nibble
import com.twobard.kmpchip8.hardware.System
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class SystemTests {

    val defaultFont = Config.DEFAULT_FONT
    lateinit var system : System

    @BeforeTest
    fun before(){
        system = System()
    }

    @Test
    fun `given intial system state when init then font should be loaded`() {
        system.memory.getAll().subList(0, defaultFont.component1().size).forEachIndexed{ index, sprite ->
            assertEquals(defaultFont.component1()[index], sprite)
        }
    }

    @Test
    fun `given a loaded OpCode when fetch first OpCode correct OpCode should be returned`() {
        system.memory[Config.PROGRAM_COUNTER_INIT] = 255.toByte()
        system.memory[Config.PROGRAM_COUNTER_INIT + 1] = 255.toByte()

        val opcode = system.fetchOpcode()
        assertEquals(System.OpCode(255.toByte(), 255.toByte()), opcode)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given a new delay of 60 when 1 second passes then delay should reduce to 0`() = runTest {
        system.setDelayTimer(60)
        system.startTimers(this)
        system.startRunning()

        advanceTimeBy(1000)

        system.stopRunning()

        assertEquals(0, system.getDelayTimer())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given a new sound timer value of 60 when 1 second passes then sound timer should reduce to 0`() = runTest {
        system.setSoundTimer(60)
        system.startTimers(this)
        system.startRunning()

        advanceTimeBy(1000)

        system.stopRunning()

        assertEquals(0, system.getSoundTimer())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given a byte when it is split into nibbles then nibbles should be correct`() {
        val someByte = 128.toByte()
        val nibbles = someByte.toNibbles()
        assertEquals(Nibble(0x8), nibbles.first)
        assertEquals(Nibble(0x0), nibbles.second)

        val someOtherByte = 0.toByte()
        val zeroNibbles = someOtherByte.toNibbles()
        assertEquals(Nibble(0x0), zeroNibbles.first)
        assertEquals(Nibble(0x0), zeroNibbles.second)

        val topByte = 255.toByte()
        val topNibbles = topByte.toNibbles()
        assertEquals(Nibble(0xF), topNibbles.first)
        assertEquals(Nibble(0xF), topNibbles.second)

    }
}