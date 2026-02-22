package com.twobard.kmpchip8

import com.twobard.kmpchip8.Utils.Companion.toNibbles
import com.twobard.kmpchip8.hardware.Config
import com.twobard.kmpchip8.hardware.KeyboardInterface
import com.twobard.kmpchip8.hardware.Nibble
import com.twobard.kmpchip8.hardware.System
import com.twobard.kmpchip8.hardware.combineNibbles
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.atMost
import kotlinx.coroutines.test.runTest
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
    fun `given a Load opcode when executed then register should have new value at correct destination`() = runTest {

        val destination = 0xA
        val value = 0x05.toNibbles()
        val loadOpCode = System.OpCode(Nibble(0x6), Nibble(destination), value.first, value.second)
        system.cpu.execute(loadOpCode)

        assertEquals(0x05, system.cpu.getRegisterValue(destination))

    }

    @Test
    fun `given a CLS opcode when executed then register should have new value at correct destination`() = runTest {
        val loadOpCode = System.OpCode(Nibble(0x0), Nibble(0x0), Nibble(0xE), Nibble(0x0))
        system.cpu.execute(loadOpCode)

        verify(atMost(1)) {
            system.display.clear()
        }
    }

    @Test
    fun `given a JMP opcode when executed then register should have new value at correct destination`() = runTest {
        // Jump (1nnn) sets PC = nnn. PC must remain even (2-byte opcodes).
        val addressPt1 = Nibble(0xA)
        val addressPt2 = Nibble(0x3)
        val addressPt3 =  Nibble(0x6) // 0xA36 (even)
        val loadOpCode = System.OpCode(Nibble(0x1), addressPt1 ,addressPt2, addressPt3)
        system.cpu.execute(loadOpCode)

        verify(atMost(1)) {
            system.cpu.jump(addressPt1, addressPt2, addressPt3)
        }

        assertEquals(0xA36, system.cpu.getProgramCounter())
    }

    @Test
    fun `given a RET opcode when executed then programCounter and stackPointer are restored`() = runTest {

        val initialPc = system.cpu.getProgramCounter()

        system.cpu.call(0x300)
        val pcAfterFirstCall = system.cpu.getProgramCounter()

        system.cpu.call(0x400)

        // Sanity: we should now be at the last called address with 2 items on the stack.
        assertEquals(0x400, system.cpu.getProgramCounter())
        assertEquals(2, system.cpu.getStackPointer())

        // Act: execute RET (00EE)
        val retOpCode = System.OpCode(Nibble(0x0), Nibble(0x0) ,Nibble(0xE), Nibble(0xE))
        system.cpu.execute(retOpCode)

        // Assert: returned to the previous call site and stackPointer decremented.
        // Since call() pushes the current PC then sets PC=address, the return address after the second call is
        // the PC value at the time of the second call: pcAfterFirstCall.
        assertEquals(pcAfterFirstCall, system.cpu.getProgramCounter())
        assertEquals(1, system.cpu.getStackPointer())

        // Act again: RET should bring us back to where we started.
        system.cpu.execute(retOpCode)
        assertEquals(initialPc, system.cpu.getProgramCounter())
        assertEquals(0, system.cpu.getStackPointer())
    }

    @Test
    fun `given a CALL opcode when executed then programCounter and Stack should be in correct state`() = runTest {

        // call nnn (2nnn) pushes the current PC (call site) on the stack and sets PC to nnn.
        // PC must remain even (2-byte opcodes), so use an even nnn.
        val addressPt1 = Nibble(0x2)
        val addressPt2 = Nibble(0x3)
        val addressPt3 =  Nibble(0x6) // 0x236

        val callOpCode = System.OpCode(Nibble(0x2), addressPt1 ,addressPt2, addressPt3)
        system.cpu.execute(callOpCode)

        //Check programCounter
        assertEquals(combineNibbles(addressPt1, addressPt2, addressPt3), system.cpu.getProgramCounter() + 2)

        //Check stackPointer (treated as depth)
        assertEquals(1, system.cpu.getStackPointer())

        //Check stack top: call() uses addFirst(), so element 0 is the most recent return address
        assertEquals(Config.PROGRAM_COUNTER_INIT, system.cpu.getFromStack(0))
    }

    @Test
    fun `given 3xkk when Vx == kk then increment program counter by 2`() = runTest {

        val Vx = Nibble(0xA)
        val kk = 255.toNibbles()

        //set V(0xA) to 255

        system.cpu.setRegisterData(Vx.value, kk)

        val retOpCode = System.OpCode(Nibble(0x3), Vx ,kk.first, kk.second)
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT + 2, system.cpu.getProgramCounter())
    }

    @Test
    fun `given 4xkk when Vx == kk then program counter stays the same`() = runTest {

        val Vx = Nibble(0xA)
        val kk = 255.toNibbles()

        //set V(0xA) to 255

        system.cpu.setRegisterData(Vx.value, 255.toNibbles())

        val retOpCode = System.OpCode(Nibble(0x4), Vx ,kk.first, kk.second)
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT, system.cpu.getProgramCounter())
    }

    @Test
    fun `given 4xkk when Vx !== kk then program counter stays the same`() = runTest {

        val Vx = Nibble(0xA)
        val kk = 255.toNibbles()

        //set V(0xA) to 255

        system.cpu.setRegisterData(Vx.value, 254.toNibbles())

        val retOpCode = System.OpCode(Nibble(0x4), Vx ,kk.first, kk.second)
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT + 2, system.cpu.getProgramCounter())
    }

    @Test
    fun `given 5xy0 when Vx == Vy then program counter increments`() = runTest {

        val value = 255.toNibbles()
        val x = Nibble(0)
        val y = Nibble(1)

        system.cpu.setRegisterData(x.value, value)
        system.cpu.setRegisterData(y.value, value)

        val retOpCode = System.OpCode(Nibble(0x5), x ,y, Nibble(0))
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT + 2, system.cpu.getProgramCounter())
    }

    @Test
    fun `given 5xy0 when Vx !== Vy then program counter stays the same`() = runTest {

        val valueA = 255.toNibbles()
        val valueB = 254.toNibbles()
        val x = Nibble(0)
        val y = Nibble(1)

        system.cpu.setRegisterData(x.value, valueA)
        system.cpu.setRegisterData(y.value, valueB)

        val retOpCode = System.OpCode(Nibble(0x5), x ,y, Nibble(0))
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT, system.cpu.getProgramCounter())
    }

    @Test
    fun `given 7xkk when executed then Vx should equal Vx + kk`() = runTest {

        val pos = Nibble(0)
        val existingData = 10.toNibbles()
        val data = 20.toNibbles()

        system.cpu.setRegisterData(pos.value, existingData)

        val retOpCode = System.OpCode(Nibble(0x7), pos ,data.first, data.second)
        system.cpu.execute(retOpCode)

        assertEquals(30, system.cpu.getRegisterValue(pos.value))
    }

    @Test
    fun `given 8xy0 when executed then Vx should equal Vy`() = runTest {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 10.toNibbles()
        val yData = 20.toNibbles()

        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x0))
        system.cpu.execute(retOpCode)

        assertEquals(20, system.cpu.getRegisterValue(x.value))
    }

    @Test
    fun `given 8xy1 when x is 170 and y is 85 then Vx is 255`() = runTest {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 170.toNibbles()
        val yData = 85.toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)


        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x1))
        system.cpu.execute(retOpCode)

        assertEquals(255, system.cpu.getRegisterValue(x.value))
    }

    @Test
    fun `given 8xy1 when x is 255 and y is 128 then Vx is 255`() = runTest {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 255.toNibbles()
        val yData = 128.toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x1))
        system.cpu.execute(retOpCode)

        assertEquals(255, system.cpu.getRegisterValue(x.value))
    }

    @Test
    fun `given 8xy1 when x is 0 and y is 0 then Vx is 0`() = runTest {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 0.toNibbles()
        val yData = 0.toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x1))
        system.cpu.execute(retOpCode)

        assertEquals(0, system.cpu.getRegisterValue(x.value))
    }

    @Test
    fun `given 8xy2 when x is 51 and y is 85 then Vx is 17`() = runTest {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 51.toNibbles()
        val yData = 85.toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x2))
        system.cpu.execute(retOpCode)

        //51 AND 85 = 17
        assertEquals(17, system.cpu.getRegisterValue(x.value))
    }

    @Test
    fun `given 8xy2 when x is 255 and y is 128 then Vx is 128`() = runTest {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 255.toNibbles()
        val yData = 128.toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x2))
        system.cpu.execute(retOpCode)

        //288 AND 128 = 128
        assertEquals(128, system.cpu.getRegisterValue(x.value))
    }

    @Test
    fun `given 8xy3 when x is 255 and y is 128 then Vx is 127`() = runTest {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 255.toNibbles()
        val yData = 128.toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x3))
        system.cpu.execute(retOpCode)

        //51 AND 85 = 17
        assertEquals(127, system.cpu.getRegisterValue(x.value))
    }

    @Test
    fun `given 8xy3 when x is 255 and y is 0 then Vx is 255`() = runTest {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 255.toNibbles()
        val yData = 0.toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x3))
        system.cpu.execute(retOpCode)

        //255 AND 0 = 255
        assertEquals(255, system.cpu.getRegisterValue(x.value))
    }

    @Test
    fun `given 8xy3 when x is 51 and y is 85 then Vx is 102`() = runTest {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 51.toNibbles()
        val yData = 85.toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x3))
        system.cpu.execute(retOpCode)

        //51 AND 85 = 102
        assertEquals(102, system.cpu.getRegisterValue(x.value))
    }

    @Test
    fun `given 8xy4 when x is 0 and y is 1 then Vx is 1`() = runTest {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 0.toNibbles()
        val yData = 1.toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x4))
        system.cpu.execute(retOpCode)

        //0 ADD 1 = 1
        assertEquals(1, system.cpu.getRegisterValue(x.value))

        //Carry is 0
        assertEquals(0, system.cpu.getRegisterValue(0xf))
    }

    @Test
    fun `given 8xy4 when x is 128 and y is 128 then Vx is 0 and carry is 1`() = runTest {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 128.toNibbles()
        val yData = 128.toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x4))
        system.cpu.execute(retOpCode)

        //128 + 128 = 256 -> Vx = 0 (low 8 bits)
        assertEquals(0, system.cpu.getRegisterValue(x.value))

        //Carry is 1 because sum > 255
        assertEquals(1, system.cpu.getRegisterValue(0xf))
    }

    @Test
    fun `given 8xy5 when x is 10 and y is 5 then Vx is 5 and VF is 1`() = runTest {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 10.toNibbles()
        val yData = 5.toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x5))
        system.cpu.execute(retOpCode)

        //10 SUB 5 = 5 (no borrow)
        assertEquals(5, system.cpu.getRegisterValue(x.value))

        //no borrow
        assertEquals(1, system.cpu.getRegisterValue(0xf))
    }

    @Test
    fun `given 8xy5 when x is 5 and y is 10 then Vx is 251 and VF is 0`() = runTest {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 5.toNibbles()
        val yData = 10.toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x5))
        system.cpu.execute(retOpCode)

        //5 SUB 10 = 251 (borrow)
        assertEquals(251, system.cpu.getRegisterValue(x.value))

        //borrow
        assertEquals(0, system.cpu.getRegisterValue(0xf))
    }

    @Test
    fun `given 8xy6 when x is 7 and then Vx is 3 and VF is 1`() = runTest {

        val x = Nibble(0)
        val y = Nibble(0)
        val xData = 7.toNibbles()

        system.cpu.setRegisterData(x.value, xData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x6))
        system.cpu.execute(retOpCode)

        //7 shr 2 = 3 (divided by two)
        assertEquals(3, system.cpu.getRegisterValue(x.value))

        // VF = LSB of Vx
        assertEquals(1, system.cpu.getRegisterValue(0xf))
    }

    @Test
    fun `given 8xy6 when x is 10 and then Vx is 5 and VF is 0`() = runTest {

        val x = Nibble(0)
        val y = Nibble(0)
        val xData = 10.toNibbles()

        system.cpu.setRegisterData(x.value, xData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x6))
        system.cpu.execute(retOpCode)

        //10 shr 2 = 5 (divided by two)
        assertEquals(5, system.cpu.getRegisterValue(x.value))

        // VF = LSB of Vx
        assertEquals(0, system.cpu.getRegisterValue(0xf))
    }

    @Test
    fun `given 8xy7 when x is 5 and y is 10 then Vx is 5 and VF is 1`() = runTest {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 5.toNibbles()
        val yData = 10.toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x7))
        system.cpu.execute(retOpCode)

        //10 - 5 = 5
        assertEquals(5, system.cpu.getRegisterValue(x.value))

        // VF = 1 if Vy >= Vx (no borrow), else 0
        assertEquals(1, system.cpu.getRegisterValue(0xf))
    }

    @Test
    fun `given 8xy7 when x is 10 and y is 5 then Vx is 251 and VF is 0`() = runTest {

        val x = Nibble(0)
        val y = Nibble(1)
        val xData = 10.toNibbles()
        val yData = 5.toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,y, Nibble(0x7))
        system.cpu.execute(retOpCode)

        //5 - 10 = 251 (with wraparound)
        assertEquals(251, system.cpu.getRegisterValue(x.value))

        // VF = 0 becayse Vy < Vx
        assertEquals(0, system.cpu.getRegisterValue(0xf))
    }

    @Test
    fun `given 8xyE when x is 10 then result Vx is 20 and VF is 0`() = runTest {

        val x = Nibble(0)
        val xData = 10.toNibbles()

        system.cpu.setRegisterData(x.value, xData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,Nibble(0x0), Nibble(0xE))
        system.cpu.execute(retOpCode)

        //10 x 2 = 20
        assertEquals(20, system.cpu.getRegisterValue(x.value))

        // VF = 0 (no overflow)
        assertEquals(0, system.cpu.getRegisterValue(0xf))
    }

    @Test
    fun `given 8xyE when x is 200 then result Vx is 144 and VF is 1`() = runTest {

        val x = Nibble(0)
        val xData = 200.toNibbles()

        system.cpu.setRegisterData(x.value, xData)

        val retOpCode = System.OpCode(Nibble(0x8), x ,Nibble(0xE), Nibble(0xE))
        system.cpu.execute(retOpCode)

        //200 x 2 = 400. 400 % 256 = 144
        assertEquals(144, system.cpu.getRegisterValue(x.value))

        // VF = 1 (overflows)
        assertEquals(1, system.cpu.getRegisterValue(0xf))
    }

    @Test
    fun `given 9xy0 when Vx != Vy then increment program counter`() = runTest {

        val x = Nibble(0)
        val xData = 200.toNibbles()
        val y = Nibble(1)
        val yData = 201.toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x9), x ,y, Nibble(0x0))
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT + 2, system.cpu.getProgramCounter())
    }

    @Test
    fun `given 9xy0 when Vx == Vy then do not increment program counter`() = runTest {

        val x = Nibble(0)
        val xData = 200.toNibbles()
        val y = Nibble(1)
        val yData = 200.toNibbles()

        system.cpu.setRegisterData(x.value, xData)
        system.cpu.setRegisterData(y.value, yData)

        val retOpCode = System.OpCode(Nibble(0x9), x ,y, Nibble(0x0))
        system.cpu.execute(retOpCode)


        assertEquals(Config.PROGRAM_COUNTER_INIT, system.cpu.getProgramCounter())
    }

    @Test
    fun `given Annn when nnn == 6 then indexRegister == 6`() = runTest {

        val n1 = Nibble(0x1)
        val n2 = Nibble(0x2)
        val n3 = Nibble(0x3)

        val retOpCode = System.OpCode(Nibble(0xA), n1 ,n2, n3)
        system.cpu.execute(retOpCode)

        assertEquals(0x123, system.cpu.getIndexRegister())
    }

    @Test
    fun `given Bnnn when nnn == 0x206 and V0 == 6 then programCounter == 0x20C`() = runTest {

        // Bnnn jumps to nnn + V0. PC must remain even (2-byte opcodes) and in 0x200..0xFFE.
        val n1 = Nibble(0x2)
        val n2 = Nibble(0x0)
        val n3 = Nibble(0x6)

        system.cpu.setRegisterData(0, 6.toNibbles())

        val opcode = System.OpCode(Nibble(0xB), n1, n2, n3)
        system.cpu.execute(opcode)

        assertEquals(0x20C, system.cpu.getProgramCounter())
    }

    @Test
    fun `given Cxkk when x == 7 and kk == F0 and random == 0xF3 then Vx == F0`() = runTest {

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

        assertEquals(0xF0, system.cpu.getRegisterValue(n1.value))
    }

    @Test
    fun `given Cxkk when x == 7 and kk == FF and random == 0x12 then Vx == 0xF  `() = runTest {

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

        assertEquals(0x12, system.cpu.getRegisterValue(n1.value))
    }

    @Test
    fun `given Dxyn when Vx == 10 Vy == 5 and n == 5 then 4 pixels should turn on`() = runTest {

        system.cpu.setIndexRegister(0x300)
        system.memory[0x300] = combineNibbles(Nibble(0xF), Nibble(0x0))

        val x = 10
        val nibblex = Nibble(x)
        val y = 5
        val nibbley = Nibble(y)
        val n = 1 //1 bite sprite

        system.cpu.setRegisterData(x, Pair(Nibble(0), nibblex))
        system.cpu.setRegisterData(y, Pair(Nibble(0), nibbley))


        val retOpCode = System.OpCode(Nibble(0xD), nibblex , nibbley, Nibble(n))
        system.cpu.execute(retOpCode)

        assertEquals(0, system.cpu.getRegisterValue(0xF))

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
    fun `given Dxyn when V1 == 10 V2 == 5 and n == 5 then 3 pixels should turn on and collision == 1`() = runTest {

        system.cpu.setIndexRegister(0x300)
        system.memory[0x300] = combineNibbles(Nibble(0xF), Nibble(0x0))

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
        assertEquals(1,system.cpu.getRegisterValue(0xF))
    }

    @Test
    fun `given Dxyn when Vx == 10 Vy == 5 and n == 2 then pixels should turn on with collision`() = runTest {

        system.cpu.setIndexRegister(0x300)

        system.memory[0x300] = 0b11110000
        system.memory[0x301] = 0b00111100

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
        assertEquals(1, system.cpu.getRegisterValue(0xF))
    }

    @Test
    fun `given Dxyn when sprite exceeds screen boundaries then it wraps correctly`() = runTest {
        // Setup
        system.cpu.setIndexRegister(0x300)
        system.memory[0x300] = 0b11110000
        system.memory[0x301] = 0b00001111

        val displayWidth = system.cpu.systemInterface.getFrameBuffer().width
        val displayHeight = system.cpu.systemInterface.getFrameBuffer().height

        val vx = displayWidth - 2 // 62
        val vy = displayHeight - 1 // 31

        // Use separate registers for Vx and Vy
        val vxRegister = 0
        val vyRegister = 1
        system.cpu.setRegisterData(vxRegister, vx.toNibbles())   // Vx
        system.cpu.setRegisterData(vyRegister, vy.toNibbles())   // Vy

        // Create Dxyn opcode (Dxy2)
        val retOpCode = System.OpCode(Nibble(0xD), Nibble(vxRegister), Nibble(vyRegister), Nibble(2))
        system.cpu.execute(retOpCode)

        val display = system.display.matrix

        // Row 0 (sprite byte 11110000) at y=31:
        // bits 0..3 are 1 => x = 62,63,0,1
        assertEquals(true, display[62][31])
        assertEquals(true, display[63][31])
        assertEquals(true, display[0][31])
        assertEquals(true, display[1][31])

        // Row 1 (sprite byte 00001111) wraps vertically to y=0:
        // bits 4..7 are 1 => x = 2,3,4,5
        assertEquals(true, display[2][0])
        assertEquals(true, display[3][0])
        assertEquals(true, display[4][0])
        assertEquals(true, display[5][0])

        // VF should be 0 (no collision)
        assertEquals(0, system.cpu.getRegisterValue(0xF))
    }



    @Test
    fun `given Ex9E when key at Vx is pressed then programCounter should be increased`() = runTest {

        val pos = Nibble(0)
        val existingData = 0.toNibbles()
        system.cpu.setRegisterData(0, existingData)

        system.keyboard = object : KeyboardInterface {
            override fun getKeyAt(pos: Int): Boolean {
                if(pos == 0){
                    return true
                } else {
                    return false
                }
            }

            override fun getPressedKey(): Int? {
                return null
            }

            override fun pressKeyAt(pos: Int) {
                TODO("Not yet implemented")
            }
        }

        val retOpCode = System.OpCode(Nibble(0xE), pos ,Nibble(0x9), Nibble(0xE))
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT + 2, system.cpu.getProgramCounter())
    }

    @Test
    fun `given Ex9E when key at Vx is not pressed then programCounter should not be increased`() = runTest {

        val pos = Nibble(0)
        val existingData = 0.toNibbles()
        system.cpu.setRegisterData(0, existingData)

        system.keyboard = object : KeyboardInterface {
            override fun getKeyAt(pos: Int): Boolean {
                return false
            }

            override fun getPressedKey(): Int? {
                return null
            }

            override fun pressKeyAt(pos: Int) {

            }
        }

        val retOpCode = System.OpCode(Nibble(0xE), pos ,Nibble(0x9), Nibble(0xE))
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT, system.cpu.getProgramCounter())
    }

    @Test
    fun `given ExA1 when key at Vx is pressed then programCounter should not be increased`() = runTest {

        val pos = Nibble(0)
        val existingData = 0.toNibbles()
        system.cpu.setRegisterData(0, existingData)

        system.keyboard = object : KeyboardInterface {
            override fun getKeyAt(pos: Int): Boolean {
                if(pos == 0){
                    return true
                } else {
                    return false
                }
            }

            override fun getPressedKey(): Int? {
                return null
            }

            override fun pressKeyAt(pos: Int) {
                TODO("Not yet implemented")
            }
        }

        val retOpCode = System.OpCode(Nibble(0xE), pos ,Nibble(0xA), Nibble(0x1))
        system.cpu.execute(retOpCode)

        assertEquals(Config.PROGRAM_COUNTER_INIT, system.cpu.getProgramCounter())
    }

    @Test
    fun `given ExA1 when key at Vx is not pressed then programCounter should be increased`() = runTest {

        val pos = Nibble(0)
        val existingData = 0.toNibbles()
        system.cpu.setRegisterData(0, existingData)

        system.keyboard = object : KeyboardInterface {
            override fun getKeyAt(pos: Int): Boolean {

                    return false

            }

            override fun getPressedKey(): Int? {
                return null
            }

            override fun pressKeyAt(pos: Int) {
                TODO("Not yet implemented")
            }
        }

        val retOpCode = System.OpCode(Nibble(0xE), pos ,Nibble(0xA), Nibble(0x1))
        system.cpu.execute(retOpCode)
        assertEquals(Config.PROGRAM_COUNTER_INIT + 2, system.cpu.getProgramCounter())
    }

    @Test
    fun `given Fx15 when Vx == 100 then delay timer should be set to 100`() = runTest {

        val existingData = 100.toNibbles()
        val pos = Nibble(0x0)
        system.cpu.setRegisterData(pos.value, existingData)

        val retOpCode = System.OpCode(Nibble(0xF), pos ,Nibble(0x1), Nibble(0x5))
        system.cpu.execute(retOpCode)
        assertEquals(100, system.timer.getDelayTimer())
    }

    @Test
    fun `given Fx07 when Vx == 100 then sound timer should be set to 100`() = runTest {

        val existingData = 100.toNibbles()
        val pos = Nibble(0x0)
        system.cpu.setRegisterData(pos.value, existingData)

        val retOpCode = System.OpCode(Nibble(0xF), pos ,Nibble(0x1), Nibble(0x8))
        system.cpu.execute(retOpCode)
        assertEquals(100, system.timer.getSoundTimer())
    }

    @Test
    fun `given Fx1E when Vx == 100 then indexRegister should be (indexRegister + Vx)`() = runTest {

        val existingData = 100.toNibbles()
        val pos = Nibble(0x0)
        system.cpu.setRegisterData(pos.value, existingData)

        system.cpu.setIndexRegister(5)
        val indexRegisterValue = system.cpu.getIndexRegister()
        val retOpCode = System.OpCode(Nibble(0xF), pos ,Nibble(0x1), Nibble(0xE))
        system.cpu.execute(retOpCode)
        assertEquals(indexRegisterValue + 100, system.cpu.getIndexRegister())
    }

    @Test
    fun `given Fx29 when Vx == 0x7 then indexRegister should be (indexRegister + Vx)`() = runTest {

        val existingData = 0x7.toNibbles()
        val pos = Nibble(5)
        system.cpu.setRegisterData(pos.value, existingData)

        val retOpCode = System.OpCode(Nibble(0xF), pos ,Nibble(0x2), Nibble(0x9))
        system.cpu.execute(retOpCode)
        assertEquals(35, system.cpu.getIndexRegister())
    }

    @Test
    fun `given Fx33 when Vx = 254 then memory stores 2, 5, 4`() = runTest {

        // Set index register
        system.cpu.setIndexRegister(0x300)

        val vxRegister = 5
        system.cpu.setRegisterValue(vxRegister, 254)
        val retOpCode = System.OpCode(Nibble(0xF), Nibble(vxRegister), Nibble(0x3), Nibble(0x3))
        system.cpu.execute(retOpCode)

        val hundreds = system.memory[0x300].toInt() and 0xFF
        val tens = system.memory[0x301].toInt() and 0xFF
        val ones = system.memory[0x302].toInt() and 0xFF

        assertEquals(2, hundreds)  // 254 / 100 = 2
        assertEquals(5, tens)      // (254 / 10) % 10 = 5
        assertEquals(4, ones)      // 254 % 10 = 4
    }

    @Test
    fun `given Fx55 when V0 to V3 are set then memory stores values and I increments`() = runTest {

        system.cpu.setIndexRegister(0x300)

        system.cpu.setRegisterValue(0,5)
        system.cpu.setRegisterValue(1,10)
        system.cpu.setRegisterValue(2,15)
        system.cpu.setRegisterValue(3,20)

        val xRegister = 3
        val retOpCode = System.OpCode(Nibble(0xF), Nibble(xRegister), Nibble(0x5), Nibble(0x5))
        system.cpu.execute(retOpCode)

        val mem0 = system.memory[0x300].toInt() and 0xFF
        val mem1 = system.memory[0x301].toInt() and 0xFF
        val mem2 = system.memory[0x302].toInt() and 0xFF
        val mem3 = system.memory[0x303].toInt() and 0xFF

        assertEquals(5, mem0)
        assertEquals(10, mem1)
        assertEquals(15, mem2)
        assertEquals(20, mem3)

        assertEquals(0x304, system.cpu.getIndexRegister())
    }

    @Test
    fun `given Fx55 when V0 to V3 are set and I starts at 0 then memory stores values and I increments`() = runTest {

        system.cpu.setIndexRegister(0x0)

        system.cpu.setRegisterValue(0, 3)
        system.cpu.setRegisterValue(1, 6)
        system.cpu.setRegisterValue(2, 9)
        system.cpu.setRegisterValue(3, 12)

        val xRegister = 3
        val retOpCode = System.OpCode(Nibble(0xF), Nibble(xRegister), Nibble(0x5), Nibble(0x5))
        system.cpu.execute(retOpCode)

        val mem0 = system.memory[0x0].toInt() and 0xFF
        val mem1 = system.memory[0x1].toInt() and 0xFF
        val mem2 = system.memory[0x2].toInt() and 0xFF
        val mem3 = system.memory[0x3].toInt() and 0xFF

        assertEquals(3, mem0)
        assertEquals(6, mem1)
        assertEquals(9, mem2)
        assertEquals(12, mem3)

        assertEquals(4, system.cpu.getIndexRegister())
    }

    @Test
    fun `given Fx65 when memory has values then V0 to V3 are loaded and I increments`() = runTest {

        system.cpu.setIndexRegister(0)

        system.memory[0x0] = 5
        system.memory[0x1] = 10
        system.memory[0x2] = 15
        system.memory[0x3] = 20

        val xRegister = 3
        val retOpCode = System.OpCode(Nibble(0xF), Nibble(xRegister), Nibble(0x6), Nibble(0x5))
        system.cpu.execute(retOpCode)

        assertEquals(5, system.cpu.getRegisterValue(0))
        assertEquals(10, system.cpu.getRegisterValue(1))
        assertEquals(15, system.cpu.getRegisterValue(2))
        assertEquals(20, system.cpu.getRegisterValue(3))

        assertEquals(4, system.cpu.getIndexRegister())
    }







}