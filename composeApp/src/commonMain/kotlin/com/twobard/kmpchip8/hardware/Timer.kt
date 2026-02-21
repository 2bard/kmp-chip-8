package com.twobard.kmpchip8.hardware

import com.twobard.kmpchip8.hardware.Config.Companion.`60HZ_TIMER`
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class Timer {

    private var delayTimer = 0
    private var soundTimer = 0

    /**
     * True while a ticking coroutine is active.
     * This is mostly for introspection; stopping is driven by cancelling [tickJob].
     */
    var running: Boolean = false
        private set

    private var tickJob: Job? = null

    var enableLogging: Boolean = true

    /** Log once per N ticks (60 ticks ~= 1 second). Set to 0 to disable tick logs. */
    var logEveryTicks: Int = 1

    private fun log(msg: String) {
        if (enableLogging) println("Timer->$msg")
    }

    fun startRunning(scope: CoroutineScope) {
        // Don't start a second 60Hz loop.
        if (tickJob?.isActive == true) {
            log("startRunning ignored (already running)")
            return
        }

        running = true
        log("startRunning")
        startTimers(scope)
    }

    fun stopRunning() {
        log("stopRunning")
        running = false
        tickJob?.cancel()
        tickJob = null
    }

    fun setDelayTimer(timer: Int) {
        require(timer in 0..255)
        this.delayTimer = timer
        if (enableLogging) log("setDelayTimer=$timer")
    }

    fun getDelayTimer() = this.delayTimer

    fun setSoundTimer(timer: Int) {
        require(timer in 0..255)
        this.soundTimer = timer
        if (enableLogging) log("setSoundTimer=$timer")
    }

    fun getSoundTimer() = this.soundTimer

    private fun startTimers(scope: CoroutineScope) {
        // Timers shouldn't run on Main; use Default so UI work isn't blocked.
        tickJob = scope.launch(Dispatchers.Default) {
            var ticks = 0
            try {
                log("tick loop started")
                while (isActive && running) {
                    if (delayTimer > 0) delayTimer--
                    if (soundTimer > 0) soundTimer--

                    ticks++
                    if (logEveryTicks > 0 && ticks % logEveryTicks == 0) {
                        log("tick delay=$delayTimer sound=$soundTimer")
                    }

                    delay(`60HZ_TIMER`)
                }
            } catch (e: Exception) {
                log(e.message ?: "unknown timer error")
            }
                finally {
                    running = false
                    log("tick loop stopped")
                }

        }
    }
}