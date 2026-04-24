package com.axios.focusguard

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.axios.focusguard.ui.Routes
import com.axios.focusguard.ui.analysis.AnalysisScreen
import com.axios.focusguard.ui.results.ResultsScreen
import com.axios.focusguard.ui.settings.SettingsScreen
import com.axios.focusguard.ui.theme.FocusGuardTheme
import com.axios.focusguard.ui.timer.TimerScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        
        setContent {
            FocusGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.safeDrawingPadding()) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.TIMER) {
        composable(Routes.TIMER) {
            TimerScreen(
                onSessionFinished = { navController.navigate(Routes.RESULTS) },
                onSettingsClick   = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.RESULTS) {
            ResultsScreen(
                onAnalyzeClick = { navController.navigate(Routes.ANALYSIS) },
                onNextSession  = { navController.popBackStack(Routes.TIMER, false) }
            )
        }
        composable(Routes.ANALYSIS) {
            AnalysisScreen(
                onNextSession = { navController.popBackStack(Routes.TIMER, false) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
