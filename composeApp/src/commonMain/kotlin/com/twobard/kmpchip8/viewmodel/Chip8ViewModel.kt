package com.twobard.kmpchip8.viewmodel

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.twobard.kmpchip8.hardware.System
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class Chip8ViewModel : ScreenModel {

    private val _loadedRom: MutableStateFlow<String?> = MutableStateFlow(null)
    val loadedRom: StateFlow<String?> = _loadedRom

    private val _system: MutableStateFlow<System?> =
        MutableStateFlow(System(strictMode = false, logging = false))
    val system: StateFlow<System?> = _system

    private val _errors: MutableSharedFlow<String?> = MutableSharedFlow()
    val errors: SharedFlow<String?> = _errors

    companion object {
        val roms = listOf(
            "blinky.ch8",
            "clock.ch8",
            "2-ibm-logo.ch8",
            "4-flags.ch8",
            "ibm.ch8",
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
            "randomnumbertest.ch8",
            "test_opcode.ch8"
        ).sorted()
    }

    fun selectRom(str: String) {
        _loadedRom.value = str

        screenModelScope.launch(Dispatchers.Default) {
            try {
                _system.value = System(false, false)
                system.value?.startGame(str)

            } catch (e: Exception) {

                _errors.emit(e.message ?: e.toString())

            }
        }
    }

    fun getRoms() = Chip8ViewModel.roms
}