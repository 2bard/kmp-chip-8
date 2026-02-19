package com.twobard.kmpchip8.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
@Preview
fun Keyboard() {

    Card(
        colors = CardDefaults.cardColors()
            .copy(containerColor = CardDefaults.cardColors().containerColor.copy(alpha = 0.5f))
    ) {
        Column {
            Row {
                Key(text = "1")
                Key(text = "2")
                Key(text = "3")
                Key(text = "C")
            }

            Row {
                Key(text = "4")
                Key(text = "5")
                Key(text = "6")
                Key(text = "D")
            }

            Row {
                Key(text = "7")
                Key(text = "8")
                Key(text = "9")
                Key(text = "E")
            }

            Row {
                Key(text = "A")
                Key(text = "0")
                Key(text = "B")
                Key(text = "F")
            }
        }
    }

}

@Composable
@Preview
fun Key(text: String = "", onClickListener: () -> Unit = {}) {
    Card(modifier = Modifier.size(32.dp).padding(2.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(modifier = Modifier.align(Alignment.Center), text = text)
        }
    }
}