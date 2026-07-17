package com.silentpdf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.silentpdf.app.ui.screens.LibraryScreen
import com.silentpdf.app.ui.screens.ReaderScreen
import com.silentpdf.app.ui.screens.PinLockScreen
import com.silentpdf.app.ui.theme.MyApplicationTheme
import com.silentpdf.app.ui.viewmodel.SilentPdfViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SilentPdfViewModel = viewModel()
            val isTrueDarkMode by viewModel.isTrueDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isTrueDarkMode) {
                SilentPdfApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SilentPdfApp(viewModel: SilentPdfViewModel = viewModel()) {
    val navController = rememberNavController()
    val isAppLocked by viewModel.isAppLocked.collectAsState()

    if (isAppLocked) {
        PinLockScreen(viewModel = viewModel)
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "library",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("library") {
                    LibraryScreen(
                        viewModel = viewModel,
                        onNavigateToReader = { navController.navigate("reader") }
                    )
                }
                composable("reader") {
                    ReaderScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.navigateUp() }
                    )
                }
            }
        }
    }
}
