////////////////////////////////////////////////////////////
////////////// BODY TRACK // CLIENT // SESSION /////////////
////////////////////////////////////////////////////////////
//////////////// FILE: CameraSessionActivity ///////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.session

///////////////
/// IMPORTS ///
///////////////
import android.Manifest
import android.content.pm.PackageManager
import android.os.*
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.bodytrack.client.R
import com.bodytrack.client.home.CameraSide
import com.bodytrack.client.network.SessionApi
import com.bodytrack.client.network.SessionResult
import com.bodytrack.client.network.protocol.CalibrationCode
import com.bodytrack.client.network.protocol.ErrorCode
import java.util.concurrent.Executors
import android.util.Log
import com.bodytrack.client.network.protocol.ManagementCode
import android.media.AudioAttributes
import android.media.SoundPool
import android.widget.Toast
import com.bodytrack.client.network.protocol.FeedbackCode

///////////////////////////////
/// CAMERA SESSION ACTIVITY ///
///////////////////////////////
/**
 * An [AppCompatActivity] that manages a real-time camera-based exercise session.
 *
 * This activity is responsible for:
 * 1.  **Camera Setup**: Requesting camera permissions and initializing CameraX for preview and image analysis.
 * 2.  **User Guidance**: Using a state machine (`Stage`) and Text-To-Speech (`TtsManager`), it guides the
 *     user through a multi-step calibration process. This includes verifying user visibility and ensuring
 *     they are in the correct starting position for the exercise.
 * 3.  **Frame Analysis**: Capturing frames from the camera, passing them to a [FrameAnalyzer], which
 *     sends them to a backend API for processing.
 * 4.  **Real-time Feedback**: Receiving analysis results ([SessionResult]) from the backend and
 *     communicating them to the user via TTS. This includes calibration success/failure and corrective
 *     feedback during the active exercise.
 * 5.  **Session Lifecycle**: Managing the session's duration with a countdown timer, handling session
 *     start/end logic, and cleaning up resources (camera, threads, TTS) on destruction.
 *
 * The activity flow is controlled by the [Stage] enum, transitioning from initial setup
 * (`BOOT_DELAY`, `INTRO_VISIBILITY`, `POSITION_ANALYSIS`, etc.) to the main exercise (`ACTIVE`),
 * and finally to the `ENDED` state.
 *
 * It receives `session_id`, `duration_seconds`, and `camera_side` via its launch intent.
 * It returns `RESULT_OK` if the session completes successfully or `RESULT_CANCELED` if it's aborted.
 */
class CameraSessionActivity : AppCompatActivity() {

    // UI and core.
    private lateinit var previewView: PreviewView
    private lateinit var ttsManager: TtsManager
    private lateinit var sessionId: String

    // Suppress TTS while ending beeps are playing.
    private var suppressTts = false

    // CameraX.
    private var cameraProvider: ProcessCameraProvider? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private lateinit var frameAnalyzer: FrameAnalyzer

    // Timing.
    private val mainHandler = Handler(Looper.getMainLooper())
    private var sessionTimer: CountDownTimer? = null
    private var durationSeconds = 120
    private var cameraSide: String = CameraSide.FRONT.name.lowercase()
    private var audioFeedbackEnabled: Boolean = true
    private var textFeedbackEnabled: Boolean = true

    // Stage machine (FSM).
    private enum class Stage {
        BOOT_DELAY,
        INTRO_VISIBILITY,
        WAIT_BEFORE_VISIBILITY,
        VISIBILITY_ANALYSIS,
        VISIBILITY_REMINDER,
        VISIBILITY_DONE,
        INTRO_POSITION,
        POSITION_ANALYSIS,
        POSITION_REMINDER,
        READY_COUNTDOWN,
        ACTIVE,
        ENDED
    }

    enum class SessionEndReason {
        COMPLETED,
        ABORTED
    }

    private var stage = Stage.BOOT_DELAY
    private var framesSinceLastPrompt = 0

