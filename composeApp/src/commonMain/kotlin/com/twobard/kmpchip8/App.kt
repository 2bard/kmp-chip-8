package com.twobard.kmpchip8

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

import com.twobard.kmpchip8.hardware.System
import com.twobard.kmpchip8.ui.DisplayUI
import com.twobard.kmpchip8.viewmodel.Chip8ViewModel
import kotlinx.coroutines.launch

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.twobard.kmpchip8.ui.Keyboard
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
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(true) {
            coroutineScope {
                viewModel.errors.collect {
                    it?.let {
                        snackbarHostState.showSnackbar(it)
                    }
                }
            }
        }
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { paddingValues ->

            Box(modifier = Modifier.padding(paddingValues = paddingValues)){
                PlayerUI(viewModel)
            }
        }

    }
}

@Composable
fun PlayerUI(viewModel: Chip8ViewModel){
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    val romName = viewModel.loadedRom.collectAsState()

    val system = viewModel.system.collectAsState()


    system.value?.let {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){

            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                ElevatedButton(onClick = {
                    showDialog = !showDialog
                }){
                    Text("Select Rom")
                }

                romName.value?.let {
                    DisplayUI(system.value!!)

                    ElevatedCard(modifier = Modifier.border(2.dp, Color.Black, RoundedCornerShape(4.dp))) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            Keyboard() { key ->
                                system.value!!.keyboard.pressKeyAt(key)
                            }
                        }
                    }
                }


                if(showDialog || romName.value == null){
                    Dialog(
                        onDismissRequest = {
                            showDialog = false
                        },
                        content = {
                            RomList(roms = viewModel.getRoms()) {
                                viewModel.selectRom(it)
                                showDialog = false
                            }
                        }
                    )
                }
            }
        }
    } ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("System intitializing")
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