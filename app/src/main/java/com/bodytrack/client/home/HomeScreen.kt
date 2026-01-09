////////////////////////////////////////////////////////////
/////////////// BODY TRACK // CLIENT // HOME ///////////////
////////////////////////////////////////////////////////////
//////////////////// FILE: HomeScreen //////////////////////
////////////////////////////////////////////////////////////
@file:kotlin.OptIn(ExperimentalFoundationApi::class)
package com.bodytrack.client.home

///////////////
/// IMPORTS ///
///////////////
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.annotation.OptIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.bodytrack.client.exercise.ExerciseType
import com.bodytrack.client.exercise.displayName
import com.bodytrack.client.R

//////////////////////////////
/// HOME SCREEN COMPOSABLE ///
//////////////////////////////
/**
 * The main screen of the application, serving as the central hub for users.
 *
 * This composable is responsible for displaying a list of available exercises,
 * a time-based greeting, and providing access to session-level settings. It's the
 * primary interface where users select an exercise to begin a tracking session.
 *
 * The screen is structured with a top app bar for branding and settings, followed
 * by a `LazyColumn` that contains sticky headers and clickable `ExerciseCard`s.
 *
 * State management for session preferences (like camera side, audio feedback) is handled
 * locally within this composable. User selections are communicated upwards via the
 * `onExerciseSelected` callback, decoupling this screen from navigation and business logic.
 *
 * @param onExerciseSelected A callback function that is invoked when the user taps on an
 * exercise card. It provides the selected [ExerciseType] and the currently configured
 * [CameraSide], signaling the user's intent to start a session.
 */
@Composable
fun HomeScreen(
    onExerciseSelected: (ExerciseType, CameraSide) -> Unit
) {

    // UI state.
    var showSettingsDialog by remember { mutableStateOf(false) }

     // Session preference flags.
    var extendedEvaluation by remember { mutableStateOf(false) }
    var audioFeedbackEnabled by remember { mutableStateOf(true) }
    var verboseFeedback by remember { mutableStateOf(false) }
    var cameraSide by remember { mutableStateOf(CameraSide.FRONT) }

    // Root layout.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top bar.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.bodytrack_logo_ui),
                    contentDescription = "BodyTrack Logo",
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BodyTrack",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = { showSettingsDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }

        // Scrollable content.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Greeting header.
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val (greetingTitle, greetingSubtitle) = remember {
                        getTimeBasedGreeting()
                    }
                    Text(
                        text = greetingTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = greetingSubtitle,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            // Section title header.
            stickyHeader {
                Text(
                    text = "Pick your exercise",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Exercise cards.
            item {
                ExerciseCard(
                    exerciseName = ExerciseType.SQUAT.displayName(),
                    videoResId = R.raw.squats_animation,
                    onClick = { onExerciseSelected(ExerciseType.SQUAT, cameraSide) }
                )
            }
            item {
                ExerciseCard(
                    exerciseName = ExerciseType.BICEPS_CURL.displayName(),
                    videoResId = R.raw.biceps_curls_animation,
                    onClick = { onExerciseSelected(ExerciseType.BICEPS_CURL, cameraSide) }
                )
            }

            // Placeholder item.
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(horizontal = 16.dp)
                        .background(
                            color = Color(0xFFEFEFEF),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "More exercises coming soon!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    ///////////////////////
    /// SETTINGS DIALOG ///
    ///////////////////////
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingToggle(
                        title = "Extended Evaluation",
                        description = "Analyze a wider set of joint angles.",
                        checked = extendedEvaluation,
                        onCheckedChange = { extendedEvaluation = it }
                    )
                    HorizontalDivider()
                    SettingToggle(
                        title = "Audio Feedback",
                        description = "Enable spoken feedback during sessions.",
                        checked = audioFeedbackEnabled,
                        onCheckedChange = { audioFeedbackEnabled = it }
                    )
                    HorizontalDivider()
                    Column {
                        Text(
                            text = "Camera",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Choose which camera to use for filming.",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CameraSide.entries.forEach { side ->
                                Row(
                                    Modifier
                                        .selectable(
                                            selected = cameraSide == side,
                                            onClick = { cameraSide = side }
                                        )
                                        .padding(end = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = cameraSide == side,
                                        onClick = { cameraSide = side }
                                    )
                                    Text(
                                        text = if (side == CameraSide.REAR) "Rear" else "Front",
                                        modifier = Modifier.padding(start = 4.dp),
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },

            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Done", fontSize = 16.sp)
                }
            }
        )
    }
}

//////////////////////
/// SETTING TOGGLE ///
//////////////////////
@Composable
fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    /*
     * Reusable toggle row for settings.
     * Stateless except for parameters passed in.
     */
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 16.sp,
            color = Color.Gray
        )
    }
}

