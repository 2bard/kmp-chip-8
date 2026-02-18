package com.twobard.kmpchip8

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform