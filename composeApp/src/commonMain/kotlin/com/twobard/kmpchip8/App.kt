package com.twobard.kmpchip8

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.twobard.kmpchip8.hardware.System
import com.twobard.kmpchip8.ui.DisplayUI
import com.twobard.kmpchip8.viewmodel.Chip8ViewModel
import kotlinx.coroutines.launch
import kotlin.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.coroutineScope
import org.koin.core.context.startKoin

@Composable
@Preview
fun App() {

    try {
        startKoin{
            modules(myModule)
        }
    } catch (e : Exception) {

    }


    Surface {
        MaterialTheme {
            Navigator(Chip8Screen())
        }
    }


}

class Chip8Screen : Screen {


    @Composable
    override fun Content() {

        val viewModel: Chip8ViewModel = koinScreenModel<Chip8ViewModel>()


        val loadedRom by viewModel.loadedRom.collectAsState()

        Scaffold { paddingValues ->

            Box(modifier = Modifier.padding(paddingValues = paddingValues)){
                loadedRom?.let {
                    PlayerUI(it)
                } ?: run {
                    RomList(viewModel.getRoms()) {
                        viewModel.selectRom(it)
                    }
                }

            }
        }

    }
}

@Composable
fun PlayerUI(romName: String){
    val coroutineScope = rememberCoroutineScope()
    val system by remember { mutableStateOf(System(strictMode = false, logging = false)) }
    LaunchedEffect(romName) {
        coroutineScope.launch {
            system.startGame(romName)
        }
    }

    val display by system.displayData.collectAsState()

    display?.let {

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            DisplayUI(it) { key ->
                system.keyboard.pressKeyAt(key)
            }
        }

    }

}

@Composable
fun RomList(roms: List<String> = listOf(), onClickItem: (String) -> Unit = {}){
    Box {
        Column {

            Text("Select a rom")
            LazyColumn() {
                items(roms.size) {
                    RomPreview(roms[it], onClickItem)
                }
            }
        }

    }
}

@Composable
fun RomPreview(item: String, onClickItem: (String) -> Unit) {
    Card(modifier = Modifier.padding(4.dp).fillMaxWidth().clickable {
        onClickItem.invoke(item)
    }){
        Box(modifier = Modifier.padding(12.dp)) {
            Text(item)
        }
    }

}