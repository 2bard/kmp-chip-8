package com.twobard.kmpchip8.hardware

class Cpu {

    //"a 64-byte stack with 8-bit stack pointer"
    //Am using an Int here to avoid autoboxing - better perfomance
    private val stack = ArrayDeque<Int>(16)
    private var stackPointer = 0x0
    private var programCounter = Config.PROGRAM_COUNTER_INIT

    //Stack
    fun call(address: Int){
        stack.addFirst(programCounter)
        stackPointer++
        programCounter = address
    }

    fun ret(){
        val lastAddress = stack.removeLast()
        stackPointer--
        programCounter = lastAddress
    }

    fun getStackPointer() = stackPointer
    fun getProgramCounter() = programCounter
    fun getFromStack(pos: Int) = stack[pos]
    //End stack



}