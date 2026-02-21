package com.twobard.kmpchip8.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
@Preview
fun DisplayUI(display: Array<BooleanArray>) {

    println("recomposing")
    val pixelSize = 12f
    val width = display.size
    val height = if (width > 0) display[0].size else 0
    Card(
        colors = CardDefaults.cardColors()
            .copy(containerColor = CardDefaults.cardColors().containerColor.copy(alpha = 0.5f))
    ) {
        Canvas(modifier = Modifier.size((width * pixelSize).dp, (height * pixelSize).dp)) {
            for (x in 0 until width) {
                for (y in 0 until height) {
                    val on = display[x][y]
                    drawRect(
                        color = if (on) Color.Blue else Color.Green,
                        topLeft = Offset(x * pixelSize, y * pixelSize),
                        size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize),
                        style = Fill
                    )
                }
            }
        }
    }
}
