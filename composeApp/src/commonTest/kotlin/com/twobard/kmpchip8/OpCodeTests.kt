package com.twobard.kmpchip8

import com.twobard.kmpchip8.Utils.Companion.toNibbles
import com.twobard.kmpchip8.hardware.Cpu
import com.twobard.kmpchip8.hardware.System
import dev.mokkery.matcher.any
import dev.mokkery.verify
import kotlin.test.Test
import kotlin.test.assertEquals


class OpCodeTests {

    @Test
    fun `given a Load opcode when executed then register should have new value at correct destination`() {
        val cpu = Cpu()
        val destination = 0xA
        val value = 0x05.toByte().toNibbles()
        val loadOpCode = System.OpCode(System.Nibble(0x6), System.Nibble(destination), value.first, value.second)
        val nibbles = loadOpCode.toNibbles()
        cpu.execute(loadOpCode)

        verify {
            cpu.load(nibbles.first(), nibbles[1] + nibbles[2])
        }

        assertEquals(0x05, cpu.registers[destination])

    }

    @Test
    fun `given a CLS opcode when executed then register should have new value at correct destination`() {
        val cpu = Cpu()
        val loadOpCode = System.OpCode(System.Nibble(0x0), System.Nibble(0x0), System.Nibble(0xE), System.Nibble(0x0))
        cpu.execute(loadOpCode)

        verify {
            cpu.clearDisplay()
        }
    }
}