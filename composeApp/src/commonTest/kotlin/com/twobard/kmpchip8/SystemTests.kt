package com.twobard.kmpchip8

import com.twobard.kmpchip8.hardware.Config
import com.twobard.kmpchip8.hardware.System
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
}