    // Permissions.
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else {
                if (!suppressTts) {
                    ttsManager.speak("Camera permission is required to continue.")
                }
            }
        }

    // Beep sound.
    private lateinit var soundPool: SoundPool
    private var beepLoaded = false
    private var beepSoundId: Int = 0

    // Lifecycle.
    /**
     * Initializes the activity. This is where the activity's lifecycle begins.
     *
     * This function performs the following key initializations:
     * 1.  Sets the content view from the XML layout `activity_camera_session`.
     * 2.  Initializes the [SoundPool] for playing beep sounds, including setting up audio attributes
     *     and loading the beep sound resource.
     * 3.  Finds UI elements like the `previewView` and the end session button, and sets the
     *     button's click listener to call [endSession].
     * 4.  Initializes the [TtsManager] for text-to-speech functionality.
     * 5.  Retrieves session details (`sessionId`, `durationSeconds`, `cameraSide`) from the intent extras.
     *     If the `sessionId` is missing, the activity finishes immediately.
     * 6.  Checks for camera permissions. If granted, it calls [startCamera]. If not, it launches
     *     the permission request flow.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *                           shut down, this Bundle contains the data it most recently supplied in
     *                           [onSaveInstanceState]. Otherwise, it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_session)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && sampleId == beepSoundId) {
                beepLoaded = true
                Log.d("Sound", "Beep sound loaded successfully")
            }
        }

        beepSoundId = soundPool.load(this, R.raw.beep_sound, 1)
        previewView = findViewById(R.id.previewView)
        findViewById<Button>(R.id.btnEndSession).setOnClickListener {
            if (stage == Stage.ACTIVE || stage == Stage.ENDED) {
                endSession(SessionEndReason.COMPLETED)
            } else {
                endSession(SessionEndReason.ABORTED)
            }
        }

        ttsManager = TtsManager(this)
        sessionId = intent.getStringExtra("session_id") ?: run { finish(); return }
        durationSeconds = intent.getIntExtra("duration_seconds", 120)
        cameraSide = intent.getStringExtra("camera_side")
            ?: CameraSide.FRONT.name.lowercase()
        audioFeedbackEnabled = intent.getBooleanExtra("audio_feedback", true)
        textFeedbackEnabled = intent.getBooleanExtra("text_feedback", true)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) startCamera()
        else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    /**
     * Cleans up resources when the activity is destroyed.
     * This method ensures that all components related to the camera session are properly
     * released to prevent memory leaks and unexpected behavior. It cancels any ongoing timers,
     * removes pending messages from the handler, unbinds camera use cases, shuts down the
     * analysis executor, and releases the TTS manager and sound pool resources.
     */
    override fun onDestroy() {
        super.onDestroy()
        stage = Stage.ENDED
        sessionTimer?.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        cameraProvider?.unbindAll()
        analysisExecutor.shutdown()
        ttsManager.shutdown()
        soundPool.release()
    }

    ////////////////////
    /// START CAMERA ///
    ////////////////////
    /**
     * Initializes and starts the camera.
     *
     * This function performs the following steps:
     * 1. Retrieves an instance of `ProcessCameraProvider`.
     * 2. Sets up the `Preview` use case to display the camera feed on the `previewView`.
     * 3. Sets up the `ImageAnalysis` use case with a `FrameAnalyzer` to process camera frames.
     *    - The `FrameAnalyzer` sends frames to the backend API for analysis.
     *    - It handles callbacks for session results (`onResult`) and network failures (`onNetworkAbort`).
     * 4. Determines which camera to use (front or rear) based on the `cameraSide` preference.
     * 5. Unbinds any previous camera use cases and binds the new `Preview` and `ImageAnalysis`
     *    use cases to the activity's lifecycle.
     * 6. Calls `startScript()` to begin the session's state machine logic.
     *
     * The entire setup is performed asynchronously, using a listener on the `ProcessCameraProvider` future.
     */
    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({
            cameraProvider = providerFuture.get()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            frameAnalyzer = FrameAnalyzer(
                sessionId = sessionId,
                api = SessionApi,
                onResult = { runOnUiThread { handleSessionResult(it) } },
                onNetworkAbort = {
                    runOnUiThread {
                        if (!suppressTts) {
                            ttsManager.speak("Network connection lost. Ending session.")
                        }
                        endSession(SessionEndReason.ABORTED)
                    }
                }
            )
            analysis.setAnalyzer(analysisExecutor, frameAnalyzer)

            val selector = when (cameraSide) {
                CameraSide.REAR.name.lowercase() -> CameraSelector.DEFAULT_BACK_CAMERA
                else -> CameraSelector.DEFAULT_FRONT_CAMERA
            }
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(this, selector, preview, analysis)
            startScript()

        }, ContextCompat.getMainExecutor(this))
    }

    ////////////////////
    /// START SCRIPT ///
    ////////////////////
    /**
     * Initiates the session's state machine by transitioning to the first stage.
     * This is the entry point for the entire scripted sequence of the session,
     * starting from initial checks to the active exercise phase.
     */
    private fun startScript() {
        enterStage(Stage.BOOT_DELAY)
    }

    ///////////////////
    /// ENTER STAGE ///
    ///////////////////
    /**
     * Manages the state machine for the session, transitioning to the specified [next] stage.
     * Each stage defines a specific part of the session flow, such as user visibility checks,
     * position calibration, active exercise, and session conclusion. This function orchestrates
     * the logic for entering each stage, including starting/stopping frame analysis,
     * providing TTS (Text-To-Speech) instructions to the user, and setting up timers.
     *
     * @param next The [Stage] to transition into.
     */
    private fun enterStage(next: Stage) {
        stage = next
        framesSinceLastPrompt = 0
        Log.d("State", stage.name)
        when (stage) {

            Stage.BOOT_DELAY -> {
                frameAnalyzer.stopSending()
                mainHandler.postDelayed({
                    enterStage(Stage.INTRO_VISIBILITY)
                }, 3_000)
            }

            Stage.INTRO_VISIBILITY -> {
                if (!suppressTts) {
                    ttsManager.speak(
                        "Please stand in front of the camera. We need to check your visibility and camera angle."
                    ) {
                        mainHandler.postDelayed({
                            enterStage(Stage.WAIT_BEFORE_VISIBILITY)
                        }, 5_000)
                    }
                } else {
                    mainHandler.postDelayed({
                        enterStage(Stage.WAIT_BEFORE_VISIBILITY)
                    }, 5_000)
                }
            }

            Stage.WAIT_BEFORE_VISIBILITY -> {
                if (!suppressTts) {
                    ttsManager.speak("Checking is in progress, please hold.") {
                        enterStage(Stage.VISIBILITY_ANALYSIS)
                    }
                } else {
                    enterStage(Stage.VISIBILITY_ANALYSIS)
                }
            }

            Stage.VISIBILITY_ANALYSIS -> {
                frameAnalyzer.startSending(fps = 3)
            }

            Stage.VISIBILITY_REMINDER -> {
                frameAnalyzer.stopSending()
                if (!suppressTts) {
                    ttsManager.speak("Please make sure your visibility is clear.") {
                        enterStage(Stage.VISIBILITY_ANALYSIS)
                    }
                } else {
                    enterStage(Stage.VISIBILITY_ANALYSIS)
                }
            }

            Stage.VISIBILITY_DONE -> {
                frameAnalyzer.stopSending()
                if (!suppressTts) {
                    ttsManager.speak("Thanks, we can see you perfectly.") {
                        mainHandler.postDelayed({
                            enterStage(Stage.INTRO_POSITION)
                        }, 1_000)
                    }
                } else {
                    mainHandler.postDelayed({
                        enterStage(Stage.INTRO_POSITION)
                    }, 1_000)
                }
            }

            Stage.INTRO_POSITION -> {
                if (!suppressTts) {
                    ttsManager.speak(
                        "We now need to check you are standing in the correct initial pose for the exercise."
                    ) {
                        enterStage(Stage.POSITION_ANALYSIS)
                    }
                } else {
                    enterStage(Stage.POSITION_ANALYSIS)
                }
            }

            Stage.POSITION_ANALYSIS -> {
                frameAnalyzer.startSending(fps = 3)
            }

            Stage.POSITION_REMINDER -> {
                frameAnalyzer.stopSending()
                if (!suppressTts) {
                    ttsManager.speak(
                        "Please make sure you are standing in the initial position."
                    ) {
                        enterStage(Stage.POSITION_ANALYSIS)
                    }
                } else {
                    enterStage(Stage.POSITION_ANALYSIS)
                }
            }

            Stage.READY_COUNTDOWN -> {
                frameAnalyzer.stopSending()
                if (!suppressTts) {
                    ttsManager.speak(
                        "Okay, you are all set. Session is starting in three. Two. One. Go!"
                    ) {
                        startAnalysis()
                    }
                } else {
                    startAnalysis()
                }
            }

            Stage.ACTIVE -> {
                frameAnalyzer.startSending(fps = 10)
                startSessionTimer()
            }

            Stage.ENDED -> frameAnalyzer.stopSending()
        }
    }

    /////////////////////////////
    /// HANDLE SESSION RESULT ///
    /////////////////////////////
    /**
     * Processes the results received from the session API via the [FrameAnalyzer].
     *
     * This function acts as a state machine handler, deciding what to do based on the current
     * `stage` of the session and the `SessionResult` received.
     *
     * - During calibration stages (`VISIBILITY_ANALYSIS`, `POSITION_ANALYSIS`), it checks for
     *   successful calibration messages to advance the state. It also implements a timeout
     *   mechanism to remind the user if calibration takes too long.
     * - During the `ACTIVE` stage, it provides real-time feedback to the user via TTS and Toasts,
     *   ignoring non-actionable or suppressed feedback.
     * - It handles critical errors, such as `SESSION_SHOULD_ABORT`, by terminating the session.
     *
     * @param result The [SessionResult] object containing feedback, calibration status, or errors
     * from the backend.
     */
    private fun handleSessionResult(result: SessionResult) {
        if (stage == Stage.VISIBILITY_ANALYSIS) {
            framesSinceLastPrompt++
            if (framesSinceLastPrompt >= 15) {
                enterStage(Stage.VISIBILITY_REMINDER)
                return
            }
        }

        if (stage == Stage.POSITION_ANALYSIS) {
            framesSinceLastPrompt++
            if (framesSinceLastPrompt >= 15) {
                enterStage(Stage.POSITION_REMINDER)
                return
            }
        }

        when (result) {
            is SessionResult.Success.Calibration -> {
                when (result.code) {
                    CalibrationCode.USER_VISIBILITY_IS_VALID ->
                        if (stage == Stage.VISIBILITY_ANALYSIS)
                            enterStage(Stage.VISIBILITY_DONE)

                    CalibrationCode.USER_POSITIONING_IS_VALID ->
                        if (stage == Stage.POSITION_ANALYSIS)
                            enterStage(Stage.READY_COUNTDOWN)

                    else -> {}
                }
            }

            is SessionResult.Error -> {
                if (result.code == ErrorCode.SESSION_SHOULD_ABORT) {
                    if (!suppressTts) {
                        ttsManager.speak("You are not visible. Aborting session.")
                    }
                    endSession(SessionEndReason.ABORTED)
                }
            }

            is SessionResult.Success.Feedback -> {
                // Ignore during non-active stages.
                if (stage != Stage.ACTIVE) return

                // Ignore VALID / SILENT
                if (
                    result.code == FeedbackCode.VALID ||
                    result.code == FeedbackCode.SILENT
                ) return

                // Respect beep suppression.
                if (suppressTts) return

                // Display and/or play feedback.
                if (textFeedbackEnabled) {
                    Toast.makeText(this, result.description, Toast.LENGTH_SHORT).show()
                }
                if (audioFeedbackEnabled) {
                    ttsManager.speak(result.description)
                }
            }

            else -> {}
        }
    }

    ///////////////////////////
    /// START SESSION TIMER ///
    ///////////////////////////
    /**
     * Starts the main session countdown timer.
     * The timer's duration is determined by [durationSeconds].
     * During the last 5 seconds of the countdown, it plays a beep sound on each tick
     * and suppresses any Text-To-Speech (TTS) feedback to avoid overlap.
     * When the timer finishes, it calls [endSession] with a [SessionEndReason.COMPLETED] reason.
     */
    private fun startSessionTimer() {
        sessionTimer = object : CountDownTimer(durationSeconds * 1000L, 1000) {

            override fun onTick(ms: Long) {
                val secondsLeft = ms / 1000
                if (secondsLeft in 1..5) {
                    suppressTts = true
                    if (beepLoaded) {
                        soundPool.play(
                            beepSoundId,
                            1f,
                            1f,
                            1,
                            0,
                            1.0f
                        )
                    }
                }
            }
            override fun onFinish() {
                endSession(SessionEndReason.COMPLETED)
            }
        }.start()
    }

    //////////////////////
    /// START ANALYSIS ///
    //////////////////////
    /**
     * Initiates the analysis phase of the session with the backend.
     *
     * This function should only be called when the session is in the [Stage.READY_COUNTDOWN] stage.
     * It sends a request to the [SessionApi] to begin the analysis for the current [sessionId].
     * The function handles the API response:
     *  - On success ([ManagementCode.SESSION_IS_STARTING]), it transitions the session to the [Stage.ACTIVE] stage.
     *  - On specific errors or network failures, it announces the issue via TTS (if not suppressed) and aborts the session.
     *  - It also handles incoming [SessionResult.Success.Feedback] during the active stage, providing audio cues and TTS feedback to the user for corrections.
     *  - Any other unexpected responses will result in the session being aborted.
     */
    private fun startAnalysis() {
        if (stage != Stage.READY_COUNTDOWN) return

        SessionApi.startAnalysis(sessionId) { result ->
            when (result) {
                is SessionResult.Error -> {
                    val reason = result.description
                    if (!suppressTts) {
                        ttsManager.speak(reason) {
                            endSession(SessionEndReason.ABORTED)
                        }
                    } else {
                        endSession(SessionEndReason.ABORTED)
                    }
                }

                is SessionResult.NetworkFailure -> {
                    if (!suppressTts) {
                        ttsManager.speak("Network failure.")
                        endSession(SessionEndReason.ABORTED)
                    } else {
                        endSession(SessionEndReason.ABORTED)
                    }
                }

                is SessionResult.Success.Management -> {
                    if (result.code == ManagementCode.SESSION_IS_STARTING) {
                        enterStage(Stage.ACTIVE)
                    }
                }

                is SessionResult.Success.Feedback -> {
                    Log.d(
                        "FEEDBACK",
                        "Received feedback: code=${result.code}, desc=${result.description}"
                    )

                    if (stage == Stage.ACTIVE &&
                        !suppressTts &&
                        result.code != FeedbackCode.VALID &&
                        result.code != FeedbackCode.SILENT
                    ) {
                        if (beepLoaded) {
                            soundPool.play(
                                beepSoundId,
                                1f,
                                1f,
                                1,
                                0,
                                1.0f
                            )
                        }
                        ttsManager.speak(result.description)
                    }
                }

                else -> {
                    if (!suppressTts) {
                        ttsManager.speak("Unknown error.")
                    }
                    endSession(SessionEndReason.ABORTED)
                }
            }
        }
    }

    ///////////////////
    /// END SESSION ///
    ///////////////////
    /**
     * Terminates the current session and performs cleanup.
     *
     * This function is idempotent; it will do nothing if the session has already ended.
     * It stops the session timer, stops sending frames for analysis, and makes a network
     * call to inform the backend that the session is over.
     *
     * Based on the reason for ending, it sets the activity result to either
     * `RESULT_OK` (for completed sessions) or `RESULT_CANCELED` (for aborted sessions)
     * before finishing the activity.
     *
     * @param reason The reason why the session is being ended (e.g., COMPLETED, ABORTED).
     */
    private fun endSession(reason: SessionEndReason) {
        if (stage == Stage.ENDED) return
        stage = Stage.ENDED
        sessionTimer?.cancel()
        frameAnalyzer.stopSending()

        SessionApi.endSession(sessionId) {
            if (reason == SessionEndReason.COMPLETED) {
                setResult(RESULT_OK)
            }
            else {
                setResult(RESULT_CANCELED)
            }
            finish()
        }
    }
}