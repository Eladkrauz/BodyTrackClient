////////////////////////////////////////////////////////////
////////////// BODY TRACK // CLIENT // EXERCISE ////////////
////////////////////////////////////////////////////////////
///////////////// FILE: ExerciseSetupScreen ////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.exercise

///////////////
/// IMPORTS ///
///////////////
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/////////////////////////////
/// EXERCISE SETUP SCREEN ///
/////////////////////////////
/**
 * A Jetpack Compose screen that guides the user through the setup process before starting a motion analysis session.
 *
 * This screen serves as the final step before the `CameraSessionScreen`. Its main responsibilities include:
 * - Displaying a demo video of the exercise.
 * - Presenting a series of crucial instructions for correct setup (e.g., body visibility, camera angle).
 * - Showing the unique session ID.
 * - Enforcing a time limit for the setup process to prevent orphaned sessions on the server.
 * - Allowing the user to select or input a session duration before proceeding.
 *
 * The screen uses local state to manage the instruction step index, dialog visibility, and the setup countdown.
 * Navigation and session management logic (e.g., unregistering a session on exit) are handled via callbacks
 * (`onBack`, `onStartSession`), promoting a clean separation of concerns.
 *
 * @param sessionId The server-side identifier for the active session, displayed for user reference.
 * @param exerciseName The human-readable name of the exercise, shown in the top app bar.
 * @param videoResId The raw resource ID for the looping video that demonstrates the exercise.
 * @param positionSide The required camera viewing angle (e.g., LEFT, RIGHT, FRONT) for the exercise.
 * @param initialPhaseName The name of the required starting pose for the exercise (e.g., "Standing").
 * @param onBack A callback invoked when the user decides to exit the setup process. The caller is responsible for cleaning up the session and navigating away.
 * @param onStartSession A callback invoked when the user has completed the setup and chosen a session duration. The selected duration (in seconds) is passed to the caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSetupScreen(
    sessionId: String,
    exerciseName: String,
    videoResId: Int,
    positionSide: PositionSide,
    initialPhaseName: String,
    onBack: () -> Unit,
    onStartSession: (durationSeconds: Int) -> Unit
) {
    var stepIndex by remember { mutableStateOf(0) }
    var showExitDialog by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(120) } // 2 minutes
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showDurationDialog by remember { mutableStateOf(false) }
    var selectedDuration by remember { mutableStateOf<Int?>(null) }
    var customDurationInput by remember { mutableStateOf("") }
    var isStartingSession by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0 && !isStartingSession) {
            kotlinx.coroutines.delay(1_000)
            remainingSeconds--
        }
        /*
         * If time expires before session start:
         * - Unregister the session
         * - Notify the user
         */
        if (!isStartingSession && remainingSeconds == 0) {
            showTimeoutDialog = true
        }
    }

    /////////////////////////
    /// INSTRUCTION STEPS ///
    /////////////////////////
    /*
     * List of instruction steps.
     * Built once per composition.
     */
    val steps = remember {
        listOf(
            visibilityInstruction(),
            positionInstruction(positionSide),
            phasesInstruction(initialPhaseName, exerciseName),
            stabilityInstruction()
        )
    }

    //////////////////////
    /// SCREEN CONTENT ///
    //////////////////////
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        ///////////////
        /// TOP BAR ///
        ///////////////
        TopAppBar(
            title = {
                Text(
                    text = exerciseName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = { showExitDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60

                Text(
                    text = String.format("%d:%02d", minutes, seconds),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = _root_ide_package_.com.bodytrack.client.theme.TextGray,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        )

        ///////////////////////////
        /// EXERCISE DEMO VIDEO ///
        ///////////////////////////
        /*
         * Looping demo video showing correct exercise execution.
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
        ) {
            _root_ide_package_.com.bodytrack.client.home.LoopingVideo(
                rawResId = videoResId,
                modifier = Modifier.fillMaxSize()
            )
            _root_ide_package_.com.bodytrack.client.home.EdgeFadeOverlay()
        }

        //////////////////////////
        /// SESSION ID DISPLAY ///
        //////////////////////////
        /*
         * Displays the session ID for transparency/debugging.
         */
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Your Session ID",
                fontSize = 13.sp,
                color = _root_ide_package_.com.bodytrack.client.theme.TextGray
            )
            Text(
                text = sessionId,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        ///////////////////////////
        /// INSTRUCTION CONTENT ///
        ///////////////////////////
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Important Instructions",
                fontSize = 13.sp,
                color = _root_ide_package_.com.bodytrack.client.theme.TextGray
            )
        }

        /*
         * Animated container for step-by-step instructions.
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(16.dp)
        ) {

            AnimatedContent(
                targetState = stepIndex,
                transitionSpec = {
                    fadeIn(tween(250)) togetherWith
                            fadeOut(tween(200))
                },
                label = "instruction_transition"
            ) { index ->

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = steps[index].title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = _root_ide_package_.com.bodytrack.client.theme.BodyTrackBlueDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = steps[index].body,
                        fontSize = 14.sp,
                        color = _root_ide_package_.com.bodytrack.client.theme.TextGray,
                        lineHeight = 20.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))

        //////////////////////////
        /// NAVIGATION BUTTONS ///
        //////////////////////////
        /*
         * Controls navigation between instruction steps
         * and session start.
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = { if (stepIndex > 0) stepIndex-- },
                enabled = stepIndex > 0
            ) {
                Text("Back")
            }

            Button(
                onClick = {
                    if (stepIndex < steps.lastIndex) {
                        stepIndex++
                    } else {
                        showDurationDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = _root_ide_package_.com.bodytrack.client.theme.BodyTrackBlue
                )
            ) {
                Text(
                    text = if (stepIndex == steps.lastIndex)
                        "Start Session"
                    else
                        "Next",
                    color = Color.White
                )
            }
        }
    }

    ////////////////////////////////
    /// EXIT CONFIRMATION DIALOG ///
    ////////////////////////////////
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit setup?") },
            text = {
                Text("If you go back now, the session setup will be cancelled.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onBack()
                    }
                ) {
                    Text("Yes, exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Stay")
                }
            }
        )
    }

    //////////////////////
    /// TIMEOUT DIALOG ///
    //////////////////////
    if (showTimeoutDialog) {
        var secondsLeft by remember { mutableStateOf(5) }

        LaunchedEffect(showTimeoutDialog) {
            if (showTimeoutDialog) {
                secondsLeft = 5
                while (secondsLeft > 0) {
                    delay(1_000)
                    secondsLeft--
                }
                // Auto-execute confirm action.
                showTimeoutDialog = false
                onBack()
            }
        }

        AlertDialog(
            onDismissRequest = { /* block dismiss */ },
            title = { Text("Time expired") },
            text = {
                Text(
                    "The setup time has expired.\n" +
                            "You will now be returned to the home screen.\n\n"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimeoutDialog = false
                        onBack()
                    }
                ) {
                    Text("Okay ($secondsLeft)")
                }
            }
        )
    }

    //////////////////////////
    /// DURATION SELECTION ///
    //////////////////////////
    if (showDurationDialog) {
        AlertDialog(
            onDismissRequest = { showDurationDialog = false },
            title = { Text("Choose session duration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text =
                            "This session will run for a predefined time.\n" +
                                    "If you finish early, stay in the initial position\n" +
                                    "or press END during the session.",
                        fontSize = 14.sp,
                        color = _root_ide_package_.com.bodytrack.client.theme.TextGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(15, 30, 45, 60).forEach { seconds ->
                        OutlinedButton(
                            onClick = {
                                selectedDuration = seconds
                                showDurationDialog = false
                                isStartingSession = true
                                onStartSession(seconds)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$seconds seconds")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customDurationInput,
                        onValueChange = { input ->
                            customDurationInput = input.filter { it.isDigit() }
                        },
                        label = { Text("Custom (seconds, max 120)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val custom = customDurationInput.toIntOrNull()
                        if (custom != null && custom in 10..120) {
                            showDurationDialog = false
                            isStartingSession = true
                            onStartSession(custom)
                        }
                    }
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDurationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

//////////////////
/// DATA MODEL ///
//////////////////
/*
 * Simple data holder for instruction content.
 * Kept private because it is only relevant to this file.
 */
private data class InstructionStep(
    val title: String,
    val body: String
)

///////////////////////////
/// INSTRUCTION CONTENT ///
///////////////////////////
private fun visibilityInstruction() = InstructionStep(
    title = "Visibility and Environment",
    body = """
        Ensure your full body is visible and well-lit.
        Wear fitted clothing and make sure only one person is in frame.
    """.trimIndent()
)

private fun positionInstruction(side: PositionSide) = InstructionStep(
    title = "Camera Position",
    body = """
        This exercise must be performed from a ${side.name.lowercase()} view.
        Do not change camera position during the session.
    """.trimIndent()
)

private fun phasesInstruction(
    initialPhase: String,
    exerciseName: String
) = InstructionStep(
    title = "Initial Phase",
    body = """
        For $exerciseName, the starting position must be "$initialPhase".
        The system will verify this before analysis begins.
    """.trimIndent()
)

private fun stabilityInstruction() = InstructionStep(
    title = "Session Instructions",
    body = """
        Perform movements smoothly and avoid sudden changes.
        This ensures accurate and stable analysis.
    """.trimIndent()
)