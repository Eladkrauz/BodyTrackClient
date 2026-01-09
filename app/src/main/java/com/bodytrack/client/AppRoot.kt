////////////////////////////////////////////////////////////
//////////////////// BODY TRACK // CLIENT //////////////////
////////////////////////////////////////////////////////////
/////////////////////// FILE: AppRoot //////////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client

///////////////
/// IMPORTS ///
///////////////
import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import com.bodytrack.client.exercise.ExerciseType
import com.bodytrack.client.exercise.displayName
import com.bodytrack.client.exercise.positionSide
import com.bodytrack.client.exercise.initialPhaseName
import com.bodytrack.client.network.SessionApi
import com.bodytrack.client.network.SessionResult
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.bodytrack.client.home.CameraSide
import com.bodytrack.client.summary.SessionSummaryScreen

///////////////////////////
///// APP SCREEN ENUM /////
///////////////////////////
/**
 * Represents the distinct screens of the application.
 *
 * This enum is the cornerstone of the app's navigation system, which uses a state-driven
 * approach instead of a traditional NavController or fragments. The `currentScreen` state
 * variable in `AppRoot` holds one of these values, and the UI reacts by rendering the
 * corresponding composable screen.
 *
 * @property START The initial landing screen of the app.
 * @property HOME The main screen where users can select an exercise.
 * @property EXERCISE_SETUP Screen that displays instructions and setup options for the selected exercise before starting a session.
 * @property CAMERA_SESSION The screen responsible for the live camera feed, pose detection, and exercise evaluation. This is a special case that launches a separate Activity.
 * @property SESSION_SUMMARY A screen that displays the results and summary of a completed exercise session.
 */
enum class AppScreen {
    START,
    HOME,
    EXERCISE_SETUP,
    CAMERA_SESSION,
    SESSION_SUMMARY
}

////////////////////
///// APP ROOT /////
////////////////////
/**
 * The root composable of the application, acting as the central state holder and navigator.
 *
 * This function embodies a "single-source-of-truth" architecture where all critical
 * application state is managed in one place. It replaces traditional navigation controllers
 * (like `NavController`) with a state-driven approach using `AnimatedContent`.
 *
 * Responsibilities:
 * - **Navigation State:** Manages `currentScreen`, which dictates which UI is currently visible.
 * - **Exercise State:** Holds the `selectedExercise` and its configuration.
 * - **Session State:** Manages the lifecycle of a user session, including the `activeSessionId`,
 *   duration, and running status.
 * - **Network Call Orchestration:** Initiates API calls for registering and starting sessions,
 *   handling both success and failure outcomes.
 * - **Error Handling:** Displays a global error dialog via `errorDialogMessage` for any
 *   critical failures (e.g., network issues, server errors).
 * - **Screen Composition:** Renders the appropriate screen composable (`StartScreen`, `HomeScreen`, etc.)
 *   based on the `currentScreen` state, passing down necessary data and callbacks.
 * - **Interscreen Communication:** Facilitates data flow between screens. For example, the exercise
 *   selected in `HomeScreen` is used to configure `ExerciseSetupScreen`.
 * - **Safety and Invariants:** Implements `LaunchedEffect` guards to prevent illegal state
 *   transitions, such as navigating to a session screen without a valid session ID.
 */
