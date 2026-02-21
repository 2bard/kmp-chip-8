package com.twobard.kmpchip8

import com.twobard.kmpchip8.hardware.Nibble
import com.twobard.kmpchip8.hardware.combineNibbles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CombineNibblesTests {

    @Test
    fun `combineNibbles of three nibbles constructs a 12-bit address`() {
        val address = combineNibbles(Nibble(0xA), Nibble(0x3), Nibble(0x7))
        assertEquals(0xA37, address)
    }

    @Test
    fun `combineNibbles allows full 0xFFF`() {
        val address = combineNibbles(Nibble(0xF), Nibble(0xF), Nibble(0xF))
        assertEquals(0xFFF, address)
    }

    @Test
    fun `combineNibbles of two nibbles constructs kk`() {
        val kk = combineNibbles(Nibble(0xF), Nibble(0x0))
        assertEquals(0xF0, kk)
    }

    @Test
    fun `combineNibbles throws when no args provided`() {
        assertFailsWith<IllegalArgumentException> {
            combineNibbles()
        }
    }
}
