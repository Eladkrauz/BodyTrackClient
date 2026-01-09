////////////////////////////////////////////////////////////
////////////// BODY TRACK // CLIENT // SESSION /////////////
////////////////////////////////////////////////////////////
//////////////////// FILE: FrameAnalyzer ///////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.session

///////////////
/// IMPORTS ///
///////////////
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.SystemClock
import android.util.Base64
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.bodytrack.client.network.SessionApi
import com.bodytrack.client.network.SessionResult
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

////////////////////////////
/// FRAME ANALYZER CLASS ///
////////////////////////////
/**
 * An [ImageAnalysis.Analyzer] that processes camera frames for a session, sends them to a remote API,
 * and handles the results.
 *
 * This class is designed to be a robust, thread-safe link between the CameraX pipeline and a
 * network backend. It manages several key aspects of real-time video streaming analysis:
 *
 * - **State Management:** Operates in three modes: `SENDING` (actively processing), `IDLE` (camera
 *   is running but analysis is paused), and `STOPPED` (permanently shut down).
 *
 * - **FPS Pacing:** Throttles frame analysis to a target frames-per-second (FPS) rate. It drops
 *   frames that arrive too early, ensuring a steady stream to the backend without causing bursts.
 *
 * - **Concurrency Control:** Allows a configurable number of network requests to be "in-flight"
 *   simultaneously (`maxInFlight`). This improves throughput by not waiting for each request to
 *   complete before sending the next. If the number of active requests reaches the limit, new
 *   frames are dropped until a request completes.
 *
 * - **Network Health Watchdog:** Monitors the time since the last successful response was received.
 *   If no responses arrive within a specified timeout (`networkStallTimeoutMs`), it triggers a
 *   one-time `onNetworkAbort` callback, signaling a potential network stall or server issue.
 *
 * - **Image Processing:** Converts frames from the camera's `YUV_420_888` format into a Base64-encoded
 *   JPEG string, which is a common format for JSON-based APIs.
 *
 */
