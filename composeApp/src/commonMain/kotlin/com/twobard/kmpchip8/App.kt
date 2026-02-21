package com.twobard.kmpchip8

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.tooling.preview.Preview
import com.twobard.kmpchip8.ui.DisplayUI

@Composable
@Preview
fun App(frame: Int, display: Array<BooleanArray>, onKeyPressed: (Int) -> Unit) {
    DisplayUI(display = display, onKeyPressed)
}