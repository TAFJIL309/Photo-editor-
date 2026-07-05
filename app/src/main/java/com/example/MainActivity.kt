package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.data.repository.ProjectRepositoryImpl
import com.example.presentation.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize DB and Repositories via clean Constructor Injection
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ProjectRepositoryImpl(database.projectDao())
        val viewModelFactory = SharedViewModelFactory(repository, applicationContext)
        val viewModel: SharedViewModel = ViewModelProvider(this, viewModelFactory)[SharedViewModel::class.java]

        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController, 
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { navController.navigate("editor") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        
                        composable("editor") {
                            if (viewModel.isGenerating) {
                                GenerationScreen(stageText = viewModel.generationStage)
                            } else {
                                EditorScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = {
                                        viewModel.clearWorkspace()
                                        navController.popBackStack()
                                    },
                                    onNavigateToResult = { navController.navigate("result") }
                                )
                            }
                        }

                        composable("result") {
                            if (viewModel.isGenerating) {
                                GenerationScreen(stageText = viewModel.generationStage)
                            } else {
                                ResultScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onEditAgain = { navController.navigate("editor") }
                                )
                            }
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
