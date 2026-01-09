////////////////////////////////////////////////////////////
////////////// BODY TRACK // CLIENT // NETWORK /////////////
////////////////////////////////////////////////////////////
//////////////////// FILE: SessionAPI //////////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.network

///////////////
/// IMPORTS ///
///////////////
import android.os.Handler
import android.os.Looper
import com.bodytrack.client.network.protocol.CalibrationCode
import com.bodytrack.client.network.protocol.ErrorCode
import com.bodytrack.client.network.protocol.FeedbackCode
import com.bodytrack.client.network.protocol.ManagementCode
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.bodytrack.client.network.protocol.MessageType
import com.bodytrack.client.network.protocol.ResponseType
import com.bodytrack.client.network.protocol.SessionSummary
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

///////////////////
/// SESSION API ///
///////////////////
/**
 * A singleton object that provides an API for interacting with the Body Track session server.
 *
 * This object encapsulates all network communication, handling the sending of requests
 * and the parsing of responses for managing and analyzing exercise sessions. It uses
 * OkHttp for asynchronous network calls and posts results back to the main thread.
 *
 * The API supports various session-related actions, including:
 * - Registering, starting, pausing, resuming, and ending a session.
 * - Sending individual frames for real-time analysis.
 * - Checking session status and retrieving a final session summary.
 * - A `ping` method to check server connectivity.
 *
 * All public methods are asynchronous and accept a callback lambda `onResult: (SessionResult) -> Unit`
 * which will be invoked on the main UI thread with the result of the operation.
 * Results are wrapped in a `SessionResult` sealed class, which can represent success,
 * an application-level error, or a network failure.
 *
 * @see SessionResult for possible outcomes of an API call.
 * @see OkHttpClient for the underlying HTTP client implementation.
 */
object SessionApi {
    // Configuration.
    private const val BASE_URL = "http://46.101.195.128:8080"
    private val JSON = "application/json".toMediaType()
    private val mainHandler = Handler(Looper.getMainLooper())

