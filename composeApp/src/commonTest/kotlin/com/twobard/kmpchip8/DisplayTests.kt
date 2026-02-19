package com.twobard.kmpchip8

import com.twobard.kmpchip8.hardware.Display
import com.twobard.kmpchip8.hardware.FrameBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayTests {

    @Test
    fun `give a filled framebuffer when display sets framebuffer check display contents equals buffer`() {
        val frameBuffer = FrameBuffer()
        val display = Display()
        var buffer = Array(64) { BooleanArray(32) { false } }

        var isOn = false

        frameBuffer.buffer.forEachIndexed{ xIndex, column ->
            column.forEachIndexed { yIndex, value ->
                buffer[xIndex][yIndex] = isOn
                isOn = !isOn
            }
        }

        frameBuffer.buffer = buffer
        display.display(frameBuffer)
        assertEquals(frameBuffer.toString(), display.toString())
    }

}


