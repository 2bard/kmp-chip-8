package com.twobard.kmpchip8

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.twobard.kmpchip8.ui.DisplayUI

@Composable
@Preview
fun App(frame: Int, display: Array<BooleanArray>) {
    DisplayUI(display = display)
}