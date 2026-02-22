package com.twobard.kmpchip8.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Keyboard(onKeyPressed: (Int) -> Unit) {
    // 4x4 keypad layout
    val keys: List<List<Pair<Int, String>>> = listOf(
        listOf(1 to "1", 2 to "2", 3 to "3", 12 to "C"),
        listOf(4 to "4", 5 to "5", 6 to "6", 13 to "D"),
        listOf(7 to "7", 8 to "8", 9 to "9", 14 to "E"),
        listOf(10 to "A", 0 to "0", 11 to "B", 15 to "F")
    )

    // Track focused key coordinates (row, col)
    var focusedRow by remember { mutableStateOf(0) }
    var focusedCol by remember { mutableStateOf(0) }

    // Focus requester for the keypad container
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionUp -> focusedRow = (focusedRow - 1).coerceAtLeast(0)
                        Key.DirectionDown -> focusedRow = (focusedRow + 1).coerceAtMost(3)
                        Key.DirectionLeft -> focusedCol = (focusedCol - 1).coerceAtLeast(0)
                        Key.DirectionRight -> focusedCol = (focusedCol + 1).coerceAtMost(3)
                        Key.Enter, Key.Spacebar -> {
                            val (num, label) = keys[focusedRow][focusedCol]
                            println("Key pressed: $num ($label)")
                            onKeyPressed(num)
                        }

                        else -> {}
                    }
                    true
                } else {
                    false
                }
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keys.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { colIndex, (num, label) ->
                    val isFocused = rowIndex == focusedRow && colIndex == focusedCol
                    KeyButton(
                        text = label,
                        value = num,
                        focused = isFocused,
                        onClick = {
                            println("Clicked: $num ($label)")
                            onKeyPressed(num)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KeyButton(text: String, value: Int, focused: Boolean, onClick: (Int) -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .size(64.dp)
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) Color.Blue else Color.Gray,
                shape = RoundedCornerShape(4.dp)
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        println("Pointer DOWN at ${down.position}")
                        onClick.invoke(value)

                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            println("Pointer UP at ${down.position}")
                            onClick.invoke(value)
                        } else {
                            println("Pointer UP at ${down.position}")
                            onClick.invoke(value)
                        }
                    }
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = text, fontSize = 24.sp)
        }
    }
}