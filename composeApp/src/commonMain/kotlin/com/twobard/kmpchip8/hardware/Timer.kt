package com.twobard.kmpchip8.hardware

import com.twobard.kmpchip8.hardware.Config.Companion.`60HZ_TIMER`
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.compareTo

class Timer {

    private var delayTimer = 0
    private var soundTimer = 0

    var running = false

    fun startRunning(){
        running = true
    }

    fun stopRunning(){
        running = false
    }

    fun setDelayTimer(timer: Int){
        this.delayTimer = timer
    }

    fun getDelayTimer() = this.delayTimer

    fun setSoundTimer(timer: Int){
        this.soundTimer = timer
    }

    fun getSoundTimer() = this.soundTimer

    fun startTimers(scope: CoroutineScope) {
        scope.launch {
            while (running) {
                if (delayTimer > 0) delayTimer--
                if (soundTimer > 0) soundTimer--

                println("Timers. Delay: $delayTimer Sound:$soundTimer")
                delay(`60HZ_TIMER`)
            }
        }
    }
}