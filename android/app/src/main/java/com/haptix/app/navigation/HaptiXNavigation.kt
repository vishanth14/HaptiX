package com.haptix.app.navigation

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.haptix.app.haptics.AndroidHapticPlayer
import com.haptix.app.session.StudySessionViewModel
import com.haptix.app.ui.components.CyberTopBar
import com.haptix.app.ui.screens.completion.CompletionScreen
import com.haptix.app.ui.screens.feedback.FeedbackScreen
import com.haptix.app.ui.screens.profile.ProfileScreen
import com.haptix.app.ui.screens.video.VideoListScreen
import com.haptix.app.ui.screens.video.VideoPlayerScreen
import com.haptix.app.ui.screens.welcome.WelcomeScreen

/**
 * Navigation destination routes for HaptiX experimental flows.
 */
object HaptiXDestinations {
    const val WELCOME = "welcome"
    const val PARTICIPANT_SETUP = "participant_setup"
    const val VIDEO_LIBRARY = "video_library"
    const val VIDEO_PLAYER = "video_player/{videoId}"
    const val FEEDBACK = "feedback/{videoId}"
    const val COMPLETION = "completion"

    fun videoPlayerRoute(videoId: String): String = "video_player/$videoId"
    fun feedbackRoute(videoId: String): String = "feedback/$videoId"
}

/**
 * Top-level Jetpack Compose NavHost managing full experimental study flow:
 * Welcome -> Participant Profile -> Video Library -> Video Player -> Feedback -> Completion
 * Integrates persistent CyberTopBar for session-wide light/dark theme switching and subtle screen transitions.
 */
@Composable
fun HaptiXNavigation(
    navController: NavHostController = rememberNavController(),
    sessionViewModel: StudySessionViewModel = viewModel()
) {
    val context = LocalContext.current
    val isHardwareSupported = remember {
        try {
            AndroidHapticPlayer(context).isHapticSupported()
        } catch (_: Exception) {
            false
        }
    }

    sessionViewModel.updateHapticHardwareSupport(isHardwareSupported)
    val sessionState by sessionViewModel.sessionState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isPlayer = currentRoute?.startsWith("video_player") == true
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val showTopBar = !(isPlayer && isLandscape)

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        if (showTopBar) {
            CyberTopBar(
                onToggleTheme = {
                    sessionViewModel.toggleThemeMode()
                }
            )
        }

        NavHost(
            navController = navController,
            startDestination = HaptiXDestinations.WELCOME,
            enterTransition = {
                fadeIn(animationSpec = tween(220)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(220)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(220)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(220)
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(200))
            },
            modifier = Modifier.weight(1f)
        ) {
            // Flow 1: Welcome / Entry
            composable(HaptiXDestinations.WELCOME) {
                WelcomeScreen(
                    onBeginStudy = {
                        navController.navigate(HaptiXDestinations.PARTICIPANT_SETUP)
                    }
                )
            }

            // Flow 2: Participant Setup / Demographics
            composable(HaptiXDestinations.PARTICIPANT_SETUP) {
                ProfileScreen(
                    onNavigateToVideoList = { participant ->
                        sessionViewModel.saveParticipant(participant)
                        navController.navigate(HaptiXDestinations.VIDEO_LIBRARY)
                    }
                )
            }

            // Flow 3: Stimuli Library Selection
            composable(HaptiXDestinations.VIDEO_LIBRARY) {
                VideoListScreen(
                    onVideoSelected = { videoId ->
                        sessionViewModel.selectVideo(videoId)
                        navController.navigate(HaptiXDestinations.videoPlayerRoute(videoId))
                    }
                )
            }

            // Flow 4: Video Stimulus Player
            composable(
                route = HaptiXDestinations.VIDEO_PLAYER,
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId").orEmpty()
                VideoPlayerScreen(
                    videoId = videoId,
                    initialHapticsEnabled = sessionState.isHapticsEnabled,
                    isHapticSupported = sessionState.isHapticSupported,
                    onHapticsToggled = { enabled ->
                        sessionViewModel.setHapticsEnabled(enabled)
                    },
                    onPlaybackFinished = { id ->
                        navController.navigate(HaptiXDestinations.feedbackRoute(id))
                    }
                )
            }

            // Flow 5: Feedback Questionnaire
            composable(
                route = HaptiXDestinations.FEEDBACK,
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId").orEmpty()
                FeedbackScreen(
                    videoId = videoId,
                    participantId = sessionState.participant?.id.orEmpty(),
                    isHapticsEnabled = sessionState.isHapticsEnabled,
                    onFeedbackSubmitted = { responses ->
                        sessionViewModel.recordFeedback(responses)
                        navController.navigate(HaptiXDestinations.COMPLETION)
                    }
                )
            }

            // Flow 6: Study Completion
            composable(HaptiXDestinations.COMPLETION) {
                CompletionScreen(
                    sessionState = sessionState,
                    onFinish = {
                        sessionViewModel.resetSession()
                        navController.navigate(HaptiXDestinations.WELCOME) {
                            popUpTo(HaptiXDestinations.WELCOME) { inclusive = true }
                        }
                    },
                    onTestAnotherClip = {
                        navController.navigate(HaptiXDestinations.VIDEO_LIBRARY) {
                            popUpTo(HaptiXDestinations.VIDEO_LIBRARY) { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}
