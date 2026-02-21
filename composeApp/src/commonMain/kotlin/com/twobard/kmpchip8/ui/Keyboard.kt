package com.twobard.kmpchip8.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Keyboard(onKeyPressed: (Int) -> Unit) {
    // 4x4 keypad layout
    val keys = listOf(
        listOf(1, 2, 3, 4),
        listOf(5, 6, 7, 8),
        listOf(9, 10, 11, 12),
        listOf(13, 0, 15, 19)
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
                            val value = keys[focusedRow][focusedCol]
                            println("Key pressed: $value")
                            onKeyPressed(value)
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
                row.forEachIndexed { colIndex, value ->
                    val isFocused = rowIndex == focusedRow && colIndex == focusedCol
                    KeyButton(
                        text = value.toString(),
                        value = value,
                        focused = isFocused,
                        onClick = {
                            println("Clicked: $value")
                            onKeyPressed(value)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KeyButton(text: String, value: Int, focused: Boolean, onClick: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .size(64.dp)
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) Color.Blue else Color.Gray,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick(value) },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = text, fontSize = 24.sp)
        }
    }
}