package com.haptix.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haptix.app.navigation.HaptiXNavigation
import com.haptix.app.session.StudySessionViewModel
import com.haptix.app.ui.theme.HaptiXTheme

/**
 * Main entry point activity for HaptiX experimental application.
 * Hosts the Jetpack Compose navigation graph, edge-to-edge UI, and dynamic Cyber Dark/Light theme switching.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startVideoId = intent?.getStringExtra("videoId")
        val startDestination = intent?.getStringExtra("startDestination")
            ?: startVideoId?.let { com.haptix.app.navigation.HaptiXDestinations.videoPlayerRoute(it) }
            ?: com.haptix.app.navigation.HaptiXDestinations.WELCOME

        setContent {
            val sessionViewModel: StudySessionViewModel = viewModel()
            val sessionState by sessionViewModel.sessionState.collectAsState()

            if (startVideoId != null) {
                sessionViewModel.selectVideo(startVideoId)
            }

            HaptiXTheme(themeMode = sessionState.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HaptiXNavigation(
                        sessionViewModel = sessionViewModel,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
