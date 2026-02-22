package com.twobard.kmpchip8.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

@Composable
@Preview
fun DisplayUI(display: Array<BooleanArray>, onKeyPressed: (Int) -> Unit) {

    println("recomposing")

    val width = display.size
    val height = if (width > 0) display[0].size else 0
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val pixelSize = (canvasSize.width / 64).toFloat()
    Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {

        Box(modifier = Modifier.wrapContentSize().align(Alignment.CenterHorizontally).border(2.dp, Color.Gray,
            RoundedCornerShape(4.dp)
        )){
            Canvas(modifier = Modifier.width(300.dp).height(150.dp).onGloballyPositioned { coordinates ->
                canvasSize = coordinates.size
            }) {
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


        ElevatedCard(modifier = Modifier.border(2.dp, Color.Black, RoundedCornerShape(4.dp))) {
            Box(modifier = Modifier.padding(12.dp)) {
                Keyboard( onKeyPressed)
            }

        }

    }

}
