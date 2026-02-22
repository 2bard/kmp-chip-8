package com.twobard.kmpchip8.viewmodel

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Chip8ViewModel : ScreenModel {

    private val _loadedRom: MutableStateFlow<String?> = MutableStateFlow(null)
    val loadedRom: StateFlow<String?> = _loadedRom

    companion object {
        val roms = listOf(
            "blinky.ch8",
            "2-ibm-logo.ch8",
            "4-flags.ch8",
            "ibm_new.ch8",
            "1-chip8-logo.ch8",
            "delaytimertest.ch8",
            "octojam1title.ch8",
            "octojam3title.ch8",
            "octojam4title.ch8",
            "octojam5title.ch8",
            "octojam6title.ch8",
            "octojam8title.ch8",
            "octojam10title.ch8",
            "octoachip8story.ch8",
            "ibm.ch8",
            "randomnumbertest",
            "test_opcode.ch8"
        )
    }

    fun selectRom(str: String) {
        _loadedRom.value = str
    }

    fun getRoms() = Chip8ViewModel.roms
}