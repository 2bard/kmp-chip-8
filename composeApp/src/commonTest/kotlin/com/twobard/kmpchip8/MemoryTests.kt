package com.twobard.kmpchip8


import com.twobard.kmpchip8.hardware.Memory
import kotlin.test.Test
import kotlin.test.assertEquals

class MemoryTests {

    @Test
    fun `give some value when set value via operator then get value via operator should be equal`() {
        val memory = Memory()
        val newValue = 100
        memory[0] = newValue
        assertEquals(100, memory[0])
    }

    @Test
    fun `given unsigned integer when converting from kotlin byte then result should be unsigned`() {
        val unsignedInt = 255
        assertEquals(255, unsignedInt)
    }

    @Test
    fun `given Memory when filled with newValue then all values should equal newValue`(){
        val memory = Memory()
        val newValue = 255
        memory.fill(newValue)

        memory.getAll().forEach {
            assertEquals(255, it)
        }
    }
}


