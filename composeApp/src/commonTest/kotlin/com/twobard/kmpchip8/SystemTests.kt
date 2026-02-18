package com.twobard.kmpchip8

import com.twobard.kmpchip8.hardware.Config
import com.twobard.kmpchip8.hardware.System
import kotlin.test.Test
import kotlin.test.assertEquals

class SystemTests {

    @Test
    fun `given intial system state when init then font should be loaded`() {
        val defaultFont = Config.DEFAULT_FONT
        val system = System()

        system.memory.getAll().subList(0, defaultFont.component1().size).forEachIndexed{ index, sprite ->
            assertEquals(defaultFont.component1()[index], sprite)
        }
    }
}