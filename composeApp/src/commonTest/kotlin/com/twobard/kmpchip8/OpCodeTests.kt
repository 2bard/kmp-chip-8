package com.twobard.kmpchip8

import com.twobard.kmpchip8.Utils.Companion.toNibbles
import com.twobard.kmpchip8.hardware.Nibble
import com.twobard.kmpchip8.hardware.System
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.atMost
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


class OpCodeTests {

    lateinit var system: System

    @BeforeTest
    fun setup(){
        system = System()
    }

    @Test
    fun `given a Load opcode when executed then register should have new value at correct destination`() {

        val destination = 0xA
        val value = 0x05.toByte().toNibbles()
        val loadOpCode = System.OpCode(Nibble(0x6), Nibble(destination), value.first, value.second)
        val nibbles = loadOpCode.toNibbles()
        system.cpu.execute(loadOpCode)

        verify(atMost(1)) {
            system.cpu.load(nibbles.first(), nibbles[1] + nibbles[2])
        }

        assertEquals(0x05, system.cpu.registers[destination])

    }

    @Test
    fun `given a CLS opcode when executed then register should have new value at correct destination`() {
        val loadOpCode = System.OpCode(Nibble(0x0), Nibble(0x0), Nibble(0xE), Nibble(0x0))
        system.cpu.execute(loadOpCode)

        verify(atMost(1)) {
            system.display.clear()
        }
    }

    @Test
    fun `given a JMP opcode when executed then register should have new value at correct destination`() {
        val addressPt1 = Nibble(0xA)
        val addressPt2 = Nibble(0x3)
        val addressPt3 =  Nibble(0x7)
        val loadOpCode = System.OpCode(Nibble(0x1), addressPt1 ,addressPt2, addressPt3)
        system.cpu.execute(loadOpCode)

        verify(atMost(1)) {
            system.cpu.jump(addressPt1, addressPt2, addressPt3)
        }

        //0xA + 0x3 + 0x7 == 20
        assertEquals(20, system.cpu.getProgramCounter())
    }

    @Test
    fun `given a RET opcode when executed then register should have new value at correct destination`() {

        system.cpu.call(1)
        system.cpu.call(2)
        val retOpCode = System.OpCode(Nibble(0x0), Nibble(0x0) ,Nibble(0xE), Nibble(0xE))
        system.cpu.execute(retOpCode)

        verify(atMost(1)) {
            system.cpu.ret()
        }
    }

//    @Test
//    fun `given a RET opcode when executed then register should have new value at correct destination`() {
//
//        cpu.call(1)
//        cpu.call(2)
//        val retOpCode = System.OpCode(Nibble(0x0), Nibble(0x0) ,Nibble(0xE), Nibble(0xE))
//        cpu.execute(retOpCode)
//
//        verify(atMost(1)) {
//            cpu.ret()
//        }
//    }
}