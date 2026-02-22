package com.twobard.kmpchip8

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import com.twobard.kmpchip8.hardware.System
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        //val system = System()
        setContent {
            val coroutineScope = rememberCoroutineScope()
            var frame by remember { mutableIntStateOf(0) }
            var system by remember { mutableStateOf(System())}
            LaunchedEffect(true) {
                coroutineScope.launch {
                    //system.startGame("blinky.ch8")
                    //system.startGame(title = "2-ibm-logo.ch8")
                    //system.startGame(title = "4-flags.ch8")
                    //system.startGame(title = "ibm_new.ch8")
                    //system.startGame(title = "1-chip8-logo.ch8")
                    //system.startGame(title = "delaytimertest.ch8")
                    system.startGame(title = "octojam1title.ch8")
                    //system.startGame(title = "octojam3title.ch8")
                    //system.startGame(title = "octojam8title.ch8")
                    //system.startGame(title = "octoachip8story.ch8")
                    //system.startGame("ibm.ch8")
                    //system.startGame("randomnumbertest")
                    //system.startGame("test_opcode.ch8")
                }
            }

            val display by system.displayData.collectAsState()


            //Text("display:" + display.value., fontSize = 32.sp)
            App(frame, display ?: arrayOf(booleanArrayOf())) { int ->
                system.keyboard.pressKeyAt( int)
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    //App()
}