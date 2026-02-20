package com.twobard.kmpchip8

import com.twobard.kmpchip8.Utils.Companion.toNibbles
import com.twobard.kmpchip8.Utils.Companion.toUnsignedInt
import com.twobard.kmpchip8.hardware.Config
import com.twobard.kmpchip8.hardware.KeyboardInterface
import com.twobard.kmpchip8.hardware.Nibble
import com.twobard.kmpchip8.hardware.System
import com.twobard.kmpchip8.hardware.combineNibbles
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.atMost
import kotlin.collections.forEach
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
        system.cpu.execute(loadOpCode)

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

        assertEquals(Config.PROGRAM_COUNTER_INIT + 2, system.cpu.getProgramCounter())
    }

    @Test
    fun `given 9xy0 when Vx == Vy then do not increment program counter`() {

        val x = Nibble(0)
        val xData = 200.toByte().toNibbles()
        val y = Nibble(1)
        val yData = 200.toByte().toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x9), x ,y, Nibble(0x0))
        system.cpu.execute(retOpCode)


        assertEquals(Config.PROGRAM_COUNTER_INIT, system.cpu.getProgramCounter())
    }

    @Test
    fun `given Annn when nnn == 6 then indexRegister == 6`() {

        val n1 = Nibble(0x1)
        val n2 = Nibble(0x2)
        val n3 = Nibble(0x3)

        val retOpCode = System.OpCode(Nibble(0xA), n1 ,n2, n3)
        system.cpu.execute(retOpCode)

        assertEquals(6, system.cpu.getIndexRegister())
    }

    @Test
    fun `given Bnnn when nnn == 6 and V0 == 5 then programCounter == 11`() {

        val n1 = Nibble(0x0)
        val n2 = Nibble(0x0)
        val n3 = Nibble(0x6)

        system.cpu.setRegisterData(0, 5.toByte().toNibbles())

        val retOpCode = System.OpCode(Nibble(0xB), n1 ,n2, n3)
        system.cpu.execute(retOpCode)

        assertEquals(11, system.cpu.getProgramCounter())
    }

    @Test
    fun `given Cxkk when x == 7 and kk == F0 and random == 0xF3 then Vx == F0`() {

        val n1 = Nibble(0x7)
        val n2 = Nibble(0xF)
        val n3 = Nibble(0x0)

        system.cpu.randomNumberGenerator = object : Utils.RandomNumberGeneratorInterface {
            override fun getRandom(): Int {
                return 0xF3
            }
        }

        val retOpCode = System.OpCode(Nibble(0xC), n1 ,n2, n3)
        system.cpu.execute(retOpCode)

        assertEquals(0xF0, system.cpu.registers[n1.value])
    }

    @Test
    fun `given Cxkk when x == 7 and kk == FF and random == 0x12 then Vx == 0xF  `() {

        val n1 = Nibble(0xF)
        val n2 = Nibble(0xF)
        val n3 = Nibble(0xF)

        system.cpu.randomNumberGenerator = object : Utils.RandomNumberGeneratorInterface {
            override fun getRandom(): Int {
                return 0x12
            }
        }

        val retOpCode = System.OpCode(Nibble(0xC), n1 ,n2, n3)
        system.cpu.execute(retOpCode)

        assertEquals(0x12, system.cpu.registers[n1.value])
    }

    @Test
    fun `given Dxyn when Vx == 10 Vy == 5 and n == 5 then 4 pixels should turn on`() {

        system.cpu.setIndexRegister(0x300)
        system.memory[0x300] = combineNibbles(Nibble(0xF), Nibble(0x0)).toByte()

        val x = 10
        val nibblex = Nibble(x)
        val y = 5
        val nibbley = Nibble(y)
        val n = 1 //1 bite sprite

        system.cpu.setRegisterData(x, Pair(Nibble(0), nibblex))
        system.cpu.setRegisterData(y, Pair(Nibble(0), nibbley))


        val retOpCode = System.OpCode(Nibble(0xD), nibblex , nibbley, Nibble(n))
        system.cpu.execute(retOpCode)

        assertEquals(0, system.cpu.registers[0xF])

        var pixelsOn = 0

        system.display.matrix.forEach {
            it.forEach {
                if(it) {
                    pixelsOn++
                }
            }
        }

        assertEquals(4, pixelsOn)
        assertEquals(true, system.display.matrix[10][5])
        assertEquals(true, system.display.matrix[11][5])
        assertEquals(true, system.display.matrix[12][5])
        assertEquals(true, system.display.matrix[13][5])
    }

    @Test
    fun `given Dxyn when V1 == 10 V2 == 5 and n == 5 then 3 pixels should turn on and collision == 1`() {

        system.cpu.setIndexRegister(0x300)
        system.memory[0x300] = combineNibbles(Nibble(0xF), Nibble(0x0)).toByte()

        val x = 10
        val nibblex = Nibble(x)
        val y = 5
        val nibbley = Nibble(y)
        val n = 1 //1 bite sprite

        system.cpu.setRegisterData(x, Pair(Nibble(0), nibblex))
        system.cpu.setRegisterData(y, Pair(Nibble(0), nibbley))

        // Manually turn on the third pixel (overlap position)
        system.cpu.systemInterface.getFrameBuffer().buffer[12][5] = true

        val retOpCode = System.OpCode(Nibble(0xD), nibblex , nibbley, Nibble(n))
        system.cpu.execute(retOpCode)


        var pixelsOn = 0

        system.display.matrix.forEach {
            it.forEach {
                if(it) {
                    pixelsOn++
                }
            }
        }

        assertEquals(3, pixelsOn)
        assertEquals(true, system.display.matrix[10][5])
        assertEquals(true, system.display.matrix[11][5])
        assertEquals(false, system.display.matrix[12][5])//overlapped
        assertEquals(true, system.display.matrix[13][5])
        assertEquals(false, system.display.matrix[14][5])

        // VF should be 1 because at least one pixel was erased
        assertEquals(1,system.cpu.registers[0xF])
    }

    @Test
    fun `given Dxyn when Vx == 10 Vy == 5 and n == 2 then pixels should turn on with collision`() {

        system.cpu.setIndexRegister(0x300)

        system.memory[0x300] = 0b11110000.toByte()
        system.memory[0x301] = 0b00111100.toByte()

        val x = 10
        val y = 5
        val nibblex = Nibble(x)
        val nibbley = Nibble(y)
        val n = 2 // multi-row sprite

        system.cpu.setRegisterData(x, Pair(Nibble(0), nibblex))   // Vx
        system.cpu.setRegisterData(y, Pair(Nibble(0), nibbley))   // Vy

        system.cpu.systemInterface.getFrameBuffer().buffer[12][5] = true
        system.cpu.systemInterface.getFrameBuffer().buffer[13][6] = true

        val retOpCode = System.OpCode(Nibble(0xD), nibblex, nibbley, Nibble(n))
        system.cpu.execute(retOpCode)

        var pixelsOn = 0
        system.display.matrix.forEach {
            it.forEach {
                if(it) pixelsOn++
            }
        }

        // first row
        assertEquals(true, system.display.matrix[10][5]) // Row 0
        assertEquals(true, system.display.matrix[11][5])
        assertEquals(false, system.display.matrix[12][5]) // Overlap → OFF
        assertEquals(true, system.display.matrix[13][5])
        assertEquals(false, system.display.matrix[14][5])

        // second row
        assertEquals(false, system.display.matrix[10][6])
        assertEquals(false, system.display.matrix[11][6])
        assertEquals(true, system.display.matrix[12][6])
        assertEquals(false, system.display.matrix[13][6]) // Overlap → OFF
        assertEquals(true, system.display.matrix[14][6])

        assertEquals(6, pixelsOn)

        // VF should be 1
        assertEquals(1, system.cpu.registers[0xF])
    }

    @Test
    fun `given Dxyn when sprite exceeds screen boundaries then it wraps correctly`() {

        // Setup
        system.cpu.setIndexRegister(0x300)

        system.memory[0x300] = 0b11110000.toByte()
        system.memory[0x301] = 0b00001111.toByte()

        val displayWidth = system.cpu.systemInterface.getFrameBuffer().width
        val displayHeight = system.cpu.systemInterface.getFrameBuffer().height

        val x = displayWidth - 2
        val y = displayHeight - 1
        val nibblex = Nibble(x)
        val nibbley = Nibble(y)
        val n = 2

        system.cpu.setRegisterData(0, Pair(Nibble(0), nibblex))   // Vx
        system.cpu.setRegisterData(0, Pair(Nibble(0), nibbley))   // Vy

        // Create Dxyn opcode
        val retOpCode = System.OpCode(Nibble(0xD), nibblex, nibbley, Nibble(n))
        system.cpu.execute(retOpCode)

        val display = system.display.matrix

        // Row 1
        assertEquals(true, display[0][0])
        assertEquals(true, display[1][0])
        assertEquals(true, display[2][0])
        assertEquals(true, display[3][0])

        //Row 2
        assertEquals(true, display[4][1])
        assertEquals(true, display[5][1])
        assertEquals(true, display[6][1])
        assertEquals(true, display[7][1])

        // VF should be 0 (no collision)
        assertEquals(0, system.cpu.registers[0xF])
    }

    @Test
    fun `given Ex9E when key at Vx is pressed then programCounter should be increased`() {

        val pos = Nibble(0)
        val existingData = 0.toByte().toNibbles()
        system.cpu.setRegisterData(0, existingData)

        system.keyboard = object : KeyboardInterface {
            override fun getKeyAt(pos: Int): Boolean {
                if(pos == 0){
                    return true
                } else {
                    return false
                }
            }
        }

        val retOpCode = System.OpCode(Nibble(0xE), pos ,Nibble(0x9), Nibble(0xE))
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT + 2, system.cpu.getProgramCounter())
    }

    @Test
    fun `given Ex9E when key at Vx is not pressed then programCounter should not be increased`() {

        val pos = Nibble(0)
        val existingData = 0.toByte().toNibbles()
        system.cpu.setRegisterData(0, existingData)

        system.keyboard = object : KeyboardInterface {
            override fun getKeyAt(pos: Int): Boolean {
                return false
            }
        }

        val retOpCode = System.OpCode(Nibble(0xE), pos ,Nibble(0x9), Nibble(0xE))
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT, system.cpu.getProgramCounter())
    }

    @Test
    fun `given ExA1 when key at Vx is pressed then programCounter should not be increased`() {

        val pos = Nibble(0)
        val existingData = 0.toByte().toNibbles()
        system.cpu.setRegisterData(0, existingData)

        system.keyboard = object : KeyboardInterface {
            override fun getKeyAt(pos: Int): Boolean {
                if(pos == 0){
                    return true
                } else {
                    return false
                }
            }
        }

        val retOpCode = System.OpCode(Nibble(0xE), pos ,Nibble(0xA), Nibble(0x1))
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT, system.cpu.getProgramCounter())
    }

    @Test
    fun `given ExA1 when key at Vx is not pressed then programCounter should be increased`() {

        val pos = Nibble(0)
        val existingData = 0.toByte().toNibbles()
        system.cpu.setRegisterData(0, existingData)

        system.keyboard = object : KeyboardInterface {
            override fun getKeyAt(pos: Int): Boolean {

                    return false

            }
        }

        val retOpCode = System.OpCode(Nibble(0xE), pos ,Nibble(0xA), Nibble(0x1))
        system.cpu.execute(retOpCode)
        assertEquals(Config.PROGRAM_COUNTER_INIT + 2, system.cpu.getProgramCounter())
    }

    @Test
    fun `given Fx15 when Vx == 100 then delay timer should be set to 100`() {

        val existingData = 100.toByte().toNibbles()
        val pos = Nibble(0x0)
        system.cpu.setRegisterData(pos.value, existingData)

        val retOpCode = System.OpCode(Nibble(0xF), pos ,Nibble(0x1), Nibble(0x5))
        system.cpu.execute(retOpCode)
        assertEquals(100, system.timer.getDelayTimer())
    }

    @Test
    fun `given Fx07 when Vx == 100 then sound timer should be set to 100`() {

        val existingData = 100.toByte().toNibbles()
        val pos = Nibble(0x0)
        system.cpu.setRegisterData(pos.value, existingData)

        val retOpCode = System.OpCode(Nibble(0xF), pos ,Nibble(0x1), Nibble(0x8))
        system.cpu.execute(retOpCode)
        assertEquals(100, system.timer.getSoundTimer())
    }

    @Test
    fun `given Fx1E when Vx == 100 then indexRegister should be (indexRegister + Vx)`() {

        val existingData = 100.toByte().toNibbles()
        val pos = Nibble(0x0)
        system.cpu.setRegisterData(pos.value, existingData)

        system.cpu.setIndexRegister(5)
        val indexRegisterValue = system.cpu.getIndexRegister()
        val retOpCode = System.OpCode(Nibble(0xF), pos ,Nibble(0x1), Nibble(0xE))
        system.cpu.execute(retOpCode)
        assertEquals(indexRegisterValue + 100, system.cpu.getIndexRegister())
    }


}