@Composable
fun AppRoot() {
    ////////////////////////
    /// NAVIGATION STATE ///
    ////////////////////////
    /*
     * The current screen shown by the application.
     *
     * This is the single source of truth for navigation.
     * Changing this value automatically triggers:
     *  - recomposition
     *  - AnimatedContent transition
     *  - disposal of the previous screen
     */
    var currentScreen by remember { mutableStateOf(AppScreen.START) }

    //////////////////////
    /// EXERCISE STATE ///
    //////////////////////
    /*
     * The exercise selected by the user on the Home screen.
     *
     * This value is required by:
     *  - ExerciseSetupScreen (to display instructions)
     *  - CameraSessionScreen (to know what is being evaluated)
     *
     * It is nullable because:
     *  - No exercise is selected at app start
     *  - It is explicitly cleared when a session ends
     */
    var selectedExercise by remember { mutableStateOf<ExerciseType?>(null) }

    /*
    Camera side.
     */
    var cameraSideChosen by remember { mutableStateOf(CameraSide.FRONT) }

    /////////////////////
    /// SESSION STATE ///
    /////////////////////
    /*
     * The active session ID received from the backend.
     *
     * This is a CRITICAL piece of state:
     *  - It represents a real server-side session
     *  - CameraSessionScreen MUST NOT exist without it
     */
    var activeSessionId by remember { mutableStateOf<String?>(null) }

    // Session duration chosen in ExerciseSetupScreen.
    var sessionDurationSeconds by remember { mutableStateOf(120) }

    /*
     * Client-side guard to prevent double-starting a session.
     *
     * This protects against:
     *  - Multiple button taps
     *  - Race conditions with async network calls
     */
    var isSessionRunning by remember { mutableStateOf(false) }

    /////////////////////////
    /// REGISTRATION GATE ///
    /////////////////////////
    /*
     * Guard flag used during session registration.
     *
     * While true:
     *  - The user cannot trigger another registration
     *  - Prevents duplicate server requests
     */
    var isRegistering by remember { mutableStateOf(false) }

    ///////////////////
    /// ERROR STATE ///
    ///////////////////
    /*
     * Holds an error message to be displayed in a global dialog.
     *
     * If this value is non-null:
     *  - An AlertDialog is shown
     * If null:
     *  - No error dialog is visible
     */
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    ////////////////////////
    /// SESSION FEEDBACK ///
    ////////////////////////
    var audioFeedbackEnabled by remember { mutableStateOf(true) }
    var textFeedbackEnabled by remember { mutableStateOf(true) }

    //////////////////////////////
    /// CAMERA ACTIVITY LAUNCH ///
    //////////////////////////////
    val context = LocalContext.current
    val cameraSessionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            isSessionRunning = false
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    // Session completed normally.
                    currentScreen = AppScreen.SESSION_SUMMARY
                }

                Activity.RESULT_CANCELED -> {
                    // Session aborted (visibility / network / server).
                    activeSessionId = null
                    selectedExercise = null
                    currentScreen = AppScreen.HOME
                }
            }
        }

    ////////////////////
    /// SAFETY GUARD ///
    ////////////////////
    /*
     * This LaunchedEffect enforces a critical invariant:
     *
     * The CAMERA_SESSION screen must NEVER be shown
     * unless a valid sessionId exists.
     *
     * If, for any reason (bug, async timing, recomposition),
     * the app tries to enter CAMERA_SESSION without a session,
     * the user is immediately redirected to HOME.
     */
    LaunchedEffect(currentScreen) {
        if (currentScreen == AppScreen.CAMERA_SESSION && activeSessionId == null) {
            currentScreen = AppScreen.HOME
        }
    }

    LaunchedEffect(currentScreen) {
        if (
            currentScreen == AppScreen.SESSION_SUMMARY &&
            activeSessionId == null
        ) {
            currentScreen = AppScreen.HOME
        }
    }

    ///////////////////////
    /// ROOT TRANSITION ///
    ///////////////////////
    /*
     * AnimatedContent is used here as a navigation container.
     *
     * - targetState = currentScreen
     * - Whenever currentScreen changes:
     *     - Old screen fades out
     *     - New screen fades in
     */
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
        },
        label = "app_screen_transition"
    ) { screen ->
        /*
         * This when-block defines WHICH screen composable
         * is rendered for each AppScreen value.
         */
        when (screen) {
            ////////////////////
            /// START SCREEN ///
            ////////////////////
            AppScreen.START -> {
                /*
                 * StartScreen is a simple entry screen.
                 *
                 * onStartClicked is a CALLBACK:
                 *  - It is passed DOWN to StartScreen
                 *  - It is invoked FROM INSIDE StartScreen
                 *    when the user presses the "Start" button
                 */
                _root_ide_package_.com.bodytrack.client.start.StartScreen(
                    onStartClicked = {
                        currentScreen = AppScreen.HOME
                    }
                )
            }

            ///////////////////
            /// HOME SCREEN ///
            ///////////////////
            AppScreen.HOME -> {
                /*
                 * HomeScreen allows the user to choose an exercise.
                 *
                 * onExerciseSelected is a CALLBACK:
                 *  - Called by HomeScreen when the user selects an exercise
                 *  - The HomeScreen does NOT know what happens next
                 *  - AppRoot handles registration and navigation
                 */
                _root_ide_package_.com.bodytrack.client.home.HomeScreen(
                    onExerciseSelected = { exercise, cameraSide ->

                        // Prevent multiple registration attempts.
                        if (isRegistering) return@HomeScreen
                        isRegistering = true

                        /*
                         * Register a new session with the backend.
                         * This is an asynchronous network call.
                         */
                        _root_ide_package_.com.bodytrack.client.network.SessionApi.registerSession(
                            exerciseType = exercise.name.lowercase()
                        ) { result ->

                            // Registration request completed.
                            isRegistering = false

                            when (result) {
                                /*
                                 * Successful registration.
                                 * Expect a session_id from the server.
                                 */
                                is com.bodytrack.client.network.SessionResult.Success.Management -> {
                                    val sessionId =
                                        result.extraInfo?.optString("session_id")

                                    // Validate server response.
                                    if (sessionId.isNullOrBlank()) {
                                        errorDialogMessage =
                                            "Server did not return a valid session ID."
                                        return@registerSession
                                    }

                                    // Store session and advance flow.
                                    activeSessionId = sessionId
                                    selectedExercise = exercise
                                    cameraSideChosen = cameraSide
                                    isSessionRunning = false
                                    currentScreen = AppScreen.EXERCISE_SETUP
                                }

                                /*
                                 * Server-side error (like an invalid request).
                                 */
                                is com.bodytrack.client.network.SessionResult.Error -> {
                                    errorDialogMessage =
                                        result.description
                                }

                                /*
                                 * Network failure (no internet, timeout, etc.).
                                 */
                                is com.bodytrack.client.network.SessionResult.NetworkFailure -> {
                                    errorDialogMessage =
                                        "Network error. Please check your connection."
                                }

                                else -> {}
                            }
                        }
                    }
                )
            }

            /////////////////////////////
            /// EXERCISE SETUP SCREEN ///
            /////////////////////////////
            AppScreen.EXERCISE_SETUP -> {
                /*
                 * Defensive extraction of required state.
                 * This screen must not exist without these values.
                 */
                val exercise = selectedExercise
                val sessionId = activeSessionId

                if (exercise != null && sessionId != null) {
                    _root_ide_package_.com.bodytrack.client.exercise.ExerciseSetupScreen(
                        sessionId = sessionId,
                        exerciseName = exercise.displayName(),
                        videoResId = when (exercise) {
                            ExerciseType.SQUAT -> R.raw.squats_animation
                            ExerciseType.BICEPS_CURL -> R.raw.biceps_curls_animation
                        },
                        audioFeedbackEnabled = audioFeedbackEnabled,
                        onAudioFeedbackChanged = { audioFeedbackEnabled = it },

                        textFeedbackEnabled = textFeedbackEnabled,
                        onTextFeedbackChanged = { textFeedbackEnabled = it },

                        cameraSide = cameraSideChosen,
                        onCameraSideChanged = { cameraSideChosen = it },
                        positionSide = exercise.positionSide(),
                        initialPhaseName = exercise.initialPhaseName(),

                        /*
                         * Called when the user presses "Back".
                         * This cancels the session entirely.
                         */
                        onBack = {
                            _root_ide_package_.com.bodytrack.client.network.SessionApi.unregisterSession(
                                sessionId
                            )

                            activeSessionId = null
                            selectedExercise = null
                            isSessionRunning = false
                            currentScreen = AppScreen.HOME
                        },

                        /*
                         * Called when the user presses "Start Session".
                         * This transitions into the camera-based evaluation.
                         */
                        onStartSession = { durationSeconds ->

                            val safeSessionId = activeSessionId
                            if (safeSessionId == null) {
                                errorDialogMessage = "Session ID is missing."
                                return@ExerciseSetupScreen
                            }

                            if (isSessionRunning) return@ExerciseSetupScreen
                            isSessionRunning = true

                            // Store duration in AppRoot state.
                            sessionDurationSeconds = durationSeconds

                            SessionApi.startSession(
                                sessionId = safeSessionId
                            ) { result ->
                                when (result) {
                                    is SessionResult.Success -> {
                                        currentScreen = AppScreen.CAMERA_SESSION
                                    }

                                    is SessionResult.Error -> {
                                        isSessionRunning = false
                                        errorDialogMessage =
                                            "Failed to start session (${result.code}): ${result.description}"
                                    }

                                    is SessionResult.NetworkFailure -> {
                                        isSessionRunning = false
                                        errorDialogMessage =
                                            "Network error while starting session."
                                    }
                                }
                            }
                        }
                    )
                }
            }

            /////////////////////////////
            /// CAMERA SESSION SCREEN ///
            /////////////////////////////
            AppScreen.CAMERA_SESSION -> {
                val sessionId = activeSessionId

                LaunchedEffect(sessionId) {
                    if (sessionId == null) {
                        currentScreen = AppScreen.HOME
                        return@LaunchedEffect
                    }

                    val intent = Intent(
                        context,
                        com.bodytrack.client.session.CameraSessionActivity::class.java
                    ).apply {
                        putExtra("session_id", sessionId)
                        putExtra("duration_seconds", sessionDurationSeconds)
                        putExtra("camera_side", cameraSideChosen.name.lowercase())
                        putExtra("audio_feedback", audioFeedbackEnabled)
                        putExtra("text_feedback", textFeedbackEnabled)
                    }
                    cameraSessionLauncher.launch(intent)
                }
            }

            //////////////////////////////
            /// SESSION SUMMARY SCREEN ///
            //////////////////////////////
            AppScreen.SESSION_SUMMARY -> {
                val sessionId = activeSessionId
                if (sessionId == null) {
                    currentScreen = AppScreen.HOME
                    return@AnimatedContent
                }

                SessionSummaryScreen(
                    sessionId = sessionId,
                    onDone = {
                        activeSessionId = null
                        selectedExercise = null
                        currentScreen = AppScreen.HOME
                    }
                )
            }
        }
    }

    ////////////////////
    /// ERROR DIALOG ///
    ////////////////////
    /*
     * Global error dialog.
     *
     * Displayed whenever errorDialogMessage is non-null.
     * Automatically disappears when the message is cleared.
     */
    errorDialogMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                errorDialogMessage = null
            },
            title = {
                Text("Unable to start session")
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        errorDialogMessage = null
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}