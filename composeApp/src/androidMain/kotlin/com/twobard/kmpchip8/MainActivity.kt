package com.twobard.kmpchip8

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.twobard.kmpchip8.hardware.System
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val system = System()
        setContent {
            val coroutineScope = rememberCoroutineScope()
            var frame by remember { mutableIntStateOf(0) }

            LaunchedEffect(true) {
                coroutineScope.launch {
                    system.startGame(title = "octojam1title.ch8")
                }
            }

            val display = system.displayData.collectAsState()

            App(frame, display.value)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    //App()
}