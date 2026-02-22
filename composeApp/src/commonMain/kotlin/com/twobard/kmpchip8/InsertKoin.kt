package com.twobard.kmpchip8

import com.twobard.kmpchip8.viewmodel.Chip8ViewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

val myModule = module {
    single { Chip8ViewModel() }
}

fun insertKoin() = startKoin{
    modules(myModule)
}

