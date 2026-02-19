package com.twobard.kmpchip8

import com.twobard.kmpchip8.Utils.Companion.toNibbles
import com.twobard.kmpchip8.Utils.Companion.toUnsignedInt
import com.twobard.kmpchip8.hardware.Config
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

    @Test
    fun `given a CALL opcode when executed then programCounter and Stack should be in correct state`() {

        val addressPt1 = Nibble(0xA)
        val addressPt2 = Nibble(0x3)
        val addressPt3 =  Nibble(0x7)

        val retOpCode = System.OpCode(Nibble(0x2), addressPt1 ,addressPt2, addressPt3)
        system.cpu.execute(retOpCode)

        //Check programCounter
        assertEquals(addressPt1.value + addressPt2.value + addressPt3.value, system.cpu.getProgramCounter())

        //Check stackPointer
        assertEquals(1, system.cpu.getStackPointer())

        //Check stack state
        assertEquals(Config.PROGRAM_COUNTER_INIT, system.cpu.getFromStack(0))
    }

    @Test
    fun `given 3xkk when Vx == kk then increment program counter by 2`() {

        val Vx = Nibble(0xA)
        val kk = 255.toByte().toNibbles()

        //set V(0xA) to 255

        system.cpu.setRegisterData(Vx.value, kk)

        val retOpCode = System.OpCode(Nibble(0x3), Vx ,kk.first, kk.second)
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT + 2, system.cpu.getProgramCounter())
    }

    @Test
    fun `given 4xkk when Vx == kk then program counter stays the same`() {

        val Vx = Nibble(0xA)
        val kk = 255.toByte().toNibbles()

        //set V(0xA) to 255

        system.cpu.setRegisterData(Vx.value, 255.toByte().toNibbles())

        val retOpCode = System.OpCode(Nibble(0x4), Vx ,kk.first, kk.second)
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT, system.cpu.getProgramCounter())
    }

    @Test
    fun `given 4xkk when Vx !== kk then program counter stays the same`() {

        val Vx = Nibble(0xA)
        val kk = 255.toByte().toNibbles()

        //set V(0xA) to 255

        system.cpu.setRegisterData(Vx.value, 254.toByte().toNibbles())

        val retOpCode = System.OpCode(Nibble(0x4), Vx ,kk.first, kk.second)
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT + 2, system.cpu.getProgramCounter())
    }

    @Test
    fun `given 5xy0 when Vx == Vy then program counter increments`() {

        val value = 255.toByte().toNibbles()
        val x = Nibble(0)
        val y = Nibble(1)

        system.cpu.setRegisterData(x.value, value)
        system.cpu.setRegisterData(y.value, value)

        val retOpCode = System.OpCode(Nibble(0x5), x ,y, Nibble(0))
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT + 2, system.cpu.getProgramCounter())
    }

    @Test
    fun `given 5xy0 when Vx !== Vy then program counter stays the same`() {

        val valueA = 255.toByte().toNibbles()
        val valueB = 254.toByte().toNibbles()
        val x = Nibble(0)
        val y = Nibble(1)

        system.cpu.setRegisterData(x.value, valueA)
        system.cpu.setRegisterData(y.value, valueB)

        val retOpCode = System.OpCode(Nibble(0x5), x ,y, Nibble(0))
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT, system.cpu.getProgramCounter())
    }

    @Test
    fun `given 7xkk when executed then Vx should equal Vx + kk`() {

        val pos = Nibble(0)
        val existingData = 10.toByte().toNibbles()
        val data = 20.toByte().toNibbles()

        system.cpu.setRegisterData(pos.value, existingData)

        val retOpCode = System.OpCode(Nibble(0x7), pos ,data.first, data.second)
        system.cpu.execute(retOpCode)

        assertEquals(30, system.cpu.registers[pos.value])
    }

    @Test
    fun `given 8xy0 when executed then Vx should equal Vy`() {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 10.toByte().toNibbles()
        val yData = 20.toByte().toNibbles()

        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x0))
        system.cpu.execute(retOpCode)

        assertEquals(20, system.cpu.registers[x.value])
    }

    @Test
    fun `given 8xy1 when x is 170 and y is 85 then Vx is 255`() {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 170.toByte().toNibbles()
        val yData = 85.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)


        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x1))
        system.cpu.execute(retOpCode)

        assertEquals(255, system.cpu.registers[x.value])
    }

    @Test
    fun `given 8xy1 when x is 255 and y is 128 then Vx is 255`() {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 255.toByte().toNibbles()
        val yData = 128.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x1))
        system.cpu.execute(retOpCode)

        assertEquals(255, system.cpu.registers[x.value])
    }

    @Test
    fun `given 8xy1 when x is 0 and y is 0 then Vx is 0`() {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 0.toByte().toNibbles()
        val yData = 0.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x1))
        system.cpu.execute(retOpCode)

        assertEquals(0, system.cpu.registers[x.value])
    }

    @Test
    fun `given 8xy2 when x is 51 and y is 85 then Vx is 17`() {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 51.toByte().toNibbles()
        val yData = 85.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x2))
        system.cpu.execute(retOpCode)

        //51 AND 85 = 17
        assertEquals(17, system.cpu.registers[x.value])
    }

    @Test
    fun `given 8xy2 when x is 255 and y is 128 then Vx is 128`() {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 255.toByte().toNibbles()
        val yData = 128.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x2))
        system.cpu.execute(retOpCode)

        //288 AND 128 = 128
        assertEquals(128, system.cpu.registers[x.value])
    }

    @Test
    fun `given 8xy3 when x is 255 and y is 128 then Vx is 127`() {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 255.toByte().toNibbles()
        val yData = 128.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x3))
        system.cpu.execute(retOpCode)

        //51 AND 85 = 17
        assertEquals(127, system.cpu.registers[x.value])
    }

    @Test
    fun `given 8xy3 when x is 255 and y is 0 then Vx is 255`() {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 255.toByte().toNibbles()
        val yData = 0.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x3))
        system.cpu.execute(retOpCode)

        //255 AND 0 = 255
        assertEquals(255, system.cpu.registers[x.value])
    }

    @Test
    fun `given 8xy3 when x is 51 and y is 85 then Vx is 102`() {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 51.toByte().toNibbles()
        val yData = 85.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x3))
        system.cpu.execute(retOpCode)

        //51 AND 85 = 102
        assertEquals(102, system.cpu.registers[x.value])
    }

    @Test
    fun `given 8xy4 when x is 0 and y is 1 then Vx is 1`() {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 0.toByte().toNibbles()
        val yData = 1.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x4))
        system.cpu.execute(retOpCode)

        //0 ADD 1 = 1
        assertEquals(1, system.cpu.registers[x.value])

        //Carry is 0
        assertEquals(0, system.cpu.registers[0xf])
    }

    @Test
    fun `given 8xy4 when x is 128 and y is 128 then Vx is 0 and carry is 1`() {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 128.toByte().toNibbles()
        val yData = 128.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x4))
        system.cpu.execute(retOpCode)

        //128 ADD 128 = 0 (carry 1)
        assertEquals(0, system.cpu.registers[x.value])

        //Carry is 1
        assertEquals(1, system.cpu.registers[0xf])
    }

    @Test
    fun `given 8xy5 when x is 10 and y is 5 then Vx is 5 and VF is 1`() {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 10.toByte().toNibbles()
        val yData = 5.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x5))
        system.cpu.execute(retOpCode)

        //10 SUB 5 = 5 (no borrow)
        assertEquals(5, system.cpu.registers[x.value])

        //no borrow
        assertEquals(1, system.cpu.registers[0xf])
    }

    @Test
    fun `given 8xy5 when x is 5 and y is 10 then Vx is 251 and VF is 0`() {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 5.toByte().toNibbles()
        val yData = 10.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x5))
        system.cpu.execute(retOpCode)

        //5 SUB 10 = 251 (borrow)
        assertEquals(251, system.cpu.registers[x.value])

        //borrow
        assertEquals(0, system.cpu.registers[0xf])
    }

    @Test
    fun `given 8xy6 when x is 7 and then Vx is 3 and VF is 1`() {

        val x = Nibble(0)
        val y = Nibble(0)
        val xData = 7.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x6))
        system.cpu.execute(retOpCode)

        //7 shr 2 = 3 (divided by two)
        assertEquals(3, system.cpu.registers[x.value])

        // VF = LSB of Vx
        assertEquals(1, system.cpu.registers[0xf])
    }

    @Test
    fun `given 8xy6 when x is 10 and then Vx is 5 and VF is 0`() {

        val x = Nibble(0)
        val y = Nibble(0)
        val xData = 10.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x6))
        system.cpu.execute(retOpCode)

        //10 shr 2 = 5 (divided by two)
        assertEquals(5, system.cpu.registers[x.value])

        // VF = LSB of Vx
        assertEquals(0, system.cpu.registers[0xf])
    }

    @Test
    fun `given 8xy7 when x is 5 and y is 10 then Vx is 5 and VF is 1`() {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 5.toByte().toNibbles()
        val yData = 10.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x7))
        system.cpu.execute(retOpCode)

        //10 - 5 = 5
        assertEquals(5, system.cpu.registers[x.value])

        // VF = 1 if Vy >= Vx (no borrow), else 0
        assertEquals(1, system.cpu.registers[0xf])
    }

    @Test
    fun `given 8xy7 when x is 10 and y is 5 then Vx is 251 and VF is 0`() {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 10.toByte().toNibbles()
        val yData = 5.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x7))
        system.cpu.execute(retOpCode)

        //5 - 10 = 251 (with wraparound)
        assertEquals(251, system.cpu.registers[x.value])

        // VF = 0 becayse Vy < Vx
        assertEquals(0, system.cpu.registers[0xf])
    }

    @Test
    fun `given 8xy8 when x is 10 then result Vx is 20 and VF is 0`() {

        val x = Nibble(0)
        val xData = 10.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,Nibble(0x0), Nibble(0x8))
        system.cpu.execute(retOpCode)

        //10 x 2 = 20
        assertEquals(20, system.cpu.registers[x.value])

        // VF = 0 (no overflow)
        assertEquals(0, system.cpu.registers[0xf])
    }

    @Test
    fun `given 8xy8 when x is 200 then result Vx is 144 and VF is 1`() {

        val x = Nibble(0)
        val xData = 200.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,Nibble(0x0), Nibble(0x8))
        system.cpu.execute(retOpCode)

        //200 x 2 = 400. 400 % 256 = 144
        assertEquals(144, system.cpu.registers[x.value])

        // VF = 1 (overflows)
        assertEquals(1, system.cpu.registers[0xf])
    }

    @Test
    fun `given 9xy0 when Vx != Vy then increment program counter`() {

        val x = Nibble(0)
        val xData = 200.toByte().toNibbles()
        val y = Nibble(1)
        val yData = 201.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x9), x ,y, Nibble(0x0))
        system.cpu.execute(retOpCode)


        // VF = 1 (overflows)
        assertEquals(Config.PROGRAM_COUNTER_INIT + 2, system.cpu.getProgramCounter())
    }
}