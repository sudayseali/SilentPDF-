package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.SilentPdfViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SilentPdfApp()
            }
        }
    }
}

@Composable
fun SilentPdfApp() {
    val navController = rememberNavController()
    val viewModel: SilentPdfViewModel = viewModel()

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
