package com.twobard.kmpchip8

import com.twobard.kmpchip8.hardware.Cpu
import com.twobard.kmpchip8.hardware.System
import dev.mokkery.matcher.any
import dev.mokkery.verify
import kotlin.test.Test


class OpCodeTests {

    @Test
    fun `test load`() {
        val cpu = Cpu()
        val loadOpCode = System.OpCode(System.Nibble(0x6), System.Nibble(0xA), System.Nibble(0x0), System.Nibble(0x5))
        val nibbles = loadOpCode.toNibbles()
        cpu.execute(loadOpCode)

        verify {
            cpu.load(nibbles.first(), nibbles[1] + nibbles[2])
        }


    }
}