/////////////////////
/// EXERCISE CARD ///
/////////////////////
/**
 * A visually engaging card component that represents a single, selectable exercise.
 *
 * This composable displays the name of an exercise and a looping video preview
 * demonstrating the proper form. The entire card is clickable, triggering the
 * provided `onClick` lambda when tapped. It is designed to be a primary interactive
 * element within a list on the `HomeScreen`.
 *
 * The card's internal structure consists of a `LoopingVideo` component for the
 * visual demonstration and a `Text` component for the exercise's name, all
 * wrapped within a styled `Column`.
 *
 * @param exerciseName The name of the exercise to be displayed on the card.
 * @param videoResId The raw resource ID (e.g., `R.raw.squats_animation`) for the
 *   video file that will be played in a loop as a preview.
 * @param onClick A () -> Unit lambda that is executed when the user taps on the card.
 */
@Composable
fun ExerciseCard(
    exerciseName: String,
    videoResId: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 16.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            LoopingVideo(rawResId = videoResId, modifier = Modifier.fillMaxSize())
            EdgeFadeOverlay()
        }
        Text(
            text = exerciseName,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/////////////////////
/// LOOPING VIDEO ///
/////////////////////
/**
 * A composable that displays a silent, looping video from a raw resource file.
 *
 * This component uses ExoPlayer to handle video playback. The player is configured to
 * loop indefinitely (`REPEAT_MODE_ALL`), be muted (`volume = 0f`), and start playing
 * as soon as it's ready. The video is scaled to zoom and fill its container.
 *
 * The `ExoPlayer` instance is managed within the composable's lifecycle. It is
 * created once for a given `rawResId` and remembered across recompositions. A
 * `DisposableEffect` ensures that the player is properly released when the
 * composable leaves the composition, preventing resource leaks.
 *
 * A simple fade-in animation is applied for a smoother visual presentation.
 *
 * @param rawResId The integer ID of the raw video resource (e.g., `R.raw.squats_animation`).
 * @param modifier The modifier to be applied to the `AndroidView` that hosts the video player.
 */
@OptIn(UnstableApi::class)
@Composable
fun LoopingVideo(
    rawResId: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(rawResId) {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/$rawResId")
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600),
        label = "video_alpha"
    )

    AndroidView(
        modifier = modifier.graphicsLayer { this.alpha = alpha },
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        }
    )
}

/////////////////////////
/// EDGE FADE OVERLAY ///
/////////////////////////
/**
 * A composable that renders a decorative overlay with a radial gradient.
 *
 * This overlay is designed to be placed on top of other content, such as a video.
 * It creates a subtle vignetting effect by applying a transparent-to-light-gray
 * gradient that emanates from the center. This effect helps to soften the hard
 * edges of the content underneath and can improve the contrast for any text or
 * UI elements placed near the borders.
 *
 * The `Box` fills the entire available space of its parent, ensuring the gradient
 * covers the intended area.
 */
@Composable
fun EdgeFadeOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.LightGray.copy(alpha = 0.35f)
                    ),
                    radius = 500f
                )
            )
    )
}

//////////////////////
/// GREETING LOGIC ///
//////////////////////
/**
 * Generates a time-appropriate greeting message and a motivational subtitle.
 *
 * This function determines the current hour of the day and returns a corresponding
 * pair of strings: a primary greeting (e.g., "Good Morning!") and a secondary,
 * context-related message (e.g., "Early workouts boost energy and focus.").
 * The function is designed to provide a dynamic and welcoming message to the user
 * on the home screen.
 *
 * The time brackets are defined as:
 * - Morning: 5 AM to 11 AM
 * - Afternoon: 12 PM to 4 PM
 * - Evening: 5 PM to 8 PM
 * - Night: All other times
 *
 * @return A [Pair] where the first element is the greeting title and the second is the subtitle.
 */
fun getTimeBasedGreeting(): Pair<String, String> {
    val hour = java.time.LocalTime.now().hour
    return when (hour) {
        in 5..11 -> "Good Morning!" to "Early workouts boost energy and focus."
        in 12..16 -> "Good Afternoon!" to "A short workout can reset your energy."
        in 17..20 -> "Good Evening!" to "Perfect time to release stress."
        else -> "Good Night!" to "A light session helps you unwind."
    }
}