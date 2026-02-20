package com.twobard.kmpchip8.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.twobard.kmpchip8.hardware.Display
import com.twobard.kmpchip8.hardware.FrameBuffer

@Composable
@Preview
fun DisplayUI(frame: Int, display: Display) {

    println("recomposing")
    Card(
        colors = CardDefaults.cardColors()
            .copy(containerColor = CardDefaults.cardColors().containerColor.copy(alpha = 0.5f))
    ) {

        Row {

            display.matrix.forEachIndexed { xIndex, col ->
                Column {
                    col.forEach { value ->
                        //val on = display.matrix[xIndex][yIndex]

                        Box(modifier = Modifier.size(5.dp)
                            .background(if(value)Color.Blue else Color.Green)) {
                        }
                    }
                }
            }
        }

    }
}