    // HTTP client.
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    /////////////////
    /// SEND POST ///
    /////////////////
    /**
     * Sends an asynchronous POST request to a specified server endpoint.
     *
     * This is the core function for making API calls. It constructs a POST request
     * with the given JSON payload, sends it to the server, and handles the response
     * asynchronously.
     *
     * It uses OkHttp's `enqueue` method for non-blocking network I/O.
     * Responses and network failures are processed, parsed into a `SessionResult`,
     * and then delivered to the `onResult` callback on the main UI thread.
     *
     * @param endpoint The specific API endpoint to which the request will be sent (e.g., "register/new/session").
     * @param payload The `JSONObject` containing the data to be sent in the request body.
     * @param onResult A callback function that will be invoked on the main thread with the result of the
     * network operation, encapsulated in a `SessionResult` object. This will be either a success,
     * an application-level error, or a network failure.
     */
    private fun sendPost(
        endpoint: String,
        payload: JSONObject,
        onResult: (SessionResult) -> Unit
    ) {
        val request = Request.Builder()
            .url("$BASE_URL/$endpoint")
            .post(payload.toString().toRequestBody(JSON))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    onResult(SessionResult.NetworkFailure(e))
                }
            }

            /**
             * Callback invoked when an HTTP response is received from the server.
             *
             * This function handles the response by:
             * 1. Reading the response body as a string.
             * 2. Checking for an empty response body and returning an error if it's null.
             * 3. Attempting to parse the body as a JSON object.
             * 4. If JSON parsing is successful, it calls `parseServerResponse` to convert the JSON
             *    into a `SessionResult`.
             * 5. If JSON parsing fails, it returns an internal server error.
             * 6. Finally, it posts the resulting `SessionResult` (be it success, error, or failure)
             *    to the main thread's message queue to be handled by the `onResult` callback.
             *
             * The `response.use` block ensures the response body is closed automatically.
             *
             * @param call The original `Call` that resulted in this response.
             * @param response The `Response` from the server.
             */
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string()

                    if (body == null) {
                        mainHandler.post {
                            onResult(
                                SessionResult.Error(
                                    code = ErrorCode.INTERNAL_SERVER_ERROR,
                                    description = "Empty response body"
                                )
                            )
                        }
                        return
                    }

                    try {
                        val json = JSONObject(body)
                        val parsed = parseServerResponse(json)
                        mainHandler.post {
                            onResult(parsed)
                        }
                    } catch (ex: Exception) {
                        mainHandler.post {
                            onResult(
                                SessionResult.Error(
                                    code = ErrorCode.INTERNAL_SERVER_ERROR,
                                    description = "Invalid JSON response"
                                )
                            )
                        }
                    }
                }
            }
        })
    }

    /////////////////////////////
    /// PARSE SERVER RESPONSE ///
    /////////////////////////////
    /**
     * Parses a JSON object from the server into a structured [SessionResult].
     *
     * This function is the core of the client's response handling logic. It deciphers the
     * server's message structure, which is expected to contain a `message_type`.
     *
     * - If `message_type` is [MessageType.ERROR], it parses the error details into a
     *   [SessionResult.Error].
     * - If `message_type` is [MessageType.RESPONSE], it further inspects the `response_type`
     *   to determine the specific kind of successful response (e.g., Calibration, Management,
     *   Feedback, Ping, Summary) and constructs the corresponding [SessionResult.Success] subclass.
     *
     * The function handles unknown or missing fields gracefully by returning a
     * [SessionResult.Error] with an appropriate error code and description, preventing crashes
     * due to unexpected server responses.
     *
     * @param json The [JSONObject] received from the server.
     * @return A [SessionResult] instance representing the parsed outcome, which can be either
     *         a specific [SessionResult.Success] type or a [SessionResult.Error].
     */
    private fun parseServerResponse(json: JSONObject): SessionResult {
        val messageType =
            MessageType.from(json.optInt("message_type"))
                ?: return SessionResult.Error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Unknown message_type"
                )

        // Handle each message type.
        return when (messageType) {
            MessageType.ERROR -> {
                val errorCode =
                    ErrorCode.from(json.optInt("code"))
                        ?: ErrorCode.INTERNAL_SERVER_ERROR

                SessionResult.Error(
                    code = errorCode,
                    description = json.optString("description")
                )
            }

            MessageType.RESPONSE -> {
                val responseType =
                    ResponseType.from(json.optInt("response_type"))
                        ?: ResponseType.UNKNOWN

                val description = json.optString("description")
                val extraInfo   = json.optJSONObject("extra_info")
                val rawCode     = json.optInt("code")

                // Handle each response type.
                when (responseType) {
                    // If it is a calibration response, parse the code.
                    ResponseType.CALIBRATION -> {
                        val code =
                            CalibrationCode.from(rawCode)
                                ?: return SessionResult.Error(
                                    ErrorCode.INTERNAL_SERVER_ERROR,
                                    "Unknown CalibrationCode"
                                )

                        SessionResult.Success.Calibration(
                            code = code,
                            description = description,
                            extraInfo = extraInfo
                        )
                    }

                    // If it is a management response, parse the code.
                    ResponseType.MANAGEMENT -> {
                        val code =
                            ManagementCode.from(rawCode)
                                ?: return SessionResult.Error(
                                    ErrorCode.INTERNAL_SERVER_ERROR,
                                    "Unknown ManagementCode"
                                )

                        SessionResult.Success.Management(
                            code = code,
                            description = description,
                            extraInfo = extraInfo
                        )
                    }

                    // If it is a feedback response, parse the code.
                    ResponseType.FEEDBACK -> {
                        val code =
                            FeedbackCode.from(rawCode)
                                ?: return SessionResult.Error(
                                    ErrorCode.INTERNAL_SERVER_ERROR,
                                    "Unknown FeedbackCode"
                                )

                        SessionResult.Success.Feedback(
                            code = code,
                            description = description,
                            extraInfo = extraInfo
                        )
                    }

                    // If it is a ping response, parse the description.
                    ResponseType.PING -> {
                        if (description.equals("pong")) {
                            SessionResult.Success.Ping(description)
                        }
                        else {
                            SessionResult.Error(
                                code = ErrorCode.INTERNAL_SERVER_ERROR,
                                description = "Server is not reachable"
                            )
                        }
                    }

                    // If it is a summary response, parse the summary.
                    ResponseType.SUMMARY -> {
                        SessionResult.Success.Summary(
                            summary = SessionSummary(
                                sessionId = json.optString("session_id"),
                                exerciseType = json.optString("exercise_type"),
                                sessionDurationSeconds = json.optDouble("session_duration_seconds"),
                                numberOfReps = json.optInt("number_of_reps"),
                                averageRepDurationSeconds = json.optDouble("average_rep_duration_seconds"),
                                overallGrade = json.optDouble("overall_grade"),
                                repBreakdown = json.optJSONArray("rep_breakdown")
                                    ?.let { arr ->
                                        List(arr.length()) { i -> arr.getJSONObject(i) }
                                    } ?: emptyList(),
                                aggregatedErrors = json.optJSONObject("aggregated_errors")
                                    ?: JSONObject(),
                                recommendations = json.optJSONArray("recommendations")
                                    ?.let { arr ->
                                        List(arr.length()) { i -> arr.getString(i) }
                                    } ?: emptyList()
                            )
                        )
                    }

                    // If it is an unknown response, return an error.
                    else -> {
                        SessionResult.Error(
                            ErrorCode.INTERNAL_SERVER_ERROR,
                            "Unsupported response type"
                        )
                    }
                }
            }

            // If it is an unknown message type, return an error.
            else -> {
                SessionResult.Error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Invalid message type"
                )
            }
        }
    }

    ////////////
    /// PING ///
    ////////////
    /**
     * Sends a GET request to the `/ping` endpoint to check for server connectivity.
     *
     * This is a simple health check to ensure the server is reachable and responding correctly.
     * The server is expected to respond with a JSON object containing a "pong" message.
     *
     * The result is delivered asynchronously to the provided callback.
     *
     * @param onResult A callback function that will be invoked on the main thread with the
     *                 result of the operation. This will be either a [SessionResult.Success.Ping]
     *                 on success, a [SessionResult.Error] if the server responds with an error,
     *                 or a [SessionResult.NetworkFailure] if a network-level error occurs.
     */
    fun ping(onResult: (SessionResult) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/ping")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    onResult(SessionResult.NetworkFailure(e))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string()
                    mainHandler.post {
                        if (body == null) {
                            onResult(
                                SessionResult.Error(ErrorCode.INTERNAL_SERVER_ERROR, "Empty response body")
                            )
                        } else {
                            onResult(parseServerResponse(JSONObject(body)))
                        }
                    }
                }
            }
        })
    }

    ////////////////////////
    /// REGISTER SESSION ///
    ////////////////////////
    /**
     * Registers a new exercise session with the server.
     *
     * This function sends a request to the server to create a new session for a specific
     * type of exercise. The server will respond with a unique session ID if successful.
     * The result of the operation, whether successful or an error, is communicated
     * through the `onResult` callback.
     *
     * @param exerciseType A string identifier for the type of exercise to be performed
     *                     (e.g., "squat", "pushup").
     * @param onResult A callback function that will be invoked with the result of the
     *                 registration attempt. The result will be a [SessionResult] object,
     *                 which can be a success (containing the session ID) or a failure.
     */
    fun registerSession(
        exerciseType: String,
        onResult: (SessionResult) -> Unit
    ) {
        sendPost(
            "register/new/session",
            JSONObject().put("exercise_type", exerciseType),
            onResult
        )
    }

    //////////////////////////
    /// UNREGISTER SESSION ///
    //////////////////////////
    /**
     * Unregisters a session from the server.
     *
     * This is a "fire-and-forget" operation, meaning it sends the request to the server
     * to clean up the session resources but does not wait for or handle a specific response.
     * This is typically used when the client-side session is being destroyed (e.g., user navigates away).
     *
     * @param sessionId The unique identifier of the session to unregister.
     */
    fun unregisterSession(sessionId: String) {
        sendPost(
            "unregister/session",
            JSONObject().put("session_id", sessionId)
        ) {}
    }

    /////////////////////
    /// START SESSION ///
    /////////////////////
    /**
     * Starts a previously registered session.
     * This signals the server to begin the real-time evaluation process.
     *
     * @param sessionId The unique identifier for the session to be started.
     * @param onResult A callback function that will be invoked with the result of the
     *   operation, either a [SessionResult.Success] or a [SessionResult.Error]/[SessionResult.NetworkFailure].
     */
    fun startSession(
        sessionId: String,
        onResult: (SessionResult) -> Unit
    ) {
        sendPost(
            "start/session",
            JSONObject()
                .put("session_id", sessionId),
            onResult
        )
    }

    //////////////////////
    /// START ANALYSIS ///
    //////////////////////
    /**
     * Triggers the start of a separate analysis thread on the server for a given session.
     *
     * This is useful for processing recorded sessions or performing analysis on-demand
     * after the main session has ended. The server will respond with a confirmation
     * that the analysis process has been initiated.
     *
     * The result of the analysis itself is not returned by this call; it should be retrieved
     * later, for example, by polling the session status or requesting a session summary.
     *
     * @param sessionId The unique identifier for the session to be analyzed.
     * @param onResult A callback that will be invoked with the result of the request.
     *                 On success, this is typically a [SessionResult.Success.Management]
     *                 message confirming the analysis has started. On failure, it will be
     *                 a [SessionResult.Error] or [SessionResult.NetworkFailure].
     */
    fun startAnalysis(
        sessionId: String,
        onResult: (SessionResult) -> Unit
    ) {
        sendPost(
            "start/analysis",
            JSONObject()
                .put("session_id", sessionId),
            onResult
        )
    }

    /////////////////////
    /// PAUSE SESSION ///
    /////////////////////
    /**
     * Pauses the current exercise session.
     *
     * This sends a request to the server to temporarily halt the processing
     * of an ongoing session. No new frames will be analyzed until the session is resumed.
     *
     * @param sessionId The unique identifier for the session to be paused.
     * @param onResult A callback function that is invoked with the result of the operation.
     *                 This will be a [SessionResult] object, indicating success or failure.
     */
    fun pauseSession(sessionId: String, onResult: (SessionResult) -> Unit) {
        sendPost(
            "pause/session",
            JSONObject().put("session_id", sessionId),
            onResult
        )
    }

    //////////////////////
    /// RESUME SESSION ///
    //////////////////////
    /**
     * Resumes a previously paused session on the server.
     *
     * This sends a request to the `resume/session` endpoint to continue a session that
     * was temporarily stopped.
     *
     * @param sessionId The unique identifier for the session to be resumed.
     * @param onResult A callback function that will be invoked with the result of the
     * operation. The result will be either a [SessionResult.Success] on successful
     * resumption or a [SessionResult.Error] or [SessionResult.NetworkFailure] if an issue occurs.
     */
    fun resumeSession(sessionId: String, onResult: (SessionResult) -> Unit) {
        sendPost(
            "resume/session",
            JSONObject().put("session_id", sessionId),
            onResult
        )
    }

    ///////////////////
    /// END SESSION ///
    ///////////////////
    /**
     * Ends a session on the server.
     *
     * This function sends a request to the `end/session` endpoint to terminate
     * the specified session. The server will stop processing frames for this session
     * and prepare a final summary. The result of this operation, which may include
     * a success confirmation or an error, is passed to the `onResult` callback.
     *
     * @param sessionId The unique identifier for the session to be ended.
     * @param onResult A callback function that will be invoked with the result of the
     *                 operation. This will typically be a [SessionResult.Success.Management] on
     *                 success, or a [SessionResult.Error] or [SessionResult.NetworkFailure] on failure.
     */
    fun endSession(sessionId: String, onResult: (SessionResult) -> Unit) {
        sendPost(
            "end/session",
            JSONObject().put("session_id", sessionId),
            onResult
        )
    }

    /////////////////////
    /// ANALYZE FRAME ///
    /////////////////////
    /**
     * Sends a single frame of data to the server for real-time analysis.
     *
     * This function should be called for each frame captured during an active exercise session.
     * The server will process the frame and may return immediate feedback or calibration instructions.
     *
     * @param sessionId The unique identifier for the current session.
     * @param frameId A sequential identifier for the frame within the session.
     * @param frameContent A string representation of the frame's data (e.g., base64 encoded keypoints).
     * @param onResult A callback function that will be invoked with the result of the analysis.
     *                 This can be a [SessionResult.Success.Feedback], [SessionResult.Success.Calibration],
     *                 or a [SessionResult.Error] if something goes wrong.
     */
    fun analyzeFrame(
        sessionId: String,
        frameId: Int,
        frameContent: String,
        onResult: (SessionResult) -> Unit
    ) {
        sendPost(
            "analyze",
            JSONObject()
                .put("session_id", sessionId)
                .put("frame_id", frameId)
                .put("frame_content", frameContent),
            onResult
        )
    }

    //////////////////////
    /// SESSION STATUS ///
    //////////////////////
    /**
     * Queries the current status of an active session.
     *
     * This function sends a request to the server to get the current status of the
     * session identified by the provided `sessionId`. The server's response, which
     * could be a success, an error, or a network failure, is passed to the `onResult`
     * callback.
     *
     * The expected successful response would be a `SessionResult.Success.Management`
     * object containing a `ManagementCode` that indicates the session's current state
     * (e.g., `RUNNING`, `PAUSED`, `ENDED`).
     *
     * @param sessionId The unique identifier for the session to query.
     * @param onResult A callback function that will be invoked with the result of the
     *                 API call (`SessionResult`). This callback is executed on the main thread.
     */
    fun sessionStatus(sessionId: String, onResult: (SessionResult) -> Unit) {
        sendPost(
            "session/status",
            JSONObject().put("session_id", sessionId),
            onResult
        )
    }

    ///////////////////////
    /// SESSION SUMMARY ///
    ///////////////////////
    /**
     * Requests a summary of a completed exercise session from the server.
     *
     * This function sends an asynchronous POST request to the `session/summary` endpoint.
     * The server processes the request and returns a detailed summary of the session,
     * which includes metrics like duration, number of reps, overall grade, and specific feedback.
     * The result, whether a success containing the summary or a failure, is delivered via the `onResult` callback.
     *
     * @param sessionId The unique identifier for the session for which the summary is requested.
     * @param onResult A callback function that will be invoked on the main thread with the result of the operation.
     *                 The result will be a `SessionResult.Success.Summary` on success, or a `SessionResult.Error` / `SessionResult.NetworkFailure` on failure.
     */
    fun sessionSummary(sessionId: String, onResult: (SessionResult) -> Unit) {
        sendPost(
            "session/summary",
            JSONObject().put("session_id", sessionId),
            onResult
        )
    }
}