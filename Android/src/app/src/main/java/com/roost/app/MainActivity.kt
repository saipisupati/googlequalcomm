package com.roost.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roost.app.ui.AddPurchaseScreen
import com.roost.app.ui.AddPurchaseViewModel
import com.roost.app.ui.AskDragonScreen
import com.roost.app.ui.AskDragonViewModel
import com.roost.app.ui.CameraScreen
import com.roost.app.ui.HomeScreen
import com.roost.app.ui.HomeViewModel
import com.roost.app.ui.theme.RoostTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as RoostApplication).container
        setContent {
            RoostTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RoostApp(container)
                }
            }
        }
    }
}

private enum class Screen { Home, AddPurchase, Camera, AskDragon }

@Composable
private fun RoostApp(container: AppContainer) {
    var screen by remember { mutableStateOf(Screen.Home) }

    val factory = remember(container) { ViewModelFactory(container) }
    // Single AddPurchaseViewModel shared between AddPurchase and Camera screens so
    // captured photo URI flows back into the same form state.
    val addPurchaseVm: AddPurchaseViewModel = viewModel(factory = factory)

    when (screen) {
        Screen.Home -> {
            val vm: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = vm,
                onAddPurchaseClick = { screen = Screen.AddPurchase },
                onAskDragonClick = { screen = Screen.AskDragon },
            )
        }
        Screen.AddPurchase -> {
            AddPurchaseScreen(
                viewModel = addPurchaseVm,
                onBack = { screen = Screen.Home },
                onScanReceiptClick = { screen = Screen.Camera },
            )
        }
        Screen.Camera -> {
            CameraScreen(
                onCaptured = { uri ->
                    addPurchaseVm.onPhotoCaptured(uri)
                    screen = Screen.AddPurchase
                },
                onCancel = { screen = Screen.AddPurchase },
            )
        }
        Screen.AskDragon -> {
            val vm: AskDragonViewModel = viewModel(factory = factory)
            AskDragonScreen(viewModel = vm, onBack = { screen = Screen.Home })
        }
    }
}

private class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            HomeViewModel::class.java -> HomeViewModel(container) as T
            AddPurchaseViewModel::class.java -> AddPurchaseViewModel(container) as T
            AskDragonViewModel::class.java -> AskDragonViewModel(container) as T
            else -> throw IllegalArgumentException("Unknown VM: $modelClass")
        }
    }
}