class FrameAnalyzer(
    private val sessionId: String,
    private val api: SessionApi,
    private val onResult: (SessionResult) -> Unit,
    private val onNetworkAbort: () -> Unit
) : ImageAnalysis.Analyzer {
    // Mode.
    private enum class Mode { STOPPED, IDLE, SENDING }
    private val mode = AtomicReference(Mode.IDLE)

    // FPS pacing (time-based).
    @Volatile private var minIntervalMs: Long = 0L
    private val nextSendAtMs = AtomicLong(0L)

    // Concurrency control (allow multiple in-flight requests).
    private val maxInFlight = 6                       // tune: 4–8 is usually a safe range
    private val inFlightCount = AtomicInteger(0)

    // Identifiers.
    private val frameId = AtomicInteger(0)

    // Network health.
    // If we are in SENDING mode and we haven't received any response
    // for too long, we abort once.
    private val networkStallTimeoutMs = 10_000L
    private val abortEmitted = AtomicBoolean(false)
    private val lastResponseAtMs = AtomicLong(0L)

    /////////////////////
    /// START SENDING ///
    /////////////////////
    /**
     * Starts the frame sending process at the specified frames per second (FPS).
     *
     * This method sets the analyzer to `SENDING` mode. It calculates the minimum time interval
     * required between frames to maintain the target FPS.
     *
     * If `fps` is zero or negative, it will call [stopSending] instead.
     *
     * The internal state is reset to begin pacing frames from the moment this method is called,
     * preventing an initial burst of frames. It also resets the network watchdog timer.
     *
     * @param fps The target number of frames to send per second. Must be a positive integer.
     */
    fun startSending(fps: Int) {
        if (fps <= 0) {
            stopSending()
            return
        }

        minIntervalMs = 1000L / fps.coerceAtLeast(1)
        val now = SystemClock.elapsedRealtime()

        // Start pacing.
        nextSendAtMs.set(now)
        abortEmitted.set(false)

        // Reset watchdog timestamp so no abort happens.
        lastResponseAtMs.set(now)
        mode.set(Mode.SENDING)
    }

    ////////////////////
    /// STOP SENDING ///
    ////////////////////
    /**
     * Stops the frame sending process, putting the analyzer into `IDLE` mode.
     *
     * In this mode, the [analyze] method will immediately return without processing or sending
     * any new frames. However, the camera continues to run.
     *
     * This method does not interrupt or affect any network requests that are already in-flight.
     * Their completion callbacks will still be processed to maintain a consistent state,
     * but the results will not be forwarded to the `onResult` listener.
     */
    fun stopSending() {
        mode.set(Mode.IDLE)
    }

    ////////////////
    /// STOP ALL ///
    ////////////////
    /**
     * Permanently stops the analyzer.
     *
     * This method sets the analyzer's mode to `STOPPED`. Once in this state, the analyzer will
     * no longer process any new frames from the camera, and it cannot be restarted. This is
     * intended for a final shutdown when the session is completely finished.
     */
    fun stopAll() {
        mode.set(Mode.STOPPED)
    }

    ///////////////
    /// ANALYZE ///
    ///////////////
    /**
     * The core analysis callback from CameraX, invoked for each new camera frame.
     *
     * This function implements the primary logic for processing, throttling, and sending frames. It is
     * designed to be thread-safe and robust, handling various real-time conditions:
     *
     * 1.  **Mode Check:** Immediately returns if the analyzer is not in `SENDING` mode, ignoring the frame.
     * 2.  **Network Watchdog:** Checks if the time since the last network response exceeds `networkStallTimeoutMs`.
     *     If it does, it triggers a one-time `onNetworkAbort` callback and stops processing this frame.
     * 3.  **FPS Pacing:** Drops the frame if not enough time has passed since the last sent frame,
     *     enforcing the target FPS rate.
     * 4.  **Concurrency Limiting:** Drops the frame if the number of `inFlightCount` network requests
     *     has reached `maxInFlight`.
     * 5.  **Dispatch:** If all checks pass, it:
     *     - Increments the `inFlightCount`.
     *     - Updates the send schedule for the next frame to prevent bursts.
     *     - Converts the `ImageProxy` to a Base64-encoded JPEG.
     *     - Calls the `api.analyzeFrame` method to send the data.
     *
     * The asynchronous network callback handles decrementing `inFlightCount`, updating the network
     * watchdog timer, and forwarding the result via `onResult`.
     *
     * A `finally` block ensures `image.close()` is always called to prevent the CameraX pipeline
     * from stalling. A `catch` block prevents `inFlightCount` from leaking if an exception occurs
     * before the network call is dispatched.
     *
     * @param image The camera frame to be analyzed, provided by CameraX.
     */
    override fun analyze(image: ImageProxy) {
        try {
            val currentMode = mode.get()
            if (currentMode != Mode.SENDING) return

            val now = SystemClock.elapsedRealtime()

            // If responses stall, abort once.
            val lastResp = lastResponseAtMs.get()
            if (!abortEmitted.get() && (now - lastResp) > networkStallTimeoutMs) {
                abortEmitted.set(true)
                onNetworkAbort()
                return
            }

            // FPS pacing: send only when allowed by time schedule.
            val nextAt = nextSendAtMs.get()
            if (minIntervalMs > 0 && now < nextAt) return

            // Concurrency limit: if too many in-flight, drop this frame.
            if (inFlightCount.get() >= maxInFlight) return

            // Reserve a slot (race-safe).
            inFlightCount.incrementAndGet()

            // Advance schedule *immediately* to avoid bursts when camera calls analyze rapidly.
            val scheduledNext = nextAt + minIntervalMs
            if (minIntervalMs > 0) {
                nextSendAtMs.set(if (scheduledNext <= now) now + minIntervalMs else scheduledNext)
            } else {
                nextSendAtMs.set(now)
            }

            // Encode + send.
            val payload = imageToBase64(image)
            val id = frameId.getAndIncrement()

            api.analyzeFrame(
                sessionId = sessionId,
                frameId = id,
                frameContent = payload
            ) { result ->
                // Mark response arrival for watchdog.
                lastResponseAtMs.set(SystemClock.elapsedRealtime())

                // Only forward results while we're actively sending.
                if (mode.get() == Mode.SENDING) {
                    onResult(result)
                }
                inFlightCount.decrementAndGet()
            }

        } catch (_: Exception) {
            // Ensure we never leak an in-flight slot if something fails before callback.
            if (inFlightCount.get() > 0) {
                inFlightCount.decrementAndGet()
            }
        } finally {
            // Always close to avoid camera pipeline stalls.
            image.close()
        }
    }

    ////////////////////////
    /// IMAGE TO BASE 64 ///
    ////////////////////////
    /**
     * Converts an [ImageProxy] in YUV_420_888 format to a Base64-encoded JPEG string.
     *
     * This multi-step process is required to prepare the camera frame for network transmission,
     * as many APIs expect a simple string payload.
     *
     * 1.  **YUV_420_888 to NV21:** The incoming [ImageProxy] provides image data in three separate
     *     planes (Y, U, and V). This data is manually rearranged into a single `ByteArray` with
     *     the NV21 format layout (a full Y plane followed by an interleaved V/U plane).
     * 2.  **NV21 to JPEG:** The resulting `YuvImage` is then compressed into the JPEG format.
     *     This significantly reduces the data size. A quality of 70 is used as a balance
     *     between size and visual fidelity.
     * 3.  **JPEG to Base64:** The binary JPEG data is encoded into a Base64 string, making it
     *     safe to embed in JSON or other text-based protocols. `Base64.NO_WRAP` is used to
     *     prevent line breaks in the output string.
     *
     * @param image The [ImageProxy] from the camera, expected to be in `YUV_420_888` format.
     * @return A Base64-encoded string representing the compressed JPEG image.
     */
    private fun imageToBase64(image: ImageProxy): String {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // NV21 expects: Y... V... U...
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            Rect(0, 0, image.width, image.height),
            70,
            out
        )
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}